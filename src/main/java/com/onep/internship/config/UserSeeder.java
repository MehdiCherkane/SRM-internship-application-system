package com.onep.internship.config;

import com.onep.internship.model.Role;
import com.onep.internship.model.User;
import com.onep.internship.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminUsername;
    private final String adminPassword;

    public UserSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${APP_ADMIN_NAME:Admin SRM}") String adminName,
                      @Value("${APP_ADMIN_EMAIL:admin@srm.ma}") String adminEmail,
                      @Value("${APP_ADMIN_USERNAME:admin}") String adminUsername,
                      @Value("${APP_ADMIN_PASSWORD:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("No admin account created: set APP_ADMIN_PASSWORD env var to seed the initial admin.");
                return;
            }
            User admin = new User(
                    adminName,
                    adminEmail,
                    adminUsername,
                    passwordEncoder.encode(adminPassword),
                    Role.ADMIN
            );
            userRepository.save(admin);
            log.info("Default admin account created (username={}). Change the password immediately after first login.", adminUsername);
        }
    }
}