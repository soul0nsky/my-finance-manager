package ru.mifi.financemanager.cli;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import ru.mifi.financemanager.domain.Transaction;
import ru.mifi.financemanager.domain.User;
import ru.mifi.financemanager.exception.InvalidCredentialsException;
import ru.mifi.financemanager.exception.ValidationException;
import ru.mifi.financemanager.export.CsvExporter;
import ru.mifi.financemanager.export.CsvImporter;
import ru.mifi.financemanager.service.AuthService;
import ru.mifi.financemanager.service.FinanceService;
import ru.mifi.financemanager.service.NotificationService;

/**
 * Консольный интерфейс приложения управления финансами.
 *
 * <p>Этот класс реализует пользовательский интерфейс командной строки:
 *
 * <ul>
 *   <li>Меню авторизации (вход, регистрация)
 *   <li>Главное меню (операции, бюджеты, статистика)
 *   <li>Обработка ввода с валидацией
 *   <li>Форматированный вывод данных
 * </ul>
 *
 * <p>Применяем числовое меню для удобства пользователя и простоты валидации.
 */
public class ConsoleApp {

    private final AuthService authService;
    private final FinanceService financeService;
    private final NotificationService notificationService;
    private final InputValidator validator;
    private final Scanner scanner;

    private boolean running;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /** Создаёт консольное приложение с необходимыми зависимостями. */
    public ConsoleApp(
            AuthService authService,
            FinanceService financeService,
            NotificationService notificationService) {
        this.authService = authService;
        this.financeService = financeService;
        this.notificationService = notificationService;
        this.validator = new InputValidator();
        this.scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        this.running = true;
    }

    /** Запускает главный цикл приложения. */
    public void run() {
        printWelcome();

        while (running) {
            try {
                if (!authService.isAuthenticated()) {
                    showAuthMenu();
                } else {
                    showMainMenu();
                }
            } catch (Exception e) {
                System.out.println("\n❌ Ошибка: " + e.getMessage());
            }
        }

        authService.saveAll();
        System.out.println("\nДо свидания! Данные сохранены.");
        scanner.close();
    }

    private void printWelcome() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║       СИСТЕМА УПРАВЛЕНИЯ ЛИЧНЫМИ ФИНАНСАМИ                 ║");
        System.out.println("║                     версия 1.0.0                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    /** Отображает меню авторизации. */
    private void showAuthMenu() {
        System.out.println("\n=== МЕНЮ АВТОРИЗАЦИИ ===");
        System.out.println("1. Вход в систему");
        System.out.println("2. Регистрация");
        System.out.println("3. Список пользователей");
        System.out.println("0. Выход");
        System.out.print("\nВыберите действие: ");

        try {
            int choice = validator.validateMenuChoice(scanner.nextLine(), 0, 3);

            switch (choice) {
                case 1 -> handleLogin();
                case 2 -> handleRegister();
                case 3 -> handleListUsers();
                case 0 -> running = false;
            }
        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    /** Обрабатывает вход пользователя в систему. */
    private void handleLogin() {
        System.out.println("\n--- Вход в систему ---");

        try {
            System.out.print("Логин: ");
            String login = validator.validateNotEmpty(scanner.nextLine(), "логин");

            System.out.print("Пароль: ");
            String password = validator.validatePassword(scanner.nextLine());

            User user = authService.login(login, password);
            System.out.println("\n✅ Добро пожаловать, " + user.getLogin() + "!");
            System.out.println("   Текущий баланс: " + formatMoney(user.getWallet().getBalance()));

        } catch (InvalidCredentialsException e) {
            System.out.println("❌ " + e.getMessage());
        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    /** Обрабатывает регистрацию нового пользователя. */
    private void handleRegister() {
        System.out.println("\n--- Регистрация ---");

        try {
            System.out.print("Придумайте логин: ");
            String login = validator.validateLogin(scanner.nextLine());

            System.out.print("Придумайте пароль: ");
            String password = validator.validatePassword(scanner.nextLine());

            User user = authService.register(login, password);
            System.out.println("\n✅ Пользователь " + user.getLogin() + " успешно зарегистрирован!");
            System.out.println("   Теперь вы можете войти в систему.");

        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    /** Отображает список всех пользователей. */
    private void handleListUsers() {
        System.out.println("\n--- Список пользователей ---");
        List<User> users = authService.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("Пользователей нет. Зарегистрируйтесь первым!");
        } else {
            for (User user : users) {
                System.out.println("• " + user.getLogin());
            }
            System.out.println("\nВсего пользователей: " + users.size());
        }
    }

    /** Отображает главное меню для авторизованного пользователя. */
    private void showMainMenu() {
        User user = authService.getCurrentUser().orElseThrow();
        System.out.println("\n=== ГЛАВНОЕ МЕНЮ [" + user.getLogin() + "] ===");
        System.out.println("--- Операции ---");
        System.out.println("1. Добавить доход");
        System.out.println("2. Добавить расход");
        System.out.println("3. История операций");
        System.out.println("--- Бюджеты ---");
        System.out.println("4. Установить бюджет");
        System.out.println("5. Просмотреть бюджеты");
        System.out.println("--- Статистика ---");
        System.out.println("6. Общая статистика");
        System.out.println("7. Статистика по категориям");
        System.out.println("8. Статистика за период");
        System.out.println("--- Дополнительно ---");
        System.out.println("9. Перевод другому пользователю");
        System.out.println("10. Экспорт в CSV");
        System.out.println("11. Импорт из CSV");
        System.out.println("12. Справка (help)");
        System.out.println("0. Выход из аккаунта");
        System.out.print("\nВыберите действие: ");

        try {
            int choice = validator.validateMenuChoice(scanner.nextLine(), 0, 12);

            switch (choice) {
                case 1 -> handleAddIncome();
                case 2 -> handleAddExpense();
                case 3 -> handleShowHistory();
                case 4 -> handleSetBudget();
                case 5 -> handleShowBudgets();
                case 6 -> handleShowStatistics();
                case 7 -> handleStatsByCategories();
                case 8 -> handleStatsByPeriod();
                case 9 -> handleTransfer();
                case 10 -> handleExportCsv();
                case 11 -> handleImportCsv();
                case 12 -> handleHelp();
                case 0 -> handleLogout();
            }
        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    /** Добавляет доход. */
    private void handleAddIncome() {
        System.out.println("\n--- Добавление дохода ---");

        try {
            System.out.print("Сумма: ");
            BigDecimal amount = validator.validateAmount(scanner.nextLine(), "сумма");

            System.out.print("Категория (например, Зарплата, Бонус): ");
            String category = validator.validateCategory(scanner.nextLine());

            System.out.print("Описание (необязательно): ");
            String description = validator.validateDescription(scanner.nextLine());

            Transaction transaction = financeService.addIncome(amount, category, description);
            System.out.println("\n✅ Доход добавлен!");
            System.out.println("   " + transaction);
            System.out.println("   Текущий баланс: " + formatMoney(financeService.getBalance()));

        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    /** Добавляет расход. */
    private void handleAddExpense() {
        System.out.println("\n--- Добавление расхода ---");

        try {
            System.out.print("Сумма: ");
            BigDecimal amount = validator.validateAmount(scanner.nextLine(), "сумма");

            System.out.print("Категория (например, Еда, Транспорт, Развлечения): ");
            String category = validator.validateCategory(scanner.nextLine());

            System.out.print("Описание (необязательно): ");
            String description = validator.validateDescription(scanner.nextLine());

            Transaction transaction = financeService.addExpense(amount, category, description);
            System.out.println("\n✅ Расход добавлен!");
            System.out.println("   " + transaction);
            System.out.println("   Текущий баланс: " + formatMoney(financeService.getBalance()));

            // Уведомления о бюджете выводятся автоматически из FinanceService

        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    /** Отображает историю операций. */
    private void handleShowHistory() {
        System.out.println("\n--- История операций ---");

        List<Transaction> transactions = financeService.getAllTransactions();

        if (transactions.isEmpty()) {
            System.out.println("Операций пока нет.");
            return;
        }

        // Выводим последние 20 операций
        int showCount = Math.min(transactions.size(), 20);
        List<Transaction> recent =
                transactions.subList(
                        Math.max(0, transactions.size() - showCount), transactions.size());

        System.out.println("\nПоследние " + showCount + " операций:");
        System.out.println("-".repeat(70));
        System.out.printf(
                "%-10s %-8s %12s %-15s %s%n", "Дата", "Тип", "Сумма", "Категория", "Описание");
        System.out.println("-".repeat(70));

        for (Transaction t : recent) {
            System.out.printf(
                    "%-10s %-8s %12s %-15s %s%n",
                    t.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    t.isIncome() ? "Доход" : "Расход",
                    formatMoney(t.getAmount()),
                    truncate(t.getCategory(), 15),
                    truncate(t.getDescription(), 20));
        }

        System.out.println("-".repeat(70));
        System.out.println("Всего операций: " + transactions.size());
    }

    /** Устанавливает бюджет для категории. */
    private void handleSetBudget() {
        System.out.println("\n--- Установка бюджета ---");

        try {
            List<String> categories = financeService.getAllCategories();
            if (!categories.isEmpty()) {
                System.out.println("Существующие категории: " + String.join(", ", categories));
            }

            System.out.print("\nКатегория: ");
            String category = validator.validateCategory(scanner.nextLine());

            BigDecimal currentBudget = financeService.getAllBudgets().get(category);
            if (currentBudget != null) {
                BigDecimal remaining = financeService.getRemainingBudget(category);
                System.out.println(
                        "Текущий бюджет: "
                                + formatMoney(currentBudget)
                                + ", осталось: "
                                + formatMoney(remaining));
            }

            System.out.print("Новый лимит бюджета (0 для удаления): ");
            String input = scanner.nextLine().trim();

            // Если 0 — удаляем бюджет
            if (input.equals("0")) {
                financeService.removeBudget(category);
                System.out.println("✅ Бюджет для категории '" + category + "' удалён.");
            } else {
                BigDecimal limit = validator.validateAmount(input, "бюджет");
                financeService.setBudget(category, limit);
                System.out.println(
                        "✅ Бюджет для категории '"
                                + category
                                + "' установлен: "
                                + formatMoney(limit));
            }

        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    /** Отображает все установленные бюджеты. */
    private void handleShowBudgets() {
        System.out.println("\n--- Бюджеты по категориям ---");

        Map<String, BigDecimal> budgets = financeService.getAllBudgets();

        if (budgets.isEmpty()) {
            System.out.println("Бюджеты не установлены.");
            System.out.println("Используйте пункт 4 для установки бюджета.");
            return;
        }

        System.out.println("-".repeat(60));
        System.out.printf("%-20s %12s %12s %12s%n", "Категория", "Бюджет", "Потрачено", "Осталось");
        System.out.println("-".repeat(60));

        for (Map.Entry<String, BigDecimal> entry : budgets.entrySet()) {
            String category = entry.getKey();
            BigDecimal budget = entry.getValue();
            BigDecimal spent =
                    financeService.getExpensesByCategory().getOrDefault(category, BigDecimal.ZERO);
            BigDecimal remaining = budget.subtract(spent);

            String status = "";
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                status = " ⚠️ ПРЕВЫШЕН!";
            } else if (budget.compareTo(BigDecimal.ZERO) > 0) {
                double percent = spent.doubleValue() / budget.doubleValue() * 100;
                if (percent >= 80) {
                    status = " 🔶";
                }
            }

            System.out.printf(
                    "%-20s %12s %12s %12s%s%n",
                    truncate(category, 20),
                    formatMoney(budget),
                    formatMoney(spent),
                    formatMoney(remaining),
                    status);
        }

        System.out.println("-".repeat(60));
    }

    /** Отображает общую статистику. */
    private void handleShowStatistics() {
        System.out.println("\n=== ФИНАНСОВАЯ СТАТИСТИКА ===");

        BigDecimal income = financeService.getTotalIncome();
        BigDecimal expense = financeService.getTotalExpense();
        BigDecimal balance = financeService.getBalance();

        System.out.println("-".repeat(40));
        System.out.printf("Общий доход:   %20s%n", formatMoney(income));
        System.out.printf("Общий расход:  %20s%n", formatMoney(expense));
        System.out.println("-".repeat(40));
        System.out.printf("Текущий баланс:%20s%n", formatMoney(balance));

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            System.out.println("\n⚠️ Внимание: расходы превышают доходы!");
        }

        // Статистика по доходам
        Map<String, BigDecimal> incomes = financeService.getIncomesByCategory();
        if (!incomes.isEmpty()) {
            System.out.println("\n--- Доходы по категориям ---");
            for (Map.Entry<String, BigDecimal> entry : incomes.entrySet()) {
                System.out.printf("  %-20s %12s%n", entry.getKey(), formatMoney(entry.getValue()));
            }
        }

        // Статистика по расходам
        Map<String, BigDecimal> expenses = financeService.getExpensesByCategory();
        if (!expenses.isEmpty()) {
            System.out.println("\n--- Расходы по категориям ---");
            for (Map.Entry<String, BigDecimal> entry : expenses.entrySet()) {
                System.out.printf("  %-20s %12s%n", entry.getKey(), formatMoney(entry.getValue()));
            }
        }
    }

    /** Статистика по выбранным категориям. */
    private void handleStatsByCategories() {
        System.out.println("\n--- Статистика по категориям ---");

        List<String> allCategories = financeService.getAllCategories();
        if (allCategories.isEmpty()) {
            System.out.println("Нет операций для анализа.");
            return;
        }

        System.out.println("Доступные категории: " + String.join(", ", allCategories));
        System.out.print("\nВведите категории через запятую: ");

        try {
            String input = validator.validateNotEmpty(scanner.nextLine(), "категории");

            List<String> categories =
                    Arrays.stream(input.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

            if (categories.isEmpty()) {
                throw new ValidationException("Не указаны категории");
            }

            BigDecimal total = financeService.getExpenseByCategories(categories);

            System.out.println("\n--- Результат ---");
            System.out.println("Категории: " + String.join(", ", categories));
            System.out.println("Общая сумма расходов: " + formatMoney(total));

            System.out.println("\nДетализация:");
            for (String category : categories) {
                Map<String, BigDecimal> expenses = financeService.getExpensesByCategory();
                BigDecimal amount = expenses.getOrDefault(category, BigDecimal.ZERO);
                System.out.printf("  %-20s %12s%n", category, formatMoney(amount));
            }

        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    /** Статистика за период. */
    private void handleStatsByPeriod() {
        System.out.println("\n--- Статистика за период ---");

        try {
            System.out.print("Дата начала (дд.мм.гггг): ");
            LocalDate fromDate = parseDate(scanner.nextLine());

            System.out.print("Дата окончания (дд.мм.гггг): ");
            LocalDate toDate = parseDate(scanner.nextLine());

            if (fromDate.isAfter(toDate)) {
                throw new ValidationException("Дата начала не может быть позже даты окончания");
            }

            LocalDateTime from = fromDate.atStartOfDay();
            LocalDateTime to = toDate.atTime(LocalTime.MAX);

            List<Transaction> transactions = financeService.getTransactionsByPeriod(from, to);

            if (transactions.isEmpty()) {
                System.out.println("\nОпераций за указанный период не найдено.");
                return;
            }

            BigDecimal periodIncome =
                    transactions.stream()
                            .filter(Transaction::isIncome)
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal periodExpense =
                    transactions.stream()
                            .filter(Transaction::isExpense)
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            System.out.println("\n--- Результат ---");
            System.out.println("Период: " + formatDate(fromDate) + " — " + formatDate(toDate));
            System.out.println("Операций: " + transactions.size());
            System.out.println("Доходы: " + formatMoney(periodIncome));
            System.out.println("Расходы: " + formatMoney(periodExpense));
            System.out.println("Разница: " + formatMoney(periodIncome.subtract(periodExpense)));

        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
        } catch (DateTimeParseException e) {
            System.out.println("❌ Неверный формат даты. Используйте дд.мм.гггг");
        }
    }

    /** Перевод средств другому пользователю. */
    private void handleTransfer() {
        System.out.println("\n--- Перевод другому пользователю ---");

        try {
            List<User> users = authService.getAllUsers();
            User currentUser = authService.getCurrentUser().orElseThrow();

            System.out.println("Доступные получатели:");
            for (User user : users) {
                if (!user.getLogin().equals(currentUser.getLogin())) {
                    System.out.println("  • " + user.getLogin());
                }
            }

            System.out.print("\nЛогин получателя: ");
            String toLogin = validator.validateNotEmpty(scanner.nextLine(), "логин получателя");

            User toUser =
                    authService
                            .findUserByLogin(toLogin)
                            .orElseThrow(
                                    () ->
                                            new ValidationException(
                                                    "Пользователь не найден: " + toLogin));

            System.out.println("Ваш баланс: " + formatMoney(financeService.getBalance()));
            System.out.print("Сумма перевода: ");
            BigDecimal amount = validator.validateAmount(scanner.nextLine(), "сумма");

            System.out.print("Комментарий (необязательно): ");
            String description = validator.validateDescription(scanner.nextLine());

            System.out.printf(
                    "\nПеревести %s пользователю %s? (да/нет): ", formatMoney(amount), toLogin);
            if (!validator.validateConfirmation(scanner.nextLine())) {
                System.out.println("Перевод отменён.");
                return;
            }

            financeService.transfer(toUser, amount, description);
            // Уведомление об успешном переводе выводится автоматически

        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    /** Экспорт данных в CSV. */
    private void handleExportCsv() {
        System.out.println("\n--- Экспорт в CSV ---");

        List<Transaction> transactions = financeService.getAllTransactions();
        if (transactions.isEmpty()) {
            System.out.println("Нет данных для экспорта.");
            return;
        }

        try {
            String defaultName =
                    "transactions_"
                            + authService.getCurrentUser().map(User::getLogin).orElse("export")
                            + ".csv";

            System.out.print("Имя файла [" + defaultName + "]: ");
            String input = scanner.nextLine().trim();
            String fileName = input.isEmpty() ? defaultName : input;

            CsvExporter exporter = new CsvExporter();
            exporter.export(transactions, fileName);

            System.out.println("✅ Данные экспортированы в файл: " + fileName);
            System.out.println("   Экспортировано записей: " + transactions.size());

        } catch (IOException e) {
            System.out.println("❌ Ошибка при экспорте: " + e.getMessage());
        }
    }

    /** Импорт данных из CSV. */
    private void handleImportCsv() {
        System.out.println("\n--- Импорт из CSV ---");

        try {
            System.out.print("Путь к файлу: ");
            String filePath = validator.validateFilePath(scanner.nextLine());

            CsvImporter importer = new CsvImporter();
            CsvImporter.ImportResult result = importer.importFromFile(filePath);

            User currentUser = authService.getCurrentUser().orElseThrow();
            for (Transaction transaction : result.getTransactions()) {
                currentUser.getWallet().addTransaction(transaction);
            }

            System.out.println("\n✅ Импорт завершён!");
            System.out.println("   Обработано строк: " + result.getTotalLines());
            System.out.println("   Успешно импортировано: " + result.getSuccessfulLines());

            if (result.hasErrors()) {
                System.out.println("\n⚠️ Ошибки при импорте:");
                for (String error : result.getErrors()) {
                    System.out.println("   " + error);
                }
            }

        } catch (IOException e) {
            System.out.println("❌ Ошибка при чтении файла: " + e.getMessage());
        } catch (ValidationException e) {
            System.out.println("❌ " + e.getMessage());
        }
    }

    /** Отображает справку по командам. */
    private void handleHelp() {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    СПРАВКА ПО КОМАНДАМ                      ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        System.out.println("\n--- Операции ---");
        System.out.println("1. Добавить доход    — запись поступлений (зарплата, бонусы)");
        System.out.println("2. Добавить расход   — запись трат (еда, транспорт, развлечения)");
        System.out.println("3. История операций  — просмотр последних 20 операций");

        System.out.println("\n--- Бюджеты ---");
        System.out.println("4. Установить бюджет — задать лимит трат для категории");
        System.out.println("   Пример: установить бюджет 5000 на категорию 'Еда'");
        System.out.println("   При превышении 80% или лимита — система предупредит!");
        System.out.println("5. Просмотреть бюджеты — обзор всех лимитов и остатков");

        System.out.println("\n--- Статистика ---");
        System.out.println("6. Общая статистика  — доходы, расходы, баланс");
        System.out.println("7. По категориям     — детализация по выбранным категориям");
        System.out.println("8. За период         — статистика за указанные даты");

        System.out.println("\n--- Дополнительно ---");
        System.out.println("9. Перевод           — отправить деньги другому пользователю");
        System.out.println("10. Экспорт CSV      — сохранить данные в файл");
        System.out.println("11. Импорт CSV       — загрузить данные из файла");

        System.out.println("\n--- Примеры работы ---");
        System.out.println("Ввод суммы: 1500 или 1500.50 или 1500,50");
        System.out.println("Ввод даты: 15.01.2025");
        System.out.println("Категории: Еда, Транспорт, Развлечения, Зарплата, Бонус");

        System.out.println("\n💡 Совет: данные сохраняются автоматически при выходе!");
    }

    /** Выход из аккаунта. */
    private void handleLogout() {
        authService.logout();
        System.out.println("\n✅ Вы вышли из аккаунта. Данные сохранены.");
    }

    /** Форматирует денежную сумму с валютой. */
    private String formatMoney(BigDecimal amount) {
        return String.format("%,.2f ₽", amount);
    }

    /** Форматирует дату. */
    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    /** Парсит дату из строки. */
    private LocalDate parseDate(String input) {
        String trimmed = input.trim();
        return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    /** Обрезает строку до указанной длины. */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 2) + "..";
    }
}
