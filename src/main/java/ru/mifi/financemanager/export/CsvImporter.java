package ru.mifi.financemanager.export;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import ru.mifi.financemanager.domain.Transaction;
import ru.mifi.financemanager.domain.TransactionType;

/**
 * Импорт транзакций из CSV формата.
 */
public class CsvImporter {

    // Разделитель полей в CSV
    private static final String DELIMITER = ";";

    private static final DateTimeFormatter PRIMARY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Альтернативный форматтер (ISO формат)
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Результат импорта со статистикой.
     */
    public static class ImportResult {
        private final List<Transaction> transactions;
        private final int totalLines;
        private final int successfulLines;
        private final List<String> errors;

        public ImportResult(
                List<Transaction> transactions,
                int totalLines,
                int successfulLines,
                List<String> errors) {
            this.transactions = transactions;
            this.totalLines = totalLines;
            this.successfulLines = successfulLines;
            this.errors = errors;
        }

        public List<Transaction> getTransactions() {
            return transactions;
        }

        public int getTotalLines() {
            return totalLines;
        }

        public int getSuccessfulLines() {
            return successfulLines;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    /**
     * Импортирует транзакции из CSV файла.
     */
    public ImportResult importFromFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("Файл не найден: " + filePath);
        }

        List<Transaction> transactions = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalLines = 0;
        int successfulLines = 0;

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // Пропускаем пустые строки
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Пропускаем заголовок
                if (isFirstLine) {
                    isFirstLine = false;
                    if (line.toLowerCase().contains("id") && line.toLowerCase().contains("тип")) {
                        continue;
                    }
                }

                totalLines++;

                try {
                    Transaction transaction = parseLine(line, totalLines);
                    if (transaction != null) {
                        transactions.add(transaction);
                        successfulLines++;
                    }
                } catch (Exception e) {
                    errors.add("Строка " + totalLines + ": " + e.getMessage());
                }
            }
        }

        return new ImportResult(transactions, totalLines, successfulLines, errors);
    }

    /**
     * Парсит строку CSV в транзакцию.
     */
    private Transaction parseLine(String line, int lineNumber) {
        String[] fields = splitCsvLine(line);

        if (fields.length < 6) {
            throw new IllegalArgumentException(
                    "Недостаточно полей (ожидается 6, получено " + fields.length + ")");
        }

        // Парсим поля
        String id = fields[0].trim();
        String typeStr = fields[1].trim();
        String amountStr = fields[2].trim();
        String category = fields[3].trim();
        String dateStr = fields[4].trim();
        String description = fields.length > 5 ? fields[5].trim() : "";

        // Определяем тип транзакции
        TransactionType type;
        if (typeStr.equalsIgnoreCase("Доход") || typeStr.equalsIgnoreCase("INCOME")) {
            type = TransactionType.INCOME;
        } else if (typeStr.equalsIgnoreCase("Расход") || typeStr.equalsIgnoreCase("EXPENSE")) {
            type = TransactionType.EXPENSE;
        } else {
            throw new IllegalArgumentException("Неизвестный тип транзакции: " + typeStr);
        }

        // Парсим сумму
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.replace(",", "."));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Сумма должна быть положительной");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверный формат суммы: " + amountStr);
        }

        // Парсим дату
        LocalDateTime dateTime = parseDateTime(dateStr);

        // Валидируем категорию
        if (category.isEmpty()) {
            throw new IllegalArgumentException("Категория не может быть пустой");
        }

        // Создаём транзакцию с полным конструктором
        return new Transaction(id, type, amount, category, description, dateTime);
    }

    /**
     * Разбивает строку CSV на поля с учётом кавычек.
     */
    private String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ';' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        fields.add(currentField.toString());

        return fields.toArray(new String[0]);
    }

    /**
     * Парсит дату/время с поддержкой нескольких форматов.
     */
    private LocalDateTime parseDateTime(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, PRIMARY_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }

        // Пробуем ISO формат
        try {
            return LocalDateTime.parse(dateStr, ISO_FORMATTER);
        } catch (DateTimeParseException ignored) {

        }

        // Пробуем формат без секунд
        try {
            return LocalDateTime.parse(
                    dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (DateTimeParseException ignored) {
        }

        // Если ничего не подошло — используем текущее время
        throw new IllegalArgumentException("Неверный формат даты: " + dateStr);
    }
}
