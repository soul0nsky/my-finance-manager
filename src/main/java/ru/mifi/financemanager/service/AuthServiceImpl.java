package ru.mifi.financemanager.service;

import java.util.List;
import java.util.Optional;
import ru.mifi.financemanager.domain.User;
import ru.mifi.financemanager.exception.InvalidCredentialsException;
import ru.mifi.financemanager.exception.ValidationException;
import ru.mifi.financemanager.repository.UserRepository;

/**
 * Реализация сервиса аутентификации и управления пользователями.
 *
 * <p>Этот класс инкапсулирует всю логику работы с пользователями:
 * <ul>
 *   <li>Регистрация с валидацией уникальности логина</li>
 *   <li>Аутентификация с проверкой пароля</li>
 *   <li>Управление текущей сессией</li>
 * </ul>
 */
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private User currentUser;

    /**
     * Создаёт сервис аутентификации с указанным репозиторием.
     */
    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.currentUser = null;
    }

    /**
     * Регистрирует нового пользователя.
     */
    @Override
    public User register(String login, String password) {
        // Валидация логина
        if (login == null || login.trim().isEmpty()) {
            throw new ValidationException("логин", "не может быть пустым");
        }

        // Валидация пароля
        if (password == null || password.isEmpty()) {
            throw new ValidationException("пароль", "не может быть пустым");
        }

        String normalizedLogin = login.trim();

        // Проверка уникальности логина
        if (userRepository.existsByLogin(normalizedLogin)) {
            throw new ValidationException(
                    "логин", "пользователь с логином '" + normalizedLogin + "' уже существует");
        }

        // Создаём и сохраняем пользователя
        User newUser = new User(normalizedLogin, password);
        userRepository.save(newUser);

        return newUser;
    }

    /**
     * Выполняет вход пользователя в систему.
     */
    @Override
    public User login(String login, String password) {
        if (login == null || login.trim().isEmpty()) {
            throw new InvalidCredentialsException("Логин не может быть пустым");
        }

        if (password == null || password.isEmpty()) {
            throw new InvalidCredentialsException("Пароль не может быть пустым");
        }

        Optional<User> userOpt = userRepository.findByLogin(login.trim());

        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        User user = userOpt.get();

        if (!user.checkPassword(password)) {
            throw new InvalidCredentialsException();
        }

        this.currentUser = user;

        return user;
    }

    /**
     * Завершает сессию текущего пользователя.
     */
    @Override
    public void logout() {
        if (currentUser != null) {
            userRepository.save(currentUser);
            userRepository.flush();
            currentUser = null;
        }
    }

    @Override
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    @Override
    public boolean isAuthenticated() {
        return currentUser != null;
    }

    /**
     * Находит пользователя по логину для переводов.
     */
    @Override
    public Optional<User> findUserByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Сохраняет все изменения в репозиторий.
     */
    @Override
    public void saveAll() {
        if (currentUser != null) {
            userRepository.save(currentUser);
        }
        userRepository.flush();
    }
}
