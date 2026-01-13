package ru.mifi.financemanager.service;

import java.util.List;
import java.util.Optional;
import ru.mifi.financemanager.domain.User;

/**
 * Интерфейс сервиса аутентификации и управления пользователями.
 *
 * <p>Интерфейс определяет контракт для:
 * <ul>
 *   <li>Регистрации новых пользователей</li>
 *   <li>Аутентификации (вход в систему)</li>
 *   <li>Управления текущей сессией</li>
 * </ul>
 */
public interface AuthService {

    /**
     * Регистрирует нового пользователя в системе.
     */
    User register(String login, String password);

    /**
     * Выполняет аутентификацию пользователя.
     */
    User login(String login, String password);

    /**
     * Завершает сессию текущего пользователя.
     */
    void logout();

    /**
     * Возвращает текущего аутентифицированного пользователя.
     */
    Optional<User> getCurrentUser();

    /**
     * Проверяет, аутентифицирован ли пользователь.
     */
    boolean isAuthenticated();

    /**
     * Находит пользователя по логину (для переводов).
     */
    Optional<User> findUserByLogin(String login);

    /**
     * Возвращает список всех зарегистрированных пользователей.
     */
    List<User> getAllUsers();

    /**
     * Сохраняет все изменения пользователей.
     */
    void saveAll();
}
