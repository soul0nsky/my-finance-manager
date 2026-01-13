package ru.mifi.financemanager.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import ru.mifi.financemanager.domain.User;
import ru.mifi.financemanager.exception.InvalidCredentialsException;
import ru.mifi.financemanager.exception.ValidationException;
import ru.mifi.financemanager.repository.JsonUserRepository;
import ru.mifi.financemanager.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/**
 * Тесты для сервиса аутентификации.
 *
 * Авторизация пользователей — тестируем вход, регистрацию.
 */
@DisplayName("AuthService — тесты аутентификации")
class AuthServiceTest {

    private AuthService authService;
    private UserRepository userRepository;
    private static final String TEST_FILE = "test_users.json";

    @BeforeEach
    void setUp() {
        userRepository = new JsonUserRepository(TEST_FILE);
        ((JsonUserRepository) userRepository).clear();
        authService = new AuthServiceImpl(userRepository);
    }

    @AfterAll
    static void cleanup() {
        try {
            Files.deleteIfExists(Path.of(TEST_FILE));
        } catch (IOException e) {
            System.err.println("Не удалось удалить тестовый файл: " + e.getMessage());
        }
    }

    @Nested
    @DisplayName("Регистрация")
    class RegisterTests {

        @Test
        @DisplayName("Успешная регистрация нового пользователя")
        void registerShouldCreateNewUser() {
            User user = authService.register("testuser", "password123");

            assertNotNull(user);
            assertEquals("testuser", user.getLogin());
            assertTrue(userRepository.existsByLogin("testuser"));
        }

        @Test
        @DisplayName("Регистрация с существующим логином выбрасывает исключение")
        void registerWithExistingLoginShouldThrowException() {
            authService.register("existinguser", "pass1");

            assertThrows(ValidationException.class, () -> {
                authService.register("existinguser", "pass2");
            });
        }

        @Test
        @DisplayName("Регистрация с пустым логином выбрасывает исключение")
        void registerWithEmptyLoginShouldThrowException() {
            assertThrows(ValidationException.class, () -> {
                authService.register("", "password");
            });
        }

        @Test
        @DisplayName("Регистрация с пустым паролем выбрасывает исключение")
        void registerWithEmptyPasswordShouldThrowException() {
            assertThrows(ValidationException.class, () -> {
                authService.register("user", "");
            });
        }

        @Test
        @DisplayName("Логин нормализуется (trim)")
        void registerShouldTrimLogin() {
            User user = authService.register("  testuser  ", "password");

            assertEquals("testuser", user.getLogin());
        }
    }

    @Nested
    @DisplayName("Вход")
    class LoginTests {

        @BeforeEach
        void registerTestUser() {
            authService.register("testuser", "correctpassword");
        }

        @Test
        @DisplayName("Успешный вход с правильными данными")
        void loginWithCorrectCredentialsShouldWork() {
            User user = authService.login("testuser", "correctpassword");

            assertNotNull(user);
            assertEquals("testuser", user.getLogin());
            assertTrue(authService.isAuthenticated());
        }

        @Test
        @DisplayName("Вход с неверным паролем выбрасывает исключение")
        void loginWithWrongPasswordShouldThrowException() {
            assertThrows(InvalidCredentialsException.class, () -> {
                authService.login("testuser", "wrongpassword");
            });
        }

        @Test
        @DisplayName("Вход с несуществующим логином выбрасывает исключение")
        void loginWithNonExistentUserShouldThrowException() {
            assertThrows(InvalidCredentialsException.class, () -> {
                authService.login("nonexistent", "password");
            });
        }

        @Test
        @DisplayName("Вход регистронезависим по логину")
        void loginShouldBeCaseInsensitive() {
            User user = authService.login("TestUser", "correctpassword");

            assertNotNull(user);
            assertTrue(authService.isAuthenticated());
        }
    }

    @Nested
    @DisplayName("Сессия")
    class SessionTests {

        @Test
        @DisplayName("getCurrentUser возвращает пустой Optional до входа")
        void getCurrentUserShouldBeEmptyBeforeLogin() {
            assertTrue(authService.getCurrentUser().isEmpty());
            assertFalse(authService.isAuthenticated());
        }

        @Test
        @DisplayName("getCurrentUser возвращает пользователя после входа")
        void getCurrentUserShouldReturnUserAfterLogin() {
            authService.register("user", "pass");
            authService.login("user", "pass");

            assertTrue(authService.getCurrentUser().isPresent());
            assertEquals("user", authService.getCurrentUser().get().getLogin());
        }

        @Test
        @DisplayName("logout сбрасывает текущего пользователя")
        void logoutShouldClearCurrentUser() {
            authService.register("user", "pass");
            authService.login("user", "pass");

            authService.logout();

            assertFalse(authService.isAuthenticated());
            assertTrue(authService.getCurrentUser().isEmpty());
        }
    }
}
