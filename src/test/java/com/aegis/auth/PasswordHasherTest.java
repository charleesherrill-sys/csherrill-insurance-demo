package com.aegis.auth;

import com.aegis.auth.service.PasswordHasher;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the BCrypt hashing behaviour and backward-compatible verification of
 * legacy MD5 hashes for migration. See REVIEW.md.
 */
public class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    public void hashIsBcryptAndNotCleartext() {
        String hash = hasher.hash("password");
        assertNotEquals("password", hash);
        assertTrue("expected a BCrypt hash", hash.startsWith("$2"));
    }

    @Test
    public void hashesAreSaltedAndUnique() {
        // Salting means two hashes of the same password differ.
        assertNotEquals(hasher.hash("password"), hasher.hash("password"));
    }

    @Test
    public void matchesBcryptHash() {
        String hash = hasher.hash("s3cret!");
        assertTrue(hasher.matches("s3cret!", hash));
        assertFalse(hasher.matches("wrong", hash));
    }

    @Test
    public void matchesLegacyMd5ForMigration() {
        // 0192023a7bbd73250516f069df18b500 is the unsalted MD5 of "admin123".
        assertTrue(hasher.matches("admin123", "0192023a7bbd73250516f069df18b500"));
        assertFalse(hasher.matches("wrong", "0192023a7bbd73250516f069df18b500"));
    }

    @Test
    public void detectsLegacyHashNeedingRehash() {
        assertTrue(hasher.needsRehash("0192023a7bbd73250516f069df18b500"));
        assertFalse(hasher.needsRehash(hasher.hash("admin123")));
    }
}
