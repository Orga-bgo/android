package de.babixgo.monopolygo.utils;

import android.content.Context;
import android.provider.Settings;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import de.babixgo.monopolygo.RootManager;
import java.util.concurrent.CompletableFuture;

/**
 * Extrahiert Device-IDs für Account-Tracking:
 * - SSAID: Android-spezifische ID aus MonopolyGo-Daten (Root erforderlich)
 * - GAID: Google Advertising ID
 * - Device ID: Android ID
 */
public class DeviceIdExtractor {
    
    /**
     * Container für alle Device-IDs
     */
    public static class DeviceIds {
        public String ssaid;
        public String gaid;
        public String deviceId;
        
        public DeviceIds(String ssaid, String gaid, String deviceId) {
            this.ssaid = ssaid;
            this.gaid = gaid;
            this.deviceId = deviceId;
        }
        
        public boolean isComplete() {
            return ssaid != null && gaid != null && deviceId != null;
        }
    }
    
    /**
     * Extrahiert SSAID aus MonopolyGo App-Daten via Root
     */
    public static String extractSSAID() {
        try {
            // Suche in shared_prefs nach android_id oder ssaid
            String findCommand = "find /data/data/com.scopely.monopolygo/shared_prefs/ -name '*.xml' -type f";
            String files = RootManager.runRootCommand(findCommand);
            
            if (files == null || files.contains("Error")) {
                return null;
            }
            
            // Durchsuche alle XML-Dateien nach SSAID
            String[] fileList = files.split("\n");
            for (String file : fileList) {
                if (file.trim().isEmpty()) continue;
                
                String content = RootManager.runRootCommand("cat \"" + file.trim() + "\"");
                if (content == null || content.contains("Error")) continue;
                
                // Suche nach android_id oder ssaid
                String ssaid = extractValueFromXml(content, "android_id");
                if (ssaid == null) {
                    ssaid = extractValueFromXml(content, "ssaid");
                }
                
                if (ssaid != null && !ssaid.isEmpty()) {
                    return ssaid;
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Hilfsmethode zum Extrahieren von Werten aus XML
     */
    private static String extractValueFromXml(String xml, String key) {
        try {
            // Pattern: <string name="key">value</string>
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "<string\\s+name=\"" + key + "\"\\s*>([^<]+)</string>");
            java.util.regex.Matcher matcher = pattern.matcher(xml);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // Alternative Pattern: <long name="key" value="12345" />
            pattern = java.util.regex.Pattern.compile(
                "<long\\s+name=\"" + key + "\"\\s+value=\"([^\"]+)\"");
            matcher = pattern.matcher(xml);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extrahiert Google Advertising ID (async)
     */
    public static CompletableFuture<String> extractGAID(Context context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                return adInfo.getId();
            } catch (Exception e) {
                return null;
            }
        });
    }
    
    /**
     * Extrahiert Android Device ID
     */
    public static String extractDeviceId(Context context) {
        try {
            return Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
            );
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extrahiert alle Device-IDs (kombiniert)
     */
    public static CompletableFuture<DeviceIds> extractAllIds(Context context) {
        return CompletableFuture.supplyAsync(() -> {
            // SSAID via Root (synchron)
            String ssaid = extractSSAID();
            
            // Device ID (synchron)
            String deviceId = extractDeviceId(context);
            
            return new DeviceIds(ssaid, null, deviceId);
        }).thenCompose(partial -> 
            // GAID (async) dann kombinieren
            extractGAID(context).thenApply(gaid -> {
                partial.gaid = gaid;
                return partial;
            })
        );
    }
}
