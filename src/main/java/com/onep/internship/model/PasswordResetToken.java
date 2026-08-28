package com.onep.internship.model;

import jakarta.persistence.*;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    private static final int EXPIRY_HOURS = 1;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    public PasswordResetToken() {}

    public PasswordResetToken(User user) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        this.token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        this.user = user;
        this.expiryDate = LocalDateTime.now().plusHours(EXPIRY_HOURS);
    }

    public boolean isValid() {
        return !used && expiryDate.isAfter(LocalDateTime.now());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
