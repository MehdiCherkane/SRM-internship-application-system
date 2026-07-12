package com.onep.internship.config;

import com.onep.internship.model.Admin;
import com.onep.internship.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminRepository.count() == 0) {
            Admin admin = new Admin(
                "Admin ONEP",
                "admin@onee.ma",
                "admin",
                passwordEncoder.encode("admin123")
            );
            adminRepository.save(admin);
            System.out.println("Default admin created: username=admin, password=admin123");
        }
    }
}
