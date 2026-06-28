package com.example.demo.controllers;

import com.example.demo.dtos.LoginRequestDTO;
import com.example.demo.dtos.LoginResponseDTO;
import com.example.demo.dtos.SignupRequestDTO;
import com.example.demo.entities.utils.Role;
import com.example.demo.services.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// Coverage criteria: direct controller unit coverage for every AuthController endpoint,
// verifying HTTP status, returned body, fixed text responses, and AuthService delegation.
class AuthControllerTest {

    @Test
    void signupReturnsOkResponseFromService() throws Exception {
        AuthController controller = new AuthController();
        LoginResponseDTO serviceResponse = loginResponse("signup-token", "Salma", Role.USER);
        TestAuthService service = new TestAuthService(serviceResponse);
        injectService(controller, service);
        SignupRequestDTO request = signupRequest("Salma", "salma@example.com", "password123");

        ResponseEntity<LoginResponseDTO> response = controller.signup(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(serviceResponse, response.getBody());
        assertSame(request, service.signupRequest.get());
    }

    @Test
    void loginReturnsOkResponseFromService() throws Exception {
        AuthController controller = new AuthController();
        LoginResponseDTO serviceResponse = loginResponse("login-token", "Omar", Role.USER);
        TestAuthService service = new TestAuthService(serviceResponse);
        injectService(controller, service);
        LoginRequestDTO request = loginRequest("omar@example.com", "password123");

        ResponseEntity<LoginResponseDTO> response = controller.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(serviceResponse, response.getBody());
        assertSame(request, service.loginRequest.get());
    }

    @Test
    void signupAdminReturnsOkResponseFromService() throws Exception {
        AuthController controller = new AuthController();
        LoginResponseDTO serviceResponse = loginResponse("admin-token", "Admin", Role.ADMIN);
        TestAuthService service = new TestAuthService(serviceResponse);
        injectService(controller, service);
        SignupRequestDTO request = signupRequest("Admin", "admin@example.com", "adminpass123");

        ResponseEntity<LoginResponseDTO> response = controller.signupAdmin(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(serviceResponse, response.getBody());
        assertSame(request, service.adminSignupRequest.get());
    }

    @Test
    void loggedinReturnsSuccessMessage() {
        AuthController controller = new AuthController();

        String response = controller.loggedin();

        assertEquals("successfully logged in", response);
    }

    private static LoginResponseDTO loginResponse(String token, String username, Role role) {
        return LoginResponseDTO.builder()
                .token(token)
                .username(username)
                .role(role)
                .build();
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

    private static void injectService(AuthController controller, AuthService service) throws Exception {
        Field field = AuthController.class.getDeclaredField("authService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    private static class TestAuthService extends AuthService {
        private final LoginResponseDTO response;
        private final AtomicReference<SignupRequestDTO> signupRequest = new AtomicReference<>();
        private final AtomicReference<LoginRequestDTO> loginRequest = new AtomicReference<>();
        private final AtomicReference<SignupRequestDTO> adminSignupRequest = new AtomicReference<>();

        private TestAuthService(LoginResponseDTO response) {
            this.response = response;
        }

        @Override
        public LoginResponseDTO signup(SignupRequestDTO request) {
            signupRequest.set(request);
            return response;
        }

        @Override
        public LoginResponseDTO login(LoginRequestDTO request) {
            loginRequest.set(request);
            return response;
        }

        @Override
        public LoginResponseDTO createAdmin(SignupRequestDTO signupRequest) {
            adminSignupRequest.set(signupRequest);
            return response;
        }
    }
}
