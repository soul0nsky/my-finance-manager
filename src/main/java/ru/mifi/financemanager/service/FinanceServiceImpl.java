package ru.mifi.financemanager.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.mifi.financemanager.domain.Transaction;
import ru.mifi.financemanager.domain.TransactionType;
import ru.mifi.financemanager.domain.User;
import ru.mifi.financemanager.domain.Wallet;
import ru.mifi.financemanager.exception.ValidationException;

/**
 * Реализация сервиса финансовых операций.
 *
 * <p>Класс содержит основную бизнес-логику работы с финансами: добавление операций, управление
 * бюджетами, расчёт статистики.
 */
public class FinanceServiceImpl implements FinanceService {
    private final AuthService authService;

    private final NotificationService notificationService;

    /** Создаёт сервис финансовых операций. */
    public FinanceServiceImpl(AuthService authService, NotificationService notificationService) {
        this.authService = authService;
        this.notificationService = notificationService;
    }

    /** Возвращает кошелёк текущего пользователя. */
    private Wallet getCurrentWallet() {
        User user =
                authService
                        .getCurrentUser()
                        .orElseThrow(() -> new ValidationException("Пользователь не авторизован"));
        return user.getWallet();
    }

    /** Валидирует сумму операции. */
    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException("сумма", "не может быть пустой");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("сумма", "должна быть положительной");
        }
    }

    /** Валидирует категорию. */
    private void validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new ValidationException("категория", "не может быть пустой");
        }
    }

    /** Добавляет доход в кошелёк текущего пользователя. */
    @Override
    public Transaction addIncome(BigDecimal amount, String category, String description) {
        // Валидация входных данных
        validateAmount(amount);
        validateCategory(category);

        // Создаём транзакцию дохода
        Transaction transaction =
                new Transaction(
                        TransactionType.INCOME,
                        amount,
                        category.trim(),
                        description != null ? description.trim() : "");

        // Добавляем в кошелёк
        getCurrentWallet().addTransaction(transaction);

        return transaction;
    }

    /** Добавляет расход в кошелёк текущего пользователя. */
    @Override
    public Transaction addExpense(BigDecimal amount, String category, String description) {
        validateAmount(amount);
        validateCategory(category);

        String normalizedCategory = category.trim();

        Transaction transaction =
                new Transaction(
                        TransactionType.EXPENSE,
                        amount,
                        normalizedCategory,
                        description != null ? description.trim() : "");

        Wallet wallet = getCurrentWallet();
        wallet.addTransaction(transaction);

        checkBudgetAndNotify(wallet, normalizedCategory);

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            notificationService.notifyNegativeBalance(wallet.getBalance());
        }

        return transaction;
    }

    /** Проверяет состояние бюджета после добавления расхода и отправляет уведомления. */
    private void checkBudgetAndNotify(Wallet wallet, String category) {
        BigDecimal budget = wallet.getBudget(category);

        if (budget == null || budget.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal spent = wallet.getExpenseByCategory(category);
        BigDecimal remaining = budget.subtract(spent);
        double usagePercent = wallet.getBudgetUsagePercent(category);

        // Проверка превышения бюджета
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            notificationService.notifyBudgetExceeded(category, budget, spent, remaining.abs());
        }
        // Проверка 80% порога
        else if (usagePercent >= 80) {
            notificationService.notifyBudgetWarning(category, usagePercent, remaining);
        }
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return getCurrentWallet().getTransactions();
    }

    @Override
    public List<Transaction> getTransactionsByPeriod(LocalDateTime from, LocalDateTime to) {
        return getCurrentWallet().getTransactionsByPeriod(from, to);
    }

    @Override
    public List<Transaction> getTransactionsByCategory(String category) {
        validateCategory(category);
        return getCurrentWallet().getTransactionsByCategory(category.trim());
    }

    /** Устанавливает бюджет для категории. */
    @Override
    public void setBudget(String category, BigDecimal limit) {
        validateCategory(category);
        validateAmount(limit);
        getCurrentWallet().setBudget(category.trim(), limit);
    }

    /** Удаляет бюджет для категории. */
    @Override
    public void removeBudget(String category) {
        validateCategory(category);
        getCurrentWallet().removeBudget(category.trim());
    }

    @Override
    public Map<String, BigDecimal> getAllBudgets() {
        return getCurrentWallet().getCategoryBudgets();
    }

    /** Возвращает оставшийся бюджет по категории. */
    @Override
    public BigDecimal getRemainingBudget(String category) {
        validateCategory(category);
        return getCurrentWallet().getRemainingBudget(category.trim());
    }

    /** Возвращает общую сумму доходов. */
    @Override
    public BigDecimal getTotalIncome() {
        return getCurrentWallet().getTotalIncome();
    }

    /** Возвращает общую сумму расходов. */
    @Override
    public BigDecimal getTotalExpense() {
        return getCurrentWallet().getTotalExpense();
    }

    /** Возвращает текущий баланс (доходы минус расходы). */
    @Override
    public BigDecimal getBalance() {
        return getCurrentWallet().getBalance();
    }

    /** Возвращает статистику расходов по категориям. */
    @Override
    public Map<String, BigDecimal> getExpensesByCategory() {
        Map<String, BigDecimal> result = new HashMap<>();
        Wallet wallet = getCurrentWallet();

        for (String category : wallet.getAllCategories()) {
            BigDecimal expense = wallet.getExpenseByCategory(category);
            if (expense.compareTo(BigDecimal.ZERO) > 0) {
                result.put(category, expense);
            }
        }

        return result;
    }

    /** Возвращает статистику доходов по категориям. */
    @Override
    public Map<String, BigDecimal> getIncomesByCategory() {
        Map<String, BigDecimal> result = new HashMap<>();
        Wallet wallet = getCurrentWallet();

        for (String category : wallet.getAllCategories()) {
            BigDecimal income = wallet.getIncomeByCategory(category);
            if (income.compareTo(BigDecimal.ZERO) > 0) {
                result.put(category, income);
            }
        }

        return result;
    }

    /** Вычисляет сумму расходов по нескольким категориям. */
    @Override
    public BigDecimal getExpenseByCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new ValidationException("Список категорий не может быть пустым");
        }

        Wallet wallet = getCurrentWallet();
        List<String> existingCategories = wallet.getAllCategories();

        // Проверяем, есть ли несуществующие категории
        for (String category : categories) {
            boolean found =
                    existingCategories.stream().anyMatch(c -> c.equalsIgnoreCase(category.trim()));
            if (!found) {
                notificationService.notifyCategoryNotFound(category);
            }
        }

        return wallet.getExpenseByCategories(categories);
    }

    @Override
    public List<String> getAllCategories() {
        return getCurrentWallet().getAllCategories();
    }

    /** Выполняет перевод между пользователями. */
    @Override
    public boolean transfer(User toUser, BigDecimal amount, String description) {
        validateAmount(amount);

        if (toUser == null) {
            throw new ValidationException("получатель", "не найден");
        }

        User currentUser =
                authService
                        .getCurrentUser()
                        .orElseThrow(() -> new ValidationException("Пользователь не авторизован"));

        // Проверка — нельзя переводить самому себе
        if (currentUser.getLogin().equalsIgnoreCase(toUser.getLogin())) {
            throw new ValidationException("Нельзя сделать перевод самому себе");
        }

        // Проверка баланса
        Wallet senderWallet = currentUser.getWallet();
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new ValidationException(
                    "Недостаточно средств для перевода. "
                            + "Баланс: "
                            + senderWallet.getBalance()
                            + ", сумма перевода: "
                            + amount);
        }

        // Формируем описание
        String transferDescription =
                description != null && !description.isEmpty() ? description : "Перевод средств";

        // Создаём расход у отправителя
        Transaction senderExpense =
                new Transaction(
                        TransactionType.EXPENSE,
                        amount,
                        "Перевод",
                        "Перевод пользователю " + toUser.getLogin() + ": " + transferDescription);
        senderWallet.addTransaction(senderExpense);

        checkBudgetAndNotify(senderWallet, "Перевод");

        // Создаём доход у получателя
        Transaction receiverIncome =
                new Transaction(
                        TransactionType.INCOME,
                        amount,
                        "Перевод",
                        "Перевод от " + currentUser.getLogin() + ": " + transferDescription);
        toUser.getWallet().addTransaction(receiverIncome);

        // Уведомляем об успешном переводе
        notificationService.notifyTransferSuccess(toUser.getLogin(), amount);

        return true;
    }
}
