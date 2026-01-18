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
     * Create a short link for a MonopolyGo friend link.
     * @param userId The user ID
     * @param path The path/title for the short link
     * @return The short URL or null if failed
     */
    public static String createShortLink(String userId, String path) {
        throw new IllegalStateException(
                "A non-null Context is required to create short links securely. " +
                "Use createShortLink(Context, String, String) instead.");
    }
    
    /**
     * Create a short link for a MonopolyGo friend link.
     * @param context The application context (must be non-null to load API credentials)
     * @param userId The user ID
     * @param path The path/title for the short link
     * @return The short URL or null if failed
     * @throws IllegalArgumentException if context is null
     * @throws IllegalStateException if API credentials cannot be loaded from resources
     */
    public static String createShortLink(Context context, String userId, String path) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null when creating short links.");
        }

        String apiKey;
        String domain;
        
        try {
            apiKey = context.getString(R.string.shortio_api_key);
            domain = context.getString(R.string.shortio_domain);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Short.io API credentials from resources.", e);
        }
        
        String originalUrl = "monopolygo://add-friend/" + userId;
        
        try {
            JSONObject json = new JSONObject();
            json.put("domain", domain);
            json.put("originalURL", originalUrl);
            json.put("path", path);
            json.put("title", path);
            
            RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("authorization", apiKey)
                .addHeader("content-type", "application/json")
                .post(body)
                .build();
            
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JSONObject responseJson = new JSONObject(responseBody);
                
                if (responseJson.has("shortURL")) {
                    return responseJson.getString("shortURL");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
}
