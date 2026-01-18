package de.babixgo.monopolygo;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manager for persistent application settings using SharedPreferences.
 * Handles paths for backup/restore operations and name prefix for accounts.
 */
public class SettingsManager {
    private static final String PREFS_NAME = "MonopolyGoSettings";
    private static final String KEY_BACKUP_OUTPUT_PATH = "backup_output_path";
    private static final String KEY_RESTORE_INPUT_PATH = "restore_input_path";
    private static final String KEY_NAME_PREFIX = "name_prefix";
    
    private static final String DEFAULT_BACKUP_PATH = "/storage/emulated/0/MonopolyGo/Backups/";
    private static final String DEFAULT_RESTORE_PATH = "/storage/emulated/0/MonopolyGo/Backups/";
    private static final String DEFAULT_PREFIX = "";
    
    private SharedPreferences prefs;
    
    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // Getter-Methoden mit Default-Werten
    public String getBackupOutputPath() {
        return prefs.getString(KEY_BACKUP_OUTPUT_PATH, DEFAULT_BACKUP_PATH);
    }
    
    public String getRestoreInputPath() {
        return prefs.getString(KEY_RESTORE_INPUT_PATH, DEFAULT_RESTORE_PATH);
    }
    
    public String getNamePrefix() {
        return prefs.getString(KEY_NAME_PREFIX, DEFAULT_PREFIX);
    }
    
    // Setter-Methoden
    public void setBackupOutputPath(String path) {
        prefs.edit().putString(KEY_BACKUP_OUTPUT_PATH, path).apply();
    }
    
    public void setRestoreInputPath(String path) {
        prefs.edit().putString(KEY_RESTORE_INPUT_PATH, path).apply();
    }
    
    public void setNamePrefix(String prefix) {
        prefs.edit().putString(KEY_NAME_PREFIX, prefix).apply();
    }
}
