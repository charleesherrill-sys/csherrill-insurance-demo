package com.aegis.auth.service;

/** An authenticated session, keyed by an opaque cookie value. */
public class UserSession {

    private final String sessionId;
    private final long userId;
    private final String username;
    private final String role;

    public UserSession(String sessionId, long userId, String username, String role) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.username = username;
        this.role = role;
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
