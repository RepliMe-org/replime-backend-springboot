package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.LoginRequestDTO;
import com.example.demo.dtos.LoginResponseDTO;
import com.example.demo.dtos.SignupRequestDTO;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.AuthProvider;
import com.example.demo.entities.utils.Role;
import com.example.demo.exceptions.AuthenticationException;
import com.example.demo.repos.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    @Test
    void signupCreatesLocalUserAndReturnsToken() throws Exception {
        AuthService service = new AuthService();
        AtomicReference<User> savedUser = new AtomicReference<>();
        TestJwtService jwtService = new TestJwtService("signup-token");
        TestPasswordEncoder passwordEncoder = new TestPasswordEncoder("encoded-password");
        injectDependencies(
                service,
                createRepoProxy(Optional.empty(), false, savedUser, new AtomicInteger(), new AtomicInteger()),
                jwtService,
                new TestAuthenticationManager(false),
                passwordEncoder);

        LoginResponseDTO response = service.signup(signupRequest("Salma", "salma@example.com", "password123"));

        assertEquals("signup-token", response.getToken());
        assertEquals("Salma", response.getUsername());
        assertEquals(Role.USER, response.getRole());
        assertEquals("password123", passwordEncoder.rawPassword.get());
        assertEquals("Salma", savedUser.get().getName());
        assertEquals("salma@example.com", savedUser.get().getEmail());
        assertEquals("encoded-password", savedUser.get().getPassword());
        assertEquals(AuthProvider.LOCAL, savedUser.get().getProvider());
        assertEquals(Role.USER, savedUser.get().getRole());
        assertSame(savedUser.get(), jwtService.user.get());
    }

    @Test
    void signupThrowsWhenEmailAlreadyExists() throws Exception {
        AuthService service = new AuthService();
        AtomicReference<User> savedUser = new AtomicReference<>();
        TestPasswordEncoder passwordEncoder = new TestPasswordEncoder("encoded-password");
        injectDependencies(
                service,
                createRepoProxy(Optional.of(User.builder().email("salma@example.com").build()), false,
                        savedUser, new AtomicInteger(), new AtomicInteger()),
                new TestJwtService("unused-token"),
                new TestAuthenticationManager(false),
                passwordEncoder);

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> service.signup(signupRequest("Salma", "salma@example.com", "password123")));

        assertEquals("Email already exists", exception.getMessage());
        assertNull(savedUser.get());
        assertFalse(passwordEncoder.encodeCalled);
    }

    @Test
    void loginAuthenticatesUserAndReturnsToken() throws Exception {
        AuthService service = new AuthService();
        User user = User.builder()
                .name("Omar")
                .email("omar@example.com")
                .role(Role.USER)
                .build();
        TestAuthenticationManager authenticationManager = new TestAuthenticationManager(false);
        TestJwtService jwtService = new TestJwtService("login-token");
        injectDependencies(
                service,
                createRepoProxy(Optional.of(user), false, new AtomicReference<>(), new AtomicInteger(), new AtomicInteger()),
                jwtService,
                authenticationManager,
                new TestPasswordEncoder("unused"));

        LoginResponseDTO response = service.login(loginRequest("omar@example.com", "password123"));

        assertEquals("login-token", response.getToken());
        assertEquals("Omar", response.getUsername());
        assertEquals(Role.USER, response.getRole());
        assertEquals("omar@example.com", authenticationManager.email.get());
        assertEquals("password123", authenticationManager.password.get());
        assertSame(user, jwtService.user.get());
    }

    @Test
    void loginThrowsWhenAuthenticationFails() throws Exception {
        AuthService service = new AuthService();
        AtomicInteger findByEmailCalls = new AtomicInteger();
        injectDependencies(
                service,
                createRepoProxy(Optional.empty(), false, new AtomicReference<>(), findByEmailCalls, new AtomicInteger()),
                new TestJwtService("unused-token"),
                new TestAuthenticationManager(true),
                new TestPasswordEncoder("unused"));

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> service.login(loginRequest("omar@example.com", "wrong-password")));

        assertEquals("Invalid email or password", exception.getMessage());
        assertEquals(0, findByEmailCalls.get());
    }

    @Test
    void loginThrowsWhenAuthenticatedUserDoesNotExist() throws Exception {
        AuthService service = new AuthService();
        injectDependencies(
                service,
                createRepoProxy(Optional.empty(), false, new AtomicReference<>(), new AtomicInteger(), new AtomicInteger()),
                new TestJwtService("unused-token"),
                new TestAuthenticationManager(false),
                new TestPasswordEncoder("unused"));

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> service.login(loginRequest("missing@example.com", "password123")));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void createAdminCreatesFirstAdminAndReturnsToken() throws Exception {
        AuthService service = new AuthService();
        AtomicReference<User> savedUser = new AtomicReference<>();
        TestJwtService jwtService = new TestJwtService("admin-token");
        TestPasswordEncoder passwordEncoder = new TestPasswordEncoder("encoded-admin-password");
        injectDependencies(
                service,
                createRepoProxy(Optional.empty(), false, savedUser, new AtomicInteger(), new AtomicInteger()),
                jwtService,
                new TestAuthenticationManager(false),
                passwordEncoder);

        LoginResponseDTO response = service.createAdmin(
                signupRequest("Admin", "admin@example.com", "adminpass123"));

        assertEquals("admin-token", response.getToken());
        assertEquals("Admin", response.getUsername());
        assertEquals(Role.ADMIN, response.getRole());
        assertEquals("adminpass123", passwordEncoder.rawPassword.get());
        assertEquals("Admin", savedUser.get().getName());
        assertEquals("admin@example.com", savedUser.get().getEmail());
        assertEquals("encoded-admin-password", savedUser.get().getPassword());
        assertEquals(AuthProvider.LOCAL, savedUser.get().getProvider());
        assertEquals(Role.ADMIN, savedUser.get().getRole());
        assertSame(savedUser.get(), jwtService.user.get());
    }

    @Test
    void createAdminThrowsWhenAdminAlreadyExists() throws Exception {
        AuthService service = new AuthService();
        AtomicReference<User> savedUser = new AtomicReference<>();
        AtomicInteger findByEmailCalls = new AtomicInteger();
        injectDependencies(
                service,
                createRepoProxy(Optional.empty(), true, savedUser, findByEmailCalls, new AtomicInteger()),
                new TestJwtService("unused-token"),
                new TestAuthenticationManager(false),
                new TestPasswordEncoder("unused"));

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> service.createAdmin(signupRequest("Admin", "admin@example.com", "adminpass123")));

        assertEquals("Admin user already exists", exception.getMessage());
        assertEquals(0, findByEmailCalls.get());
        assertNull(savedUser.get());
    }

    @Test
    void createAdminThrowsWhenEmailAlreadyExists() throws Exception {
        AuthService service = new AuthService();
        AtomicReference<User> savedUser = new AtomicReference<>();
        injectDependencies(
                service,
                createRepoProxy(Optional.of(User.builder().email("admin@example.com").build()), false,
                        savedUser, new AtomicInteger(), new AtomicInteger()),
                new TestJwtService("unused-token"),
                new TestAuthenticationManager(false),
                new TestPasswordEncoder("unused"));

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> service.createAdmin(signupRequest("Admin", "admin@example.com", "adminpass123")));

        assertEquals("Email already exists", exception.getMessage());
        assertNull(savedUser.get());
    }

    private static SignupRequestDTO signupRequest(String name, String email, String password) {
        SignupRequestDTO request = new SignupRequestDTO();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static LoginRequestDTO loginRequest(String email, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static void injectDependencies(
            AuthService service,
            UserRepo userRepo,
            JwtService jwtService,
            org.springframework.security.authentication.AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder
    ) throws Exception {
        injectField(service, "userRepo", userRepo);
        injectField(service, "jwtService", jwtService);
        injectField(service, "authenticationManager", authenticationManager);
        injectField(service, "passwordEncoder", passwordEncoder);
    }

    private static void injectField(AuthService service, String name, Object value) throws Exception {
        Field field = AuthService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    private static UserRepo createRepoProxy(
            Optional<User> user,
            boolean adminExists,
            AtomicReference<User> savedUser,
            AtomicInteger findByEmailCalls,
            AtomicInteger existsByRoleCalls
    ) {
        return (UserRepo) Proxy.newProxyInstance(
                UserRepo.class.getClassLoader(),
                new Class[]{UserRepo.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByEmail" -> {
                        findByEmailCalls.incrementAndGet();
                        yield user;
                    }
                    case "existsByRole" -> {
                        existsByRoleCalls.incrementAndGet();
                        yield adminExists;
                    }
                    case "save" -> {
                        User value = (User) args[0];
                        savedUser.set(value);
                        yield value;
                    }
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "UserRepoProxy";
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        return null;
    }

    private static class TestJwtService extends JwtService {
        private final String token;
        private final AtomicReference<User> user = new AtomicReference<>();

        private TestJwtService(String token) {
            this.token = token;
        }

        @Override
        public String generateToken(User user) {
            this.user.set(user);
            return token;
        }
    }

    private static class TestAuthenticationManager
            implements org.springframework.security.authentication.AuthenticationManager {
        private final boolean throwException;
        private final AtomicReference<String> email = new AtomicReference<>();
        private final AtomicReference<String> password = new AtomicReference<>();

        private TestAuthenticationManager(boolean throwException) {
            this.throwException = throwException;
        }

        @Override
        public Authentication authenticate(Authentication authentication) {
            UsernamePasswordAuthenticationToken token =
                    (UsernamePasswordAuthenticationToken) authentication;
            email.set((String) token.getPrincipal());
            password.set((String) token.getCredentials());
            if (throwException) {
                throw new BadCredentialsException("bad credentials");
            }
            return authentication;
        }
    }

    private static class TestPasswordEncoder implements PasswordEncoder {
        private final String encodedPassword;
        private final AtomicReference<String> rawPassword = new AtomicReference<>();
        private boolean encodeCalled;

        private TestPasswordEncoder(String encodedPassword) {
            this.encodedPassword = encodedPassword;
        }

        @Override
        public String encode(CharSequence rawPassword) {
            encodeCalled = true;
            this.rawPassword.set(rawPassword.toString());
            return encodedPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return this.encodedPassword.equals(encodedPassword)
                    && this.rawPassword.get().equals(rawPassword.toString());
        }
    }
}
