package ru.mifi.financemanager.domain;

/**
 * Тип финансовой операции.
 *
 * Enum обеспечивает type-safety и исключает невалидные типы операций.
 */
public enum TransactionType {
    /** Доход — поступление денежных средств */
    INCOME,

    /** Расход — списание денежных средств */
    EXPENSE
}
