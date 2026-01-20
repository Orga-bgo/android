package de.babixgo.monopolygo.database;

import de.babixgo.monopolygo.models.Customer;
import java.io.IOException;
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
    
    public CustomerRepository() {
        this.supabase = SupabaseManager.getInstance();
    }
    
    /**
     * Get all customers ordered by name
     */
    public CompletableFuture<List<Customer>> getAllCustomers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supabase.select("customers", Customer.class, "order=name.asc");
            } catch (IOException e) {
                throw new RuntimeException("Failed to load customers", e);
            }
        });
    }
    
    /**
     * Get customer by ID
     */
    public CompletableFuture<Customer> getCustomerById(long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supabase.selectSingle("customers", Customer.class, "id=eq." + id);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load customer", e);
            }
        });
    }
    
    /**
     * Create new customer
     */
    public CompletableFuture<Customer> createCustomer(Customer customer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String now = getCurrentTimestamp();
                customer.setCreatedAt(now);
                customer.setUpdatedAt(now);
                
                return supabase.insert("customers", customer, Customer.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create customer", e);
            }
        });
    }
    
    /**
     * Update customer
     */
    public CompletableFuture<Customer> updateCustomer(Customer customer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                customer.setUpdatedAt(getCurrentTimestamp());
                
                return supabase.update("customers", customer, "id=eq." + customer.getId(), Customer.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update customer", e);
            }
        });
    }
    
    /**
     * Delete customer
     */
    public CompletableFuture<Void> deleteCustomer(long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                supabase.delete("customers", "id=eq." + id);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete customer", e);
            }
        });
    }
    
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
