package ru.mifi.financemanager.domain;

import java.util.Objects;

/**
 * Модель пользователя системы управления финансами.
 *
 * <p>Каждый пользователь имеет:
 * <ul>
 *   <li>Уникальный логин (идентификатор)</li>
 *   <li>Пароль (хранится в открытом виде для простоты — в production нужен хэш)</li>
 *   <li>Персональный кошелёк с транзакциями и бюджетами</li>
 * </ul>
 */
public class User {

    private final String login;

    private final String password;

    private final Wallet wallet;

    /**
     * Создаёт нового пользователя с пустым кошельком.
     */
    public User(String login, String password) {
        this.login = login;
        this.password = password;
        this.wallet = new Wallet();
    }

    /**
     * Конструктор для восстановления из JSON с существующим кошельком.
     */
    public User(String login, String password, Wallet wallet) {
        this.login = login;
        this.password = password;
        this.wallet = wallet != null ? wallet : new Wallet();
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public Wallet getWallet() {
        return wallet;
    }

    /**
     * Проверяет соответствие введённого пароля.
     */
    public boolean checkPassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;

        return login.equalsIgnoreCase(user.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login.toLowerCase());
    }

    @Override
    public String toString() {
        return String.format("User{login='%s', balance=%.2f}", login, wallet.getBalance());
    }
}
