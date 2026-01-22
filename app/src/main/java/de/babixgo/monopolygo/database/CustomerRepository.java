package de.babixgo.monopolygo.database;

import de.babixgo.monopolygo.models.Customer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Repository for managing Customer data in Supabase
 * Provides async operations using CompletableFuture
 */
public class CustomerRepository {
    private final SupabaseManager supabase;
    private final CustomerAccountRepository accountRepository;
    private final CustomerActivityRepository activityRepository;
    
    public CustomerRepository() {
        this.supabase = SupabaseManager.getInstance();
        this.accountRepository = new CustomerAccountRepository();
        this.activityRepository = new CustomerActivityRepository();
    }
    
    /**
     * Get all customers ordered by name
     * @param loadAccounts If true, loads accounts for each customer
     */
    public CompletableFuture<List<Customer>> getAllCustomers(boolean loadAccounts) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConfigured();
                List<Customer> customers = supabase.select("customers", Customer.class, "order=name.asc");

                if (loadAccounts) {
                    // Load accounts for each customer in parallel and wait for all to complete
                    List<CompletableFuture<Void>> accountFutures = new ArrayList<>();
                    for (Customer customer : customers) {
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
                                    customer.setAccounts(new ArrayList<>());
                                    return null;
                                });
                        accountFutures.add(future);
                    }

                    // Wait for all account-loading operations to complete
                    CompletableFuture.allOf(accountFutures.toArray(new CompletableFuture[0])).join();
                }
                return customers;
            } catch (IOException e) {
                throw wrapIOException("Fehler beim Laden der Kunden", e);
            }
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConfigured();
                Customer customer = supabase.selectSingle("customers", Customer.class, "id=eq." + id);
                
                if (loadAccounts && customer != null) {
                    try {
                        List<de.babixgo.monopolygo.models.CustomerAccount> accounts = 
                            accountRepository.getAccountsByCustomerId(customer.getId()).get();
                        customer.setAccounts(accounts);
                    } catch (Exception e) {
                        customer.setAccounts(new ArrayList<>());
                    }
                }
                
                return customer;
            } catch (IOException e) {
                throw wrapIOException("Fehler beim Laden des Kunden", e);
            }
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
            try {
                ensureConfigured();
                
                // Timestamps are set automatically by database triggers
                // No need to set them manually
                
                Customer created = supabase.insert("customers", customer, Customer.class);
                
                // Log activity
                activityRepository.logActivity(
                    created.getId(), 
                    "create", 
                    "customer", 
                    "Kunde erstellt: " + created.getName()
                );
                
                return created;
            } catch (IOException e) {
                throw wrapIOException("Fehler beim Erstellen des Kunden", e);
            }
        });
    }
    
    /**
     * Update customer with activity logging
     */
    public CompletableFuture<Customer> updateCustomer(Customer customer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConfigured();
                
                // updated_at is set automatically by database trigger
                
                Customer updated = supabase.update("customers", customer, "id=eq." + customer.getId(), Customer.class);
                
                // Log activity
                activityRepository.logActivity(
                    updated.getId(), 
                    "update", 
                    "customer", 
                    "Kundendaten aktualisiert: " + updated.getName()
                );
                
                return updated;
            } catch (IOException e) {
                throw wrapIOException("Fehler beim Aktualisieren des Kunden", e);
            }
        });
    }
    
    /**
     * Delete customer (CASCADE will delete associated customer_accounts) with activity logging
     */
    public CompletableFuture<Void> deleteCustomer(long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureConfigured();
                
                // Get customer name before deleting for activity log
                Customer customer = supabase.selectSingle("customers", Customer.class, "id=eq." + id);
                String customerName = customer != null ? customer.getName() : "Unbekannt";
                
                // Log activity before deletion
                activityRepository.logActivity(
                    id, 
                    "delete", 
                    "customer", 
                    "Kunde gelöscht: " + customerName
                );
                
                // Delete customer (this will cascade delete activities too, but we've already logged the deletion)
                supabase.delete("customers", "id=eq." + id);
            } catch (IOException e) {
                throw wrapIOException("Fehler beim Löschen des Kunden", e);
            }
        });
    }
    
    /**
     * Check if Supabase is configured
     */
    public boolean isSupabaseConfigured() {
        return supabase.isConfigured();
    }
    
    /**
     * Ensure Supabase is configured, throw exception if not
     */
    private void ensureConfigured() {
        if (!supabase.isConfigured()) {
            throw new RuntimeException("Supabase ist nicht konfiguriert. Bitte füge deine Supabase-Zugangsdaten in gradle.properties hinzu.");
        }
    }
    
    /**
     * Wrap IOException with German error message
     */
    private RuntimeException wrapIOException(String message, IOException e) {
        return new RuntimeException(message + ": " + e.getMessage(), e);
    }
    
    /**
     * Helper method for current timestamp in ISO 8601 format
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
