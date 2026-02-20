package com.aditya.parivarpocket.service;

import com.aditya.parivarpocket.model.User;
import com.aditya.parivarpocket.model.WalletEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class LocalStoreService {
    private final Path storageDir;
    private final CryptoService cryptoService;
    private final Gson gson;

    public LocalStoreService() {
        this.storageDir = Path.of(System.getProperty("user.home"), ".parivaarpocket");
        this.cryptoService = new CryptoService();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();
        ensureStorage();
    }

    private void ensureStorage() {
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialise storage folder", e);
        }
    }

    public List<WalletEntry> loadWalletEntries(User user) {
        Path file = walletFile(user);
        if (!Files.exists(file)) {
            return Collections.emptyList();
        }
        try {
            String encrypted = Files.readString(file, StandardCharsets.UTF_8);
            String json = cryptoService.decrypt(encrypted);
            WalletEntry[] entries = gson.fromJson(json, WalletEntry[].class);
            if (entries == null) {
                return Collections.emptyList();
            }
            return List.of(entries);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void saveWalletEntries(User user, List<WalletEntry> entries) {
        Path file = walletFile(user);
        try {
            String json = gson.toJson(entries);
            String encrypted = cryptoService.encrypt(json);
            Files.writeString(file, encrypted, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist wallet entries", e);
        }
    }

    private Path walletFile(User user) {
        String safeEmail = user.getEmail().replaceAll("[^a-zA-Z0-9]", "_");
        return storageDir.resolve("wallet_" + safeEmail + ".dat");
    }
}
