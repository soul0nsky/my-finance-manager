package ru.mifi.financemanager.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mifi.financemanager.domain.Transaction;
import ru.mifi.financemanager.domain.User;
import ru.mifi.financemanager.repository.JsonUserRepository;
import ru.mifi.financemanager.repository.UserRepository;
import ru.mifi.financemanager.service.AuthService;
import ru.mifi.financemanager.service.AuthServiceImpl;
import ru.mifi.financemanager.service.FinanceService;
import ru.mifi.financemanager.service.FinanceServiceImpl;
import ru.mifi.financemanager.service.NotificationService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/**
 * Интеграционные тесты для проверки взаимодействия компонентов.
 *
 * <p>Эти тесты проверяют полные сценарии использования приложения,
 * включая взаимодействие между сервисами, репозиторием и доменными объектами.
 */
@DisplayName("Интеграционные тесты")
class FinanceIntegrationTest {

    private UserRepository userRepository;
    private AuthService authService;
    private FinanceService financeService;
    private NotificationService notificationService;
    private static final String TEST_FILE = "test_integration.json";

    @BeforeEach
    void setUp() {
        userRepository = new JsonUserRepository(TEST_FILE);
        ((JsonUserRepository) userRepository).clear();

        notificationService = new NotificationService(false);
        authService = new AuthServiceImpl(userRepository);
        financeService = new FinanceServiceImpl(authService, notificationService);
    }

    @AfterAll
    static void cleanup() {
        try {
            Files.deleteIfExists(Path.of(TEST_FILE));
        } catch (IOException e) {
            System.err.println("Не удалось удалить тестовый файл: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Полный цикл: регистрация, вход, операции, выход")
    void fullUserWorkflowShouldWork() {
        // 1. Регистрация нового пользователя
        User user = authService.register("ivan", "password123");
        assertNotNull(user);
        assertEquals("ivan", user.getLogin());

        // 2. Вход в систему
        User loggedIn = authService.login("ivan", "password123");
        assertTrue(authService.isAuthenticated());
        assertEquals("ivan", loggedIn.getLogin());

        // 3. Добавление доходов
        financeService.addIncome(new BigDecimal("50000"), "Зарплата", "За январь");
        financeService.addIncome(new BigDecimal("10000"), "Бонус", "Новогодний");

        // 4. Добавление расходов
        financeService.addExpense(new BigDecimal("5000"), "Еда", "Продукты на неделю");
        financeService.addExpense(new BigDecimal("3000"), "Транспорт", "Метро и такси");

        // 5. Проверка баланса
        assertEquals(new BigDecimal("52000"), financeService.getBalance());
        assertEquals(new BigDecimal("60000"), financeService.getTotalIncome());
        assertEquals(new BigDecimal("8000"), financeService.getTotalExpense());

        // 6. Проверка истории операций
        List<Transaction> transactions = financeService.getAllTransactions();
        assertEquals(4, transactions.size());

        // 7. Выход и сохранение
        authService.logout();
        assertFalse(authService.isAuthenticated());
    }

    @Test
    @DisplayName("Сценарий: установка и контроль бюджета")
    void budgetWorkflowShouldWork() {
        // Регистрация и вход
        authService.register("maria", "pass");
        authService.login("maria", "pass");

        // Добавляем доход
        financeService.addIncome(new BigDecimal("100000"), "Зарплата", "");

        // Устанавливаем бюджет на еду
        financeService.setBudget("Еда", new BigDecimal("15000"));

        // Тратим часть бюджета
        financeService.addExpense(new BigDecimal("5000"), "Еда", "Продукты");
        assertEquals(new BigDecimal("10000"), financeService.getRemainingBudget("Еда"));

        // Тратим ещё
        financeService.addExpense(new BigDecimal("8000"), "Еда", "Ресторан");
        assertEquals(new BigDecimal("2000"), financeService.getRemainingBudget("Еда"));

        // Превышаем бюджет
        financeService.addExpense(new BigDecimal("5000"), "Еда", "Доставка");
        assertEquals(new BigDecimal("-3000"), financeService.getRemainingBudget("Еда"));
    }

    @Test
    @DisplayName("Сценарий: перевод между пользователями")
    void transferBetweenUsersShouldWork() {
        // Регистрируем двух пользователей
        authService.register("sender", "pass1");
        authService.register("receiver", "pass2");

        // Входим как отправитель
        authService.login("sender", "pass1");

        // Добавляем доход отправителю
        financeService.addIncome(new BigDecimal("10000"), "Зарплата", "");

        // Находим получателя
        User receiver = authService.findUserByLogin("receiver").orElseThrow();

        // Выполняем перевод
        boolean success = financeService.transfer(receiver, new BigDecimal("3000"), "Возврат долга");
        assertTrue(success);

        // Проверяем баланс отправителя
        assertEquals(new BigDecimal("7000"), financeService.getBalance());

        // Проверяем баланс получателя
        assertEquals(new BigDecimal("3000"), receiver.getWallet().getBalance());

        // У получателя должен появиться доход
        assertEquals(1, receiver.getWallet().getTransactions().size());
        assertTrue(receiver.getWallet().getTransactions().get(0).isIncome());
    }

    @Test
    @DisplayName("Сценарий: несколько пользователей с независимыми данными")
    void multipleUsersShouldHaveIndependentData() {
        // Регистрируем двух пользователей
        authService.register("user1", "pass1");
        authService.register("user2", "pass2");

        // Работаем под первым пользователем
        authService.login("user1", "pass1");
        financeService.addIncome(new BigDecimal("50000"), "Зарплата", "");
        financeService.addExpense(new BigDecimal("10000"), "Еда", "");
        assertEquals(new BigDecimal("40000"), financeService.getBalance());
        authService.logout();

        // Работаем под вторым пользователем
        authService.login("user2", "pass2");
        financeService.addIncome(new BigDecimal("30000"), "Фриланс", "");
        assertEquals(new BigDecimal("30000"), financeService.getBalance());

        // Проверяем что данные первого пользователя не изменились
        List<Transaction> user2Transactions = financeService.getAllTransactions();
        assertEquals(1, user2Transactions.size());
        assertEquals("Фриланс", user2Transactions.get(0).getCategory());
    }

    @Test
    @DisplayName("Сценарий: данные сохраняются и загружаются корректно")
    void dataShouldPersistAcrossSessions() {
        // Первая сессия: создаём данные
        authService.register("persistent", "pass");
        authService.login("persistent", "pass");
        financeService.addIncome(new BigDecimal("25000"), "Зарплата", "");
        financeService.setBudget("Еда", new BigDecimal("5000"));
        authService.logout();
        authService.saveAll();

        // "Перезагружаем" приложение — создаём новый репозиторий и загружаем данные
        UserRepository newRepository = new JsonUserRepository("test_integration.json");
        newRepository.load();
        AuthService newAuthService = new AuthServiceImpl(newRepository);
        FinanceService newFinanceService = new FinanceServiceImpl(newAuthService, notificationService);

        // Вторая сессия: проверяем что данные сохранились
        newAuthService.login("persistent", "pass");
        assertEquals(new BigDecimal("25000"), newFinanceService.getBalance());
        assertEquals(new BigDecimal("5000"), newFinanceService.getAllBudgets().get("Еда"));
    }
}
