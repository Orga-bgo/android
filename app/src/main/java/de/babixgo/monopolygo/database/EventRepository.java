package de.babixgo.monopolygo.database;

import de.babixgo.monopolygo.models.Event;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Repository for managing Event data in Firebase Realtime Database
 * Provides async operations using CompletableFuture
 */
public class EventRepository {
    private final FirebaseManager firebase;
    private static final String COLLECTION = "events";
    
    public EventRepository() {
        this.firebase = FirebaseManager.getInstance();
    }
    
    /**
     * Get all events ordered by start date descending
     */
    public CompletableFuture<List<Event>> getAllEvents() {
        return firebase.getAll(COLLECTION, Event.class)
            .thenApply(events -> events.stream()
                .sorted((a, b) -> {
                    String dateA = a.getStartDate() != null ? a.getStartDate() : "";
                    String dateB = b.getStartDate() != null ? b.getStartDate() : "";
                    return dateB.compareTo(dateA); // Descending order
                })
                .collect(Collectors.toList()));
    }
    
    /**
     * Get event by ID
     */
    public CompletableFuture<Event> getEventById(long id) {
        return firebase.getById(COLLECTION, String.valueOf(id), Event.class);
    }
    
    /**
     * Create new event
     */
    public CompletableFuture<Event> createEvent(Event event) {
        if (!firebase.isConfigured()) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Firebase ist nicht konfiguriert.")
            );
        }
        
        String now = getCurrentTimestamp();
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        
        String id = event.getId() != 0 ? String.valueOf(event.getId()) : null;
        
        return firebase.save(COLLECTION, event, id);
    }
    
    /**
     * Update event
     */
    public CompletableFuture<Event> updateEvent(Event event) {
        event.setUpdatedAt(getCurrentTimestamp());
        
        return firebase.save(COLLECTION, event, String.valueOf(event.getId()));
    }
    
    /**
     * Delete event
     */
    public CompletableFuture<Void> deleteEvent(long id) {
        return firebase.delete(COLLECTION, String.valueOf(id));
    }
    
    /**
     * Helper method for current timestamp in ISO 8601 format
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * Check if Firebase is configured
     */
    public boolean isFirebaseConfigured() {
        return firebase.isConfigured();
    }
}
