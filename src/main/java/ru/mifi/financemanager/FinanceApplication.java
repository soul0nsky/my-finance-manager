package ru.mifi.financemanager;

import ru.mifi.financemanager.cli.ConsoleApp;
import ru.mifi.financemanager.config.AppConfig;
import ru.mifi.financemanager.repository.JsonUserRepository;
import ru.mifi.financemanager.repository.UserRepository;
import ru.mifi.financemanager.service.AuthService;
import ru.mifi.financemanager.service.AuthServiceImpl;
import ru.mifi.financemanager.service.FinanceService;
import ru.mifi.financemanager.service.FinanceServiceImpl;
import ru.mifi.financemanager.service.NotificationService;

/**
 * Главный класс приложения — точка входа.
 *
 * <p>Этот класс отвечает за:
 *
 * <ul>
 *   <li>Инициализацию конфигурации
 *   <li>Создание и связывание компонентов (Dependency Injection)
 *   <li>Загрузку данных из хранилища
 *   <li>Запуск консольного интерфейса
 * </ul>
 */
public class FinanceApplication {
    public static void main(String[] args) {

        System.out.println("Default Charset: " + java.nio.charset.Charset.defaultCharset());
        System.out.println("file.encoding: " + System.getProperty("file.encoding"));

        // 1. Загружаем конфигурацию
        AppConfig config = new AppConfig();

        // 2. Создаём репозиторий и загружаем данные
        UserRepository userRepository = new JsonUserRepository(config);
        userRepository.load();

        // 3. Создаём сервисы (Dependency Injection через конструктор)
        NotificationService notificationService = new NotificationService();
        AuthService authService = new AuthServiceImpl(userRepository);
        FinanceService financeService = new FinanceServiceImpl(authService, notificationService);

        // 4. Создаём и запускаем консольное приложение
        ConsoleApp consoleApp = new ConsoleApp(authService, financeService, notificationService);
        consoleApp.run();
    }
}
