package com.aegis.auth;

import com.aegis.auth.service.PasswordHasher;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Locks in the (intentionally weak) MD5 hashing behaviour. See REVIEW.md — this
 * test documents the current behaviour, it is not an endorsement of MD5.
 */
public class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    public void hashesKnownMd5() {
        assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", hasher.hash("password"));
    }

    @Test
    public void padsLeadingZeros() {
        // A digest that would otherwise drop a leading zero must still be 32 chars.
        assertEquals(32, hasher.hash("aegis").length());
    }

    @Test
    public void matchesStoredHash() {
        assertTrue(hasher.matches("admin123", "0192023a7bbd73250516f069df18b500"));
        assertFalse(hasher.matches("wrong", "0192023a7bbd73250516f069df18b500"));
    }
}
