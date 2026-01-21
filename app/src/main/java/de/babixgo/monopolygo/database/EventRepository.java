package de.babixgo.monopolygo.database;

import de.babixgo.monopolygo.models.Event;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Repository for managing Event data in Supabase
 * Provides async operations using CompletableFuture
 */
public class EventRepository {
    private final SupabaseManager supabase;
    
    public EventRepository() {
        this.supabase = SupabaseManager.getInstance();
    }
    
    /**
     * Get all events ordered by start date descending
     */
    public CompletableFuture<List<Event>> getAllEvents() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!supabase.isConfigured()) {
                    throw new RuntimeException("Supabase ist nicht konfiguriert. Bitte füge deine Supabase-Zugangsdaten in gradle.properties hinzu.");
                }
                return supabase.select("events", Event.class, "order=start_date.desc");
            } catch (IOException e) {
                throw new RuntimeException("Fehler beim Laden der Events: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Get event by ID
     */
    public CompletableFuture<Event> getEventById(long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supabase.selectSingle("events", Event.class, "id=eq." + id);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load event", e);
            }
        });
    }
    
    /**
     * Create new event
     */
    public CompletableFuture<Event> createEvent(Event event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String now = getCurrentTimestamp();
                event.setCreatedAt(now);
                event.setUpdatedAt(now);
                
                return supabase.insert("events", event, Event.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create event", e);
            }
        });
    }
    
    /**
     * Update event
     */
    public CompletableFuture<Event> updateEvent(Event event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                event.setUpdatedAt(getCurrentTimestamp());
                
                return supabase.update("events", event, "id=eq." + event.getId(), Event.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update event", e);
            }
        });
    }
    
    /**
     * Delete event
     */
    public CompletableFuture<Void> deleteEvent(long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                supabase.delete("events", "id=eq." + id);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete event", e);
            }
        });
    }
    
    /**
     * Helper method for current timestamp in ISO 8601 format
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * Check if Supabase is configured
     */
    public boolean isSupabaseConfigured() {
        return supabase.isConfigured();
    }
}
