package ru.mifi.financemanager.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Тесты для класса Transaction.
 */
@DisplayName("Transaction — тесты транзакции")
class TransactionTest {

    @Test
    @DisplayName("Транзакция дохода создаётся корректно")
    void incomeTransactionShouldBeCreatedCorrectly() {
        Transaction transaction = new Transaction(
                TransactionType.INCOME,
                new BigDecimal("50000"),
                "Зарплата",
                "Основная работа");

        assertNotNull(transaction.getId());
        assertEquals(TransactionType.INCOME, transaction.getType());
        assertEquals(new BigDecimal("50000"), transaction.getAmount());
        assertEquals("Зарплата", transaction.getCategory());
        assertEquals("Основная работа", transaction.getDescription());
        assertNotNull(transaction.getCreatedAt());
        assertTrue(transaction.isIncome());
        assertFalse(transaction.isExpense());
    }

    @Test
    @DisplayName("Транзакция расхода создаётся корректно")
    void expenseTransactionShouldBeCreatedCorrectly() {
        Transaction transaction = new Transaction(
                TransactionType.EXPENSE,
                new BigDecimal("1500.50"),
                "Еда",
                "Продукты");

        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertEquals(new BigDecimal("1500.50"), transaction.getAmount());
        assertTrue(transaction.isExpense());
        assertFalse(transaction.isIncome());
    }

    @Test
    @DisplayName("ID транзакции генерируется автоматически")
    void transactionIdShouldBeGenerated() {
        Transaction t1 = new Transaction(
                TransactionType.INCOME, new BigDecimal("100"), "Тест", "");
        Transaction t2 = new Transaction(
                TransactionType.INCOME, new BigDecimal("100"), "Тест", "");

        assertNotEquals(t1.getId(), t2.getId());
    }

    @Test
    @DisplayName("Дата создания устанавливается автоматически")
    void createdAtShouldBeSetAutomatically() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        Transaction transaction = new Transaction(
                TransactionType.INCOME, new BigDecimal("100"), "Тест", "");

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertTrue(transaction.getCreatedAt().isAfter(before));
        assertTrue(transaction.getCreatedAt().isBefore(after));
    }

    @Test
    @DisplayName("Транзакции с одинаковым ID равны")
    void transactionsWithSameIdShouldBeEqual() {
        LocalDateTime now = LocalDateTime.now();
        Transaction t1 = new Transaction(
                "abc123", TransactionType.INCOME, new BigDecimal("100"), "Тест", "", now);
        Transaction t2 = new Transaction(
                "abc123", TransactionType.EXPENSE, new BigDecimal("200"), "Другой", "Описание", now);

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    @DisplayName("toString выводит информативную строку")
    void toStringShouldBeInformative() {
        Transaction transaction = new Transaction(
                TransactionType.EXPENSE,
                new BigDecimal("1500"),
                "Еда",
                "Продукты");

        String str = transaction.toString();
        assertTrue(str.contains("Еда"));
        assertTrue(str.contains("1500"));
        assertTrue(str.contains("-"));
    }
}
