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
    private static final int ADMIN_ACTION_LIMIT = 30;
    private static final String PATH_APPLY = "/apply";
    private static final String PATH_LOGIN = "/admin/login";

    private final ConcurrentHashMap<String, RateLimitEntry> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request);
        if (key == null) {
            chain.doFilter(request, response);
            return;
        }

        int limit = resolveLimit(request);
        long now = System.currentTimeMillis();

        RateLimitEntry entry = cache.computeIfAbsent(key, k -> new RateLimitEntry(now));

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

        if (PATH_APPLY.equals(path)) {
            return "apply:" + getClientIP(request);
        }

        if (PATH_LOGIN.equals(path)) {
            return "login:" + getClientIP(request);
        }

        if (path.startsWith("/admin/accept/") || path.startsWith("/admin/reject/")
                || path.startsWith("/admin/delete/")) {
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
        return ADMIN_ACTION_LIMIT;
    }

    private String getClientIP(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Scheduled(fixedRate = 300_000)
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
            return now - windowStart >= windowMs * 2;
        }
    }
}
