package com.example.demo.controllers;

import com.example.demo.dtos.UserInfoResponseDTO;
import com.example.demo.entities.utils.Role;
import com.example.demo.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// Coverage criteria: direct controller unit coverage for user listing and promotion endpoints,
// verifying service delegation, email forwarding, and fixed success response.
class UserControllerTest {

    @Test
    void getUsersReturnsServiceUsers() throws Exception {
        UserController controller = new UserController();
        List<UserInfoResponseDTO> users = List.of(UserInfoResponseDTO.builder()
                .email("user@example.com")
                .role(Role.USER)
                .build());
        TestUserService service = new TestUserService();
        service.users = users;
        injectService(controller, service);

        List<UserInfoResponseDTO> response = controller.getUsers();

        assertSame(users, response);
    }

    @Test
    void promoteToAdminPassesEmailAndReturnsSuccessMessage() throws Exception {
        UserController controller = new UserController();
        TestUserService service = new TestUserService();
        injectService(controller, service);

        ResponseEntity<String> response = controller.promoteToAdmin("user@example.com");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("User promoted to admin successfully", response.getBody());
        assertEquals("user@example.com", service.promotedEmail.get());
    }

    private static void injectService(UserController controller, UserService service) throws Exception {
        Field field = UserController.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    private static class TestUserService extends UserService {
        private List<UserInfoResponseDTO> users;
        private final AtomicReference<String> promotedEmail = new AtomicReference<>();

        @Override
        public List<UserInfoResponseDTO> getAllUsers() {
            return users;
        }

        @Override
        public void promoteToAdmin(String email) {
            promotedEmail.set(email);
        }
    }
}
