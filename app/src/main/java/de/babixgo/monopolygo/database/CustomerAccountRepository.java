package de.babixgo.monopolygo.database;

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
    private final SupabaseManager supabase;

    public CustomerAccountRepository() {
        this.supabase = SupabaseManager.getInstance();
    }

    // ==================== CREATE ====================
    
    /**
     * Create new customer account
     * Automatically encrypts password before storing
     */
    public CompletableFuture<CustomerAccount> createCustomerAccount(CustomerAccount account) {
        return CompletableFuture.supplyAsync(() -> {
            try {
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
                
                return created;
                
            } catch (IOException e) {
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
                
                return accounts;
                
            } catch (IOException e) {
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
                throw new RuntimeException("Failed to load customer account: " + e.getMessage(), e);
            }
        });
    }

    // ==================== UPDATE ====================
    
    /**
     * Update customer account
     * Automatically encrypts password if changed
     */
    public CompletableFuture<CustomerAccount> updateCustomerAccount(CustomerAccount account) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                account.setUpdatedAt(getCurrentTimestamp());
                
                // Encrypt password if present
                if (account.getCredentialsPassword() != null && !account.getCredentialsPassword().isEmpty()) {
                    String encrypted = EncryptionHelper.encrypt(account.getCredentialsPassword());
                    account.setCredentialsPassword(encrypted);
                }
                
                CustomerAccount updated = supabase.update(
                    "customer_accounts", 
                    account, 
                    "id=eq." + account.getId(), 
                    CustomerAccount.class
                );
                
                // Decrypt password for returning to caller
                if (updated.getCredentialsPassword() != null && !updated.getCredentialsPassword().isEmpty()) {
                    String decrypted = EncryptionHelper.decrypt(updated.getCredentialsPassword());
                    updated.setCredentialsPassword(decrypted);
                }
                
                return updated;
                
            } catch (IOException e) {
                throw new RuntimeException("Failed to update customer account: " + e.getMessage(), e);
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
     * Delete customer account
     */
    public CompletableFuture<Void> deleteCustomerAccount(long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                supabase.delete("customer_accounts", "id=eq." + id);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete customer account: " + e.getMessage(), e);
            }
        });
    }

    // ==================== HELPER ====================
    
    /**
     * Helper method for current timestamp in ISO 8601 format
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
