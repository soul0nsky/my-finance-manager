package ru.mifi.financemanager.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.mifi.financemanager.exception.ValidationException;

/** Тесты для валидатора пользовательского ввода. */
@DisplayName("InputValidator — тесты валидации ввода")
class InputValidatorTest {

    private InputValidator validator;

    @BeforeEach
    void setUp() {
        validator = new InputValidator();
    }

    @Nested
    @DisplayName("Валидация меню")
    class MenuValidationTests {

        @Test
        @DisplayName("Корректный выбор пункта меню")
        void validMenuChoiceShouldWork() {
            assertEquals(1, validator.validateMenuChoice("1", 0, 5));
            assertEquals(5, validator.validateMenuChoice("5", 0, 5));
            assertEquals(0, validator.validateMenuChoice("0", 0, 5));
        }

        @Test
        @DisplayName("Выбор вне диапазона выбрасывает исключение")
        void menuChoiceOutOfRangeShouldThrowException() {
            assertThrows(
                    ValidationException.class,
                    () -> {
                        validator.validateMenuChoice("10", 0, 5);
                    });
        }

        @Test
        @DisplayName("Нечисловой ввод выбрасывает исключение")
        void nonNumericMenuChoiceShouldThrowException() {
            assertThrows(
                    ValidationException.class,
                    () -> {
                        validator.validateMenuChoice("abc", 0, 5);
                    });
        }

        @Test
        @DisplayName("Пустой ввод выбрасывает исключение")
        void emptyMenuChoiceShouldThrowException() {
            assertThrows(
                    ValidationException.class,
                    () -> {
                        validator.validateMenuChoice("", 0, 5);
                    });
        }
    }

    @Nested
    @DisplayName("Валидация суммы")
    class AmountValidationTests {

        @Test
        @DisplayName("Целое число парсится корректно")
        void integerAmountShouldWork() {
            BigDecimal amount = validator.validateAmount("1500", "сумма");
            assertEquals(new BigDecimal("1500"), amount);
        }

        @Test
        @DisplayName("Число с точкой парсится корректно")
        void amountWithDotShouldWork() {
            BigDecimal amount = validator.validateAmount("1500.50", "сумма");
            assertEquals(new BigDecimal("1500.50"), amount);
        }

        @Test
        @DisplayName("Число с запятой парсится корректно (русская локаль)")
        void amountWithCommaShouldWork() {
            BigDecimal amount = validator.validateAmount("1500,50", "сумма");
            assertEquals(new BigDecimal("1500.50"), amount);
        }

        @Test
        @DisplayName("Пробелы по краям удаляются")
        void amountWithSpacesShouldWork() {
            BigDecimal amount = validator.validateAmount("  1500  ", "сумма");
            assertEquals(new BigDecimal("1500"), amount);
        }

        @Test
        @DisplayName("Ноль выбрасывает исключение")
        void zeroAmountShouldThrowException() {
            assertThrows(
                    ValidationException.class,
                    () -> {
                        validator.validateAmount("0", "сумма");
                    });
        }

        @Test
        @DisplayName("Отрицательное число выбрасывает исключение")
        void negativeAmountShouldThrowException() {
            assertThrows(
                    ValidationException.class,
                    () -> {
                        validator.validateAmount("-100", "сумма");
                    });
        }

        @Test
        @DisplayName("Некорректный формат выбрасывает исключение")
        void invalidFormatShouldThrowException() {
            assertThrows(
                    ValidationException.class,
                    () -> {
                        validator.validateAmount("abc", "сумма");
                    });
        }
    }

    @Nested
    @DisplayName("Валидация логина")
    class LoginValidationTests {

        @Test
        @DisplayName("Корректный логин проходит валидацию")
        void validLoginShouldWork() {
            assertEquals("testuser", validator.validateLogin("testuser"));
            assertEquals("User123", validator.validateLogin("User123"));
            assertEquals("user_name", validator.validateLogin("user_name"));
        }

        @Test
        @DisplayName("Логин с кириллицей проходит валидацию")
        void cyrillicLoginShouldWork() {
            assertEquals("Пользователь", validator.validateLogin("Пользователь"));
        }

        @Test
        @DisplayName("Слишком короткий логин выбрасывает исключение")
        void shortLoginShouldThrowException() {
            assertThrows(
                    ValidationException.class,
                    () -> {
                        validator.validateLogin("a");
                    });
        }

        @Test
        @DisplayName("Логин со спецсимволами выбрасывает исключение")
        void loginWithSpecialCharsShouldThrowException() {
            assertThrows(
                    ValidationException.class,
                    () -> {
                        validator.validateLogin("user@name");
                    });
        }
    }

    @Nested
    @DisplayName("Валидация подтверждения")
    class ConfirmationTests {

        @Test
        @DisplayName("'да' подтверждает действие")
        void daConfirmsShouldWork() {
            assertTrue(validator.validateConfirmation("да"));
            assertTrue(validator.validateConfirmation("ДА"));
            assertTrue(validator.validateConfirmation("Да"));
        }

        @Test
        @DisplayName("'y' и 'yes' подтверждают действие")
        void yesConfirmsShouldWork() {
            assertTrue(validator.validateConfirmation("y"));
            assertTrue(validator.validateConfirmation("yes"));
            assertTrue(validator.validateConfirmation("Y"));
        }

        @Test
        @DisplayName("'1' подтверждает действие")
        void oneConfirmsShouldWork() {
            assertTrue(validator.validateConfirmation("1"));
        }

        @Test
        @DisplayName("'нет' отменяет действие")
        void netCancelsShouldWork() {
            assertFalse(validator.validateConfirmation("нет"));
            assertFalse(validator.validateConfirmation("n"));
            assertFalse(validator.validateConfirmation("no"));
        }

        @Test
        @DisplayName("Пустой ввод отменяет действие")
        void emptyInputCancelsShouldWork() {
            assertFalse(validator.validateConfirmation(""));
            assertFalse(validator.validateConfirmation(null));
        }
    }
}
