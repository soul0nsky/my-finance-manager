package ru.mifi.financemanager.exception;

/**
 * Исключение, выбрасываемое при неверных учётных данных (логин/пароль).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Неверный логин или пароль");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
