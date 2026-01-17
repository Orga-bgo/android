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
        return createShortLink(null, userId, path);
    }
    
    /**
     * Create a short link for a MonopolyGo friend link.
     * @param context The application context (optional - will use hardcoded values if null)
     * @param userId The user ID
     * @param path The path/title for the short link
     * @return The short URL or null if failed
     */
    public static String createShortLink(Context context, String userId, String path) {
        String apiKey;
        String domain;
        
        if (context != null) {
            try {
                apiKey = context.getString(R.string.shortio_api_key);
                domain = context.getString(R.string.shortio_domain);
            } catch (Exception e) {
                // Fallback to hardcoded values if resources not available
                apiKey = "sk_MaQODQPO0HKJTZF1";
                domain = "go.babixgo.de";
            }
        } else {
            // Fallback to hardcoded values when context is not provided
            apiKey = "sk_MaQODQPO0HKJTZF1";
            domain = "go.babixgo.de";
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
