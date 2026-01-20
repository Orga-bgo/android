package de.babixgo.monopolygo.database;

import de.babixgo.monopolygo.models.Team;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Repository for managing Team data in Supabase
 * Provides async operations using CompletableFuture
 */
public class TeamRepository {
    private final SupabaseManager supabase;
    
    public TeamRepository() {
        this.supabase = SupabaseManager.getInstance();
    }
    
    /**
     * Get all teams for a specific event
     */
    public CompletableFuture<List<Team>> getTeamsByEventId(long eventId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supabase.select("teams", Team.class, "event_id=eq." + eventId + "&order=name.asc");
            } catch (IOException e) {
                throw new RuntimeException("Failed to load teams", e);
            }
        });
    }
    
    /**
     * Get team by ID
     */
    public CompletableFuture<Team> getTeamById(long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supabase.selectSingle("teams", Team.class, "id=eq." + id);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load team", e);
            }
        });
    }
    
    /**
     * Create new team
     */
    public CompletableFuture<Team> createTeam(Team team) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String now = getCurrentTimestamp();
                team.setCreatedAt(now);
                team.setUpdatedAt(now);
                
                return supabase.insert("teams", team, Team.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create team", e);
            }
        });
    }
    
    /**
     * Update team
     */
    public CompletableFuture<Team> updateTeam(Team team) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                team.setUpdatedAt(getCurrentTimestamp());
                
                return supabase.update("teams", team, "id=eq." + team.getId(), Team.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update team", e);
            }
        });
    }
    
    /**
     * Delete team
     */
    public CompletableFuture<Void> deleteTeam(long id) {
        return CompletableFuture.runAsync(() -> {
            try {
                supabase.delete("teams", "id=eq." + id);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete team", e);
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
