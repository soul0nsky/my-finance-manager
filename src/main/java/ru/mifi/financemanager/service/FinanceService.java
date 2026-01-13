package ru.mifi.financemanager.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import ru.mifi.financemanager.domain.Transaction;
import ru.mifi.financemanager.domain.User;

/**
 * Интерфейс сервиса финансовых операций.
 *
 * <p>Этот интерфейс определяет основные финансовые операции:
 *
 * <ul>
 *   <li>Добавление доходов и расходов
 *   <li>Управление бюджетами по категориям
 *   <li>Получение статистики и аналитики
 *   <li>Переводы между пользователями
 * </ul>
 */
public interface FinanceService {
    /** Добавляет доход в кошелёк текущего пользователя. */
    Transaction addIncome(BigDecimal amount, String category, String description);

    /** Добавляет расход в кошелёк текущего пользователя. */
    Transaction addExpense(BigDecimal amount, String category, String description);

    /** Возвращает все транзакции текущего пользователя. */
    List<Transaction> getAllTransactions();

    /** Возвращает транзакции за указанный период. */
    List<Transaction> getTransactionsByPeriod(LocalDateTime from, LocalDateTime to);

    /** Возвращает транзакции по категории. */
    List<Transaction> getTransactionsByCategory(String category);

    /** Устанавливает бюджет для категории. */
    void setBudget(String category, BigDecimal limit);

    /** Удаляет бюджет для категории. */
    void removeBudget(String category);

    /** Возвращает все установленные бюджеты. */
    Map<String, BigDecimal> getAllBudgets();

    /** Возвращает оставшийся бюджет по категории. */
    BigDecimal getRemainingBudget(String category);

    /** Возвращает общую сумму доходов. */
    BigDecimal getTotalIncome();

    /** Возвращает общую сумму расходов. */
    BigDecimal getTotalExpense();

    /** Возвращает текущий баланс. */
    BigDecimal getBalance();

    /** Возвращает статистику по категориям расходов. */
    Map<String, BigDecimal> getExpensesByCategory();

    /** Возвращает статистику по категориям доходов. */
    Map<String, BigDecimal> getIncomesByCategory();

    /** Вычисляет сумму расходов по нескольким категориям. */
    BigDecimal getExpenseByCategories(List<String> categories);

    /** Возвращает список всех использованных категорий. */
    List<String> getAllCategories();

    /** Выполняет перевод между пользователями. */
    boolean transfer(User toUser, BigDecimal amount, String description);
}
