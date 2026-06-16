package com.example.demo.services;

import com.example.demo.entities.User;
import com.example.demo.entities.utils.Role;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.UserRepo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest {

    @Test
    void promoteToAdminUpdatesAndSavesUser() throws Exception {
        UserService service = new UserService();
        User user = User.builder()
                .id(7L)
                .role(Role.USER)
                .build();
        AtomicReference<User> savedUser = new AtomicReference<>();
        injectRepo(service, createRepoProxy(Optional.of(user), savedUser));

        service.promoteToAdmin(7L);

        assertEquals(Role.ADMIN, user.getRole());
        assertSame(user, savedUser.get());
    }

    @Test
    void promoteToAdminThrowsWhenUserDoesNotExist() throws Exception {
        UserService service = new UserService();
        AtomicReference<User> savedUser = new AtomicReference<>();
        injectRepo(service, createRepoProxy(Optional.empty(), savedUser));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.promoteToAdmin(99L));

        assertEquals("User not found with id: 99", exception.getMessage());
        assertNull(savedUser.get());
    }

    private static void injectRepo(UserService service, UserRepo repo) throws Exception {
        Field repoField = UserService.class.getDeclaredField("userRepo");
        repoField.setAccessible(true);
        repoField.set(service, repo);
    }

    private static UserRepo createRepoProxy(
            Optional<User> user,
            AtomicReference<User> savedUser
    ) {
        return (UserRepo) Proxy.newProxyInstance(
                UserRepo.class.getClassLoader(),
                new Class[]{UserRepo.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> user;
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
}
