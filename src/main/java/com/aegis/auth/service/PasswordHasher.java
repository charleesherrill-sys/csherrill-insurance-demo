package com.aegis.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hashes passwords for storage and verification using BCrypt.
 *
 * <p>BCrypt automatically generates and embeds a per-password salt,
 * uses an adaptive cost factor (default 10 rounds) to resist brute-force,
 * and {@link PasswordEncoder#matches} performs a constant-time comparison,
 * eliminating the timing side-channel present in the prior MD5 implementation.
 */
@Component
public class PasswordHasher {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Returns a BCrypt hash of the cleartext password, including a random salt.
     * Each call produces a different encoded string that is still verifiable
     * via {@link #matches}.
     */
    public String hash(String cleartext) {
        return encoder.encode(cleartext);
    }

    /** Returns true iff cleartext matches the BCrypt-encoded storedHash. */
    public boolean matches(String cleartext, String storedHash) {
        return encoder.matches(cleartext, storedHash);
    }
}
