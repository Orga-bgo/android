package de.babixgo.monopolygo.database;

import android.util.Log;
import de.babixgo.monopolygo.models.Customer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for managing Customer data in Firebase Realtime Database
 * Provides async operations using CompletableFuture
 */
public class CustomerRepository {
    private static final String TAG = "CustomerRepository";
    private final FirebaseManager firebase;
    private final CustomerAccountRepository accountRepository;
    private final CustomerActivityRepository activityRepository;
    private static final String COLLECTION = "customers";
    
    public CustomerRepository() {
        this.firebase = FirebaseManager.getInstance();
        this.accountRepository = new CustomerAccountRepository();
        this.activityRepository = new CustomerActivityRepository();
    }
    
    /**
     * Get all customers ordered by name
     * @param loadAccounts If true, loads accounts for each customer
     */
    public CompletableFuture<List<Customer>> getAllCustomers(boolean loadAccounts) {
        return firebase.getAll(COLLECTION, Customer.class)
            .thenApply(customers -> {
                // Sort by name client-side
                List<Customer> sortedCustomers = customers.stream()
                    .sorted((a, b) -> {
                        String nameA = a.getName() != null ? a.getName() : "";
                        String nameB = b.getName() != null ? b.getName() : "";
                        return nameA.compareToIgnoreCase(nameB);
                    })
                    .collect(Collectors.toList());

                if (loadAccounts) {
                    // Load accounts for each customer in parallel and wait for all to complete
                    List<CompletableFuture<Void>> accountFutures = new ArrayList<>();
                    for (Customer customer : sortedCustomers) {
                        CompletableFuture<Void> future = accountRepository
                                .getAccountsByCustomerId(customer.getId())
                                .thenAccept(accounts -> {
                                    if (accounts != null) {
                                        customer.setAccounts(accounts);
                                    } else {
                                        customer.setAccounts(new ArrayList<>());
                                    }
                                })
                                .exceptionally(e -> {
                                    // Log error but continue with other customers
                                    Log.e(TAG, "Failed to load accounts for customer: " + customer.getId(), (Throwable) e);
                                    customer.setAccounts(new ArrayList<>());
                                    return null;
                                });
                        accountFutures.add(future);
                    }

                    // Wait for all account-loading operations to complete
                    CompletableFuture.allOf(accountFutures.toArray(new CompletableFuture[0])).join();
                }
                return sortedCustomers;
            });
    }
    
    /**
     * Get all customers ordered by name (without loading accounts)
     */
    public CompletableFuture<List<Customer>> getAllCustomers() {
        return getAllCustomers(false);
    }
    
    /**
     * Get customer by ID
     * @param loadAccounts If true, loads accounts for the customer
     */
    public CompletableFuture<Customer> getCustomerById(long id, boolean loadAccounts) {
        return firebase.getById(COLLECTION, String.valueOf(id), Customer.class)
            .thenApply(customer -> {
                if (loadAccounts && customer != null) {
                    try {
                        List<de.babixgo.monopolygo.models.CustomerAccount> accounts = 
                            accountRepository.getAccountsByCustomerId(customer.getId()).get();
                        customer.setAccounts(accounts);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to load accounts for customer: " + id, e);
                        customer.setAccounts(new ArrayList<>());
                    }
                }
                
                return customer;
            });
    }
    
    /**
     * Get customer by ID (without loading accounts)
     */
    public CompletableFuture<Customer> getCustomerById(long id) {
        return getCustomerById(id, false);
    }
    
    /**
     * Create new customer with activity logging
     */
    public CompletableFuture<Customer> createCustomer(Customer customer) {
        return CompletableFuture.supplyAsync(() -> {
            if (!firebase.isConfigured()) {
                throw new RuntimeException("Firebase ist nicht konfiguriert.");
            }
            
            // Set timestamps
            String now = getCurrentTimestamp();
            customer.setCreatedAt(now);
            customer.setUpdatedAt(now);
            
            // Generate ID if not set
            String id = customer.getId() != 0 ? String.valueOf(customer.getId()) : null;
            
            Customer created = firebase.save(COLLECTION, customer, id).join();
            
            // Log activity
            activityRepository.logActivity(
                created.getId(), 
                "create", 
                "customer", 
                "Kunde erstellt: " + created.getName()
            ).exceptionally(e -> {
                // Log error but don't fail the operation
                Log.e(TAG, "Failed to log customer creation activity", (Throwable) e);
                return null;
            });
            
            return created;
        });
    }
    
    /**
     * Update customer with activity logging
     */
    public CompletableFuture<Customer> updateCustomer(Customer customer) {
        return CompletableFuture.supplyAsync(() -> {
            // Set updated timestamp
            customer.setUpdatedAt(getCurrentTimestamp());
            
            Customer updated = firebase.save(COLLECTION, customer, String.valueOf(customer.getId())).join();
            
            // Log activity
            activityRepository.logActivity(
                updated.getId(), 
                "update", 
                "customer", 
                "Kundendaten aktualisiert: " + updated.getName()
            ).exceptionally(e -> {
                Log.e(TAG, "Failed to log customer update activity", (Throwable) e);
                return null;
            });
            
            return updated;
        });
    }
    
    /**
     * Delete customer (soft delete)
     * Note: Associated customer_accounts should also be soft-deleted
     */
    public CompletableFuture<Void> deleteCustomer(long id) {
        return CompletableFuture.runAsync(() -> {
            // Get customer name before deleting for activity log
            Customer customer = firebase.getById(COLLECTION, String.valueOf(id), Customer.class).join();
            String customerName = customer != null ? customer.getName() : "Unbekannt";
            
            // Log activity before deletion
            activityRepository.logActivity(
                id, 
                "delete", 
                "customer", 
                "Kunde gelöscht: " + customerName
            ).exceptionally(e -> {
                Log.e(TAG, "Failed to log customer deletion activity", (Throwable) e);
                return null;
            });
            
            // Soft delete customer
            Map<String, Object> updates = new HashMap<>();
            updates.put("deletedAt", getCurrentTimestamp());
            firebase.updateFields(COLLECTION, String.valueOf(id), updates).join();
        });
    }
    
    /**
     * Check if Firebase is configured
     */
    public boolean isFirebaseConfigured() {
        return firebase.isConfigured();
    }
    
    /**
     * Helper method for current timestamp in ISO 8601 format
     * Creates a new SimpleDateFormat instance for thread safety
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
