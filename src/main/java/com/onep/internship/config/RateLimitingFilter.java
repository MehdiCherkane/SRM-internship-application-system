package com.onep.internship.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final long WINDOW_MS = 60_000;
    private static final int APPLY_LIMIT = 5;
    private static final int LOGIN_LIMIT = 5;
    private static final int REGISTER_LIMIT = 3;
    private static final int PASSWORD_RESET_LIMIT = 3;
    private static final int AVAILABILITY_CHECK_LIMIT = 20;
    private static final int ADMIN_ACTION_LIMIT = 30;
    private static final int MAX_CACHE_ENTRIES = 10_000;
    private static final String PATH_APPLY = "/applicant/apply";
    private static final String PATH_LOGIN = "/home/login";
    private static final String PATH_REGISTER = "/home/register";
    private static final String PATH_FORGOT_PASSWORD = "/home/forgot-password";
    private static final String PATH_CHECK_USERNAME = "/home/check-username";
    private static final String PATH_CHECK_EMAIL = "/home/check-email";

    private final ConcurrentHashMap<String, RateLimitEntry> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = resolveKey(request);
        if (key == null) {
            chain.doFilter(request, response);
            return;
        }

        int limit = resolveLimit(request);
        long now = System.currentTimeMillis();

        RateLimitEntry entry = cache.get(key);
        if (entry == null) {
            RateLimitEntry fresh = new RateLimitEntry(now);
            RateLimitEntry existing = cache.putIfAbsent(key, fresh);
            entry = existing != null ? existing : fresh;
            if (existing == null && cache.size() > MAX_CACHE_ENTRIES) {
                cache.remove(key, fresh);
                response.setStatus(429);
                response.setHeader("Retry-After", "60");
                response.getWriter().write("Trop de requêtes. Veuillez réessayer dans une minute.");
                return;
            }
        }

        if (!entry.tryAcquire(now, WINDOW_MS, limit)) {
            log.warn("Rate limit exceeded for key={}, path={}", key, request.getRequestURI());
            response.setStatus(429);
            response.setContentType("text/plain;charset=UTF-8");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("Trop de requêtes. Veuillez réessayer dans une minute.");
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method) && PATH_APPLY.equals(path)) {
            return "apply:" + getClientIP(request);
        }

        if ("POST".equalsIgnoreCase(method) && PATH_LOGIN.equals(path)) {
            return "login:" + getClientIP(request);
        }

        if ("POST".equalsIgnoreCase(method) && PATH_REGISTER.equals(path)) {
            return "register:" + getClientIP(request);
        }

        if ("POST".equalsIgnoreCase(method) && PATH_FORGOT_PASSWORD.equals(path)) {
            return "forgot-password:" + getClientIP(request);
        }

        if ("GET".equalsIgnoreCase(method)
                && (PATH_CHECK_USERNAME.equals(path) || PATH_CHECK_EMAIL.equals(path))) {
            return "availability:" + getClientIP(request);
        }

        if ("POST".equalsIgnoreCase(method)
                && (path.startsWith("/admin/accept/") || path.startsWith("/admin/reject/")
                || path.startsWith("/admin/delete/"))) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                return "admin:" + auth.getName();
            }
            return "admin-anon:" + getClientIP(request);
        }

        return null;
    }

    private int resolveLimit(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (PATH_APPLY.equals(path)) return APPLY_LIMIT;
        if (PATH_LOGIN.equals(path)) return LOGIN_LIMIT;
        if (PATH_REGISTER.equals(path)) return REGISTER_LIMIT;
        if (PATH_FORGOT_PASSWORD.equals(path)) return PASSWORD_RESET_LIMIT;
        if (PATH_CHECK_USERNAME.equals(path) || PATH_CHECK_EMAIL.equals(path)) return AVAILABILITY_CHECK_LIMIT;
        return ADMIN_ACTION_LIMIT;
    }

    // NOTE: We deliberately ignore X-Forwarded-For and X-Real-IP headers here.
    // When the app is directly reachable (no trusted reverse proxy that
    // overwrites those headers), they are fully client-controlled and would
    // let an attacker get a fresh rate-limit bucket on every request by simply
    // varying the header value, defeating every limit in this filter.
    // The only honest source of the client address on a direct deployment is
    // the TCP connection's remote address. When deployed behind a trusted
    // reverse proxy that is configured to OVERRIDE these headers (rather than
    // append to them), rely on the proxy rewriting them and on Spring's
    // ForwardedHeaderFilter (server.forward-headers-strategy=framework, plus a
    // proxy allow-list) to surface the real client IP via getRemoteAddr().
    private String getClientIP(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        int before = cache.size();
        Iterator<Map.Entry<String, RateLimitEntry>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, RateLimitEntry> entry = it.next();
            if (entry.getValue().isExpired(now, WINDOW_MS)) {
                it.remove();
            }
        }
        if (before > 0) {
            log.debug("Rate limit cache cleanup: {} → {} entries", before, cache.size());
        }
    }

    private static class RateLimitEntry {
        private int count;
        private long windowStart;

        RateLimitEntry(long now) {
            this.windowStart = now;
        }

        synchronized boolean tryAcquire(long now, long windowMs, int max) {
            if (now - windowStart >= windowMs) {
                windowStart = now;
                count = 0;
            }
            if (count >= max) return false;
            count++;
            return true;
        }

        synchronized boolean isExpired(long now, long windowMs) {
            return now - windowStart >= windowMs;
        }
    }
}
