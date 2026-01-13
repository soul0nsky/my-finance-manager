package ru.mifi.financemanager.export;

import java.io.IOException;
import java.util.List;
import ru.mifi.financemanager.domain.Transaction;

/** Интерфейс для экспорта финансовых данных в различные форматы. */
public interface DataExporter {

    /** Экспортирует список транзакций в файл. */
    void export(List<Transaction> transactions, String filePath) throws IOException;

    /** Возвращает расширение файла для данного формата экспорта. */
    String getFileExtension();

    /** Возвращает название формата для отображения пользователю. */
    String getFormatName();
}
