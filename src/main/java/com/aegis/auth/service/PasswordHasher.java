package com.aegis.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes passwords for storage and comparison.
 *
 * <p>Passwords are stored using <strong>BCrypt</strong>, an adaptive salted hash
 * (fixes CWE-327 "broken/risky algorithm" and CWE-916 "insufficient computational
 * effort"). Each call to {@link #hash(String)} produces a distinct salted digest.
 *
 * <p>For backward compatibility with legacy rows that still hold an unsalted MD5
 * hex digest, {@link #matches(String, String)} transparently verifies against the
 * old format so those accounts can still authenticate. {@link AuthService} rehashes
 * such credentials to BCrypt on the next successful login (see
 * {@link #isLegacyHash(String)}), providing an in-place upgrade path.
 */
@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** Returns a salted BCrypt hash of the cleartext password. */
    public String hash(String cleartext) {
        return encoder.encode(cleartext);
    }

    /**
     * Verifies a cleartext password against a stored hash. Supports the current
     * BCrypt format and the legacy unsalted-MD5 format for un-migrated rows.
     */
    public boolean matches(String cleartext, String storedHash) {
        if (storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        if (isLegacyHash(storedHash)) {
            return constantTimeEquals(legacyMd5(cleartext), storedHash.toLowerCase());
        }
        return encoder.matches(cleartext, storedHash);
    }

    /** True when the stored hash is a legacy unsalted-MD5 digest rather than BCrypt. */
    public boolean isLegacyHash(String storedHash) {
        return storedHash != null && storedHash.matches("(?i)[0-9a-f]{32}");
    }

    /** Lowercase hex MD5 digest — retained only to verify/upgrade legacy hashes. */
    private String legacyMd5(String cleartext) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(cleartext.getBytes("UTF-8"));
            String hex = new BigInteger(1, digest).toString(16);
            while (hex.length() < 32) {
                hex = "0" + hex;
            }
            return hex;
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
