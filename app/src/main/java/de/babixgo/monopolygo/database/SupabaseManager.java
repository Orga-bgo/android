package de.babixgo.monopolygo.database;

import de.babixgo.monopolygo.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages Supabase connections using OkHttp client
 * Provides a simple Java-based interface to Supabase REST API
 */
public class SupabaseManager {
    private static SupabaseManager instance;
    private final OkHttpClient client;
    private final Gson gson;
    private final String supabaseUrl;
    private final String supabaseKey;
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    /**
     * Private constructor for singleton pattern
     */
    private SupabaseManager() {
        this.supabaseUrl = BuildConfig.SUPABASE_URL;
        this.supabaseKey = BuildConfig.SUPABASE_ANON_KEY;
        
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        
        this.gson = new Gson();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized SupabaseManager getInstance() {
        if (instance == null) {
            instance = new SupabaseManager();
        }
        return instance;
    }
    
    /**
     * Check if Supabase is configured
     */
    public boolean isConfigured() {
        return supabaseUrl != null && !supabaseUrl.isEmpty() && 
               !supabaseUrl.equals("https://your-project.supabase.co") &&
               supabaseKey != null && !supabaseKey.isEmpty() &&
               !supabaseKey.equals("your-anon-key-here");
    }
    
    /**
     * Execute GET request to Supabase
     */
    public <T> List<T> select(String table, Class<T> clazz, String filters) throws IOException {
        String url = supabaseUrl + "/rest/v1/" + table;
        if (filters != null && !filters.isEmpty()) {
            url += "?" + filters;
        }
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer " + supabaseKey)
            .addHeader("Content-Type", "application/json")
            .get()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response " + response);
            }
            
            String jsonData = response.body().string();
            Type listType = TypeToken.getParameterized(List.class, clazz).getType();
            return gson.fromJson(jsonData, listType);
        }
    }
    
    /**
     * Execute GET request for single item
     */
    public <T> T selectSingle(String table, Class<T> clazz, String filters) throws IOException {
        String url = supabaseUrl + "/rest/v1/" + table + "?" + filters;
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer " + supabaseKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/vnd.pgrst.object+json")
            .get()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response " + response);
            }
            
            String jsonData = response.body().string();
            return gson.fromJson(jsonData, clazz);
        }
    }
    
    /**
     * Execute POST request to insert data
     */
    public <T> T insert(String table, T data, Class<T> clazz) throws IOException {
        String url = supabaseUrl + "/rest/v1/" + table;
        String json = gson.toJson(data);
        
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer " + supabaseKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .post(body)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response " + response);
            }
            
            String jsonData = response.body().string();
            // Response is an array, get first element
            Type listType = TypeToken.getParameterized(List.class, clazz).getType();
            List<T> result = gson.fromJson(jsonData, listType);
            return result.isEmpty() ? null : result.get(0);
        }
    }
    
    /**
     * Execute PATCH request to update data
     */
    public <T> T update(String table, T data, String filters, Class<T> clazz) throws IOException {
        String url = supabaseUrl + "/rest/v1/" + table + "?" + filters;
        String json = gson.toJson(data);
        
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer " + supabaseKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .patch(body)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response " + response);
            }
            
            String jsonData = response.body().string();
            // Response is an array, get first element
            Type listType = TypeToken.getParameterized(List.class, clazz).getType();
            List<T> result = gson.fromJson(jsonData, listType);
            return result.isEmpty() ? null : result.get(0);
        }
    }
    
    /**
     * Execute DELETE request
     */
    public void delete(String table, String filters) throws IOException {
        String url = supabaseUrl + "/rest/v1/" + table + "?" + filters;
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer " + supabaseKey)
            .delete()
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response " + response);
            }
        }
    }
}
