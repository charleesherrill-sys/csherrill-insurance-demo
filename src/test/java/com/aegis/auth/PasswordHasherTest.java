package com.aegis.auth;

import com.aegis.auth.service.PasswordHasher;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the BCrypt hashing behaviour and the legacy-MD5 upgrade path.
 */
public class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    public void hashIsBcryptAndSalted() {
        String h1 = hasher.hash("password");
        String h2 = hasher.hash("password");
        assertTrue(h1.startsWith("$2"));
        // BCrypt is salted: the same cleartext yields different digests.
        assertNotEquals(h1, h2);
        assertNotEquals("password", h1);
    }

    @Test
    public void matchesBcryptHash() {
        String h = hasher.hash("s3cret!");
        assertTrue(hasher.matches("s3cret!", h));
        assertFalse(hasher.matches("wrong", h));
    }

    @Test
    public void verifiesLegacyMd5ForUnmigratedRows() {
        // "password" -> MD5 5f4dcc3b5aa765d61d8327deb882cf99 (legacy seed format).
        String legacy = "5f4dcc3b5aa765d61d8327deb882cf99";
        assertTrue(hasher.isLegacyHash(legacy));
        assertTrue(hasher.matches("password", legacy));
        assertFalse(hasher.matches("wrong", legacy));
    }

    @Test
    public void bcryptHashIsNotFlaggedAsLegacy() {
        assertFalse(hasher.isLegacyHash(hasher.hash("password")));
    }
}
