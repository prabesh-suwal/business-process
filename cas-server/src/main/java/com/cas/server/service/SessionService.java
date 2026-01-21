package com.cas.server.service;

import com.cas.server.config.CasProperties;
import com.cas.server.domain.session.CasSession;
import com.cas.server.domain.user.User;
import com.cas.server.repository.CasSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Service for managing CAS SSO sessions.
 * Sessions enable Single Sign-On: login once at CAS, auto-approve for other
 * products.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final CasSessionRepository sessionRepository;
    private final CasProperties casProperties;

    private static final String SESSION_COOKIE_NAME = "CAS_SESSION";

    /**
     * Create a new SSO session for a user and set the session cookie.
     */
    public CasSession createSession(User user, String ipAddress, String userAgent,
            HttpServletResponse response) {
        // Calculate TTL in seconds (default 8 hours)
        long ttlSeconds = casProperties.getSession().getTimeout().toSeconds();

        CasSession session = CasSession.create(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                ipAddress,
                userAgent,
                ttlSeconds);

        sessionRepository.save(session);
        setSessionCookie(session.getId(), ttlSeconds, response);

        log.info("Created SSO session {} for user {}", session.getId(), user.getUsername());
        return session;
    }

    /**
     * Validate an existing session from cookie.
     * Returns the session if valid, empty if not.
     */
    public Optional<CasSession> validateSession(HttpServletRequest request) {
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId == null) {
            return Optional.empty();
        }

        Optional<CasSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            log.debug("Session {} not found in Redis", sessionId);
            return Optional.empty();
        }

        CasSession session = sessionOpt.get();

        // Update last activity
        session.touch();
        sessionRepository.save(session);

        log.debug("Validated SSO session {} for user {}", session.getId(), session.getUsername());
        return Optional.of(session);
    }

    /**
     * Destroy a session (global logout).
     */
    public void destroySession(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId != null) {
            sessionRepository.deleteById(sessionId);
            log.info("Destroyed SSO session {}", sessionId);
        }
        clearSessionCookie(response);
    }

    /**
     * Destroy all sessions for a user (force logout everywhere).
     */
    public void destroyAllUserSessions(User user) {
        var sessions = sessionRepository.findByUserId(user.getId());
        sessions.forEach(session -> {
            sessionRepository.deleteById(session.getId());
            log.info("Destroyed session {} for user {}", session.getId(), user.getUsername());
        });
    }

    /**
     * Get session ID from cookie.
     */
    private String getSessionIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> SESSION_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Set session cookie in response.
     * Uses Set-Cookie header for better control over SameSite attribute.
     */
    private void setSessionCookie(String sessionId, long ttlSeconds, HttpServletResponse response) {
        String domain = casProperties.getSession().getCookieDomain();
        boolean secure = casProperties.getSession().isCookieSecure();
        boolean httpOnly = casProperties.getSession().isCookieHttpOnly();

        // Build cookie with appropriate SameSite attribute
        // SameSite=None requires Secure=true in all browsers
        // For localhost dev (Secure=false), we use SameSite=Lax
        StringBuilder cookieBuilder = new StringBuilder();
        cookieBuilder.append(SESSION_COOKIE_NAME).append("=").append(sessionId);
        cookieBuilder.append("; Path=/");
        cookieBuilder.append("; Max-Age=").append(ttlSeconds);

        if (secure) {
            // Production: SameSite=None with Secure allows cross-origin
            cookieBuilder.append("; SameSite=None");
            cookieBuilder.append("; Secure");
        } else {
            // Development: SameSite=Lax - cookie sent for same-origin and top-level
            // navigation
            cookieBuilder.append("; SameSite=Lax");
        }

        if (httpOnly) {
            cookieBuilder.append("; HttpOnly");
        }
        if (domain != null && !domain.isBlank()) {
            cookieBuilder.append("; Domain=").append(domain);
        }

        response.addHeader("Set-Cookie", cookieBuilder.toString());
        log.debug("Set SSO cookie: {}", cookieBuilder.toString());
    }

    /**
     * Clear session cookie.
     */
    private void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
