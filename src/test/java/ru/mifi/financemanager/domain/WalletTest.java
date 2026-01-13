package ru.mifi.financemanager.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Тесты для класса Wallet.
 *
 * <p>Покрываем:
 * - Добавление транзакций
 * - Расчёт баланса
 * - Работу с бюджетами
 * - Фильтрацию по категориям и периодам
 */
@DisplayName("Wallet — тесты кошелька")
class WalletTest {

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
    }

    @Nested
    @DisplayName("Транзакции")
    class TransactionTests {

        @Test
        @DisplayName("Новый кошелёк должен быть пустым")
        void newWalletShouldBeEmpty() {
            assertTrue(wallet.getTransactions().isEmpty());
            assertEquals(BigDecimal.ZERO, wallet.getBalance());
        }

        @Test
        @DisplayName("Добавление дохода увеличивает баланс")
        void addIncomeShouldIncreaseBalance() {
            Transaction income = new Transaction(
                    TransactionType.INCOME,
                    new BigDecimal("5000"),
                    "Зарплата",
                    "Аванс");

            wallet.addTransaction(income);

            assertEquals(new BigDecimal("5000"), wallet.getBalance());
            assertEquals(1, wallet.getTransactions().size());
        }

        @Test
        @DisplayName("Добавление расхода уменьшает баланс")
        void addExpenseShouldDecreaseBalance() {
            wallet.addTransaction(new Transaction(
                    TransactionType.INCOME, new BigDecimal("10000"), "Зарплата", ""));

            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("3000"), "Еда", "Продукты"));

            assertEquals(new BigDecimal("7000"), wallet.getBalance());
        }

        @Test
        @DisplayName("Баланс может быть отрицательным")
        void balanceCanBeNegative() {
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("1000"), "Еда", ""));

            assertEquals(new BigDecimal("-1000"), wallet.getBalance());
        }
    }


    @Nested
    @DisplayName("Статистика")
    class StatisticsTests {

        @BeforeEach
        void setUpTransactions() {
            wallet.addTransaction(new Transaction(
                    TransactionType.INCOME, new BigDecimal("50000"), "Зарплата", ""));
            wallet.addTransaction(new Transaction(
                    TransactionType.INCOME, new BigDecimal("10000"), "Бонус", ""));

            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("5000"), "Еда", ""));
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("3000"), "Транспорт", ""));
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("2000"), "Еда", ""));
        }

        @Test
        @DisplayName("Общий доход считается корректно")
        void getTotalIncomeShouldSumAllIncomes() {
            assertEquals(new BigDecimal("60000"), wallet.getTotalIncome());
        }

        @Test
        @DisplayName("Общий расход считается корректно")
        void getTotalExpenseShouldSumAllExpenses() {
            assertEquals(new BigDecimal("10000"), wallet.getTotalExpense());
        }

        @Test
        @DisplayName("Расход по категории считается корректно")
        void getExpenseByCategoryShouldWork() {
            assertEquals(new BigDecimal("7000"), wallet.getExpenseByCategory("Еда"));
            assertEquals(new BigDecimal("3000"), wallet.getExpenseByCategory("Транспорт"));
        }

        @Test
        @DisplayName("Расход по несуществующей категории равен нулю")
        void getExpenseByNonExistentCategoryShouldReturnZero() {
            assertEquals(BigDecimal.ZERO, wallet.getExpenseByCategory("Несуществующая"));
        }

        @Test
        @DisplayName("Список категорий формируется корректно")
        void getAllCategoriesShouldReturnUniqueCategories() {
            List<String> categories = wallet.getAllCategories();

            assertEquals(4, categories.size());
            assertTrue(categories.contains("Зарплата"));
            assertTrue(categories.contains("Бонус"));
            assertTrue(categories.contains("Еда"));
            assertTrue(categories.contains("Транспорт"));
        }
    }

    @Nested
    @DisplayName("Бюджеты")
    class BudgetTests {

        @Test
        @DisplayName("Установка бюджета сохраняется")
        void setBudgetShouldWork() {
            wallet.setBudget("Еда", new BigDecimal("10000"));

            assertEquals(new BigDecimal("10000"), wallet.getBudget("Еда"));
        }

        @Test
        @DisplayName("Оставшийся бюджет вычисляется корректно")
        void getRemainingBudgetShouldWork() {
            wallet.setBudget("Еда", new BigDecimal("10000"));
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("3000"), "Еда", ""));

            assertEquals(new BigDecimal("7000"), wallet.getRemainingBudget("Еда"));
        }

        @Test
        @DisplayName("Превышение бюджета даёт отрицательный остаток")
        void budgetExceededShouldReturnNegativeRemaining() {
            wallet.setBudget("Еда", new BigDecimal("5000"));
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("7000"), "Еда", ""));

            assertEquals(new BigDecimal("-2000"), wallet.getRemainingBudget("Еда"));
        }

        @Test
        @DisplayName("Процент использования бюджета вычисляется корректно")
        void getBudgetUsagePercentShouldWork() {
            wallet.setBudget("Еда", new BigDecimal("10000"));
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("8000"), "Еда", ""));

            assertEquals(80.0, wallet.getBudgetUsagePercent("Еда"), 0.01);
        }

        @Test
        @DisplayName("Удаление бюджета работает")
        void removeBudgetShouldWork() {
            wallet.setBudget("Еда", new BigDecimal("10000"));

            wallet.removeBudget("Еда");

            assertNull(wallet.getBudget("Еда"));
        }
    }

    @Nested
    @DisplayName("Фильтрация")
    class FilterTests {

        @Test
        @DisplayName("Фильтрация по категории работает")
        void getTransactionsByCategoryShouldWork() {
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("1000"), "Еда", ""));
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("2000"), "Транспорт", ""));
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("1500"), "Еда", ""));

            List<Transaction> foodTransactions = wallet.getTransactionsByCategory("Еда");

            assertEquals(2, foodTransactions.size());
        }

        @Test
        @DisplayName("Расход по нескольким категориям считается корректно")
        void getExpenseByCategoriesShouldWork() {
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("1000"), "Еда", ""));
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("2000"), "Транспорт", ""));
            wallet.addTransaction(new Transaction(
                    TransactionType.EXPENSE, new BigDecimal("3000"), "Развлечения", ""));

            BigDecimal total = wallet.getExpenseByCategories(List.of("Еда", "Транспорт"));

            assertEquals(new BigDecimal("3000"), total);
        }
    }
}
