package com.aegis.auth.service;

import com.aegis.auth.model.User;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * In-memory session store. Sessions are looked up by the {@code AEGIS_SESSION}
 * cookie value.
 *
 * <p>Sessions now expire: they are dropped after {@link #idleTimeoutMillis} of
 * inactivity or once they exceed {@link #absoluteTimeoutMillis} since creation,
 * whichever comes first. Each successful login mints a brand-new random session
 * id ({@link #create(User)}), so an attacker-supplied cookie value cannot be
 * promoted to an authenticated session (session-fixation protection). Sessions
 * are still held only in memory and are lost on restart.
 */
@Component
public class SessionManager {

    public static final String COOKIE_NAME = "AEGIS_SESSION";

    /** Default idle timeout: 30 minutes of inactivity. */
    public static final long DEFAULT_IDLE_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    /** Default absolute lifetime: 8 hours regardless of activity. */
    public static final long DEFAULT_ABSOLUTE_TIMEOUT_MILLIS = 8L * 60L * 60L * 1000L;

    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final long idleTimeoutMillis;
    private final long absoluteTimeoutMillis;

    public SessionManager() {
        this(DEFAULT_IDLE_TIMEOUT_MILLIS, DEFAULT_ABSOLUTE_TIMEOUT_MILLIS);
    }

    public SessionManager(long idleTimeoutMillis, long absoluteTimeoutMillis) {
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.absoluteTimeoutMillis = absoluteTimeoutMillis;
    }

    public UserSession create(User user) {
        // A fresh, unguessable id per login prevents session fixation.
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
        long now = System.currentTimeMillis();
        if (isExpired(session, now)) {
            sessions.remove(sessionId);
            return null;
        }
        session.touch(now);
        return session;
    }

    public void invalidate(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    private boolean isExpired(UserSession session, long now) {
        boolean idleExpired = now - session.getLastAccessedMillis() > idleTimeoutMillis;
        boolean absoluteExpired = now - session.getCreatedAtMillis() > absoluteTimeoutMillis;
        return idleExpired || absoluteExpired;
    }
}
