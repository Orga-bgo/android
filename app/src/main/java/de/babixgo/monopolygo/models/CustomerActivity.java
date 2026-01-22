package de.babixgo.monopolygo.models;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * CustomerActivity model for tracking all customer-related activities
 * Provides comprehensive audit trail and history for customer operations
 */
public class CustomerActivity {
    @SerializedName("id")
    private long id;
    
    @SerializedName("customer_id")
    private long customerId;
    
    @SerializedName("activity_type")
    private String activityType; // 'create', 'update', 'delete', 'account_add', 'account_update', 'account_delete', 'service_change'
    
    @SerializedName("activity_category")
    private String activityCategory; // 'customer', 'account', 'service'
    
    @SerializedName("description")
    private String description; // Human-readable description
    
    @SerializedName("details")
    private String details; // JSON with detailed changes
    
    @SerializedName("customer_account_id")
    private Long customerAccountId; // Optional: which account was affected
    
    @SerializedName("performed_by")
    private String performedBy; // Optional: user who performed the action
    
    @SerializedName("created_at")
    private String createdAt;
    
    // Constructors
    public CustomerActivity() {}
    
    public CustomerActivity(long customerId, String activityType, String activityCategory, String description) {
        this.customerId = customerId;
        this.activityType = activityType;
        this.activityCategory = activityCategory;
        this.description = description;
    }
    
    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    
    public String getActivityCategory() { return activityCategory; }
    public void setActivityCategory(String activityCategory) { this.activityCategory = activityCategory; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    
    public Long getCustomerAccountId() { return customerAccountId; }
    public void setCustomerAccountId(Long customerAccountId) { this.customerAccountId = customerAccountId; }
    
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    // Helper methods
    
    /**
     * Get formatted timestamp for display
     * Format: "21.01.2026, 14:30"
     */
    public String getFormattedTimestamp() {
        if (createdAt == null) return "";
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.GERMAN);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMAN);
            Date date = inputFormat.parse(createdAt);
            return outputFormat.format(date);
        } catch (Exception e) {
            return createdAt;
        }
    }
    
    /**
     * Get activity icon as emoji string based on activity type
     * For UI display purposes in text-based components
     */
    public String getActivityIcon() {
        switch (activityType) {
            case "create": return "➕";
            case "update": return "✏️";
            case "delete": return "🗑️";
            case "account_add": return "👤➕";
            case "account_update": return "👤✏️";
            case "account_delete": return "👤🗑️";
            case "service_change": return "🔧";
            default: return "📝";
        }
    }
    
    /**
     * Get category display text
     */
    public String getCategoryDisplay() {
        switch (activityCategory) {
            case "customer": return "Kunde";
            case "account": return "Account";
            case "service": return "Service";
            default: return "Sonstiges";
        }
    }
}
