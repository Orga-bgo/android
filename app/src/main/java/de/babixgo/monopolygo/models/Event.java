package de.babixgo.monopolygo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Event model for Tycoon Racers events
 * Represents an event in the Firebase Realtime Database
 */
public class Event {
    @SerializedName("id")
    private long id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("start_date")
    private String startDate; // ISO 8601 format
    
    @SerializedName("end_date")
    private String endDate; // ISO 8601 format
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    // Constructors
    public Event() {}
    
    public Event(String name, String startDate, String endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    /**
     * Get formatted date range for UI: "01.02 bis 05.02"
     */
    public String getFormattedDateRange() {
        if (startDate == null || endDate == null) return "Kein Datum";
        
        try {
            java.text.SimpleDateFormat inputFormat = 
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.GERMAN);
            java.text.SimpleDateFormat outputFormat = 
                new java.text.SimpleDateFormat("dd.MM", java.util.Locale.GERMAN);
            
            java.util.Date start = inputFormat.parse(startDate);
            java.util.Date end = inputFormat.parse(endDate);
            
            return outputFormat.format(start) + " bis " + outputFormat.format(end);
        } catch (Exception e) {
            return startDate + " bis " + endDate;
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
}
