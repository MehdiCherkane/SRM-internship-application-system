package com.onep.internship.config;

import com.onep.internship.model.Role;
import com.onep.internship.model.User;
import com.onep.internship.repository.ApplicationRepository;
import com.onep.internship.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public CustomAuthenticationSuccessHandler(UserRepository userRepository,
                                               ApplicationRepository applicationRepository) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + username));

        if (user.getRole() == Role.ADMIN) {
            response.sendRedirect("/admin/dashboard");
        } else {
            var apps = applicationRepository.findByApplicantIdOrderBySubmittedDateDesc(user.getId());
            if (apps.isEmpty()) {
                response.sendRedirect("/applicant/apply");
            } else {
                response.sendRedirect("/applicant/dashboard");
            }
        }
    }
}