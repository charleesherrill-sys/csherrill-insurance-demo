package com.aegis.auth.service;

import java.time.Instant;

/** An authenticated session, keyed by an opaque cookie value. */
public class UserSession {

    private final String sessionId;
    private final long userId;
    private final String username;
    private final String role;
    private final Instant createdAt;

    public UserSession(String sessionId, long userId, String username, String role) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.createdAt = Instant.now();
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

    public Instant getCreatedAt() {
        return createdAt;
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
