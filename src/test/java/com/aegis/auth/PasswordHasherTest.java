package com.aegis.auth;

import com.aegis.auth.service.PasswordHasher;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    public void roundTripMatches() {
        assertTrue(hasher.matches("password", hasher.hash("password")));
    }

    @Test
    public void wrongPasswordDoesNotMatch() {
        assertFalse(hasher.matches("wrong", hasher.hash("password")));
    }

    @Test
    public void hashesAreSalted() {
        String first = hasher.hash("password");
        String second = hasher.hash("password");
        assertNotEquals(first, second);
        assertTrue(hasher.matches("password", first));
        assertTrue(hasher.matches("password", second));
    }

    @Test
    public void nullStoredHashDoesNotMatch() {
        assertFalse(hasher.matches("password", null));
    }

    @Test
    public void legacyMd5HashDoesNotMatch() {
        assertFalse(hasher.matches("password", "5f4dcc3b5aa765d61d8327deb882cf99"));
    }
}
