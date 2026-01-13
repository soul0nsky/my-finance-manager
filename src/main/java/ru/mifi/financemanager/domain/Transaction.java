package ru.mifi.financemanager.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Модель финансовой операции (транзакции).
 *
 * <p>Используем BigDecimal вместо double для точных финансовых расчётов. Это стандарт в enterprise
 * Java — исключает проблемы с плавающей точкой (например, 0.1 + 0.2 != 0.3 в double).
 */
public class Transaction {

    private final String id;

    // Тип операции: INCOME (доход) или EXPENSE (расход)
    private final TransactionType type;

    private final BigDecimal amount;

    private final String category;

    private final String description;

    private final LocalDateTime createdAt;

    /** Создаёт новую транзакцию с автоматической генерацией ID и timestamp. */
    public Transaction(
            TransactionType type, BigDecimal amount, String category, String description) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    /** Конструктор для восстановления из JSON (все поля явно указаны). */
    public Transaction(
            String id,
            TransactionType type,
            BigDecimal amount,
            String category,
            String description,
            LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Проверяет, является ли транзакция доходом. */
    public boolean isIncome() {
        return type == TransactionType.INCOME;
    }

    /** Проверяет, является ли транзакция расходом. */
    public boolean isExpense() {
        return type == TransactionType.EXPENSE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
                "[%s] %s: %s %.2f (%s)",
                id, type == TransactionType.INCOME ? "+" : "-", category, amount, description);
    }
}
