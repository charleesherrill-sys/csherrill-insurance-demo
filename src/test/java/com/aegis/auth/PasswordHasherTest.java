package com.aegis.auth;

import com.aegis.auth.service.PasswordHasher;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies BCrypt hashing behaviour in {@link PasswordHasher}.
 */
public class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    public void hashProducesBcryptFormat() {
        String hash = hasher.hash("password");
        assertTrue("BCrypt hash must start with $2", hash.startsWith("$2"));
    }

    @Test
    public void hashesAreDifferentForSameInput() {
        // BCrypt salts each hash independently.
        String h1 = hasher.hash("password");
        String h2 = hasher.hash("password");
        assertFalse("Two BCrypt hashes of the same input must differ (different salts)", h1.equals(h2));
    }

    @Test
    public void matchesReturnsTrueForCorrectPassword() {
        String hash = hasher.hash("admin123");
        assertTrue(hasher.matches("admin123", hash));
    }

    @Test
    public void matchesReturnsFalseForWrongPassword() {
        String hash = hasher.hash("admin123");
        assertFalse(hasher.matches("wrong", hash));
    }
}
