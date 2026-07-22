package com.aegis.auth;

import com.aegis.auth.service.PasswordHasher;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the salted, adaptive PBKDF2 hashing (CWE-327/CWE-916 remediation) and
 * the legacy MD5 verification path retained for migrating pre-existing accounts.
 */
public class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    public void hashUsesSaltedPbkdf2Format() {
        String hash = hasher.hash("password");
        assertTrue("hash should use the self-describing pbkdf2 format",
                hash.startsWith("pbkdf2$sha256$"));
        // Must NOT be a bare MD5 digest anymore.
        assertNotEquals("5f4dcc3b5aa765d61d8327deb882cf99", hash);
        assertFalse(hasher.isLegacyHash(hash));
    }

    @Test
    public void hashRoundTripsThroughMatches() {
        String hash = hasher.hash("s3cret-pw");
        assertTrue(hasher.matches("s3cret-pw", hash));
        assertFalse(hasher.matches("wrong", hash));
    }

    @Test
    public void saltMakesEachHashUnique() {
        assertNotEquals("same input must not produce identical hashes (salted)",
                hasher.hash("password"), hasher.hash("password"));
    }

    @Test
    public void verifiesLegacyMd5ForMigration() {
        // admin123 -> MD5, as seeded in db/seed.sql for pre-existing accounts.
        String legacy = "0192023a7bbd73250516f069df18b500";
        assertTrue(hasher.isLegacyHash(legacy));
        assertTrue(hasher.matches("admin123", legacy));
        assertFalse(hasher.matches("wrong", legacy));
    }

    @Test
    public void rejectsUnrecognizedHashFormat() {
        assertFalse(hasher.matches("password", "not-a-real-hash"));
    }
}
