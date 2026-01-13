package ru.mifi.financemanager.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Модель категории расходов/доходов с возможностью установки бюджета.
 *
 * <p>Каждая категория может иметь установленный лимит бюджета. Если бюджет не установлен (null),
 * категория используется только для группировки.
 */
public class Category {

    private final String name;

    private BigDecimal budgetLimit;

    /** Создаёт категорию без установленного бюджета. */
    public Category(String name) {
        this.name = name;
        this.budgetLimit = null;
    }

    /** Создаёт категорию с установленным бюджетом. */
    public Category(String name, BigDecimal budgetLimit) {
        this.name = name;
        this.budgetLimit = budgetLimit;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    /** Устанавливает лимит бюджета для категории. */
    public void setBudgetLimit(BigDecimal budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    /** Проверяет, установлен ли бюджет для категории. */
    public boolean hasBudget() {
        return budgetLimit != null && budgetLimit.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;

        return name.equalsIgnoreCase(category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    @Override
    public String toString() {
        if (hasBudget()) {
            return String.format("%s (бюджет: %.2f)", name, budgetLimit);
        }
        return name;
    }
}
