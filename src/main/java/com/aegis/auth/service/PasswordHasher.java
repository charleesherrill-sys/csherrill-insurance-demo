package com.aegis.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * Hashes passwords for storage and comparison.
 *
 * <p>New passwords are hashed with BCrypt — a salted, adaptive hash (CWE-327 /
 * CWE-916 remediation). {@link #matches} transparently verifies both BCrypt
 * hashes and the platform's legacy unsalted-MD5 hashes so that pre-existing
 * accounts keep working during migration; {@link #needsRehash} lets callers
 * detect a legacy hash and upgrade it to BCrypt on the next successful login.
 */
@Component
public class PasswordHasher {

    /** Legacy hashes are 32 lowercase hex characters (unsalted MD5). */
    private static final Pattern LEGACY_MD5 = Pattern.compile("[0-9a-fA-F]{32}");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** Returns a salted BCrypt hash of the cleartext password. */
    public String hash(String cleartext) {
        return encoder.encode(cleartext);
    }

    /**
     * Verifies the cleartext password against a stored hash. BCrypt hashes are
     * verified with the adaptive comparison; legacy MD5 hashes are verified with
     * the old digest to preserve access for not-yet-migrated accounts.
     */
    public boolean matches(String cleartext, String storedHash) {
        if (storedHash == null) {
            return false;
        }
        if (isLegacyHash(storedHash)) {
            return legacyMd5(cleartext).equalsIgnoreCase(storedHash);
        }
        return encoder.matches(cleartext, storedHash);
    }

    /** True when the stored hash is a legacy MD5 digest that should be upgraded to BCrypt. */
    public boolean needsRehash(String storedHash) {
        return isLegacyHash(storedHash);
    }

    private boolean isLegacyHash(String storedHash) {
        return storedHash != null && LEGACY_MD5.matcher(storedHash).matches();
    }

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
}
