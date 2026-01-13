package ru.mifi.financemanager.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import ru.mifi.financemanager.config.AppConfig;
import ru.mifi.financemanager.domain.User;

/**
 * Реализация репозитория пользователей с хранением в JSON файле (имитация БД).
 *
 * <p>Используем Gson для сериализации/десериализации. ConcurrentHashMap обеспечивает
 * потокобезопасность при работе с несколькими пользователями.
 */
public class JsonUserRepository implements UserRepository {

    private final Map<String, User> users;

    private final Gson gson;

    private final Path dataFilePath;

    /** Создаёт репозиторий с настройками из AppConfig. */
    public JsonUserRepository(AppConfig config) {
        this.users = new ConcurrentHashMap<>();
        this.gson = createGson();
        this.dataFilePath = Paths.get(config.getDataFilePath());
    }

    public JsonUserRepository(String dataFilePath) {
        this.users = new ConcurrentHashMap<>();
        this.gson = createGson();
        this.dataFilePath = Paths.get(dataFilePath);
    }

    private Gson createGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    @Override
    public void save(User user) {
        users.put(user.getLogin().toLowerCase(), user);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return Optional.ofNullable(users.get(login.toLowerCase()));
    }

    @Override
    public boolean existsByLogin(String login) {
        return users.containsKey(login.toLowerCase());
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public boolean deleteByLogin(String login) {
        return users.remove(login.toLowerCase()) != null;
    }

    @Override
    public void flush() {
        try {
            Path parentDir = dataFilePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Сериализуем и записываем в файл
            try (Writer writer = Files.newBufferedWriter(dataFilePath, StandardCharsets.UTF_8)) {
                gson.toJson(new ArrayList<>(users.values()), writer);
            }
        } catch (IOException e) {
            System.err.println("Ошибка сохранения данных: " + e.getMessage());
        }
    }

    @Override
    public void load() {
        if (!Files.exists(dataFilePath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(dataFilePath, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<User>>() {}.getType();
            List<User> loadedUsers = gson.fromJson(reader, listType);

            if (loadedUsers != null) {
                users.clear();
                for (User user : loadedUsers) {
                    users.put(user.getLogin().toLowerCase(), user);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки данных: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка парсинга файла данных: " + e.getMessage());
        }
    }

    /** Возвращает количество пользователей в репозитории. */
    public int count() {
        return users.size();
    }

    /** Очищает репозиторий. */
    public void clear() {
        users.clear();
    }
}
