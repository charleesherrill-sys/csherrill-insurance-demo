package com.aegis.auth;

import com.aegis.auth.service.PasswordHasher;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for PasswordHasher after migration from MD5 to BCrypt.
 *
 * <p>BCrypt hashes are salted — two calls on the same input produce different
 * encoded strings. Tests therefore assert round-trip correctness via
 * {@code matches} rather than a fixed digest value.
 */
public class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    public void hashThenMatchesReturnsTrue() {
        String encoded = hasher.hash("password");
        assertTrue(hasher.matches("password", encoded));
    }

    @Test
    public void matchesReturnsFalseForWrongPassword() {
        String encoded = hasher.hash("password");
        assertFalse(hasher.matches("wrong", encoded));
    }

    @Test
    public void eachHashIsUniqueDueToSalt() {
        // BCrypt embeds a random salt; two hashes of the same input must differ.
        String first = hasher.hash("password");
        String second = hasher.hash("password");
        assertNotEquals(first, second);
    }

    @Test
    public void hashIsBcryptFormat() {
        // BCrypt-encoded strings start with $2a$ or $2b$.
        String encoded = hasher.hash("admin123");
        assertTrue("Expected BCrypt format", encoded.startsWith("$2"));
    }
}
