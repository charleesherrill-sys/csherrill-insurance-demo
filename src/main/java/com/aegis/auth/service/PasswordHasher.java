package com.aegis.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hashes passwords for storage and comparison using BCrypt (strength 12).
 * Replaces the previous unsalted MD5 implementation (CWE-327 / CWE-916).
 */
@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    /** Returns a BCrypt hash of the cleartext password. */
    public String hash(String cleartext) {
        return encoder.encode(cleartext);
    }

    public boolean matches(String cleartext, String storedHash) {
        return encoder.matches(cleartext, storedHash);
    }
}
