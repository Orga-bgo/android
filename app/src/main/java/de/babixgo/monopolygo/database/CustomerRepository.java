package de.babixgo.monopolygo.database;

import de.babixgo.monopolygo.models.Customer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Repository for managing Customer data
 * STUB IMPLEMENTATION - No Supabase calls yet, returns empty/dummy data
 * This prevents crashes while UI is being developed
 */
public class CustomerRepository {
    // In-memory storage for stub implementation
    private final List<Customer> customers = new ArrayList<>();
    private long nextId = 1;
    
    public CustomerRepository() {
        // No Supabase needed for stub
    }
    
    /**
     * Get all customers ordered by name
     * STUB: Returns empty list for now
     */
    public CompletableFuture<List<Customer>> getAllCustomers() {
        return CompletableFuture.supplyAsync(() -> {
            // Return copy of in-memory list
            return new ArrayList<>(customers);
        });
    }
    
    /**
     * Get customer by ID
     * STUB: Returns null if not found in memory
     */
    public CompletableFuture<Customer> getCustomerById(long id) {
        return CompletableFuture.supplyAsync(() -> {
            for (Customer c : customers) {
                if (c.getId() == id) {
                    return c;
                }
            }
            return null;
        });
    }
    
    /**
     * Create new customer
     * STUB: Stores in memory with dummy ID
     */
    public CompletableFuture<Customer> createCustomer(Customer customer) {
        return CompletableFuture.supplyAsync(() -> {
            // Set dummy ID
            customer.setId(nextId++);
            
            // Set timestamps
            String now = getCurrentTimestamp();
            customer.setCreatedAt(now);
            customer.setUpdatedAt(now);
            
            // Add to in-memory list
            customers.add(customer);
            
            return customer;
        });
    }
    
    /**
     * Update customer
     * STUB: Updates in memory
     */
    public CompletableFuture<Customer> updateCustomer(Customer customer) {
        return CompletableFuture.supplyAsync(() -> {
            customer.setUpdatedAt(getCurrentTimestamp());
            
            // Update in memory
            for (int i = 0; i < customers.size(); i++) {
                if (customers.get(i).getId() == customer.getId()) {
                    customers.set(i, customer);
                    break;
                }
            }
            
            return customer;
        });
    }
    
    /**
     * Delete customer
     * STUB: Removes from memory
     */
    public CompletableFuture<Void> deleteCustomer(long id) {
        return CompletableFuture.runAsync(() -> {
            customers.removeIf(c -> c.getId() == id);
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
     * STUB: Always returns false for now
     */
    public boolean isSupabaseConfigured() {
        return false;
    }
}
