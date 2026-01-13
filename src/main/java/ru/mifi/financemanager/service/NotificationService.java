package ru.mifi.financemanager.service;

import java.math.BigDecimal;

/**
 * Сервис уведомлений пользователя о финансовых событиях.
 *
 * <p>Преимущества отдельного сервиса:
 *
 * <ul>
 *   <li>Централизованное управление форматом сообщений
 *   <li>Легко заменить консольный вывод на GUI или push-уведомления
 *   <li>Упрощает тестирование финансовой логики отдельно от UI
 * </ul>
 */
public class NotificationService {

    // Цвет - жёлтый цвета (предупреждения)
    private static final String YELLOW = "\u001B[33m";

    // Цвет - красный (ошибки/критические)
    private static final String RED = "\u001B[31m";

    // Цвет - зелёный (успех)
    private static final String GREEN = "\u001B[32m";

    // Сброс цвета
    private static final String RESET = "\u001B[0m";

    // Использовать ли цветной вывод (можно отключить для совместимости)
    private final boolean useColors;

    /** Создаёт сервис уведомлений с цветным выводом. */
    public NotificationService() {
        this.useColors = true;
    }

    /** Создаёт сервис уведомлений с настройкой цвета. */
    public NotificationService(boolean useColors) {
        this.useColors = useColors;
    }

    /** Уведомление о превышении бюджета по категории. */
    public void notifyBudgetExceeded(
            String category, BigDecimal budget, BigDecimal spent, BigDecimal overAmount) {
        String message =
                String.format(
                        "⚠️ ВНИМАНИЕ: Превышен бюджет по категории '%s'!%n"
                                + "   Лимит: %.2f, Потрачено: %.2f, Перерасход: %.2f",
                        category, budget, spent, overAmount);
        printWarning(message);
    }

    /** Предупреждение о приближении к лимиту бюджета (80%+). */
    public void notifyBudgetWarning(
            String category, double usagePercent, BigDecimal remainingBudget) {
        String message =
                String.format(
                        "🔶 ПРЕДУПРЕЖДЕНИЕ: Использовано %.1f%% бюджета по категории '%s'.%n"
                                + "   Осталось: %.2f",
                        usagePercent, category, remainingBudget);
        printWarning(message);
    }

    /** Уведомление об отрицательном балансе (расходы превысили доходы). */
    public void notifyNegativeBalance(BigDecimal balance) {
        String message =
                String.format(
                        "🔴 ВНИМАНИЕ: Расходы превысили доходы!%n" + "   Текущий баланс: %.2f",
                        balance);
        printError(message);
    }

    /** Уведомление о нулевом или близком к нулю балансе. */
    public void notifyLowBalance(BigDecimal balance) {
        String message =
                String.format(
                        "🔶 ПРЕДУПРЕЖДЕНИЕ: Низкий баланс!%n" + "   Текущий баланс: %.2f", balance);
        printWarning(message);
    }

    /** Уведомление о том, что категория не найдена. */
    public void notifyCategoryNotFound(String category) {
        String message =
                String.format(
                        "ℹ️ Категория '%s' не найдена среди существующих операций.", category);
        printInfo(message);
    }

    /** Уведомление об успешном переводе. */
    public void notifyTransferSuccess(String toLogin, BigDecimal amount) {
        String message =
                String.format(
                        "✅ Перевод выполнен успешно!%n" + "   Получатель: %s, Сумма: %.2f",
                        toLogin, amount);
        printSuccess(message);
    }

    /** Выводит сообщение об успехе (зелёный цвет). */
    public void printSuccess(String message) {
        if (useColors) {
            System.out.println(GREEN + message + RESET);
        } else {
            System.out.println(message);
        }
    }

    /** Выводит предупреждение (жёлтый цвет). */
    public void printWarning(String message) {
        if (useColors) {
            System.out.println(YELLOW + message + RESET);
        } else {
            System.out.println(message);
        }
    }

    /** Выводит ошибку (красный цвет). */
    public void printError(String message) {
        if (useColors) {
            System.out.println(RED + message + RESET);
        } else {
            System.out.println(message);
        }
    }

    /** Выводит информационное сообщение. */
    public void printInfo(String message) {
        System.out.println(message);
    }
}
