package de.babixgo.monopolygo;

import android.util.Log;

/**
 * Helper class for consistent logging across the application.
 */
public class LogHelper {
    private static final String TAG = "BabixGO";
    
    public static void d(String message) {
        Log.d(TAG, message);
    }
    
    public static void e(String message) {
        Log.e(TAG, message);
    }
    
    public static void e(String message, Exception e) {
        Log.e(TAG, message, e);
    }
}
