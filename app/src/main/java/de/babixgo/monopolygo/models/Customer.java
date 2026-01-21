package de.babixgo.monopolygo.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Customer model for customer management
 * Represents a customer in the Supabase database
 */
public class Customer {
    @SerializedName("id")
    private long id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("notes")
    private String notes;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    // Transient - not in DB, will be populated when loaded
    private List<CustomerAccount> accounts = new ArrayList<>();
    
    // Constructors
    public Customer() {}
    
    public Customer(String name) {
        this.name = name;
    }
    
    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    public List<CustomerAccount> getAccounts() { return accounts; }
    public void setAccounts(List<CustomerAccount> accounts) { this.accounts = accounts; }
    
    // Helper methods
    
    /**
     * Number of accounts for this customer
     */
    public int getAccountCount() {
        return accounts != null ? accounts.size() : 0;
    }
    
    /**
     * Returns all services that this customer uses (across all accounts)
     * Format: "Partner / Race / Boost"
     */
    public String getServicesDisplay() {
        if (accounts == null || accounts.isEmpty()) {
            return "-";
        }
        
        Set<String> services = new HashSet<>();
        for (CustomerAccount acc : accounts) {
            if (acc.isServicePartner()) services.add("Partner");
            if (acc.isServiceRace()) services.add("Race");
            if (acc.isServiceBoost()) services.add("Boost");
        }
        
        return services.isEmpty() ? "-" : String.join(" / ", services);
    }
}
