package com.onep.internship;

import com.onep.internship.dto.ApplicationForm;
import com.onep.internship.model.Application;
import com.onep.internship.repository.ApplicationRepository;
import com.onep.internship.service.EmailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
public class ApplicationController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationController.class);

    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;
    private final Path uploadDir;

    public ApplicationController(ApplicationRepository applicationRepository,
                                 EmailService emailService,
                                 @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.applicationRepository = applicationRepository;
        this.emailService = emailService;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
            log.info("Upload directory: {}", this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + this.uploadDir, e);
        }
    }

    @GetMapping("/apply")
    public String showApplyForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ApplicationForm());
        }
        return "apply";
    }

    @PostMapping("/apply")
    public String handleApply(
            @Valid @ModelAttribute("form") ApplicationForm form,
            BindingResult result,
            @RequestParam(value = "website", required = false) String website,
            Model model) {

        // Honeypot: if filled, silently discard (likely a bot)
        if (website != null && !website.isBlank()) {
            return "redirect:/success";
        }

        // CV validation (not handled by DTO annotations)
        MultipartFile cvFile = form.getCvFile();
        if (cvFile == null || cvFile.isEmpty()) {
            result.rejectValue("cvFile", "error.cvFile", "Veuillez sélectionner un fichier CV.");
        } else {
            String ext = getExtension(cvFile.getOriginalFilename());
            if (!"pdf".equals(ext) && !"doc".equals(ext) && !"docx".equals(ext)) {
                result.rejectValue("cvFile", "error.cvFile", "Format non accepté. Utilisez PDF, DOC ou DOCX.");
            } else if (cvFile.getSize() > 10 * 1024 * 1024) {
                result.rejectValue("cvFile", "error.cvFile", "Le fichier dépasse la limite de 10 Mo.");
            } else {
                try {
                    byte[] fileBytes = cvFile.getBytes();
                    if (!isValidContent(fileBytes, ext)) {
                        result.rejectValue("cvFile", "error.cvFile", "Le contenu du fichier ne correspond pas au format déclaré.");
                    }
                } catch (IOException e) {
                    log.error("Failed to read CV file bytes: {}", e.getMessage(), e);
                    result.rejectValue("cvFile", "error.cvFile", "Erreur lors de la lecture du CV. Veuillez réessayer.");
                }
            }
        }

        if (result.hasErrors()) {
            return "apply";
        }

        // Save CV to disk
        String ext = getExtension(cvFile.getOriginalFilename());
        String safeName = UUID.randomUUID() + "." + ext;
        Path filePath = uploadDir.resolve(safeName);

        try {
            byte[] fileBytes = cvFile.getBytes();
            Files.write(filePath, fileBytes);
            log.info("CV saved: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save CV file {}: {}", filePath, e.getMessage(), e);
            result.rejectValue("cvFile", "error.cvFile", "Erreur lors de l'enregistrement du CV. Veuillez réessayer.");
            return "apply";
        }

        Application app = new Application(
                form.getFirstName().trim(),
                form.getLastName().trim(),
                form.getEmail().trim(),
                form.getPhone().trim(),
                form.getResolvedUniversity().trim(),
                form.getMajor().trim(),
                form.getCoverMessage() != null ? form.getCoverMessage().trim() : null
        );
        app.setCvFilePath(filePath.toString());
        applicationRepository.save(app);
        emailService.sendReceived(app);

        return "redirect:/success";
    }

    @GetMapping("/success")
    public String showSuccess() {
        return "success";
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".") || filename.lastIndexOf('.') == filename.length() - 1)
            return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private static boolean isValidContent(byte[] bytes, String ext) {
        if (bytes.length < 4) return false;

        boolean isPdf = bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46;
        boolean isOle2 = bytes.length >= 8
            && bytes[0] == (byte)0xD0 && bytes[1] == (byte)0xCF
            && bytes[2] == 0x11 && bytes[3] == (byte)0xE0;
        boolean isZip = bytes[0] == 0x50 && bytes[1] == 0x4B
            && bytes[2] == 0x03 && bytes[3] == 0x04;

        if (isPdf) return true;
        if ("pdf".equals(ext)) return false;
        return ("doc".equals(ext) && isOle2) || ("docx".equals(ext) && isZip);
    }
}
