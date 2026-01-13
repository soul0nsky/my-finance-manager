package ru.mifi.financemanager.exception;

/** Исключение, выбрасываемое при ошибке валидации пользовательского ввода. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String field, String reason) {
        super(String.format("Ошибка валидации поля '%s': %s", field, reason));
    }
}
