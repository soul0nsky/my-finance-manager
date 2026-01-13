package ru.mifi.financemanager.cli;

import java.math.BigDecimal;
import ru.mifi.financemanager.exception.ValidationException;

/**
 * Валидатор пользовательского ввода в CLI.
 *
 * <p>ТЗ п.9: Валидация ввода — проверка форматов, чисел, пустых значений,
 * неизвестных категорий с понятными сообщениями (2 балла).
 *
 * <p>Этот класс централизует всю логику валидации ввода из консоли.
 * Выделен в отдельный класс по принципу единственной ответственности (SRP).
 *
 * <p>Преимущества централизованной валидации:
 * <ul>
 *   <li>Единообразные сообщения об ошибках</li>
 *   <li>Легко добавлять новые проверки</li>
 *   <li>Упрощает тестирование</li>
 * </ul>
 */
public class InputValidator {

    /**
     * Валидирует и парсит выбор пункта меню.
     */
    public int validateMenuChoice(String input, int minValue, int maxValue) {
        if (input == null || input.trim().isEmpty()) {
            throw new ValidationException("Введите номер пункта меню");
        }

        try {
            int choice = Integer.parseInt(input.trim());
            if (choice < minValue || choice > maxValue) {
                throw new ValidationException(
                        String.format("Выберите пункт от %d до %d", minValue, maxValue));
            }
            return choice;
        } catch (NumberFormatException e) {
            throw new ValidationException("Введите целое число");
        }
    }

    /**
     * Валидирует непустую строку (например, логин, категория).
     */
    public String validateNotEmpty(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            throw new ValidationException(fieldName, "не может быть пустым");
        }

        String cleaned = input.replaceAll("\\p{C}", "").trim();

        if (cleaned.isEmpty()) {
            throw new ValidationException(fieldName + " не может быть пустым");
        }

        return cleaned;
    }

    /**
     * Валидирует и парсит денежную сумму.
     */
    public BigDecimal validateAmount(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            throw new ValidationException(fieldName, "не может быть пустой");
        }

        try {
            // Заменяем запятую на точку для поддержки русской локали
            String normalized = input.trim().replace(",", ".");
            BigDecimal amount = new BigDecimal(normalized);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException(fieldName, "должна быть положительной");
            }

            return amount;
        } catch (NumberFormatException e) {
            throw new ValidationException(
                    fieldName, "неверный формат числа. Используйте цифры и точку/запятую");
        }
    }

    /**
     * Валидирует пароль.
     */
    public String validatePassword(String input) {
        if (input == null || input.isEmpty()) {
            throw new ValidationException("пароль", "не может быть пустым");
        }
        return input;
    }

    /**
     * Валидирует логин пользователя.
     */
    public String validateLogin(String input) {
        String login = validateNotEmpty(input, "логин");

        if (login.length() < 2) {
            throw new ValidationException("логин", "должен содержать минимум 2 символа");
        }

        if (!login.matches("^[a-zA-Zа-яА-Я0-9_]+$")) {
            throw new ValidationException(
                    "логин", "может содержать только буквы, цифры и символ подчёркивания");
        }

        return login;
    }

    /**
     * Валидирует категорию.
     */
    public String validateCategory(String input) {
        String category = validateNotEmpty(input, "категория");

        if (category.length() > 50) {
            throw new ValidationException("категория", "не может быть длиннее 50 символов");
        }

        return category;
    }

    /**
     * Валидирует описание (может быть пустым).
     */
    public String validateDescription(String input) {
        if (input == null) {
            return "";
        }

        String description = input.trim();

        if (description.length() > 200) {
            throw new ValidationException("описание", "не может быть длиннее 200 символов");
        }

        return description;
    }

    /**
     * Валидирует подтверждение (да/нет).
     */
    public boolean validateConfirmation(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String normalized = input.trim().toLowerCase();
        return normalized.equals("да")
                || normalized.equals("y")
                || normalized.equals("yes")
                || normalized.equals("1");
    }

    /**
     * Валидирует путь к файлу.
     */
    public String validateFilePath(String input) {
        return validateNotEmpty(input, "путь к файлу");
    }
}
