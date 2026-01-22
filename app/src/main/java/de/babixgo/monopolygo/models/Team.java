package de.babixgo.monopolygo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Team model for event teams
 * Represents a team with 4 account slots in the Firebase Realtime Database
 */
public class Team {
    @SerializedName("id")
    private long id;
    
    @SerializedName("event_id")
    private long eventId;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("customer_id")
    private Long customerId; // Nullable
    
    // Account slots (1-4)
    @SerializedName("slot_1_account_id")
    private Long slot1AccountId;
    
    @SerializedName("slot_2_account_id")
    private Long slot2AccountId;
    
    @SerializedName("slot_3_account_id")
    private Long slot3AccountId;
    
    @SerializedName("slot_4_account_id")
    private Long slot4AccountId;
    
    // Account names for display (not stored in DB, filled from join)
    private String slot1Name;
    private String slot2Name;
    private String slot3Name;
    private String slot4Name;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    // Constructors
    public Team() {}
    
    public Team(long eventId, String name) {
        this.eventId = eventId;
        this.name = name;
    }
    
    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getEventId() { return eventId; }
    public void setEventId(long eventId) { this.eventId = eventId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    
    public Long getSlot1AccountId() { return slot1AccountId; }
    public void setSlot1AccountId(Long slot1AccountId) { this.slot1AccountId = slot1AccountId; }
    
    public Long getSlot2AccountId() { return slot2AccountId; }
    public void setSlot2AccountId(Long slot2AccountId) { this.slot2AccountId = slot2AccountId; }
    
    public Long getSlot3AccountId() { return slot3AccountId; }
    public void setSlot3AccountId(Long slot3AccountId) { this.slot3AccountId = slot3AccountId; }
    
    public Long getSlot4AccountId() { return slot4AccountId; }
    public void setSlot4AccountId(Long slot4AccountId) { this.slot4AccountId = slot4AccountId; }
    
    public String getSlot1Name() { return slot1Name; }
    public void setSlot1Name(String slot1Name) { this.slot1Name = slot1Name; }
    
    public String getSlot2Name() { return slot2Name; }
    public void setSlot2Name(String slot2Name) { this.slot2Name = slot2Name; }
    
    public String getSlot3Name() { return slot3Name; }
    public void setSlot3Name(String slot3Name) { this.slot3Name = slot3Name; }
    
    public String getSlot4Name() { return slot4Name; }
    public void setSlot4Name(String slot4Name) { this.slot4Name = slot4Name; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
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
}
