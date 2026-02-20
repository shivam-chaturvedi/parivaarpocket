package com.aditya.parivarpocket.model;

import java.time.LocalDate;

public class WalletEntry {
    private final WalletEntryType type;
    private final String category;
    private final double amount;
    private final String note;
    private final LocalDate date;

    public WalletEntry(WalletEntryType type, String category, double amount, String note, LocalDate date) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.note = note;
        this.date = date;
    }

    public WalletEntryType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public LocalDate getDate() {
        return date;
    }
}
