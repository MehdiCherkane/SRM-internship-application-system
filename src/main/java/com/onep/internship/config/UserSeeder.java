package com.onep.internship.config;

import com.onep.internship.model.Role;
import com.onep.internship.model.User;
import com.onep.internship.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User(
                    "Admin SRM",
                    "admin@srm.ma",
                    "admin",
                    passwordEncoder.encode("admin123"),
                    Role.ADMIN
            );
            userRepository.save(admin);
            log.info("Default admin account created (username=admin). Change the password immediately after first login.");
        }
    }
}