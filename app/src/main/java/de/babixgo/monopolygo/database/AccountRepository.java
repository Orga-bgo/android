package de.babixgo.monopolygo.database;

import de.babixgo.monopolygo.models.Account;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for managing Account data in Firebase Realtime Database
 * Provides async operations using CompletableFuture
 */
public class AccountRepository {
    private final FirebaseManager firebase;
    private static final String COLLECTION = "accounts";
    
    public AccountRepository() {
        this.firebase = FirebaseManager.getInstance();
    }
    
    /**
     * Alle Accounts laden (nicht gelöscht)
     */
    public CompletableFuture<List<Account>> getAllAccounts() {
        return firebase.getAll(COLLECTION, Account.class)
            .thenApply(accounts -> accounts.stream()
                .filter(account -> account.getDeletedAt() == null || account.getDeletedAt().isEmpty())
                .sorted((a, b) -> {
                    String nameA = a.getName() != null ? a.getName() : "";
                    String nameB = b.getName() != null ? b.getName() : "";
                    return nameA.compareToIgnoreCase(nameB);
                })
                .collect(Collectors.toList()));
    }
    
    /**
     * Lädt alle Accounts, die NICHT zu Kunden gehören
     * (für AccountListFragment)
     */
    public CompletableFuture<List<Account>> getNonCustomerAccounts() {
        return firebase.getAll(COLLECTION, Account.class)
            .thenApply(accounts -> accounts.stream()
                .filter(account -> (account.getDeletedAt() == null || account.getDeletedAt().isEmpty()) &&
                                 !account.isCustomerAccount())
                .sorted((a, b) -> {
                    String nameA = a.getName() != null ? a.getName() : "";
                    String nameB = b.getName() != null ? b.getName() : "";
                    return nameA.compareToIgnoreCase(nameB);
                })
                .collect(Collectors.toList()));
    }
    
    /**
     * Account nach ID laden
     */
    public CompletableFuture<Account> getAccountById(long id) {
        return firebase.getById(COLLECTION, String.valueOf(id), Account.class);
    }
    
    /**
     * Account nach Name laden
     */
    public CompletableFuture<Account> getAccountByName(String name) {
        return firebase.getByField(COLLECTION, "name", name, Account.class)
            .thenApply(account -> {
                // Filter out deleted accounts
                if (account != null && (account.getDeletedAt() == null || account.getDeletedAt().isEmpty())) {
                    return account;
                }
                return null;
            });
    }
    
    /**
     * Neuen Account erstellen
     */
    public CompletableFuture<Account> createAccount(Account account) {
        if (!firebase.isConfigured()) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Firebase ist nicht konfiguriert. Account wurde lokal gesichert, aber nicht in der Datenbank gespeichert.")
            );
        }
        
        // Set timestamps
        String now = getCurrentTimestamp();
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        
        // Generate ID if not set
        String id = account.getId() != 0 ? String.valueOf(account.getId()) : null;
        
        return firebase.save(COLLECTION, account, id);
    }
    
    /**
     * Account aktualisieren
     */
    public CompletableFuture<Account> updateAccount(Account account) {
        // Set updated timestamp
        account.setUpdatedAt(getCurrentTimestamp());
        
        return firebase.save(COLLECTION, account, String.valueOf(account.getId()));
    }
    
    /**
     * Account löschen (soft delete)
     */
    public CompletableFuture<Void> deleteAccount(long id) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("deletedAt", getCurrentTimestamp());
        
        return firebase.updateFields(COLLECTION, String.valueOf(id), updates);
    }
    
    /**
     * Last Played Timestamp aktualisieren
     */
    public CompletableFuture<Void> updateLastPlayed(long id) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastPlayed", getCurrentTimestamp());
        updates.put("updatedAt", getCurrentTimestamp());
        
        return firebase.updateFields(COLLECTION, String.valueOf(id), updates);
    }
    
    /**
     * Account Status aktualisieren
     */
    public CompletableFuture<Void> updateAccountStatus(long id, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("accountStatus", status);
        updates.put("updatedAt", getCurrentTimestamp());
        
        return firebase.updateFields(COLLECTION, String.valueOf(id), updates);
    }
    
    /**
     * Device IDs aktualisieren
     */
    public CompletableFuture<Void> updateDeviceIds(long id, String ssaid, String gaid, String deviceId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("ssaid", ssaid);
        updates.put("gaid", gaid);
        updates.put("deviceId", deviceId);
        updates.put("updatedAt", getCurrentTimestamp());
        
        return firebase.updateFields(COLLECTION, String.valueOf(id), updates);
    }
    
    /**
     * Suspension Status aktualisieren
     */
    public CompletableFuture<Void> updateSuspensionStatus(long id, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("suspensionStatus", status);
        updates.put("updatedAt", getCurrentTimestamp());
        
        return firebase.updateFields(COLLECTION, String.valueOf(id), updates);
    }
    
    /**
     * Notiz aktualisieren
     */
    public CompletableFuture<Void> updateNote(long id, String note) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("note", note);
        updates.put("updatedAt", getCurrentTimestamp());
        
        return firebase.updateFields(COLLECTION, String.valueOf(id), updates);
    }
    
    /**
     * Hilfsmethode für aktuellen Timestamp im ISO 8601 Format
     * Creates a new SimpleDateFormat instance for thread safety
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * Check if Firebase is configured
     */
    public boolean isFirebaseConfigured() {
        return firebase.isConfigured();
    }
}
