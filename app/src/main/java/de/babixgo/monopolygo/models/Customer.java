package de.babixgo.monopolygo.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer model for customer management
 * Represents a customer in the Firebase Realtime Database
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
    private transient List<CustomerAccount> accounts = new ArrayList<>();
    
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
     * Calculated from loaded accounts list
     */
    public int getAccountCount() {
        return accounts != null ? accounts.size() : 0;
    }
    
    /**
     * Returns all services that this customer uses (across all accounts)
     * Format: "Partner / Race / Boost"
     * Aggregates services from all accounts in deterministic order
     */
    public String getServicesDisplay() {
        if (accounts == null || accounts.isEmpty()) {
            return "-";
        }
        
        boolean hasPartner = false;
        boolean hasRace = false;
        boolean hasBoost = false;
        
        for (CustomerAccount account : accounts) {
            if (account.isServicePartner()) {
                hasPartner = true;
            }
            if (account.isServiceRace()) {
                hasRace = true;
            }
            if (account.isServiceBoost()) {
                hasBoost = true;
            }
        }
        
        List<String> services = new ArrayList<>();
        if (hasPartner) services.add("Partner");
        if (hasRace) services.add("Race");
        if (hasBoost) services.add("Boost");
        
        return services.isEmpty() ? "-" : String.join(" / ", services);
    }
    
    /**
     * Get total count of each service across all accounts
     */
    public int getServiceCount(String serviceType) {
        if (accounts == null || accounts.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (CustomerAccount account : accounts) {
            switch (serviceType.toLowerCase()) {
                case "partner":
                    if (account.isServicePartner()) count++;
                    break;
                case "race":
                    if (account.isServiceRace()) count++;
                    break;
                case "boost":
                    if (account.isServiceBoost()) count++;
                    break;
            }
        }
        return count;
    }
}
