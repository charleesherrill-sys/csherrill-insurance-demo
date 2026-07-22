package com.aegis.auth.service;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashes passwords for storage and comparison.
 *
 * <p>SECURITY (CWE-327, CWE-916): new passwords are hashed with PBKDF2-HMAC-SHA256
 * using a per-password random salt and a high iteration count — a salted, adaptive
 * algorithm suitable for password storage. The stored value is self-describing:
 * {@code pbkdf2$sha256$<iterations>$<base64 salt>$<base64 hash>}, so the parameters
 * travel with the hash and can be tuned over time.
 *
 * <p>MIGRATION NOTE: existing accounts were stored as unsalted MD5 (32 hex chars),
 * including the seeded users in {@code db/seed.sql}. Those hashes cannot be
 * converted without the cleartext, so {@link #matches(String, String)} still
 * verifies legacy MD5 hashes to preserve login for pre-existing accounts. Because
 * the cleartext is available at login, callers should transparently re-hash and
 * persist with {@link #hash(String)} (and {@link #isLegacyHash(String)} detects
 * which stored hashes still need upgrading). MD5 is only ever used to *verify* a
 * legacy hash; it is never used to produce a new one. Seeded fixtures may be
 * regenerated with PBKDF2 values to remove MD5 entirely.
 */
@Component
public class PasswordHasher {

    private static final String PBKDF2_PREFIX = "pbkdf2$sha256$";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final SecureRandom secureRandom = new SecureRandom();

    /** Returns a salted PBKDF2 hash of the cleartext password in a self-describing format. */
    public String hash(String cleartext) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derived = pbkdf2(cleartext, salt, ITERATIONS, KEY_BITS);
        Base64.Encoder enc = Base64.getEncoder();
        return PBKDF2_PREFIX + ITERATIONS + "$"
                + enc.encodeToString(salt) + "$"
                + enc.encodeToString(derived);
    }

    /**
     * Verifies a cleartext password against a stored hash. Supports the current
     * PBKDF2 format and, for migration, the legacy unsalted MD5 format.
     */
    public boolean matches(String cleartext, String storedHash) {
        if (cleartext == null || storedHash == null) {
            return false;
        }
        if (storedHash.startsWith(PBKDF2_PREFIX)) {
            return matchesPbkdf2(cleartext, storedHash);
        }
        if (isLegacyHash(storedHash)) {
            return constantTimeEquals(md5Hex(cleartext).getBytes(),
                    storedHash.toLowerCase().getBytes());
        }
        return false;
    }

    /** True if the stored hash is a legacy unsalted MD5 digest (32 hex chars) needing an upgrade. */
    public boolean isLegacyHash(String storedHash) {
        return storedHash != null && storedHash.matches("(?i)[0-9a-f]{32}");
    }

    private boolean matchesPbkdf2(String cleartext, String storedHash) {
        String[] parts = storedHash.split("\\$");
        // Expected: ["pbkdf2", "sha256", iterations, salt, hash]
        if (parts.length != 5) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[2]);
            Base64.Decoder dec = Base64.getDecoder();
            byte[] salt = dec.decode(parts[3]);
            byte[] expected = dec.decode(parts[4]);
            byte[] actual = pbkdf2(cleartext, salt, iterations, expected.length * 8);
            return constantTimeEquals(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] pbkdf2(String cleartext, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(cleartext.toCharArray(), salt, iterations, keyBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        }
    }

    private String md5Hex(String cleartext) {
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

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
