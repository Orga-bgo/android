package de.babixgo.monopolygo.database;

import android.util.Log;
import com.google.gson.JsonObject;
import de.babixgo.monopolygo.models.CustomerAccount;
import de.babixgo.monopolygo.utils.EncryptionHelper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Repository for managing CustomerAccount data in Supabase
 * Handles encryption/decryption of passwords
 * Provides async operations using CompletableFuture
 */
public class CustomerAccountRepository {
    private static final String TAG = "CustomerAccountRepository";
    private final SupabaseManager supabase;
    private CustomerActivityRepository activityRepository;

    public CustomerAccountRepository() {
        this.supabase = SupabaseManager.getInstance();
    }
    
    /**
     * Set activity repository for logging (lazy init to avoid circular dependency)
     */
    private CustomerActivityRepository getActivityRepository() {
        if (activityRepository == null) {
            activityRepository = new CustomerActivityRepository();
        }
        return activityRepository;
    }

    // ==================== CREATE ====================
    
    /**
     * Create new customer account with activity logging
     * Automatically encrypts password before storing
     */
    public CompletableFuture<CustomerAccount> createCustomerAccount(CustomerAccount account) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Creating customer account for customer: " + account.getCustomerId());
                
                String now = getCurrentTimestamp();
                account.setCreatedAt(now);
                account.setUpdatedAt(now);
                
                // Encrypt password if present
                if (account.getCredentialsPassword() != null && !account.getCredentialsPassword().isEmpty()) {
                    String encrypted = EncryptionHelper.encrypt(account.getCredentialsPassword());
                    account.setCredentialsPassword(encrypted);
                }
                
                CustomerAccount created = supabase.insert("customer_accounts", account, CustomerAccount.class);
                
                // Decrypt password for returning to caller
                if (created.getCredentialsPassword() != null && !created.getCredentialsPassword().isEmpty()) {
                    String decrypted = EncryptionHelper.decrypt(created.getCredentialsPassword());
                    created.setCredentialsPassword(decrypted);
                }
                
                // Log activity with customer_account_id
                getActivityRepository().logActivity(
                    created.getCustomerId(),
                    "account_add",
                    "account",
                    "Account hinzugefügt: " + (created.getIngameName() != null ? created.getIngameName() : "Unbekannt"),
                    created.getId()
                ).exceptionally(e -> {
                    Log.e(TAG, "Failed to log activity for account creation", e);
                    return null;
                });
                
                Log.d(TAG, "Customer account created with ID: " + created.getId());
                return created;
                
            } catch (IOException e) {
                Log.e(TAG, "Create customer account failed", e);
                throw new RuntimeException("Failed to create customer account: " + e.getMessage(), e);
            }
        });
    }

    // ==================== READ ====================
    
    /**
     * Get all accounts for a specific customer
     * Automatically decrypts passwords
     */
    public CompletableFuture<List<CustomerAccount>> getAccountsByCustomerId(long customerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Loading accounts for customer: " + customerId);
                
                List<CustomerAccount> accounts = supabase.select(
                    "customer_accounts", 
                    CustomerAccount.class, 
                    "customer_id=eq." + customerId
                );
                
                // Decrypt passwords
                for (CustomerAccount account : accounts) {
                    if (account.getCredentialsPassword() != null && !account.getCredentialsPassword().isEmpty()) {
                        String decrypted = EncryptionHelper.decrypt(account.getCredentialsPassword());
                        account.setCredentialsPassword(decrypted);
                    }
                }
                
                Log.d(TAG, "Loaded " + accounts.size() + " accounts");
                return accounts;
                
            } catch (IOException e) {
                Log.e(TAG, "Get customer accounts failed", e);
                throw new RuntimeException("Failed to load customer accounts: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Get single customer account by ID
     * Automatically decrypts password
     */
    public CompletableFuture<CustomerAccount> getAccountById(long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.d(TAG, "Loading customer account: " + id);
                
                CustomerAccount account = supabase.selectSingle(
                    "customer_accounts", 
                    CustomerAccount.class, 
                    "id=eq." + id
                );
                
                // Decrypt password
                if (account.getCredentialsPassword() != null && !account.getCredentialsPassword().isEmpty()) {
                    String decrypted = EncryptionHelper.decrypt(account.getCredentialsPassword());
                    account.setCredentialsPassword(decrypted);
                }
                
                return account;
                
            } catch (IOException e) {
                Log.e(TAG, "Get customer account failed", e);
                throw new RuntimeException("Failed to load customer account: " + e.getMessage(), e);
            }
        });
    }

    // ==================== UPDATE ====================
    
    /**
     * Update customer account with activity logging
     * Automatically encrypts password if changed
     */
    public CompletableFuture<Void> updateCustomerAccount(CustomerAccount account) {
        return CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "Updating customer account: " + account.getId());
                
                JsonObject json = buildJsonObject(account);
                json.addProperty("updated_at", java.time.Instant.now().toString());
                
                supabase.update(
                    "customer_accounts", 
                    "id=eq." + account.getId(), 
                    json.toString()
                );
                
                // Log activity with customer_account_id
                getActivityRepository().logActivity(
                    account.getCustomerId(),
                    "account_update",
                    "account",
                    "Account aktualisiert: " + (account.getIngameName() != null ? account.getIngameName() : "ID " + account.getId()),
                    account.getId()
                ).exceptionally(e -> {
                    Log.e(TAG, "Failed to log activity for account update", e);
                    return null;
                });
                
                Log.d(TAG, "Customer account updated successfully");
                
            } catch (Exception e) {
                Log.e(TAG, "Update customer account failed", e);
                throw new RuntimeException("Update customer account failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Update backup reference for boost service
     */
    public CompletableFuture<Void> updateBackupReference(long customerAccountId, long accountId) {
        return CompletableFuture.runAsync(() -> {
            try {
                CustomerAccount account = new CustomerAccount();
                account.setId(customerAccountId);
                account.setBackupAccountId(accountId);
                account.setBackupCreatedAt(getCurrentTimestamp());
                account.setUpdatedAt(getCurrentTimestamp());
                
                supabase.update(
                    "customer_accounts", 
                    account, 
                    "id=eq." + customerAccountId, 
                    CustomerAccount.class
                );
                
            } catch (IOException e) {
                throw new RuntimeException("Failed to update backup reference: " + e.getMessage(), e);
            }
        });
    }

    // ==================== DELETE ====================
    
    /**
     * Delete customer account with activity logging
     */
    public CompletableFuture<Void> deleteCustomerAccount(long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                Log.d(TAG, "Deleting customer account: " + id);
                
                // Get account info before deleting for activity log
                CustomerAccount account = supabase.selectSingle("customer_accounts", CustomerAccount.class, "id=eq." + id);
                
                if (account != null) {
                    // Log activity before deletion with customer_account_id
                    getActivityRepository().logActivity(
                        account.getCustomerId(),
                        "account_delete",
                        "account",
                        "Account gelöscht: " + (account.getIngameName() != null ? account.getIngameName() : "ID " + id),
                        id
                    ).exceptionally(e -> {
                        Log.e(TAG, "Failed to log activity for account deletion", e);
                        return null;
                    });
                }
                
                supabase.delete("customer_accounts", "id=eq." + id);
                Log.d(TAG, "Customer account deleted successfully");
                
            } catch (Exception e) {
                Log.e(TAG, "Delete customer account failed", e);
                throw new RuntimeException("Delete customer account failed: " + e.getMessage(), e);
            }
        });
    }

    // ==================== HELPER ====================
    
    /**
     * Build JsonObject from CustomerAccount for update operations
     */
    private JsonObject buildJsonObject(CustomerAccount account) {
        JsonObject json = new JsonObject();
        
        json.addProperty("customer_id", account.getCustomerId());
        
        if (account.getIngameName() != null) {
            json.addProperty("ingame_name", account.getIngameName());
        }
        if (account.getFriendLink() != null) {
            json.addProperty("friend_link", account.getFriendLink());
        }
        if (account.getFriendCode() != null) {
            json.addProperty("friend_code", account.getFriendCode());
        }
        
        // Services
        json.addProperty("service_partner", account.isServicePartner());
        json.addProperty("service_race", account.isServiceRace());
        json.addProperty("service_boost", account.isServiceBoost());
        
        // Partner-specific
        if (account.isServicePartner()) {
            json.addProperty("partner_count", account.getPartnerCount());
        }
        
        // Boost-specific
        if (account.isServiceBoost()) {
            if (account.getBackupAccountId() != null) {
                json.addProperty("backup_account_id", account.getBackupAccountId());
            }
            if (account.getBackupCreatedAt() != null) {
                json.addProperty("backup_created_at", account.getBackupCreatedAt());
            }
            if (account.getCredentialsUsername() != null) {
                json.addProperty("credentials_username", account.getCredentialsUsername());
            }
            if (account.getCredentialsPassword() != null && !account.getCredentialsPassword().isEmpty()) {
                String encrypted = EncryptionHelper.encrypt(account.getCredentialsPassword());
                json.addProperty("credentials_password", encrypted);
            }
        }
        
        return json;
    }
    
    /**
     * Helper method for current timestamp in ISO 8601 format
     * Uses system default locale for consistency with CustomerRepository
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
