package com.onep.internship.config;

import com.onep.internship.model.Role;
import com.onep.internship.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {

    private final RateLimitingFilter rateLimitingFilter;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final boolean requireHttps;

    public SecurityConfig(RateLimitingFilter rateLimitingFilter,
                          CustomAuthenticationSuccessHandler successHandler,
                          @Value("${app.security.require-https:false}") boolean requireHttps,
                          @Value("${server.servlet.session.cookie.secure:false}") boolean sessionCookieSecure) {
        this.rateLimitingFilter = rateLimitingFilter;
        this.successHandler = successHandler;
        this.requireHttps = requireHttps;

        // Defense-in-depth: refuse to boot if HTTPS is required but the session
        // cookie is not flagged Secure. Otherwise the app would happily run with
        // `require-https=true` while the JSESSIONID still travels in cleartext
        // because TLS is terminated at the proxy and the cookie flag is the
        // app's responsibility. Fail closed on misconfiguration rather than
        // silently shipping an insecure session cookie over HTTP.
        if (requireHttps && !sessionCookieSecure) {
            throw new IllegalStateException(
                    "Configuration conflict: app.security.require-https=true requires "
                    + "SESSION_COOKIE_SECURE=true (server.servlet.session.cookie.secure). "
                    + "Set SESSION_COOKIE_SECURE=true in the environment and restart.");
        }
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return login -> userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmail(login.toLowerCase()))
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants incorrects."));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/apply", "/success", "/css/**", "/js/**", "/logo/**", "/hero-home/**").permitAll()
                .requestMatchers("/home/login", "/home/register", "/home/forgot-password", "/home/reset-password", "/home/check-username", "/home/check-email").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/applicant/**").hasAnyRole("ADMIN", "APPLICANT")
                .anyRequest().denyAll()
            )
            .formLogin(form -> form
                .loginPage("/home/login")
                .loginProcessingUrl("/home/login")
                .successHandler(successHandler)
                .failureUrl("/home/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/home/login?logout")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.newSession())
                .invalidSessionUrl("/home/login")
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );

        // HTTPS redirection is handled at the reverse proxy level (nginx, haproxy, etc.).
        // To enable, set APP_SECURITY_REQUIRE_HTTPS=true and terminate TLS at the proxy.

        return http.build();
    }
}
