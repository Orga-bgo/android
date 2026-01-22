package de.babixgo.monopolygo.database;

import de.babixgo.monopolygo.models.Team;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Repository for managing Team data in Firebase Realtime Database
 * Provides async operations using CompletableFuture
 */
public class TeamRepository {
    private final FirebaseManager firebase;
    private static final String COLLECTION = "teams";
    
    public TeamRepository() {
        this.firebase = FirebaseManager.getInstance();
    }
    
    /**
     * Get all teams for a specific event
     */
    public CompletableFuture<List<Team>> getTeamsByEventId(long eventId) {
        return firebase.getAll(COLLECTION, Team.class)
            .thenApply(teams -> teams.stream()
                .filter(team -> team.getEventId() == eventId)
                .sorted((a, b) -> {
                    String nameA = a.getName() != null ? a.getName() : "";
                    String nameB = b.getName() != null ? b.getName() : "";
                    return nameA.compareToIgnoreCase(nameB);
                })
                .collect(Collectors.toList()));
    }
    
    /**
     * Get team by ID
     */
    public CompletableFuture<Team> getTeamById(long id) {
        return firebase.getById(COLLECTION, String.valueOf(id), Team.class);
    }
    
    /**
     * Create new team
     */
    public CompletableFuture<Team> createTeam(Team team) {
        return CompletableFuture.supplyAsync(() -> {
            if (!firebase.isConfigured()) {
                throw new RuntimeException("Firebase ist nicht konfiguriert.");
            }
            
            String now = getCurrentTimestamp();
            team.setCreatedAt(now);
            team.setUpdatedAt(now);
            
            String id = team.getId() != 0 ? String.valueOf(team.getId()) : null;
            
            return firebase.save(COLLECTION, team, id).join();
        });
    }
    
    /**
     * Update team
     */
    public CompletableFuture<Team> updateTeam(Team team) {
        return CompletableFuture.supplyAsync(() -> {
            team.setUpdatedAt(getCurrentTimestamp());
            
            return firebase.save(COLLECTION, team, String.valueOf(team.getId())).join();
        });
    }
    
    /**
     * Delete team
     */
    public CompletableFuture<Void> deleteTeam(long id) {
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
