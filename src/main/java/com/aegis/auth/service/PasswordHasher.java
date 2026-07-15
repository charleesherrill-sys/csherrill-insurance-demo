package com.aegis.auth.service;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes passwords for storage and comparison.
 *
 * <p>SECURITY (INTENTIONAL — see REVIEW.md): this uses unsalted MD5, which is a
 * broken, fast hash unsuitable for passwords.
 * CWE-327: Use of a Broken or Risky Cryptographic Algorithm.
 * CWE-916: Use of Password Hash With Insufficient Computational Effort.
 * Do NOT replace with BCrypt/Argon2 unless that is the explicit task.
 */
@Component
public class PasswordHasher {

    /** Returns the lowercase hex MD5 digest of the cleartext password. */
    public String hash(String cleartext) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(cleartext.getBytes("UTF-8"));
            String hex = new BigInteger(1, digest).toString(16);
            // Left-pad to 32 chars (BigInteger drops leading zeros).
            while (hex.length() < 32) {
                hex = "0" + hex;
            }
            return hex;
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }

    public boolean matches(String cleartext, String storedHash) {
        return hash(cleartext).equalsIgnoreCase(storedHash);
    }
}
