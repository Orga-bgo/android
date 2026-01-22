package de.babixgo.monopolygo.models;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CustomerAccount model representing a customer's account with services
 */
public class CustomerAccount {
    @SerializedName("id")
    private long id;
    
    @SerializedName("customer_id")
    private long customerId;
    
    @SerializedName("ingame_name")
    private String ingameName;
    
    @SerializedName("friend_link")
    private String friendLink;
    
    @SerializedName("friend_code")
    private String friendCode;
    
    // Services (multiple services possible)
    @SerializedName("service_partner")
    private boolean servicePartner;
    
    @SerializedName("service_race")
    private boolean serviceRace;
    
    @SerializedName("service_boost")
    private boolean serviceBoost;
    
    // Partner-specific
    @SerializedName("partner_count")
    private int partnerCount; // 1-4
    
    // Boost-specific
    @SerializedName("backup_account_id")
    private Long backupAccountId;
    
    @SerializedName("backup_created_at")
    private String backupCreatedAt;
    
    @SerializedName("credentials_username")
    private String credentialsUsername;
    
    @SerializedName("credentials_password")
    private String credentialsPassword; // Encrypted
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    // Constructors
    public CustomerAccount() {}
    
    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }
    
    public String getIngameName() { return ingameName; }
    public void setIngameName(String ingameName) { this.ingameName = ingameName; }
    
    public String getFriendLink() { return friendLink; }
    public void setFriendLink(String friendLink) { this.friendLink = friendLink; }
    
    public String getFriendCode() { return friendCode; }
    public void setFriendCode(String friendCode) { this.friendCode = friendCode; }
    
    public boolean isServicePartner() { return servicePartner; }
    public void setServicePartner(boolean servicePartner) { this.servicePartner = servicePartner; }
    
    public boolean isServiceRace() { return serviceRace; }
    public void setServiceRace(boolean serviceRace) { this.serviceRace = serviceRace; }
    
    public boolean isServiceBoost() { return serviceBoost; }
    public void setServiceBoost(boolean serviceBoost) { this.serviceBoost = serviceBoost; }
    
    public int getPartnerCount() { return partnerCount; }
    public void setPartnerCount(int partnerCount) { this.partnerCount = partnerCount; }
    
    public Long getBackupAccountId() { return backupAccountId; }
    public void setBackupAccountId(Long backupAccountId) { this.backupAccountId = backupAccountId; }
    
    public String getBackupCreatedAt() { return backupCreatedAt; }
    public void setBackupCreatedAt(String backupCreatedAt) { this.backupCreatedAt = backupCreatedAt; }
    
    public String getCredentialsUsername() { return credentialsUsername; }
    public void setCredentialsUsername(String credentialsUsername) { this.credentialsUsername = credentialsUsername; }
    
    public String getCredentialsPassword() { return credentialsPassword; }
    public void setCredentialsPassword(String credentialsPassword) { this.credentialsPassword = credentialsPassword; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    // Helper methods
    
    /**
     * Check if account backup exists
     */
    public boolean hasBackup() {
        return backupAccountId != null && backupAccountId > 0;
    }
    
    /**
     * Format backup date for display
     * Format: "21.01.26"
     */
    public String getBackupDateDisplay() {
        if (backupCreatedAt == null || backupCreatedAt.isEmpty()) {
            return "Keins vorhanden";
        }
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.GERMAN);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yy", Locale.GERMAN);
            Date date = inputFormat.parse(backupCreatedAt);
            return "Backup vom " + outputFormat.format(date);
        } catch (Exception e) {
            return "Backup vorhanden";
        }
    }
    
    /**
     * Returns all services of this account
     * Format: "Partner (2) / Race / Boost"
     */
    public String getServicesDisplay() {
        List<String> services = new ArrayList<>();
        
        if (servicePartner) {
            services.add("Partner (" + partnerCount + ")");
        }
        if (serviceRace) {
            services.add("Race");
        }
        if (serviceBoost) {
            services.add("Boost");
        }
        
        return services.isEmpty() ? "-" : String.join(" / ", services);
    }
    
    /**
     * Check if at least one service is enabled
     */
    public boolean hasAnyService() {
        return servicePartner || serviceRace || serviceBoost;
    }
    
    // Firebase-compatible timestamp helpers
    
    /**
     * Get created_at as Unix timestamp (milliseconds)
     */
    public long getCreatedAtTimestamp() {
        try {
            return Long.parseLong(createdAt);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Set created_at from Unix timestamp (milliseconds)
     */
    public void setCreatedAtTimestamp(long timestamp) {
        this.createdAt = String.valueOf(timestamp);
    }
    
    /**
     * Get updated_at as Unix timestamp (milliseconds)
     */
    public long getUpdatedAtTimestamp() {
        try {
            return Long.parseLong(updatedAt);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Set updated_at from Unix timestamp (milliseconds)
     */
    public void setUpdatedAtTimestamp(long timestamp) {
        this.updatedAt = String.valueOf(timestamp);
    }
    
    /**
     * Get backup_created_at as Unix timestamp (milliseconds)
     */
    public long getBackupCreatedAtTimestamp() {
        try {
            return Long.parseLong(backupCreatedAt);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Set backup_created_at from Unix timestamp (milliseconds)
     */
    public void setBackupCreatedAtTimestamp(long timestamp) {
        this.backupCreatedAt = String.valueOf(timestamp);
    }
}
