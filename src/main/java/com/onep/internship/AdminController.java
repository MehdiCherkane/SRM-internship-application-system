package com.onep.internship;

import com.onep.internship.model.Application;
import com.onep.internship.model.ApplicationStatus;
import com.onep.internship.repository.ApplicationRepository;
import com.onep.internship.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;

    public AdminController(ApplicationRepository applicationRepository, EmailService emailService) {
        this.applicationRepository = applicationRepository;
        this.emailService = emailService;
    }

    @GetMapping("/login")
    public String showLogin() {
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String showDashboard(@RequestParam(required = false) String status, Model model) {
        List<Application> applications;
        if (status != null && !status.isEmpty()) {
            try {
                applications = applicationRepository.findByStatus(ApplicationStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                applications = applicationRepository.findAll();
            }
        } else {
            applications = applicationRepository.findAll();
        }
        model.addAttribute("applications", applications);
        model.addAttribute("currentFilter", status);
        return "admin/dashboard";
    }

    @GetMapping("/application/{id}")
    public String showApplication(@PathVariable Long id, Model model) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));
        model.addAttribute("app", app);
        return "admin/detail";
    }

    @PostMapping("/accept/{id}")
    public String accept(@PathVariable Long id, @RequestParam(defaultValue = "dashboard") String from) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found: " + id));
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
            Path filePath = Path.of(app.getCvFilePath());
            try {
                Files.deleteIfExists(filePath);
                log.info("Deleted CV file: {}", filePath);
            } catch (IOException e) {
                log.warn("Failed to delete CV file {}: {}", filePath, e.getMessage());
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

        if (app.getCvFilePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No CV uploaded for this application");
        }

        Path filePath = Path.of(app.getCvFilePath());
        if (!Files.exists(filePath)) {
            log.warn("CV file not found on disk: {}", filePath);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV file not found on server");
        }

        String filename = filePath.getFileName().toString();
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";

        MediaType mediaType = switch (ext) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "doc" -> MediaType.parseMediaType("application/msword");
            case "docx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };

        try {
            InputStream is = Files.newInputStream(filePath);
            long contentLength = Files.size(filePath);
            boolean isPdf = "pdf".equals(ext);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(contentLength)
                    .header(HttpHeaders.CONTENT_DISPOSITION, isPdf ? "inline" : "attachment; filename=\"" + filename + "\"")
                    .body(new InputStreamResource(is));
        } catch (IOException e) {
            log.error("Failed to read CV file {}: {}", filePath, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read CV file");
        }
    }
}
