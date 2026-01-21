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
                if (!supabase.isConfigured()) {
                    throw new RuntimeException("Supabase ist nicht konfiguriert. Bitte füge deine Supabase-Zugangsdaten in gradle.properties hinzu.");
                }
                return supabase.select("customers", Customer.class, "order=name.asc");
            } catch (IOException e) {
                throw new RuntimeException("Fehler beim Laden der Kunden: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Get customer by ID
     */
    public CompletableFuture<Customer> getCustomerById(long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!supabase.isConfigured()) {
                    throw new RuntimeException("Supabase ist nicht konfiguriert.");
                }
                return supabase.selectSingle("customers", Customer.class, "id=eq." + id);
            } catch (IOException e) {
                throw new RuntimeException("Fehler beim Laden des Kunden: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Create new customer
     */
    public CompletableFuture<Customer> createCustomer(Customer customer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!supabase.isConfigured()) {
                    throw new RuntimeException("Supabase ist nicht konfiguriert. Kunde kann nicht erstellt werden.");
                }
                
                // Timestamps are set automatically by database triggers
                // No need to set them manually
                
                return supabase.insert("customers", customer, Customer.class);
            } catch (IOException e) {
                throw new RuntimeException("Fehler beim Erstellen des Kunden: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Update customer
     */
    public CompletableFuture<Customer> updateCustomer(Customer customer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!supabase.isConfigured()) {
                    throw new RuntimeException("Supabase ist nicht konfiguriert.");
                }
                
                // updated_at is set automatically by database trigger
                
                return supabase.update("customers", customer, "id=eq." + customer.getId(), Customer.class);
            } catch (IOException e) {
                throw new RuntimeException("Fehler beim Aktualisieren des Kunden: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Delete customer (CASCADE will delete associated customer_accounts)
     */
    public CompletableFuture<Void> deleteCustomer(long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!supabase.isConfigured()) {
                    throw new RuntimeException("Supabase ist nicht konfiguriert.");
                }
                supabase.delete("customers", "id=eq." + id);
            } catch (IOException e) {
                throw new RuntimeException("Fehler beim Löschen des Kunden: " + e.getMessage(), e);
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
