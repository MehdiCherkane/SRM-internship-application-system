package com.onep.internship;

import com.onep.internship.model.Role;
import com.onep.internship.model.User;
import com.onep.internship.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/home")
public class AuthController {

    private static final String USERNAME_PATTERN = "[A-Za-z0-9._-]{3,50}";
    private static final String EMAIL_PATTERN = "^[^\\s@\\r\\n]+@[^\\s@\\r\\n]+\\.[^\\s@\\r\\n]+$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegister(Model model) {
        if (!model.containsAttribute("firstName")) {
            model.addAttribute("firstName", "");
            model.addAttribute("lastName", "");
            model.addAttribute("email", "");
            model.addAttribute("username", "");
        }
        return "register";
    }

    @GetMapping("/check-username")
    @ResponseBody
    public Map<String, Object> checkUsername(@RequestParam String username) {
        boolean available = !userRepository.existsByUsername(username.trim());
        return Map.of("available", available);
    }

    @GetMapping("/check-email")
    @ResponseBody
    public Map<String, Object> checkEmail(@RequestParam String email) {
        boolean available = !userRepository.existsByEmail(email.trim().toLowerCase());
        return Map.of("available", available);
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam String firstName,
                                 @RequestParam String lastName,
                                 @RequestParam String email,
                                 @RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 Model model) {

        firstName = firstName.trim();
        lastName = lastName.trim();
        email = email.trim().toLowerCase(Locale.ROOT);
        username = username.trim();
        String name = (firstName + " " + lastName).trim();

        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || username.isBlank() || password.isBlank()) {
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);
            model.addAttribute("username", username);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "Tous les champs sont obligatoires.");
            return "register";
        }

        if (firstName.length() > 100 || lastName.length() > 100 || email.length() > 150
                || !email.matches(EMAIL_PATTERN) || !username.matches(USERNAME_PATTERN)) {
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);
            model.addAttribute("username", username);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "Veuillez fournir un email valide et un identifiant de 3 à 50 caractères (lettres, chiffres, point, tiret ou underscore).");
            return "register";
        }

        if (password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72 ||
                !password.matches(".*[A-Z].*") ||
                !password.matches(".*[a-z].*") ||
                !password.matches(".*[0-9].*") ||
                !password.matches(".*[^a-zA-Z0-9].*")) {
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);
            model.addAttribute("username", username);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un symbole.");
            return "register";
        }

        if (!constantTimeEquals(password, confirmPassword)) {
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);
            model.addAttribute("username", username);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "Les mots de passe ne correspondent pas.");
            return "register";
        }

        if (userRepository.existsByEmail(email)) {
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);
            model.addAttribute("username", username);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "Un compte avec cet email existe déjà.");
            return "register";
        }

        if (userRepository.existsByUsername(username)) {
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);
            model.addAttribute("username", username);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "Ce nom d'utilisateur est déjà pris.");
            return "register";
        }

        User user = new User(name, email, username,
                passwordEncoder.encode(password), Role.APPLICANT);
        userRepository.save(user);

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_APPLICANT"));
        var auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return "redirect:/applicant/apply";
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
