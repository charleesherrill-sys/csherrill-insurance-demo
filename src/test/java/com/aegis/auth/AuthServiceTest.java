package com.aegis.auth;

import com.aegis.auth.model.User;
import com.aegis.auth.repository.UserRepository;
import com.aegis.auth.service.AuthService;
import com.aegis.auth.service.PasswordHasher;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Authentication and the transparent legacy-hash upgrade path. */
public class AuthServiceTest {

    private final PasswordHasher hasher = new PasswordHasher();
    private UserRepository userRepository;
    private AuthService authService;

    @Before
    public void setUp() {
        userRepository = mock(UserRepository.class);
        authService = new AuthService(userRepository, hasher);
    }

    private User user(long id, String username, String hash) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setPasswordHash(hash);
        u.setRole("MEMBER");
        return u;
    }

    @Test
    public void rejectsUnknownUser() {
        when(userRepository.findByUsername("nobody")).thenReturn(null);
        assertNull(authService.authenticate("nobody", "x"));
    }

    @Test
    public void rejectsWrongPassword() {
        User u = user(5583L, "amorgan", hasher.hash("password"));
        when(userRepository.findByUsername("amorgan")).thenReturn(u);
        assertNull(authService.authenticate("amorgan", "wrong"));
    }

    @Test
    public void authenticatesBcryptUserWithoutRehash() {
        User u = user(5583L, "amorgan", hasher.hash("password"));
        when(userRepository.findByUsername("amorgan")).thenReturn(u);
        assertNotNull(authService.authenticate("amorgan", "password"));
        verify(userRepository, never()).updatePasswordHash(anyLong(), anyString());
    }

    @Test
    public void upgradesLegacyMd5HashOnSuccessfulLogin() {
        // MD5("password") legacy seed hash.
        String legacy = "5f4dcc3b5aa765d61d8327deb882cf99";
        User u = user(5583L, "amorgan", legacy);
        when(userRepository.findByUsername("amorgan")).thenReturn(u);

        User result = authService.authenticate("amorgan", "password");

        assertNotNull(result);
        // The stored hash was upgraded to BCrypt and persisted.
        assertTrue(result.getPasswordHash().startsWith("$2"));
        verify(userRepository).updatePasswordHash(eq(5583L), anyString());
    }
}
