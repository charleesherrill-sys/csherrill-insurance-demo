package com.aegis.auth.model;

/** A platform user: MEMBER, ADJUSTER, or ADMIN. */
public class User {

    private long id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String role;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isAdjuster() {
        return "ADJUSTER".equalsIgnoreCase(role);
    }

    /** Adjusters and admins are allowed to view any member's records. */
    public boolean canViewAllMembers() {
        return isAdmin() || isAdjuster();
    }
}
