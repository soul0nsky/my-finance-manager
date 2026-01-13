package ru.mifi.financemanager.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Кошелёк пользователя — хранит все финансовые операции и бюджеты по категориям.
 *
 * <p>Кошелёк является агрегатом, содержащим:
 *
 * <ul>
 *   <li>Список всех транзакций (доходы и расходы)
 *   <li>Карту категорий с установленными бюджетами
 * </ul>
 */
public class Wallet {

    // Список всех транзакций пользователя
    private final List<Transaction> transactions;

    // Бюджеты по категориям: название категории -> лимит
    private final Map<String, BigDecimal> categoryBudgets;

    /** Создаёт пустой кошелёк. */
    public Wallet() {
        this.transactions = new ArrayList<>();
        this.categoryBudgets = new HashMap<>();
    }

    /** Добавляет транзакцию в кошелёк. */
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    /** Возвращает все транзакции. */
    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    /** Возвращает транзакции за указанный период. */
    public List<Transaction> getTransactionsByPeriod(LocalDateTime from, LocalDateTime to) {
        return transactions.stream()
                .filter(t -> !t.getCreatedAt().isBefore(from) && !t.getCreatedAt().isAfter(to))
                .collect(Collectors.toList());
    }

    /** Возвращает транзакции по указанной категории. */
    public List<Transaction> getTransactionsByCategory(String category) {
        return transactions.stream()
                .filter(t -> t.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    /** Вычисляет общую сумму доходов. */
    public BigDecimal getTotalIncome() {
        return transactions.stream()
                .filter(Transaction::isIncome)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Вычисляет общую сумму расходов. */
    public BigDecimal getTotalExpense() {
        return transactions.stream()
                .filter(Transaction::isExpense)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Вычисляет текущий баланс (доходы минус расходы). */
    public BigDecimal getBalance() {
        return getTotalIncome().subtract(getTotalExpense());
    }

    /** Вычисляет сумму расходов по указанной категории. */
    public BigDecimal getExpenseByCategory(String category) {
        return transactions.stream()
                .filter(Transaction::isExpense)
                .filter(t -> t.getCategory().equalsIgnoreCase(category))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Вычисляет сумму доходов по указанной категории. */
    public BigDecimal getIncomeByCategory(String category) {
        return transactions.stream()
                .filter(Transaction::isIncome)
                .filter(t -> t.getCategory().equalsIgnoreCase(category))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Вычисляет сумму расходов по нескольким категориям. */
    public BigDecimal getExpenseByCategories(List<String> categories) {
        List<String> lowerCategories =
                categories.stream().map(String::toLowerCase).collect(Collectors.toList());

        return transactions.stream()
                .filter(Transaction::isExpense)
                .filter(t -> lowerCategories.contains(t.getCategory().toLowerCase()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Устанавливает бюджет для категории. */
    public void setBudget(String category, BigDecimal limit) {
        categoryBudgets.put(category, limit);
    }

    /** Удаляет бюджет для категории. */
    public void removeBudget(String category) {
        categoryBudgets.remove(category);
    }

    /** Возвращает все установленные бюджеты. */
    public Map<String, BigDecimal> getCategoryBudgets() {
        return new HashMap<>(categoryBudgets);
    }

    /** Возвращает бюджет для указанной категории. */
    public BigDecimal getBudget(String category) {
        return categoryBudgets.get(category);
    }

    /** Вычисляет оставшийся бюджет по категории. */
    public BigDecimal getRemainingBudget(String category) {
        BigDecimal budget = categoryBudgets.get(category);
        if (budget == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal spent = getExpenseByCategory(category);
        return budget.subtract(spent);
    }

    /** Вычисляет процент использования бюджета по категории. */
    public double getBudgetUsagePercent(String category) {
        BigDecimal budget = categoryBudgets.get(category);
        if (budget == null || budget.compareTo(BigDecimal.ZERO) == 0) {
            return -1;
        }
        BigDecimal spent = getExpenseByCategory(category);
        // Используем doubleValue() только для вычисления процента (не для денег)
        return spent.doubleValue() / budget.doubleValue() * 100;
    }

    /** Возвращает список всех уникальных категорий из транзакций. */
    public List<String> getAllCategories() {
        return transactions.stream()
                .map(Transaction::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** Возвращает список категорий с установленными бюджетами. */
    public List<String> getCategoriesWithBudget() {
        return new ArrayList<>(categoryBudgets.keySet());
    }
}
