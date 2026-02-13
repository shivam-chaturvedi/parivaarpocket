package com.athena.parivarpocket.model;

import java.util.Locale;

public enum WalletEntryType {
    INCOME,
    EXPENSE,
    SAVINGS,
    BUDGET

    ;

    public static WalletEntryType fromString(String value) {
        if (value == null || value.isBlank()) {
            return EXPENSE;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "income" -> INCOME;
            case "savings" -> SAVINGS;
            case "budget" -> BUDGET;
            default -> EXPENSE;
        };
    }
}
