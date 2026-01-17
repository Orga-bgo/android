package de.babixgo.monopolygo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for reading and extracting data from MonopolyGo app files.
 */
public class DataExtractor {
    private static final String PACKAGE_NAME = "com.scopely.monopolygo";
    
    /**
     * Extract the UserID from the app's SharedPreferences.
     * Tries multiple possible field names as fallbacks.
     * @return The UserID or null if not found
     */
    public static String extractUserId() {
        String prefsFile = "/data/data/" + PACKAGE_NAME + 
                          "/shared_prefs/" + PACKAGE_NAME + ".v2.playerprefs.xml";
        
        String content = RootManager.runRootCommand("cat \"" + prefsFile + "\"");
        
        if (content == null || content.contains("Error") || content.isEmpty()) {
            return null;
        }
        
        // Try multiple possible UserID field names
        // Ordered from most specific to most general
        String[] possibleFields = {
            "Scopely.Attribution.UserId",
            "ScopelyProfile.UserId",
            "Scopely.UserId",
            "UserId",
            "user_id",
            "userId",
            "PlayerId",
            "player_id",
            "playerId",
            "PlayerID",
            "UserID"
        };
        
        for (String fieldName : possibleFields) {
            // Parse XML for the UserID field as string element
            // Pattern is simple and anchored, no ReDoS risk with standard quantifiers
            String regex = "<string\\s*name=\"" + Pattern.quote(fieldName) + "\"\\s*>(\\d+)</string>";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // Also try integer element format (Unity sometimes stores as int)
            String intRegex = "<int\\s*name=\"" + Pattern.quote(fieldName) + "\"\\s*value=\"(\\d+)\"\\s*/>";
            Pattern intPattern = Pattern.compile(intRegex);
            Matcher intMatcher = intPattern.matcher(content);
            
            if (intMatcher.find()) {
                return intMatcher.group(1);
            }
        }
        
        return null;
    }
    
    /**
     * Read the content of a file from the app's data directory.
     * @param filePath The relative path within the app's data directory
     * @return The file content or error message
     */
    public static String readDataFile(String filePath) {
        String fullPath = "/data/data/" + PACKAGE_NAME + "/" + filePath;
        return RootManager.runRootCommand("cat \"" + fullPath + "\"");
    }
    
    /**
     * Check if a file exists in the app's data directory.
     * @param filePath The relative path within the app's data directory
     * @return true if the file exists, false otherwise
     */
    public static boolean fileExists(String filePath) {
        String fullPath = "/data/data/" + PACKAGE_NAME + "/" + filePath;
        String result = RootManager.runRootCommand("[ -f \"" + fullPath + "\" ] && echo 'exists' || echo 'not found'");
        return result.contains("exists");
    }
}
