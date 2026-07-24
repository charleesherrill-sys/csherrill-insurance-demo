package com.aegis.auth.service;

/** An authenticated session, keyed by an opaque cookie value. */
public class UserSession {

    private final String sessionId;
    private final long userId;
    private final String username;
    private final String role;
    private final long createdAtMillis;
    private volatile long lastAccessedMillis;

    public UserSession(String sessionId, long userId, String username, String role) {
        this(sessionId, userId, username, role, System.currentTimeMillis());
    }

    public UserSession(String sessionId, long userId, String username, String role, long createdAtMillis) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.createdAtMillis = createdAtMillis;
        this.lastAccessedMillis = createdAtMillis;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getLastAccessedMillis() {
        return lastAccessedMillis;
    }

    /** Records activity on the session for idle-timeout tracking. */
    public void touch(long nowMillis) {
        this.lastAccessedMillis = nowMillis;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isAdjuster() {
        return "ADJUSTER".equalsIgnoreCase(role);
    }

    public boolean canViewAllMembers() {
        return isAdmin() || isAdjuster();
    }
}
