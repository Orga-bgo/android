package de.babixgo.monopolygo;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

/**
 * Utility class for creating short links using Short.io API.
 * 
 * SECURITY NOTE: API key is currently hardcoded. For production use, consider:
 * - Moving to Android Keystore
 * - Using BuildConfig for environment-specific keys
 * - Implementing server-side proxy for API calls
 */
public class ShortLinkManager {
    // TODO: Move API key to secure storage (Android Keystore or server-side)
    private static final String API_KEY = "sk_MaQODQPO0HKJTZF1";
    private static final String DOMAIN = "go.babixgo.de";
    private static final String API_URL = "https://api.short.io/links";
    
    private static final OkHttpClient client = new OkHttpClient();
    
    /**
     * Create a short link for a MonopolyGo friend link.
     * @param userId The user ID
     * @param path The path/title for the short link
     * @return The short URL or null if failed
     */
    public static String createShortLink(String userId, String path) {
        String originalUrl = "monopolygo://add-friend/" + userId;
        
        try {
            JSONObject json = new JSONObject();
            json.put("domain", DOMAIN);
            json.put("originalURL", originalUrl);
            json.put("path", path);
            json.put("title", path);
            
            RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("authorization", API_KEY)
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
