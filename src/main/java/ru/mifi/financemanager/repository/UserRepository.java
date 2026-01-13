package ru.mifi.financemanager.repository;

import java.util.List;
import java.util.Optional;
import ru.mifi.financemanager.domain.User;

/** Интерфейс репозитория для работы с пользователями. */
public interface UserRepository {

    /** Сохраняет пользователя в хранилище. */
    void save(User user);

    /** Находит пользователя по логину. */
    Optional<User> findByLogin(String login);

    /** Проверяет существование пользователя с указанным логином. */
    boolean existsByLogin(String login);

    /** Возвращает список всех пользователей. */
    List<User> findAll();

    /** Удаляет пользователя по логину. */
    boolean deleteByLogin(String login);

    /** Сохраняет все изменения в постоянное хранилище. */
    void flush();

    /** Загружает данные из постоянного хранилища. */
    void load();
}
