package com.aegis.auth.service;

import com.aegis.auth.model.User;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * In-memory session store. Sessions are looked up by the {@code AEGIS_SESSION}
 * cookie value. Sessions expire after {@link #SESSION_TTL} and are evicted lazily
 * on lookup.
 */
@Component
public class SessionManager {

    public static final String COOKIE_NAME = "AEGIS_SESSION";

    static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession create(User user) {
        // Session id derived from a random UUID.
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        UserSession session = new UserSession(sessionId, user.getId(), user.getUsername(), user.getRole());
        sessions.put(sessionId, session);
        return session;
    }

    public UserSession get(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        UserSession session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        if (isExpired(session)) {
            sessions.remove(sessionId);
            return null;
        }
        return session;
    }

    public void invalidate(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    private boolean isExpired(UserSession session) {
        return Instant.now().isAfter(session.getCreatedAt().plus(SESSION_TTL));
    }
}
