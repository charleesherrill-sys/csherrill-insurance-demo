package com.aegis.auth;

import com.aegis.auth.model.User;
import com.aegis.auth.service.SessionManager;
import com.aegis.auth.service.UserSession;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/** Session lifecycle: creation, expiration, and invalidation. */
public class SessionManagerTest {

    private User member() {
        User u = new User();
        u.setId(5583L);
        u.setUsername("amorgan");
        u.setRole("MEMBER");
        return u;
    }

    @Test
    public void createAndResolveSession() {
        SessionManager sm = new SessionManager();
        UserSession session = sm.create(member());
        assertNotNull(sm.get(session.getSessionId()));
    }

    @Test
    public void eachLoginMintsANewSessionId() {
        SessionManager sm = new SessionManager();
        UserSession a = sm.create(member());
        UserSession b = sm.create(member());
        assertNotEquals(a.getSessionId(), b.getSessionId());
    }

    @Test
    public void invalidatedSessionCannotBeResolved() {
        SessionManager sm = new SessionManager();
        UserSession session = sm.create(member());
        sm.invalidate(session.getSessionId());
        assertNull(sm.get(session.getSessionId()));
    }

    @Test
    public void idleSessionExpires() throws InterruptedException {
        // 5ms idle timeout, generous absolute timeout.
        SessionManager sm = new SessionManager(5L, 60_000L);
        UserSession session = sm.create(member());
        Thread.sleep(20L);
        assertNull(sm.get(session.getSessionId()));
    }

    @Test
    public void absoluteLifetimeExpires() throws InterruptedException {
        // Generous idle timeout but a 5ms absolute lifetime.
        SessionManager sm = new SessionManager(60_000L, 5L);
        UserSession session = sm.create(member());
        Thread.sleep(20L);
        assertNull(sm.get(session.getSessionId()));
    }

    @Test
    public void nullSessionIdResolvesToNull() {
        assertNull(new SessionManager().get(null));
    }
}
