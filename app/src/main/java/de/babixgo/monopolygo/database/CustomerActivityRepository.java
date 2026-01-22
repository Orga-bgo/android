package de.babixgo.monopolygo.database;

import android.util.Log;
import de.babixgo.monopolygo.models.CustomerActivity;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * Repository for managing CustomerActivity data in Firebase Realtime Database
 * Provides async operations for activity tracking and audit trail
 */
public class CustomerActivityRepository {
    private static final String TAG = "CustomerActivityRepository";
    private final FirebaseManager firebase;
    private static final String COLLECTION = "customer_activities";
    
    public CustomerActivityRepository() {
        this.firebase = FirebaseManager.getInstance();
    }
    
    /**
     * Log a new customer activity
     */
    public CompletableFuture<CustomerActivity> logActivity(CustomerActivity activity) {
        if (!firebase.isConfigured()) {
            Log.w(TAG, "Firebase not configured, skipping activity log");
            return CompletableFuture.completedFuture(activity);
        }
        
        // Set created_at if not already set
        if (activity.getCreatedAt() == null) {
            activity.setCreatedAt(getCurrentTimestamp());
        }
        
        // Generate ID if not set
        String id = activity.getId() != 0 ? String.valueOf(activity.getId()) : null;
        
        return firebase.save(COLLECTION, activity, id)
            .thenApply(created -> {
                Log.d(TAG, "Activity logged: " + activity.getActivityType() + " for customer " + activity.getCustomerId());
                return created;
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
        return firebase.getAll(COLLECTION, CustomerActivity.class)
            .thenApply(activities -> {
                Log.d(TAG, "Loading activities for customer: " + customerId);
                
                // Filter by customer_id and sort by created_at desc client-side
                List<CustomerActivity> filtered = activities.stream()
                    .filter(activity -> activity.getCustomerId() == customerId)
                    .sorted(Comparator.comparing(CustomerActivity::getCreatedAt, 
                        Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
                
                Log.d(TAG, "Loaded " + filtered.size() + " activities");
                return filtered;
            });
    }
    
    /**
     * Get activities for a specific customer account
     */
    public CompletableFuture<List<CustomerActivity>> getActivitiesByCustomerAccountId(long customerAccountId) {
        return firebase.getAll(COLLECTION, CustomerActivity.class)
            .thenApply(activities -> {
                Log.d(TAG, "Loading activities for customer account: " + customerAccountId);
                
                // Filter by customer_account_id and sort by created_at desc client-side
                List<CustomerActivity> filtered = activities.stream()
                    .filter(activity -> activity.getCustomerAccountId() != null && 
                                       activity.getCustomerAccountId() == customerAccountId)
                    .sorted(Comparator.comparing(CustomerActivity::getCreatedAt, 
                        Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
                
                Log.d(TAG, "Loaded " + filtered.size() + " activities");
                return filtered;
            });
    }
    
    /**
     * Get activities by type for a customer
     */
    public CompletableFuture<List<CustomerActivity>> getActivitiesByType(long customerId, String activityType) {
        return firebase.getAll(COLLECTION, CustomerActivity.class)
            .thenApply(activities -> {
                Log.d(TAG, "Loading activities of type " + activityType + " for customer: " + customerId);
                
                // Filter by customer_id and activity_type, then sort by created_at desc client-side
                List<CustomerActivity> filtered = activities.stream()
                    .filter(activity -> activity.getCustomerId() == customerId && 
                                       activityType.equals(activity.getActivityType()))
                    .sorted(Comparator.comparing(CustomerActivity::getCreatedAt, 
                        Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
                
                return filtered;
            });
    }
    
    /**
     * Get activities by category for a customer
     */
    public CompletableFuture<List<CustomerActivity>> getActivitiesByCategory(long customerId, String activityCategory) {
        return firebase.getAll(COLLECTION, CustomerActivity.class)
            .thenApply(activities -> {
                Log.d(TAG, "Loading activities of category " + activityCategory + " for customer: " + customerId);
                
                // Filter by customer_id and activity_category, then sort by created_at desc client-side
                List<CustomerActivity> filtered = activities.stream()
                    .filter(activity -> activity.getCustomerId() == customerId && 
                                       activityCategory.equals(activity.getActivityCategory()))
                    .sorted(Comparator.comparing(CustomerActivity::getCreatedAt, 
                        Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
                
                return filtered;
            });
    }
    
    /**
     * Get recent activities across all customers (limit to last N)
     */
    public CompletableFuture<List<CustomerActivity>> getRecentActivities(int limit) {
        return firebase.getAll(COLLECTION, CustomerActivity.class)
            .thenApply(activities -> {
                Log.d(TAG, "Loading recent activities (limit: " + limit + ")");
                
                // Sort by created_at desc and limit client-side
                List<CustomerActivity> sorted = activities.stream()
                    .sorted(Comparator.comparing(CustomerActivity::getCreatedAt, 
                        Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limit)
                    .collect(Collectors.toList());
                
                return sorted;
            });
    }
    
    /**
     * Delete all activities for a customer
     * Usually not needed as Firebase handles cascades differently
     */
    public CompletableFuture<Void> deleteActivitiesByCustomerId(long customerId) {
        return firebase.getAll(COLLECTION, CustomerActivity.class)
            .thenCompose(activities -> {
                Log.d(TAG, "Deleting all activities for customer: " + customerId);
                
                // Filter activities for this customer
                List<CustomerActivity> toDelete = activities.stream()
                    .filter(activity -> activity.getCustomerId() == customerId)
                    .collect(Collectors.toList());
                
                // Delete each activity
                List<CompletableFuture<Void>> deleteFutures = toDelete.stream()
                    .map(activity -> firebase.delete(COLLECTION, String.valueOf(activity.getId())))
                    .collect(Collectors.toList());
                
                return CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0]));
            });
    }
    
    /**
     * Check if Firebase is configured
     */
    public boolean isFirebaseConfigured() {
        return firebase.isConfigured();
    }
    
    /**
     * Helper method for current timestamp in ISO 8601 format
     * Creates a new SimpleDateFormat instance for thread safety
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
