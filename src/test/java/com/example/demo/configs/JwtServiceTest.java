package com.example.demo.configs;

import com.example.demo.entities.User;
import com.example.demo.entities.utils.Role;
import com.example.demo.repos.UserRepo;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    @Test
    void generateTokenCanBeParsedAndValidatedForSameUser() {
        JwtService service = service(mock(UserRepo.class), 60_000L);
        User user = User.builder()
                .email("creator@example.com")
                .role(Role.INFLUENCER)
                .build();

        String token = service.generateToken(user);

        assertNotNull(token);
        assertEquals("creator@example.com", service.extractUsername(token));
        assertTrue(service.extractExpiration(token).getTime() > System.currentTimeMillis());
        assertTrue(service.isTokenValid(token, user));
    }

    @Test
    void extractUserAcceptsBearerTokenAndLoadsUserByEmail() {
        UserRepo userRepo = mock(UserRepo.class);
        JwtService service = service(userRepo, 60_000L);
        User user = User.builder()
                .email("creator@example.com")
                .role(Role.INFLUENCER)
                .build();
        String token = service.generateToken(user);
        when(userRepo.findByEmail("creator@example.com")).thenReturn(Optional.of(user));

        User extracted = service.extractUser("Bearer " + token);

        assertSame(user, extracted);
    }

    @Test
    void extractUserThrowsWhenTokenUserIsMissing() {
        UserRepo userRepo = mock(UserRepo.class);
        JwtService service = service(userRepo, 60_000L);
        User user = User.builder()
                .email("missing@example.com")
                .role(Role.USER)
                .build();
        String token = service.generateToken(user);
        when(userRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.extractUser(token));

        assertEquals("User not found", exception.getMessage());
    }

    private static JwtService service(UserRepo userRepo, long expiration) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", SECRET);
        ReflectionTestUtils.setField(service, "jwtExpiration", expiration);
        ReflectionTestUtils.setField(service, "userRepo", userRepo);
        return service;
    }
}
