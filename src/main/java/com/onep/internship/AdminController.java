package com.onep.internship;

import com.onep.internship.model.Application;
import com.onep.internship.model.ApplicationStatus;
import com.onep.internship.repository.ApplicationRepository;
import com.onep.internship.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;
    private final Path uploadDir;

    public AdminController(ApplicationRepository applicationRepository,
                           EmailService emailService,
                           @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.applicationRepository = applicationRepository;
        this.emailService = emailService;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @GetMapping("/stats")
    public String showStats(Model model) {
        List<Object[]> statusCounts = applicationRepository.countByStatus();
        long total = 0, submitted = 0, underReview = 0, accepted = 0, rejected = 0;
        for (Object[] row : statusCounts) {
            ApplicationStatus s = (ApplicationStatus) row[0];
            long c = (long) row[1];
            if (s == ApplicationStatus.DRAFT) continue;
            total += c;
            switch (s) {
                case SUBMITTED -> submitted = c;
                case UNDER_REVIEW -> underReview = c;
                case ACCEPTED -> accepted = c;
                case REJECTED -> rejected = c;
                case DRAFT -> { }
            }
        }
        model.addAttribute("totalApplications", total);
        model.addAttribute("submittedCount", submitted);
        model.addAttribute("underReviewCount", underReview);
        model.addAttribute("acceptedCount", accepted);
        model.addAttribute("rejectedCount", rejected);

        // Status donut
        model.addAttribute("statusLabels", new String[]{"Soumises", "En cours d'examen", "Acceptées", "Refusées"});
        model.addAttribute("statusData", new long[]{submitted, underReview, accepted, rejected});

        // Top 10 universities
        List<Object[]> topUnis = applicationRepository.findTopUniversities(ApplicationStatus.DRAFT, PageRequest.of(0, 10));
        List<String> uniLabels = new ArrayList<>();
        List<Long> uniData = new ArrayList<>();
        for (Object[] row : topUnis) {
            uniLabels.add((String) row[0]);
            uniData.add((long) row[1]);
        }
        model.addAttribute("uniLabels", uniLabels);
        model.addAttribute("uniData", uniData);

        // Top 10 majors
        List<Object[]> topMajors = applicationRepository.findTopMajors(ApplicationStatus.DRAFT, PageRequest.of(0, 10));
        List<String> majorLabels = new ArrayList<>();
        List<Long> majorData = new ArrayList<>();
        for (Object[] row : topMajors) {
            majorLabels.add((String) row[0]);
            majorData.add((long) row[1]);
        }
        model.addAttribute("majorLabels", majorLabels);
        model.addAttribute("majorData", majorData);

        // Monthly breakdown — last 12 months
        List<Object[]> monthly = applicationRepository.countByMonth(ApplicationStatus.DRAFT);
        Map<String, Long> monthMap = new HashMap<>();
        for (Object[] row : monthly) {
            String key = row[0] + "-" + row[1];
            monthMap.put(key, (long) row[2]);
        }
        List<String> monthLabels = new ArrayList<>();
        List<Long> monthData = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate d = now.minusMonths(i);
            String key = d.getYear() + "-" + d.getMonthValue();
            monthLabels.add(d.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + d.getYear());
            monthData.add(monthMap.getOrDefault(key, 0L));
        }
        model.addAttribute("monthLabels", monthLabels);
        model.addAttribute("monthData", monthData);

        return "admin/stats";
    }

    @GetMapping("/dashboard")
    public String showDashboard(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            Model model) {
        List<Application> applications;
        if (q != null && !q.isBlank()) {
            String safe = q.trim()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            if (safe.length() > 100) safe = safe.substring(0, 100);
            applications = applicationRepository.search(safe);
        } else if (status != null && !status.isEmpty()) {
            try {
                applications = applicationRepository.findByStatus(ApplicationStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                applications = applicationRepository.findAll();
            }
        } else {
            applications = applicationRepository.findAll();
        }
        applications.removeIf(application -> application.getStatus() == ApplicationStatus.DRAFT);
        model.addAttribute("applications", applications);
        model.addAttribute("currentFilter", status);
        model.addAttribute("searchQuery", q);
        return "admin/dashboard";
    }

    @GetMapping("/application/{id}")
    public String showApplication(@PathVariable Long id, Model model) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));
        if (app.getStatus() == ApplicationStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not submitted");
        }
        model.addAttribute("app", app);
        return "admin/detail";
    }

    @PostMapping("/review/{id}")
    public String startReview(@PathVariable Long id, @RequestParam(defaultValue = "dashboard") String from) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));
        if (app.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only submitted applications can enter review");
        }
        app.setStatus(ApplicationStatus.UNDER_REVIEW);
        applicationRepository.save(app);
        log.info("Application {} moved to review", id);
        return "detail".equals(from) ? "redirect:/admin/application/" + id : "redirect:/admin/dashboard";
    }

    @PostMapping("/accept/{id}")
    public String accept(@PathVariable Long id, @RequestParam(defaultValue = "dashboard") String from) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));
        requirePending(app);
        app.setStatus(ApplicationStatus.ACCEPTED);
        applicationRepository.save(app);
        emailService.sendAccepted(app);
        log.info("Application {} accepted", id);
        if ("detail".equals(from)) {
            return "redirect:/admin/application/" + id;
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/reject/{id}")
    public String reject(@PathVariable Long id, @RequestParam(defaultValue = "dashboard") String from) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));
        requirePending(app);
        app.setStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(app);
        emailService.sendRejected(app);
        log.info("Application {} rejected", id);
        if ("detail".equals(from)) {
            return "redirect:/admin/application/" + id;
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, @RequestParam(defaultValue = "dashboard") String from) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));

        if (app.getStatus() != ApplicationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only rejected applications can be deleted");
        }

        // Delete CV file from disk if it exists
        if (app.getCvFilePath() != null) {
            String storedPath = app.getCvFilePath();
            Path filePath = resolveCvPath(storedPath);
            if (filePath != null) {
                try {
                    Files.deleteIfExists(filePath);
                    log.info("Deleted CV file: {}", filePath.getFileName());
                } catch (IOException e) {
                    log.warn("Failed to delete CV file: {}", e.getClass().getSimpleName());
                }
            }
        }

        applicationRepository.delete(app);
        log.info("Application {} deleted", id);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/cv/{id}")
    public ResponseEntity<Resource> viewCv(@PathVariable Long id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));

        if (app.getStatus() == ApplicationStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not submitted");
        }

        if (app.getCvFilePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No CV uploaded for this application");
        }

        Path filePath = resolveCvPath(app.getCvFilePath());
        if (filePath == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid CV file path");
        }
        if (!Files.exists(filePath)) {
            log.warn("CV file not found on disk: {}", filePath.getFileName());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV file not found on server");
        }

        String filename = filePath.getFileName().toString();
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV file not found on server");
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(resource.contentLength())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("Failed to read CV file: {}", e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read CV file");
        }
    }

    private Path resolveCvPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return null;
        Path storedFile = Path.of(storedPath);
        if (storedFile.isAbsolute() || storedFile.getNameCount() != 1) return null;
        Path resolved = uploadDir.resolve(storedFile).normalize();
        if (!resolved.startsWith(uploadDir.normalize())) {
            log.warn("Path traversal blocked: {}", storedPath);
            return null;
        }
        return resolved;
    }

    private void requirePending(Application app) {
        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only applications under review can be accepted or rejected");
        }
    }
}