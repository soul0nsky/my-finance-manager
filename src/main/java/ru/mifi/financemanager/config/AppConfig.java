package ru.mifi.financemanager.config;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Конфигурация приложения, загружаемая из properties файла.
 *
 * <p>Этот класс реализует паттерн "конфигурация как код", позволяя:
 *
 * <ul>
 *   <li>Хранить настройки во внешнем файле
 *   <li>Переопределять параметры без перекомпиляции
 *   <li>Использовать значения по умолчанию, если файл не найден
 * </ul>
 */
public class AppConfig {

    // Путь к файлу данных пользователей
    private String dataFilePath;

    // Порог предупреждения о бюджете (по умолчанию 80%)
    private double budgetWarningThreshold;

    // Валюта по умолчанию
    private String defaultCurrency;

    // Имя приложения для отображения
    private String appName;

    // Версия приложения
    private String appVersion;

    /**
     * Загружает конфигурацию из внешнего файла или classpath. Приоритет: внешний файл > classpath >
     * значения по умолчанию.
     */
    public AppConfig() {
        Properties props = new Properties();
        boolean loaded = false;

        // Пробуем загрузить из внешнего файла
        Path externalConfig = Paths.get("config.properties");
        if (Files.exists(externalConfig)) {
            try (InputStream is = Files.newInputStream(externalConfig)) {
                props.load(is);
                loaded = true;
            } catch (IOException e) {
                System.err.println("Не удалось загрузить внешний конфиг: " + e.getMessage());
            }
        }

        // Если внешний файл не найден, пробуем classpath
        if (!loaded) {
            try (InputStream is =
                    getClass().getClassLoader().getResourceAsStream("application.properties")) {
                if (is != null) {
                    props.load(is);
                    loaded = true;
                }
            } catch (IOException e) {
                System.err.println("Не удалось загрузить конфиг из classpath: " + e.getMessage());
            }
        }

        // Устанавливаем значения (из файла или по умолчанию)
        initializeFromProperties(props);
    }

    /** Создаёт конфигурацию с указанными значениями. */
    public AppConfig(String dataFilePath, double budgetWarningThreshold) {
        this.dataFilePath = dataFilePath;
        this.budgetWarningThreshold = budgetWarningThreshold;
        this.defaultCurrency = "RUB";
        this.appName = "Finance Manager";
        this.appVersion = "1.0.0";
    }

    /** Инициализирует поля из Properties с fallback на значения по умолчанию. */
    private void initializeFromProperties(Properties props) {
        this.dataFilePath = props.getProperty("app.data.file", "data/users.json");
        this.budgetWarningThreshold =
                Double.parseDouble(props.getProperty("app.budget.warning.threshold", "80.0"));
        this.defaultCurrency = props.getProperty("app.currency", "RUB");
        this.appName = props.getProperty("app.name", "Finance Manager");
        this.appVersion = props.getProperty("app.version", "1.0.0");
    }

    /** Возвращает путь к файлу данных пользователей. */
    public String getDataFilePath() {
        return dataFilePath;
    }

    /** Возвращает порог предупреждения о бюджете в процентах. */
    public double getBudgetWarningThreshold() {
        return budgetWarningThreshold;
    }

    /** Возвращает валюту по умолчанию. */
    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    /** Возвращает название приложения. */
    public String getAppName() {
        return appName;
    }

    /** Возвращает версию приложения. */
    public String getAppVersion() {
        return appVersion;
    }

    /** Возвращает порог предупреждения как BigDecimal (для сравнения с процентами). */
    public BigDecimal getBudgetWarningThresholdAsBigDecimal() {
        return BigDecimal.valueOf(budgetWarningThreshold);
    }

    @Override
    public String toString() {
        return String.format(
                "AppConfig{dataFile='%s', warningThreshold=%.1f%%, currency='%s'}",
                dataFilePath, budgetWarningThreshold, defaultCurrency);
    }
}
