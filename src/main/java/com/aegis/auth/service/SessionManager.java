package com.aegis.auth.service;

import com.aegis.auth.model.User;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * In-memory session store. Sessions are looked up by the {@code AEGIS_SESSION}
 * cookie value. This is a simplistic legacy implementation — sessions do not
 * expire and are lost on restart.
 */
@Component
public class SessionManager {

    public static final String COOKIE_NAME = "AEGIS_SESSION";

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
        return sessions.get(sessionId);
    }

    public void invalidate(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }
}
