package de.babixgo.monopolygo;

import android.content.Context;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

/**
 * Utility class for creating short links using Short.io API.
 * 
 * SECURITY NOTE: API keys are now stored in secrets.xml resource file.
 * For production use, consider:
 * - Moving to Android Keystore
 * - Using BuildConfig for environment-specific keys
 * - Implementing server-side proxy for API calls
 */
public class ShortLinkManager {
    private static final String API_URL = "https://api.short.io/links";
    
    private static final OkHttpClient client = new OkHttpClient();
    
    /**
     * DEAKTIVIERT - Wird später implementiert
     * Benötigt funktionierende UserID-Extraktion
     * 
     * Create a short link for a MonopolyGo friend link.
     * @param userId The user ID
     * @param path The path/title for the short link
     * @return The short URL or null if failed
     */
    public static String createShortLink(String userId, String path) {
        // TODO: Aktivieren wenn UserID-Extraktion funktioniert
        return "N/A";  // Temporär deaktiviert
    }
    
    /**
     * DEAKTIVIERT - Wird später implementiert
     * Benötigt funktionierende UserID-Extraktion
     * 
     * Create a short link for a MonopolyGo friend link.
     * @param context The application context (must be non-null to load API credentials)
     * @param userId The user ID
     * @param path The path/title for the short link
     * @return The short URL or null if failed
     * @throws IllegalArgumentException if context is null
     * @throws IllegalStateException if API credentials cannot be loaded from resources
     */
    public static String createShortLink(Context context, String userId, String path) {
        // TODO: Aktivieren wenn UserID-Extraktion funktioniert
        return "N/A";  // Temporär deaktiviert
    }
}

// ORIGINAL CODE DISABLED - Will be re-implemented later when UserID extraction works
// The original createShortLink() methods used Short.io API to create short URLs
// This required valid API credentials and a working UserID extraction
// This functionality will be restored in a future update
