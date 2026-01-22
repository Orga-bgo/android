package de.babixgo.monopolygo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Account model for MonopolyGo accounts
 * Represents an account in the Firebase Realtime Database
 */
public class Account {
    @SerializedName("id")
    private long id;
    
    @SerializedName("firebase_key")
    private String firebaseKey;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("user_id")
    private String userId; // MonopolyGo User ID
    
    @SerializedName("short_link")
    private String shortLink;
    
    @SerializedName("friend_link")
    private String friendLink;
    
    @SerializedName("friend_code")
    private String friendCode;
    
    @SerializedName("account_status")
    private String accountStatus; // 'active', 'suspended', 'banned', 'inactive'
    
    // Suspension tracking (simplified)
    @SerializedName("suspension_status")
    private String suspensionStatus; // '0', '3', '7', 'perm'
    
    // Device IDs
    @SerializedName("ssaid")
    private String ssaid;
    
    @SerializedName("gaid")
    private String gaid;
    
    @SerializedName("device_id")
    private String deviceId;
    
    @SerializedName("device_token")
    private String deviceToken;
    
    @SerializedName("app_set_id")
    private String appSetId;
    
    // Flags
    @SerializedName("has_error")
    private Boolean hasError;
    
    // Customer Account Link
    @SerializedName("is_customer_account")
    private Boolean isCustomerAccount;
    
    @SerializedName("customer_account_id")
    private Long customerAccountId;
    
    @SerializedName("note")
    private String note;
    
    // Timestamps
    @SerializedName("last_played")
    private String lastPlayed;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    @SerializedName("deleted_at")
    private String deletedAt;
    
    // Constructors
    public Account() {}
    
    public Account(String name, String userId) {
        this.name = name;
        this.userId = userId;
        this.accountStatus = "active";
        this.suspensionStatus = "0"; // Default: no suspension
        this.hasError = false;
    }
    
    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getFirebaseKey() { 
        return firebaseKey; 
    }
    
    public void setFirebaseKey(String firebaseKey) { 
        this.firebaseKey = firebaseKey;
    }
    
    /**
     * Set ID from String - supports both numeric and non-numeric Firebase keys
     * For non-numeric keys, uses hashCode as fallback
     */
    public void setIdFromString(String idString) {
        this.firebaseKey = idString;
        try {
            this.id = Long.parseLong(idString);
        } catch (NumberFormatException e) {
            // Firebase key is not a Long - use hashCode as fallback
            this.id = Math.abs(idString.hashCode());
        }
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getShortLink() { return shortLink; }
    public void setShortLink(String shortLink) { this.shortLink = shortLink; }
    
    public String getFriendLink() { return friendLink; }
    public void setFriendLink(String friendLink) { this.friendLink = friendLink; }
    
    public String getFriendCode() { return friendCode; }
    public void setFriendCode(String friendCode) { this.friendCode = friendCode; }
    
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    
    public String getSuspensionStatus() { 
        return suspensionStatus != null ? suspensionStatus : "0"; 
    }
    public void setSuspensionStatus(String suspensionStatus) { 
        this.suspensionStatus = suspensionStatus; 
    }
    
    public String getSsaid() { return ssaid; }
    public void setSsaid(String ssaid) { this.ssaid = ssaid; }
    
    public String getGaid() { return gaid; }
    public void setGaid(String gaid) { this.gaid = gaid; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
    
    public String getAppSetId() { return appSetId; }
    public void setAppSetId(String appSetId) { this.appSetId = appSetId; }
    
    public boolean isSuspended() { 
        return suspensionStatus != null && !suspensionStatus.equals("0");
    }
    
    public boolean isHasError() { return hasError != null ? hasError : false; }
    public void setHasError(boolean hasError) { this.hasError = hasError; }
    
    public boolean isCustomerAccount() { return isCustomerAccount != null ? isCustomerAccount : false; }
    public void setCustomerAccount(boolean customerAccount) { this.isCustomerAccount = customerAccount; }
    
    public Long getCustomerAccountId() { return customerAccountId; }
    public void setCustomerAccountId(Long customerAccountId) { this.customerAccountId = customerAccountId; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public String getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(String lastPlayed) { this.lastPlayed = lastPlayed; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    public String getDeletedAt() { return deletedAt; }
    public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }
    
    /**
     * Get suspension display text for UI
     */
    public String getSuspensionDisplayText() {
        switch (getSuspensionStatus()) {
            case "0": return "Keine";
            case "3": return "3 Tage";
            case "7": return "7 Tage";
            case "perm": return "Permanent";
            default: return "Unbekannt";
        }
    }
    
    /**
     * Get suspension summary for UI (backward compatible)
     * Returns simple status value for display
     */
    public String getSuspensionSummary() {
        return getSuspensionStatus();
    }
    
    /**
     * Get error status text for UI
     */
    public String getErrorStatusText() {
        return (hasError != null && hasError) ? "ja" : "nein";
    }
    
    /**
     * Format last played date for UI
     */
    public String getFormattedLastPlayed() {
        if (lastPlayed == null || lastPlayed.isEmpty()) {
            return "Nie gespielt";
        }
        
        // Handle "0" as a special case
        if (lastPlayed.equals("0")) {
            return "Nie gespielt";
        }
        
        try {
            // Try parsing as ISO 8601 format first
            java.text.SimpleDateFormat inputFormat = 
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.GERMAN);
            java.text.SimpleDateFormat outputFormat = 
                new java.text.SimpleDateFormat("dd.MM.yyyy, HH:mm", java.util.Locale.GERMAN);
            
            java.util.Date date = inputFormat.parse(lastPlayed);
            return outputFormat.format(date);
        } catch (Exception e) {
            // Try parsing as Unix timestamp (milliseconds)
            try {
                long timestamp = Long.parseLong(lastPlayed);
                java.text.SimpleDateFormat outputFormat = 
                    new java.text.SimpleDateFormat("dd.MM.yyyy, HH:mm", java.util.Locale.GERMAN);
                return outputFormat.format(new java.util.Date(timestamp));
            } catch (Exception ex) {
                // If all parsing fails, return the raw value
                return lastPlayed;
            }
        }
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
     * Get last_played as Unix timestamp (milliseconds)
     */
    public long getLastPlayedTimestamp() {
        try {
            return Long.parseLong(lastPlayed);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Set last_played from Unix timestamp (milliseconds)
     */
    public void setLastPlayedTimestamp(long timestamp) {
        this.lastPlayed = String.valueOf(timestamp);
    }
}
