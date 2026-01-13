package ru.mifi.financemanager.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import ru.mifi.financemanager.domain.Transaction;
import ru.mifi.financemanager.domain.TransactionType;

/**
 * Экспорт транзакций в CSV формат.
 */
public class CsvExporter implements DataExporter {

    // Разделитель полей в CSV (точка с запятой для совместимости с Excel в RU локали)
    private static final String DELIMITER = ";";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String HEADER = "ID;Тип;Сумма;Категория;Дата;Описание";

    @Override
    public void export(List<Transaction> transactions, String filePath) throws IOException {
        Path path = Paths.get(filePath);

        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (Transaction transaction : transactions) {
                String line = formatTransaction(transaction);
                writer.write(line);
                writer.newLine();
            }
        }
    }

    /**
     * Форматирует транзакцию в строку CSV.
     */
    private String formatTransaction(Transaction transaction) {
        return String.join(
                DELIMITER,
                transaction.getId(),
                transaction.getType() == TransactionType.INCOME ? "Доход" : "Расход",
                transaction.getAmount().toPlainString(),
                escapeField(transaction.getCategory()),
                transaction.getCreatedAt().format(DATE_FORMATTER),
                escapeField(transaction.getDescription()));
    }

    /**
     * Экранирует поле CSV — оборачивает в кавычки если содержит спецсимволы.
     */
    private String escapeField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(DELIMITER) || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public String getFormatName() {
        return "CSV";
    }
}
