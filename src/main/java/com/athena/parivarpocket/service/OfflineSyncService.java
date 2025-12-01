package com.athena.parivarpocket.service;

import java.util.LinkedList;
import java.util.List;

public class OfflineSyncService {
    private final List<String> pendingOperations = new LinkedList<>();
    private boolean offlineMode = false;

    public void setOfflineMode(boolean offline) {
        this.offlineMode = offline;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public void queueOperation(String description) {
        pendingOperations.add(description);
    }

    public int syncNow() {
        int synced = pendingOperations.size();
        pendingOperations.clear();
        return synced;
    }

    public List<String> getPendingOperations() {
        return List.copyOf(pendingOperations);
    }
}
