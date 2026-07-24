package com.aegis.auth.service;

import com.aegis.auth.model.User;
import com.aegis.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Authenticates username/password credentials against the user store. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /** Returns the authenticated user, or null if the credentials are invalid. */
    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return null;
        }
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            return null;
        }
        // Transparent upgrade path: rehash legacy (MD5) credentials to BCrypt on
        // successful login so stored hashes migrate off the weak format over time.
        if (passwordHasher.isLegacyHash(user.getPasswordHash())) {
            String upgraded = passwordHasher.hash(password);
            userRepository.updatePasswordHash(user.getId(), upgraded);
            user.setPasswordHash(upgraded);
        }
        return user;
    }
}
