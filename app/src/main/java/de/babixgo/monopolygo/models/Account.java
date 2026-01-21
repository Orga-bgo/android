package de.babixgo.monopolygo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Account model for MonopolyGo accounts
 * Represents an account in the Supabase database
 */
public class Account {
    @SerializedName("id")
    private long id;
    
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
    
    // Suspension tracking
    @SerializedName("suspension_0_days")
    private Integer suspension0Days;
    
    @SerializedName("suspension_3_days")
    private Integer suspension3Days;
    
    @SerializedName("suspension_7_days")
    private Integer suspension7Days;
    
    @SerializedName("suspension_permanent")
    private Boolean suspensionPermanent;
    
    @SerializedName("suspension_count")
    private transient Integer suspensionCount; // GENERATED column - not serialized
    
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
    @SerializedName("is_suspended")
    private transient Boolean isSuspended; // GENERATED column - not serialized
    
    @SerializedName("has_error")
    private Boolean hasError;
    
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
        // isSuspended and suspensionCount are GENERATED columns - set by database
        this.hasError = false;
        this.suspension0Days = 0;
        this.suspension3Days = 0;
        this.suspension7Days = 0;
        this.suspensionPermanent = false;
    }
    
    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
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
    
    public int getSuspension0Days() { return suspension0Days != null ? suspension0Days : 0; }
    public void setSuspension0Days(int suspension0Days) { 
        this.suspension0Days = suspension0Days;
        updateSuspensionStatus();
    }
    
    public int getSuspension3Days() { return suspension3Days != null ? suspension3Days : 0; }
    public void setSuspension3Days(int suspension3Days) { 
        this.suspension3Days = suspension3Days;
        updateSuspensionStatus();
    }
    
    public int getSuspension7Days() { return suspension7Days != null ? suspension7Days : 0; }
    public void setSuspension7Days(int suspension7Days) { 
        this.suspension7Days = suspension7Days;
        updateSuspensionStatus();
    }
    
    public boolean isSuspensionPermanent() { return suspensionPermanent != null ? suspensionPermanent : false; }
    public void setSuspensionPermanent(boolean suspensionPermanent) { 
        this.suspensionPermanent = suspensionPermanent;
        updateSuspensionStatus();
    }
    
    public int getSuspensionCount() { return suspensionCount != null ? suspensionCount : 0; }
    public void setSuspensionCount(int suspensionCount) { this.suspensionCount = suspensionCount; }
    
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
    
    public boolean isSuspended() { return isSuspended != null ? isSuspended : false; }
    public void setSuspended(boolean suspended) { isSuspended = suspended; }
    
    public boolean isHasError() { return hasError != null ? hasError : false; }
    public void setHasError(boolean hasError) { this.hasError = hasError; }
    
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
     * Update suspension status based on suspension counts
     * Note: isSuspended and suspensionCount are GENERATED columns in database
     * They are calculated automatically and should not be set manually
     */
    private void updateSuspensionStatus() {
        // These fields are GENERATED by the database, so we don't set them here
        // The database will automatically calculate them based on suspension counts
    }
    
    /**
     * Get suspension summary for UI: "0 3 7 X"
     */
    public String getSuspensionSummary() {
        int s0 = suspension0Days != null ? suspension0Days : 0;
        int s3 = suspension3Days != null ? suspension3Days : 0;
        int s7 = suspension7Days != null ? suspension7Days : 0;
        boolean sp = suspensionPermanent != null ? suspensionPermanent : false;
        
        return s0 + " " + s3 + " " + s7 + " " + (sp ? "X" : "-");
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
        if (lastPlayed == null) return "Nie gespielt";
        
        try {
            java.text.SimpleDateFormat inputFormat = 
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.GERMAN);
            java.text.SimpleDateFormat outputFormat = 
                new java.text.SimpleDateFormat("dd.MM.yyyy, HH:mm", java.util.Locale.GERMAN);
            
            java.util.Date date = inputFormat.parse(lastPlayed);
            return outputFormat.format(date);
        } catch (Exception e) {
            return lastPlayed;
        }
    }
}
