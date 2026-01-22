package de.babixgo.monopolygo.database;

import android.util.Log;
import de.babixgo.monopolygo.models.CustomerActivity;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Repository for managing CustomerActivity data in Supabase
 * Provides async operations for activity tracking and audit trail
 */
public class CustomerActivityRepository {
    private static final String TAG = "CustomerActivityRepository";
    private final SupabaseManager supabase;
    
    public CustomerActivityRepository() {
        this.supabase = SupabaseManager.getInstance();
    }
    
    /**
     * Log a new customer activity
     */
    public CompletableFuture<CustomerActivity> logActivity(CustomerActivity activity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConfigured();
                
                // Set created_at if not already set
                if (activity.getCreatedAt() == null) {
                    activity.setCreatedAt(getCurrentTimestamp());
                }
                
                CustomerActivity created = supabase.insert("customer_activities", activity, CustomerActivity.class);
                Log.d(TAG, "Activity logged: " + activity.getActivityType() + " for customer " + activity.getCustomerId());
                
                return created;
            } catch (IOException e) {
                Log.e(TAG, "Failed to log activity", e);
                throw wrapIOException("Fehler beim Protokollieren der Aktivität", e);
            }
        });
    }
    
    /**
     * Log activity with automatic timestamp (convenience method)
     */
    public CompletableFuture<CustomerActivity> logActivity(long customerId, String activityType, 
                                                            String activityCategory, String description) {
        CustomerActivity activity = new CustomerActivity(customerId, activityType, activityCategory, description);
        return logActivity(activity);
    }
    
    /**
     * Log activity with customer_account_id (convenience method)
     */
    public CompletableFuture<CustomerActivity> logActivity(long customerId, String activityType, 
                                                            String activityCategory, String description,
                                                            Long customerAccountId) {
        CustomerActivity activity = new CustomerActivity(customerId, activityType, activityCategory, description);
        activity.setCustomerAccountId(customerAccountId);
        return logActivity(activity);
    }
    
    /**
     * Get all activities for a specific customer (ordered by most recent)
     */
    public CompletableFuture<List<CustomerActivity>> getActivitiesByCustomerId(long customerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConfigured();
                Log.d(TAG, "Loading activities for customer: " + customerId);
                
                List<CustomerActivity> activities = supabase.select(
                    "customer_activities", 
                    CustomerActivity.class, 
                    "customer_id=eq." + customerId + "&order=created_at.desc"
                );
                
                Log.d(TAG, "Loaded " + activities.size() + " activities");
                return activities;
                
            } catch (IOException e) {
                Log.e(TAG, "Failed to load activities", e);
                throw wrapIOException("Fehler beim Laden der Aktivitäten", e);
            }
        });
    }
    
    /**
     * Get activities for a specific customer account
     */
    public CompletableFuture<List<CustomerActivity>> getActivitiesByCustomerAccountId(long customerAccountId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConfigured();
                Log.d(TAG, "Loading activities for customer account: " + customerAccountId);
                
                List<CustomerActivity> activities = supabase.select(
                    "customer_activities", 
                    CustomerActivity.class, 
                    "customer_account_id=eq." + customerAccountId + "&order=created_at.desc"
                );
                
                Log.d(TAG, "Loaded " + activities.size() + " activities");
                return activities;
                
            } catch (IOException e) {
                Log.e(TAG, "Failed to load activities", e);
                throw wrapIOException("Fehler beim Laden der Aktivitäten", e);
            }
        });
    }
    
    /**
     * Get activities by type for a customer
     */
    public CompletableFuture<List<CustomerActivity>> getActivitiesByType(long customerId, String activityType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConfigured();
                Log.d(TAG, "Loading activities of type " + activityType + " for customer: " + customerId);
                
                List<CustomerActivity> activities = supabase.select(
                    "customer_activities", 
                    CustomerActivity.class, 
                    "customer_id=eq." + customerId + "&activity_type=eq." + activityType + "&order=created_at.desc"
                );
                
                return activities;
                
            } catch (IOException e) {
                Log.e(TAG, "Failed to load activities", e);
                throw wrapIOException("Fehler beim Laden der Aktivitäten", e);
            }
        });
    }
    
    /**
     * Get activities by category for a customer
     */
    public CompletableFuture<List<CustomerActivity>> getActivitiesByCategory(long customerId, String activityCategory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConfigured();
                Log.d(TAG, "Loading activities of category " + activityCategory + " for customer: " + customerId);
                
                List<CustomerActivity> activities = supabase.select(
                    "customer_activities", 
                    CustomerActivity.class, 
                    "customer_id=eq." + customerId + "&activity_category=eq." + activityCategory + "&order=created_at.desc"
                );
                
                return activities;
                
            } catch (IOException e) {
                Log.e(TAG, "Failed to load activities", e);
                throw wrapIOException("Fehler beim Laden der Aktivitäten", e);
            }
        });
    }
    
    /**
     * Get recent activities across all customers (limit to last N)
     */
    public CompletableFuture<List<CustomerActivity>> getRecentActivities(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureConfigured();
                Log.d(TAG, "Loading recent activities (limit: " + limit + ")");
                
                List<CustomerActivity> activities = supabase.select(
                    "customer_activities", 
                    CustomerActivity.class, 
                    "order=created_at.desc&limit=" + limit
                );
                
                return activities;
                
            } catch (IOException e) {
                Log.e(TAG, "Failed to load recent activities", e);
                throw wrapIOException("Fehler beim Laden der neuesten Aktivitäten", e);
            }
        });
    }
    
    /**
     * Delete all activities for a customer (when customer is deleted)
     * Usually called automatically via CASCADE, but available for manual cleanup
     */
    public CompletableFuture<Void> deleteActivitiesByCustomerId(long customerId) {
        return CompletableFuture.runAsync(() -> {
            try {
                ensureConfigured();
                Log.d(TAG, "Deleting all activities for customer: " + customerId);
                
                supabase.delete("customer_activities", "customer_id=eq." + customerId);
                
            } catch (IOException e) {
                Log.e(TAG, "Failed to delete activities", e);
                throw wrapIOException("Fehler beim Löschen der Aktivitäten", e);
            }
        });
    }
    
    /**
     * Check if Supabase is configured
     */
    public boolean isSupabaseConfigured() {
        return supabase.isConfigured();
    }
    
    /**
     * Ensure Supabase is configured, throw exception if not
     */
    private void ensureConfigured() {
        if (!supabase.isConfigured()) {
            throw new RuntimeException("Supabase ist nicht konfiguriert. Bitte füge deine Supabase-Zugangsdaten in gradle.properties hinzu.");
        }
    }
    
    /**
     * Wrap IOException with German error message
     */
    private RuntimeException wrapIOException(String message, IOException e) {
        return new RuntimeException(message + ": " + e.getMessage(), e);
    }
    
    /**
     * Helper method for current timestamp in ISO 8601 format
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
