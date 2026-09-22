package com.aegis.auth.service;

import org.springframework.stereotype.Component;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Hashes passwords for storage and comparison. */
@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String cleartext) {
        return encoder.encode(cleartext);
    }

    public boolean matches(String cleartext, String storedHash) {
        return storedHash != null && encoder.matches(cleartext, storedHash);
    }
}
