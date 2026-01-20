package de.babixgo.monopolygo.database;

import de.babixgo.monopolygo.models.Account;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Repository for managing Account data in Supabase
 * Provides async operations using CompletableFuture
 */
public class AccountRepository {
    private final SupabaseManager supabase;
    
    public AccountRepository() {
        this.supabase = SupabaseManager.getInstance();
    }
    
    /**
     * Alle Accounts laden (nicht gelöscht)
     */
    public CompletableFuture<List<Account>> getAllAccounts() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supabase.select("accounts", Account.class, "deleted_at=is.null&order=name.asc");
            } catch (IOException e) {
                throw new RuntimeException("Failed to load accounts", e);
            }
        });
    }
    
    /**
     * Account nach ID laden
     */
    public CompletableFuture<Account> getAccountById(long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supabase.selectSingle("accounts", Account.class, "id=eq." + id);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load account", e);
            }
        });
    }
    
    /**
     * Account nach Name laden
     */
    public CompletableFuture<Account> getAccountByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supabase.selectSingle("accounts", Account.class, 
                    "name=eq." + name + "&deleted_at=is.null");
            } catch (IOException e) {
                return null; // Account existiert nicht
            }
        });
    }
    
    /**
     * Neuen Account erstellen
     */
    public CompletableFuture<Account> createAccount(Account account) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Set timestamps
                String now = getCurrentTimestamp();
                account.setCreatedAt(now);
                account.setUpdatedAt(now);
                
                return supabase.insert("accounts", account, Account.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create account", e);
            }
        });
    }
    
    /**
     * Account aktualisieren
     */
    public CompletableFuture<Account> updateAccount(Account account) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Set updated timestamp
                account.setUpdatedAt(getCurrentTimestamp());
                
                return supabase.update("accounts", account, "id=eq." + account.getId(), Account.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update account", e);
            }
        });
    }
    
    /**
     * Account löschen (soft delete)
     */
    public CompletableFuture<Void> deleteAccount(long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                Account account = new Account();
                account.setDeletedAt(getCurrentTimestamp());
                
                supabase.update("accounts", account, "id=eq." + id, Account.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete account", e);
            }
        });
    }
    
    /**
     * Last Played Timestamp aktualisieren
     */
    public CompletableFuture<Void> updateLastPlayed(long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                Account account = new Account();
                account.setLastPlayed(getCurrentTimestamp());
                account.setUpdatedAt(getCurrentTimestamp());
                
                supabase.update("accounts", account, "id=eq." + id, Account.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update last played", e);
            }
        });
    }
    
    /**
     * Account Status aktualisieren
     */
    public CompletableFuture<Void> updateAccountStatus(long id, String status) {
        return CompletableFuture.runAsync(() -> {
            try {
                Account account = new Account();
                account.setAccountStatus(status);
                account.setUpdatedAt(getCurrentTimestamp());
                
                supabase.update("accounts", account, "id=eq." + id, Account.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update account status", e);
            }
        });
    }
    
    /**
     * Device IDs aktualisieren
     */
    public CompletableFuture<Void> updateDeviceIds(long id, String ssaid, String gaid, String deviceId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Account account = new Account();
                account.setSsaid(ssaid);
                account.setGaid(gaid);
                account.setDeviceId(deviceId);
                account.setUpdatedAt(getCurrentTimestamp());
                
                supabase.update("accounts", account, "id=eq." + id, Account.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update device IDs", e);
            }
        });
    }
    
    /**
     * Suspension Counts aktualisieren
     */
    public CompletableFuture<Void> updateSuspensionCounts(long id, int days0, int days3, int days7, boolean permanent) {
        return CompletableFuture.runAsync(() -> {
            try {
                Account account = new Account();
                account.setSuspension0Days(days0);
                account.setSuspension3Days(days3);
                account.setSuspension7Days(days7);
                account.setSuspensionPermanent(permanent);
                account.setUpdatedAt(getCurrentTimestamp());
                
                supabase.update("accounts", account, "id=eq." + id, Account.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update suspension counts", e);
            }
        });
    }
    
    /**
     * Notiz aktualisieren
     */
    public CompletableFuture<Void> updateNote(long id, String note) {
        return CompletableFuture.runAsync(() -> {
            try {
                Account account = new Account();
                account.setNote(note);
                account.setUpdatedAt(getCurrentTimestamp());
                
                supabase.update("accounts", account, "id=eq." + id, Account.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update note", e);
            }
        });
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
     * Check if Supabase is configured
     */
    public boolean isSupabaseConfigured() {
        return supabase.isConfigured();
    }
}
