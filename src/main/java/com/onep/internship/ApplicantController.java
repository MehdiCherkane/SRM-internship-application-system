package com.onep.internship;

import com.onep.internship.dto.ApplicationForm;
import com.onep.internship.dto.SubmissionValidation;
import com.onep.internship.model.Application;
import com.onep.internship.model.ApplicationStatus;
import com.onep.internship.model.User;
import com.onep.internship.repository.ApplicationRepository;
import com.onep.internship.repository.UserRepository;
import com.onep.internship.service.EmailService;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequestMapping("/applicant")
public class ApplicantController {

    private static final Logger log = LoggerFactory.getLogger(ApplicantController.class);

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final Path uploadDir;
    private final Validator validator;

    public ApplicantController(ApplicationRepository applicationRepository,
                               UserRepository userRepository,
                               EmailService emailService,
                               Validator validator,
                               @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.validator = validator;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private User getCurrentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User user = getCurrentUser(principal);
        List<Application> apps = applicationRepository.findByApplicantIdOrderBySubmittedDateDesc(user.getId());
        List<ApplicationStatus> activeInCycle = List.of(ApplicationStatus.values());
        boolean cycleBlocked = applicationRepository.existsByApplicantInCurrentCycle(user.getId(), activeInCycle);
        model.addAttribute("applications", apps);
        model.addAttribute("user", user);
        model.addAttribute("cycleBlocked", cycleBlocked);
        return "applicant/dashboard";
    }

    @GetMapping("/apply")
    public String showForm(@RequestParam(required = false) Long id,
                           Principal principal, Model model) {
        User user = getCurrentUser(principal);
        ApplicationForm form = new ApplicationForm();

        Application existingApp = null;
        if (id != null) {
            existingApp = applicationRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (existingApp.getApplicant() == null || !existingApp.getApplicant().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            if (!existingApp.getStatus().isApplicantEditable()) {
                return "redirect:/applicant/application/" + id;
            }
            form.setId(existingApp.getId());
            form.setFirstName(existingApp.getFirstName());
            form.setLastName(existingApp.getLastName());
            form.setEmail(existingApp.getEmail());
            form.setCni(existingApp.getCni());
            form.setPhone(existingApp.getPhone());
            form.setUniversity(existingApp.getUniversity());
            form.setMajor(existingApp.getMajor());
            form.setCoverMessage(existingApp.getCoverMessage());
            form.setExistingCv(existingApp.getCvFilePath() != null);
        } else {
            List<ApplicationStatus> activeInCycle = List.of(ApplicationStatus.values());
            if (applicationRepository.existsByApplicantInCurrentCycle(user.getId(), activeInCycle)) {
                return "applicant/cycle-blocked";
            }
            String[] nameParts = user.getName().split(" ", 2);
            form.setFirstName(nameParts[0]);
            if (nameParts.length > 1) form.setLastName(nameParts[1]);
            form.setEmail(user.getEmail());
        }

        model.addAttribute("user", user);
        model.addAttribute("form", form);
        model.addAttribute("editingApp", existingApp);
        if (existingApp != null) {
            model.addAttribute("editing", true);
        }
        return "applicant/apply";
    }

    @PostMapping("/apply")
    @Transactional
    public String handleForm(@ModelAttribute("form") ApplicationForm form,
                             BindingResult result,
                             Principal principal,
                             Model model,
                             @RequestParam(defaultValue = "submit") String action) {

        User user = getCurrentUser(principal);
        boolean saveDraft = "draft".equals(action);
        if (!saveDraft && !"submit".equals(action)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Long id = form.getId();
        Application existingApp = id != null
                ? applicationRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
                : null;

        if (existingApp != null) {
            if (existingApp.getApplicant() == null || !existingApp.getApplicant().getId().equals(user.getId())
                    || !existingApp.getStatus().isApplicantEditable()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            if (saveDraft && existingApp.getStatus() != ApplicationStatus.DRAFT) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            if (existingApp.getStatus() != ApplicationStatus.DRAFT && !identityMatches(existingApp, form)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identity fields are locked after submission");
            }
        }

        // Re-derive existingCv from the loaded entity instead of trusting the form-submitted value.
        if (existingApp != null) form.setExistingCv(existingApp.getCvFilePath() != null);

        List<ApplicationStatus> activeInCycle = List.of(ApplicationStatus.values());
        boolean duplicate = existingApp == null
                ? applicationRepository.existsByApplicantInCurrentCycle(user.getId(), activeInCycle)
                : applicationRepository.existsByApplicantInCurrentCycleExcludingId(user.getId(), activeInCycle, existingApp.getId());
        if (!duplicate) {
            String normalizedCni = normalizeCni(form.getCni());
            String normalizedEmail = normalize(form.getEmail());
            if (normalizedCni != null && normalizedEmail != null) {
                duplicate = existingApp == null
                        ? applicationRepository.existsByCniAndEmailInCurrentCycle(normalizedCni, normalizedEmail, activeInCycle)
                        : applicationRepository.existsByCniAndEmailInCurrentCycleExcludingId(normalizedCni, normalizedEmail, activeInCycle, existingApp.getId());
            }
        }
        if (duplicate) {
            result.rejectValue("cni", "error.cni", "Une candidature avec ces informations est déjà en cours.");
        }

        if (!saveDraft) {
            validator.validate(form, SubmissionValidation.class).forEach(violation ->
                    result.rejectValue(violation.getPropertyPath().toString(),
                            violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                            violation.getMessage()));
        }

        MultipartFile cvFile = form.getCvFile();
        boolean newCvProvided = cvFile != null && !cvFile.isEmpty();

        if (!newCvProvided && !saveDraft && (existingApp == null || existingApp.getCvFilePath() == null)) {
            result.rejectValue("cvFile", "error.cvFile", "Veuillez sélectionner un CV.");
        } else if (newCvProvided) {
            String ext = getExt(cvFile.getOriginalFilename());
            if (!"pdf".equals(ext)) {
                result.rejectValue("cvFile", "error.cvFile", "Format non accepté (PDF uniquement).");
            } else if (cvFile.getSize() > 10 * 1024 * 1024) {
                result.rejectValue("cvFile", "error.cvFile", "Le fichier dépasse 10 Mo.");
            } else {
                try {
                    if (!isValidContent(cvFile)) {
                        result.rejectValue("cvFile", "error.cvFile", "Contenu invalide.");
                    }
                } catch (IOException e) {
                    result.rejectValue("cvFile", "error.cvFile", "Erreur de lecture.");
                }
            }
        }

        if (result.hasErrors()) {
            return formWithErrors(user, form, existingApp, model);
        }

        String cvFileName = null;
        if (newCvProvided) {
            String ext = getExt(cvFile.getOriginalFilename());
            String safeName = UUID.randomUUID() + "." + ext;
            Path filePath = uploadDir.resolve(safeName);
            Path temporaryFile = uploadDir.resolve(safeName + ".uploading");
            try {
                Files.createDirectories(uploadDir);
                try (InputStream input = cvFile.getInputStream()) {
                    Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
                }
                try {
                    Files.move(temporaryFile, filePath,
                            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporaryFile, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                log.info("CV saved: {}", filePath.getFileName());
                cvFileName = safeName;
            } catch (IOException e) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupError) {
                    log.warn("Failed to clean up incomplete CV upload: {}", cleanupError.getClass().getSimpleName());
                }
                result.rejectValue("cvFile", "error.cvFile", "Erreur d'enregistrement.");
                return formWithErrors(user, form, existingApp, model);
            }
        }

        Application app = existingApp != null ? existingApp : new Application();
        boolean wasDraft = existingApp == null || app.getStatus() == ApplicationStatus.DRAFT;
        String oldCvFilePath = existingApp != null ? existingApp.getCvFilePath() : null;

        if (wasDraft) {
            app.setFirstName(normalize(form.getFirstName()));
            app.setLastName(normalize(form.getLastName()));
            app.setEmail(normalize(existingApp != null ? existingApp.getEmail() : user.getEmail()));
            app.setCni(existingApp != null ? existingApp.getCni() : normalizeCni(form.getCni()));
        }
        app.setPhone(normalize(form.getPhone()));
        app.setUniversity("__other__".equals(form.getUniversity()) ? normalize(form.getUniversityOther()) : normalize(form.getUniversity()));
        app.setMajor(normalize(form.getMajor()));
        app.setCoverMessage(normalize(form.getCoverMessage()));
        if (newCvProvided) app.setCvFilePath(cvFileName);

        if (existingApp == null) {
            app.setApplicant(user);
        }

        int currentYear = java.time.LocalDate.now().getYear();
        if (saveDraft) {
            app.setStatus(ApplicationStatus.DRAFT);
            app.setCycleYear(currentYear);
        } else if (wasDraft) {
            app.setStatus(ApplicationStatus.SUBMITTED);
            app.setSubmittedDate(java.time.LocalDateTime.now());
            app.setCycleYear(currentYear);
        }

        try {
            applicationRepository.save(app);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate application prevented for user={}, existingAppId={}",
                     user.getId(), existingApp == null ? null : existingApp.getId());
            result.rejectValue("cni", "error.cni", "Une candidature avec ces informations est déjà en cours.");
            return formWithErrors(user, form, existingApp, model);
        }
        if (existingApp != null && newCvProvided && oldCvFilePath != null) {
            Path oldFile = resolve(oldCvFilePath);
            if (oldFile != null) {
                try { Files.deleteIfExists(oldFile); log.info("Old CV deleted: {}", oldFile.getFileName()); }
                catch (IOException e) { log.warn("Failed to delete old CV: {}", e.getClass().getSimpleName()); }
            }
        }
        if (!saveDraft && wasDraft) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.sendReceived(app);
                }
            });
            return "redirect:/success";
        }

        return "redirect:/applicant/dashboard";
    }

    private String formWithErrors(User user, ApplicationForm form, Application existingApp, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("editingApp", existingApp);
        if (existingApp != null) {
            form.setExistingCv(existingApp.getCvFilePath() != null);
            model.addAttribute("editing", true);
        }
        return "applicant/apply";
    }

    private boolean identityMatches(Application app, ApplicationForm form) {
        return java.util.Objects.equals(app.getFirstName(), normalize(form.getFirstName()))
                && java.util.Objects.equals(app.getLastName(), normalize(form.getLastName()))
                && java.util.Objects.equals(app.getEmail(), normalize(form.getEmail()))
                && java.util.Objects.equals(app.getCni(), normalizeCni(form.getCni()));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeCni(String value) {
        String normalized = normalize(value);
        return (normalized == null || normalized.isEmpty()) ? null : normalized.toUpperCase(Locale.ROOT);
    }

    @GetMapping("/application/{id}")
    public String viewApplication(@PathVariable Long id, Principal principal, Model model) {
        User user = getCurrentUser(principal);
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (app.getApplicant() == null || !app.getApplicant().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        model.addAttribute("app", app);
        return "applicant/application";
    }

    @GetMapping("/cv/{id}")
    public ResponseEntity<Resource> viewCv(@PathVariable Long id, Principal principal) {
        User user = getCurrentUser(principal);
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (app.getApplicant() == null || !app.getApplicant().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (app.getCvFilePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Path file = resolve(app.getCvFilePath());
        if (file == null || !Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String fn = file.getFileName().toString();

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(resource.contentLength())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fn + "\"")
                    .body(resource);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Path resolve(String stored) {
        if (stored == null || stored.isBlank()) return null;
        Path storedPath = Path.of(stored);
        if (storedPath.isAbsolute() || storedPath.getNameCount() != 1) return null;
        Path r = uploadDir.resolve(storedPath).normalize();
        return r.startsWith(uploadDir.normalize()) ? r : null;
    }

    private static String getExt(String fn) {
        if (fn == null || !fn.contains(".") || fn.lastIndexOf('.') == fn.length() - 1) return "";
        return fn.substring(fn.lastIndexOf('.') + 1).toLowerCase();
    }

    private static boolean isValidContent(MultipartFile file) throws IOException {
        byte[] bytes;
        try (InputStream input = file.getInputStream()) {
            bytes = input.readNBytes(4);
        }
        return bytes.length >= 4
                && bytes[0] == 0x25 && bytes[1] == 0x50
                && bytes[2] == 0x44 && bytes[3] == 0x46;
    }

}
