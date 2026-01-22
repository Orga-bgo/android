package de.babixgo.monopolygo.database;

import android.util.Log;
import com.google.gson.JsonObject;
import de.babixgo.monopolygo.models.CustomerAccount;
import de.babixgo.monopolygo.utils.EncryptionHelper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for managing CustomerAccount data in Firebase Realtime Database
 * Handles encryption/decryption of passwords
 * Provides async operations using CompletableFuture
 */
public class CustomerAccountRepository {
    private static final String TAG = "CustomerAccountRepository";
    private final FirebaseManager firebase;
    private CustomerActivityRepository activityRepository;
    private static final String COLLECTION = "customer_accounts";

    public CustomerAccountRepository() {
        this.firebase = FirebaseManager.getInstance();
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
            Log.d(TAG, "Creating customer account for customer: " + account.getCustomerId());
            
            String now = getCurrentTimestamp();
            account.setCreatedAt(now);
            account.setUpdatedAt(now);
            
            // Encrypt password if present
            if (account.getCredentialsPassword() != null && !account.getCredentialsPassword().isEmpty()) {
                String encrypted = EncryptionHelper.encrypt(account.getCredentialsPassword());
                account.setCredentialsPassword(encrypted);
            }
            
            // Generate ID if not set
            String id = account.getId() != 0 ? String.valueOf(account.getId()) : null;
            
            CustomerAccount created = firebase.save(COLLECTION, account, id).join();
            
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
        });
    }

    // ==================== READ ====================
    
    /**
     * Get all accounts for a specific customer
     * Automatically decrypts passwords
     */
    public CompletableFuture<List<CustomerAccount>> getAccountsByCustomerId(long customerId) {
        return firebase.getAll(COLLECTION, CustomerAccount.class)
            .thenApply(accounts -> {
                Log.d(TAG, "Loading accounts for customer: " + customerId);
                
                // Filter by customer_id client-side
                List<CustomerAccount> filtered = accounts.stream()
                    .filter(account -> account.getCustomerId() == customerId)
                    .collect(Collectors.toList());
                
                // Decrypt passwords
                for (CustomerAccount account : filtered) {
                    if (account.getCredentialsPassword() != null && !account.getCredentialsPassword().isEmpty()) {
                        String decrypted = EncryptionHelper.decrypt(account.getCredentialsPassword());
                        account.setCredentialsPassword(decrypted);
                    }
                }
                
                Log.d(TAG, "Loaded " + filtered.size() + " accounts");
                return filtered;
            });
    }
    
    /**
     * Get single customer account by ID
     * Automatically decrypts password
     */
    public CompletableFuture<CustomerAccount> getAccountById(long id) {
        return firebase.getById(COLLECTION, String.valueOf(id), CustomerAccount.class)
            .thenApply(account -> {
                Log.d(TAG, "Loading customer account: " + id);
                
                // Decrypt password
                if (account != null && account.getCredentialsPassword() != null && !account.getCredentialsPassword().isEmpty()) {
                    String decrypted = EncryptionHelper.decrypt(account.getCredentialsPassword());
                    account.setCredentialsPassword(decrypted);
                }
                
                return account;
            });
    }

    // ==================== UPDATE ====================
    
    /**
     * Update customer account with activity logging
     * Automatically encrypts password if changed
     */
    public CompletableFuture<Void> updateCustomerAccount(CustomerAccount account) {
        return CompletableFuture.runAsync(() -> {
            Log.d(TAG, "Updating customer account: " + account.getId());
            
            Map<String, Object> updates = buildUpdateMap(account);
            updates.put("updatedAt", getCurrentTimestamp());
            
            firebase.updateFields(COLLECTION, String.valueOf(account.getId()), updates).join();
            
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
        });
    }
    
    /**
     * Update backup reference for boost service
     */
    public CompletableFuture<Void> updateBackupReference(long customerAccountId, long accountId) {
        return CompletableFuture.runAsync(() -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("backupAccountId", accountId);
            updates.put("backupCreatedAt", getCurrentTimestamp());
            updates.put("updatedAt", getCurrentTimestamp());
            
            firebase.updateFields(COLLECTION, String.valueOf(customerAccountId), updates).join();
        });
    }

    // ==================== DELETE ====================
    
    /**
     * Delete customer account with activity logging
     */
    public CompletableFuture<Void> deleteCustomerAccount(long id) {
        return CompletableFuture.runAsync(() -> {
            Log.d(TAG, "Deleting customer account: " + id);
            
            // Get account info before deleting for activity log
            CustomerAccount account = firebase.getById(COLLECTION, String.valueOf(id), CustomerAccount.class).join();
            
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
            
            firebase.delete(COLLECTION, String.valueOf(id)).join();
            Log.d(TAG, "Customer account deleted successfully");
        });
    }

    // ==================== HELPER ====================
    
    /**
     * Build update map from CustomerAccount for update operations
     */
    private Map<String, Object> buildUpdateMap(CustomerAccount account) {
        Map<String, Object> updates = new HashMap<>();
        
        updates.put("customerId", account.getCustomerId());
        
        if (account.getIngameName() != null) {
            updates.put("ingameName", account.getIngameName());
        }
        if (account.getFriendLink() != null) {
            updates.put("friendLink", account.getFriendLink());
        }
        if (account.getFriendCode() != null) {
            updates.put("friendCode", account.getFriendCode());
        }
        
        // Services
        updates.put("servicePartner", account.isServicePartner());
        updates.put("serviceRace", account.isServiceRace());
        updates.put("serviceBoost", account.isServiceBoost());
        
        // Partner-specific
        if (account.isServicePartner()) {
            updates.put("partnerCount", account.getPartnerCount());
        }
        
        // Boost-specific
        if (account.isServiceBoost()) {
            if (account.getBackupAccountId() != null) {
                updates.put("backupAccountId", account.getBackupAccountId());
            }
            if (account.getBackupCreatedAt() != null) {
                updates.put("backupCreatedAt", account.getBackupCreatedAt());
            }
            if (account.getCredentialsUsername() != null) {
                updates.put("credentialsUsername", account.getCredentialsUsername());
            }
            if (account.getCredentialsPassword() != null && !account.getCredentialsPassword().isEmpty()) {
                String encrypted = EncryptionHelper.encrypt(account.getCredentialsPassword());
                updates.put("credentialsPassword", encrypted);
            }
        }
        
        return updates;
    }
    
    /**
     * Helper method for current timestamp in ISO 8601 format
     * Uses system default locale for consistency with CustomerRepository
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
