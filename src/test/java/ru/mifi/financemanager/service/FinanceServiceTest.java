package ru.mifi.financemanager.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.*;
import ru.mifi.financemanager.domain.Transaction;
import ru.mifi.financemanager.exception.ValidationException;
import ru.mifi.financemanager.repository.JsonUserRepository;
import ru.mifi.financemanager.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/**
 * Тесты для сервиса финансовых операций.
 *
 * Доходы и расходы — тестируем добавление операций.
 * Категории и бюджеты — тестируем работу с бюджетами.
 */
@DisplayName("FinanceService — тесты финансовых операций")
class FinanceServiceTest {

    private AuthService authService;
    private FinanceService financeService;
    private NotificationService notificationService;
    private static final String TEST_FILE = "test_finance.json";

    @BeforeEach
    void setUp() {
        UserRepository userRepository = new JsonUserRepository(TEST_FILE);
        ((JsonUserRepository) userRepository).clear();

        notificationService = new NotificationService(false); // Без цветов в тестах
        authService = new AuthServiceImpl(userRepository);
        financeService = new FinanceServiceImpl(authService, notificationService);

        authService.register("testuser", "pass");
        authService.login("testuser", "pass");
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
    @DisplayName("Доходы")
    class IncomeTests {

        @Test
        @DisplayName("Добавление дохода увеличивает баланс")
        void addIncomeShouldIncreaseBalance() {
            financeService.addIncome(new BigDecimal("50000"), "Зарплата", "Аванс");

            assertEquals(new BigDecimal("50000"), financeService.getBalance());
            assertEquals(new BigDecimal("50000"), financeService.getTotalIncome());
        }

        @Test
        @DisplayName("Добавление дохода возвращает транзакцию")
        void addIncomeShouldReturnTransaction() {
            Transaction transaction = financeService.addIncome(
                    new BigDecimal("10000"), "Бонус", "Квартальный");

            assertNotNull(transaction);
            assertTrue(transaction.isIncome());
            assertEquals(new BigDecimal("10000"), transaction.getAmount());
            assertEquals("Бонус", transaction.getCategory());
        }

        @Test
        @DisplayName("Добавление дохода с нулевой суммой выбрасывает исключение")
        void addIncomeWithZeroAmountShouldThrowException() {
            assertThrows(ValidationException.class, () -> {
                financeService.addIncome(BigDecimal.ZERO, "Тест", "");
            });
        }

        @Test
        @DisplayName("Добавление дохода с отрицательной суммой выбрасывает исключение")
        void addIncomeWithNegativeAmountShouldThrowException() {
            assertThrows(ValidationException.class, () -> {
                financeService.addIncome(new BigDecimal("-100"), "Тест", "");
            });
        }

        @Test
        @DisplayName("Добавление дохода с пустой категорией выбрасывает исключение")
        void addIncomeWithEmptyCategoryShouldThrowException() {
            assertThrows(ValidationException.class, () -> {
                financeService.addIncome(new BigDecimal("1000"), "", "");
            });
        }
    }

    @Nested
    @DisplayName("Расходы")
    class ExpenseTests {

        @Test
        @DisplayName("Добавление расхода уменьшает баланс")
        void addExpenseShouldDecreaseBalance() {
            financeService.addIncome(new BigDecimal("10000"), "Зарплата", "");

            financeService.addExpense(new BigDecimal("3000"), "Еда", "Продукты");

            assertEquals(new BigDecimal("7000"), financeService.getBalance());
            assertEquals(new BigDecimal("3000"), financeService.getTotalExpense());
        }

        @Test
        @DisplayName("Расход может сделать баланс отрицательным")
        void expenseCanMakeBalanceNegative() {
            financeService.addExpense(new BigDecimal("5000"), "Еда", "");

            assertEquals(new BigDecimal("-5000"), financeService.getBalance());
        }
    }

    @Nested
    @DisplayName("Бюджеты")
    class BudgetTests {

        @Test
        @DisplayName("Установка бюджета работает")
        void setBudgetShouldWork() {
            financeService.setBudget("Еда", new BigDecimal("10000"));

            Map<String, BigDecimal> budgets = financeService.getAllBudgets();
            assertEquals(new BigDecimal("10000"), budgets.get("Еда"));
        }

        @Test
        @DisplayName("Оставшийся бюджет вычисляется корректно")
        void getRemainingBudgetShouldWork() {
            financeService.setBudget("Еда", new BigDecimal("10000"));
            financeService.addExpense(new BigDecimal("3000"), "Еда", "");

            assertEquals(new BigDecimal("7000"), financeService.getRemainingBudget("Еда"));
        }

        @Test
        @DisplayName("Удаление бюджета работает")
        void removeBudgetShouldWork() {
            financeService.setBudget("Еда", new BigDecimal("10000"));

            financeService.removeBudget("Еда");

            assertFalse(financeService.getAllBudgets().containsKey("Еда"));
        }
    }

    @Nested
    @DisplayName("Статистика")
    class StatisticsTests {

        @BeforeEach
        void setupTransactions() {
            financeService.addIncome(new BigDecimal("50000"), "Зарплата", "");
            financeService.addIncome(new BigDecimal("10000"), "Бонус", "");
            financeService.addExpense(new BigDecimal("5000"), "Еда", "");
            financeService.addExpense(new BigDecimal("3000"), "Транспорт", "");
        }

        @Test
        @DisplayName("Общий доход считается корректно")
        void getTotalIncomeShouldWork() {
            assertEquals(new BigDecimal("60000"), financeService.getTotalIncome());
        }

        @Test
        @DisplayName("Общий расход считается корректно")
        void getTotalExpenseShouldWork() {
            assertEquals(new BigDecimal("8000"), financeService.getTotalExpense());
        }

        @Test
        @DisplayName("Баланс считается корректно")
        void getBalanceShouldWork() {
            assertEquals(new BigDecimal("52000"), financeService.getBalance());
        }

        @Test
        @DisplayName("Статистика по категориям расходов работает")
        void getExpensesByCategoryShouldWork() {
            Map<String, BigDecimal> expenses = financeService.getExpensesByCategory();

            assertEquals(new BigDecimal("5000"), expenses.get("Еда"));
            assertEquals(new BigDecimal("3000"), expenses.get("Транспорт"));
        }
    }

    @Test
    @DisplayName("Операции без авторизации выбрасывают исключение")
    void operationsWithoutAuthShouldThrowException() {
        authService.logout();

        assertThrows(ValidationException.class, () -> {
            financeService.addIncome(new BigDecimal("100"), "Тест", "");
        });
    }
}
