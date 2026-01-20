package de.babixgo.monopolygo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Customer model for event customers
 * Represents a customer in the Supabase database
 */
public class Customer {
    @SerializedName("id")
    private long id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("friend_link")
    private String friendLink;
    
    @SerializedName("friend_code")
    private String friendCode;
    
    @SerializedName("user_id")
    private String userId; // Extracted from friend link
    
    @SerializedName("slots")
    private int slots; // Default 4
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;
    
    // Constructors
    public Customer() {}
    
    public Customer(String name, String friendLink, int slots) {
        this.name = name;
        this.friendLink = friendLink;
        this.slots = slots;
    }
    
    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getFriendLink() { return friendLink; }
    public void setFriendLink(String friendLink) { this.friendLink = friendLink; }
    
    public String getFriendCode() { return friendCode; }
    public void setFriendCode(String friendCode) { this.friendCode = friendCode; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public int getSlots() { return slots; }
    public void setSlots(int slots) { this.slots = slots; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
