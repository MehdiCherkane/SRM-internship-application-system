package com.onep.internship;

import com.onep.internship.model.PasswordResetToken;
import com.onep.internship.model.User;
import com.onep.internship.repository.PasswordResetTokenRepository;
import com.onep.internship.repository.UserRepository;
import com.onep.internship.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequestMapping("/home")
public class PasswordController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final String publicBaseUrl;

    public PasswordController(UserRepository userRepository,
                              PasswordResetTokenRepository tokenRepository,
                              EmailService emailService,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.public-base-url}") String publicBaseUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.publicBaseUrl = publicBaseUrl;
    }

    @GetMapping("/forgot-password")
    public String showForgotForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    @Transactional
    public String handleForgot(@RequestParam String email,
                               Model model) {
        if (email == null || email.isBlank()) {
            model.addAttribute("error", "Veuillez entrer votre adresse email.");
            return "forgot-password";
        }

        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            tokenRepository.deleteByUser(user);

            PasswordResetToken resetToken = new PasswordResetToken(user);
            tokenRepository.save(resetToken);

            String resetLink = UriComponentsBuilder.fromUriString(publicBaseUrl)
                    .path("/home/reset-password")
                    .queryParam("token", resetToken.getToken())
                    .build()
                    .encode()
                    .toUriString();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.sendPasswordReset(user.getEmail(), user.getName(), resetLink);
                }
            });
        }

        // The same response prevents attackers from discovering registered accounts.
        model.addAttribute("sent", true);
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetForm(@RequestParam(required = false) String token,
                                 Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("invalid", true);
            return "reset-password";
        }
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || !tokenOpt.get().isValid()) {
            model.addAttribute("invalid", true);
            return "reset-password";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    @Transactional
    public String handleReset(@RequestParam String token,
                              @RequestParam String password,
                              @RequestParam String confirmPassword,
                              Model model) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || !tokenOpt.get().isValid()) {
            model.addAttribute("invalid", true);
            return "reset-password";
        }

        if (password == null || password.isBlank() || password.length() < 8 ||
                password.getBytes(StandardCharsets.UTF_8).length > 72 ||
                !password.matches(".*[A-Z].*") ||
                !password.matches(".*[a-z].*") ||
                !password.matches(".*[0-9].*") ||
                !password.matches(".*[^a-zA-Z0-9].*")) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un symbole (max 72 octets).");
            return "reset-password";
        }

        if (!constantTimeEquals(password, confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Les mots de passe ne correspondent pas.");
            return "reset-password";
        }

        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return "redirect:/home/login?reset";
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
