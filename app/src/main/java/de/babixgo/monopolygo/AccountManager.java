package de.babixgo.monopolygo;

import android.os.Build;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import com.opencsv.CSVReader;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;

/**
 * Manager class for MonopolyGo account operations (backup, restore, etc).
 */
public class AccountManager {
    private static final String TAG = "AccountManager";
    private static final String PACKAGE_NAME = "com.scopely.monopolygo";
    private static final String BASE_PATH = "/storage/emulated/0/MonopolyGo/";
    private static final String ACCOUNTS_EIGENE = BASE_PATH + "Accounts/Eigene/";
    private static final String ACCOUNTS_KUNDEN = BASE_PATH + "Accounts/Kunden/";
    private static final String PARTNEREVENTS_PATH = BASE_PATH + "Partnerevents/";
    private static final String BACKUPS_PATH = BASE_PATH + "Backups/";
    
    private static final String TEMP_PATH = "/data/local/tmp/";
    
    // MonopolyGo Dateipfade
    private static final String DATA_DIR = "/data/data/" + PACKAGE_NAME + "/";
    
    private static final String DATA_FILE_PATH = "/data/data/" + PACKAGE_NAME + 
        "/files/DiskBasedCacheDirectory/WithBuddies.Services.User.0Production.dat";
    
    // MUSS IMMER gesichert werden
    private static final String REQUIRED_FILE = 
        DATA_DIR + "files/DiskBasedCacheDirectory/WithBuddies.Services.User.0Production.dat";
    
    // Optional - nur wenn vorhanden
    private static final String[] OPTIONAL_FILES = {
        DATA_DIR + "files/device-id",
        DATA_DIR + "files/internal-device-id",
        DATA_DIR + "files/generatefid.lock",
        DATA_DIR + "shared_prefs/com.scopely.monopolygo.v2.playerprefs.xml",
        DATA_DIR + "shared_prefs/mys_mod_window_positions.xml",
        DATA_DIR + "shared_prefs/mys_mod_feature_settings.xml"
    };
    
    // FB-Token (nur auf Anfrage)
    private static final String FB_TOKEN_FILE = 
        DATA_DIR + "shared_prefs/com.facebook.AccessTokenManager.SharedPreferences.xml";
    
    /**
     * Initialize the required directories on the device.
     */
    public static void initializeDirectories() {
        new File(ACCOUNTS_EIGENE).mkdirs();
        new File(ACCOUNTS_KUNDEN).mkdirs();
        new File(PARTNEREVENTS_PATH).mkdirs();
        new File(BACKUPS_PATH).mkdirs();
    }
    
    /**
     * Force stop the MonopolyGo app.
     */
    public static void forceStopApp() {
        RootManager.runRootCommand("am force-stop " + PACKAGE_NAME);
    }
    
    /**
     * Start the MonopolyGo app.
     */
    public static void startApp() {
        RootManager.runRootCommand("monkey -p " + PACKAGE_NAME + " 1");
    }
    
    /**
     * Restore an account from a backup.
     * @param sourceFile The source account file path
     * @return true if successful, false otherwise
     */
    public static boolean restoreAccount(String sourceFile) {
        // 1. Stop the app
        forceStopApp();
        
        try {
            Thread.sleep(1000); // Wait for app to fully stop
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 2. Copy the file with root privileges
        String command = "cp \"" + sourceFile + "\" \"" + DATA_FILE_PATH + "\"";
        String result = RootManager.runRootCommand(command);
        
        if (result.contains("Error")) {
            return false;
        }
        
        // 3. Set proper permissions
        String[] permCommands = {
            "chmod 660 \"" + DATA_FILE_PATH + "\"",
            "chown $(stat -c %u:%g /data/data/" + PACKAGE_NAME + ") \"" + DATA_FILE_PATH + "\""
        };
        RootManager.runRootCommands(permCommands);
        
        return true;
    }
    
    /**
     * Backup the current account to a specified directory.
     * @param targetDirectory The directory to save the account to
     * @param accountName The name for the account backup
     * @return true if successful, false otherwise
     */
    public static boolean backupAccount(String targetDirectory, String accountName) {
        // 1. Stop the app to ensure file consistency
        forceStopApp();
        
        try {
            Thread.sleep(1000); // Wait for app to fully stop
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 2. Ensure target directory exists
        File targetDir = new File(targetDirectory, accountName);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        String destPath = targetDir.getAbsolutePath() + "/WithBuddies.Services.User.0Production.dat";
        
        // 3. Copy the file
        String command = "cp \"" + DATA_FILE_PATH + "\" \"" + destPath + "\"";
        String result = RootManager.runRootCommand(command);
        
        return !result.contains("Error");
    }
    
    /**
     * Get the list of backed up accounts (jetzt ZIP-basiert).
     * @param isOwnAccounts true for own accounts, false for customer accounts
     * @return Array of account names
     */
    public static String[] getBackedUpAccounts(boolean isOwnAccounts) {
        String basePath = isOwnAccounts ? ACCOUNTS_EIGENE : ACCOUNTS_KUNDEN;
        File baseDir = new File(basePath);
        
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            return new String[0];
        }
        
        File[] files = baseDir.listFiles(File::isDirectory);
        if (files == null || files.length == 0) {
            return new String[0];
        }
        
        // Nur Ordner mit .zip Datei oder .dat Datei anzeigen (für Abwärtskompatibilität)
        List<String> validAccounts = new ArrayList<>();
        for (File dir : files) {
            String accountName = dir.getName();
            File zipFile = new File(dir, accountName + ".zip");
            File datFile = new File(dir, "WithBuddies.Services.User.0Production.dat");
            if (zipFile.exists() || datFile.exists()) {
                validAccounts.add(accountName);
            }
        }
        
        return validAccounts.toArray(new String[0]);
    }
    
    /**
     * Erweiterte Backup-Funktion mit Java-ZIP
     */
    public static boolean backupAccountExtended(String accountName, boolean includeFbToken) {
        // Validate accountName to prevent command injection
        if (!isValidAccountName(accountName)) {
            Log.e(TAG, "Invalid account name: " + accountName);
            return false;
        }
        
        // 1. App stoppen
        forceStopApp();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 2. Berechtigungen prüfen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: MANAGE_EXTERNAL_STORAGE erforderlich
            if (!Environment.isExternalStorageManager()) {
                Log.e("BabixGO", "FEHLER: MANAGE_EXTERNAL_STORAGE Berechtigung fehlt!");
                return false;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10: Prüfe ob Temp-Verzeichnis beschreibbar ist
            File tempTestFile = new File(TEMP_PATH, ".permission_test");
            try {
                tempTestFile.getParentFile().mkdirs();
                if (!tempTestFile.createNewFile()) {
                    Log.e("BabixGO", "FEHLER: Kann nicht in Temp-Verzeichnis schreiben - Berechtigung fehlt möglicherweise");
                    return false;
                }
                tempTestFile.delete();
            } catch (Exception e) {
                Log.e("BabixGO", "FEHLER: Keine Schreibberechtigung für Speicher: " + e.getMessage());
                return false;
            }
        }
        
        // 3. Temporäres Verzeichnis erstellen (im öffentlichen Bereich)
        String tempDir = TEMP_PATH + accountName + "/";
        File tempDirFile = new File(tempDir);
        
        Log.d("BabixGO", "Erstelle Temp-Verzeichnis: " + tempDir);
        
        // Altes Temp-Verzeichnis löschen falls vorhanden
        if (tempDirFile.exists()) {
            Log.d("BabixGO", "Lösche altes Temp-Verzeichnis");
            deleteRecursive(tempDirFile);
        }
        
        boolean dirCreated = tempDirFile.mkdirs();
        if (!dirCreated && !tempDirFile.exists()) {
            Log.e("BabixGO", "FEHLER: Temp-Verzeichnis konnte nicht erstellt werden!");
            return false;
        }
        
        Log.d("BabixGO", "Temp-Verzeichnis erstellt: " + tempDirFile.exists());
        
        // 4. REQUIRED FILE prüfen
        String checkCommand = "[ -f " + escapeShellArg(REQUIRED_FILE) + " ] && echo 'exists' || echo 'not found'";
        String checkResult = RootManager.runRootCommand(checkCommand);
        
        if (!checkResult.contains("exists")) {
            Log.e("BabixGO", "FEHLER: Required File nicht gefunden: " + REQUIRED_FILE);
            deleteRecursive(tempDirFile);
            return false;
        }
        
        Log.d("BabixGO", "Required File gefunden, kopiere...");
        
        // 5. REQUIRED FILE kopieren
        String cpCommand = "cp " + escapeShellArg(REQUIRED_FILE) + " " + escapeShellArg(tempDir + "account.dat");
        String cpResult = RootManager.runRootCommand(cpCommand);
        
        boolean success = new File(tempDir + "account.dat").exists() && new File(tempDir + "account.dat").length() > 0;
        
        if (!success) {
            Log.e("BabixGO", "FEHLER: Kopieren der Account-Datei fehlgeschlagen: " + cpResult);
            deleteRecursive(tempDirFile);
            return false;
        }
        
        Log.d("BabixGO", "Account-Datei kopiert");
        
        // 6. Optionale Dateien kopieren
        List<String> copiedFiles = new ArrayList<>();
        copiedFiles.add("account.dat");
        
        for (int i = 0; i < OPTIONAL_FILES.length; i++) {
            String sourceFile = OPTIONAL_FILES[i];
            
            // Check if file exists
            String fileCheckCommand = "[ -f " + escapeShellArg(sourceFile) + " ] && echo 'exists' || echo 'not found'";
            String fileCheckResult = RootManager.runRootCommand(fileCheckCommand);
            
            if (fileCheckResult.contains("exists")) {
                String fileName = getFileName(sourceFile, i);
                String fileCpCommand = "cp " + escapeShellArg(sourceFile) + " " + escapeShellArg(tempDir + fileName);
                String fileCpResult = RootManager.runRootCommand(fileCpCommand);
                
                boolean copied = new File(tempDir + fileName).exists();
                
                if (copied) {
                    copiedFiles.add(fileName);
                    Log.d("BabixGO", "Optional kopiert: " + fileName);
                }
            }
        }
        
        // 7. FB-Token kopieren (falls gewünscht)
        if (includeFbToken) {
            String fbCheckCommand = "[ -f " + escapeShellArg(FB_TOKEN_FILE) + " ] && echo 'exists' || echo 'not found'";
            String fbCheckResult = RootManager.runRootCommand(fbCheckCommand);
            
            if (fbCheckResult.contains("exists")) {
                String fbCpCommand = "cp " + escapeShellArg(FB_TOKEN_FILE) + " " + escapeShellArg(tempDir + "fb_token.xml");
                String fbCpResult = RootManager.runRootCommand(fbCpCommand);
                
                boolean copied = new File(tempDir + "fb_token.xml").exists();
                
                if (copied) {
                    copiedFiles.add("fb_token.xml");
                    Log.d("BabixGO", "FB-Token kopiert");
                }
            }
        }
        
        // 8. Backup-Info erstellen
        createFileList(tempDir, copiedFiles, includeFbToken);
        
        // 9. Berechtigungen für Temp-Dateien setzen (lesbar für App)
        RootManager.runRootCommand("chmod -R 777 \"" + tempDir + "\"");
        
        Log.d("BabixGO", "Erstelle ZIP...");
        
        // 10. ZIP erstellen (shell command)
        String zipFile = TEMP_PATH + accountName + ".zip";
        String zipCommand = "cd " + escapeShellArg(tempDir) + " && zip -r " + escapeShellArg(zipFile) + " . 2>&1";
        String zipResult = RootManager.runRootCommand(zipCommand);
        
        boolean zipSuccess = new File(zipFile).exists() && new File(zipFile).length() > 0;
        
        if (!zipSuccess) {
            Log.e("BabixGO", "FEHLER: ZIP-Erstellung fehlgeschlagen: " + zipResult);
            deleteRecursive(tempDirFile);
            return false;
        }
        
        Log.d("BabixGO", "ZIP erstellt: " + zipFile);
        
        // 11. Zielverzeichnis erstellen
        String targetDir = ACCOUNTS_EIGENE + accountName + "/";
        File target = new File(targetDir);
        if (!target.exists()) {
            target.mkdirs();
        }
        
        // 12. ZIP in finales Verzeichnis verschieben
        String finalZip = targetDir + accountName + ".zip";
        File zipFileObj = new File(zipFile);
        File finalZipObj = new File(finalZip);
        
        // Alte ZIP löschen falls vorhanden
        if (finalZipObj.exists()) {
            finalZipObj.delete();
        }
        
        boolean moved = zipFileObj.renameTo(finalZipObj);
        
        Log.d("BabixGO", "ZIP verschoben: " + moved + " nach " + finalZip);
        
        // 13. Aufräumen
        deleteRecursive(tempDirFile);
        if (zipFileObj.exists()) {
            zipFileObj.delete();
        }
        
        // 14. Erfolgsprüfung
        boolean finalSuccess = moved && finalZipObj.exists() && finalZipObj.length() > 0;
        
        Log.d("BabixGO", "Backup erfolgreich: " + finalSuccess + 
            " (Größe: " + finalZipObj.length() + " bytes)");
        
        return finalSuccess;
    }
    
    /**
     * Erweiterte Restore-Funktion für ZIP-Archive
     */
    public static boolean restoreAccountExtended(String accountName) {
        // Validate accountName to prevent command injection
        if (!isValidAccountName(accountName)) {
            Log.e(TAG, "Invalid account name: " + accountName);
            return false;
        }
        
        // 1. App stoppen
        forceStopApp();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted during restore", e);
        }
        
        // 2. ZIP-Pfad finden
        String zipPath = ACCOUNTS_EIGENE + accountName + "/" + accountName + ".zip";
        File zipFile = new File(zipPath);
        
        if (!zipFile.exists()) {
            return false;
        }
        
        // 3. Temporäres Verzeichnis erstellen
        String tempDir = TEMP_PATH + accountName + "_restore/";
        File tempDirFile = new File(tempDir);
        
        // Altes Temp-Verzeichnis löschen falls vorhanden
        if (tempDirFile.exists()) {
            deleteRecursive(tempDirFile);
        }
        
        tempDirFile.mkdirs();
        
        // 4. ZIP entpacken (shell command)
        String unzipCommand = "unzip -o " + escapeShellArg(zipPath) + " -d " + escapeShellArg(tempDir) + " 2>&1";
        String unzipResult = RootManager.runRootCommand(unzipCommand);
        
        boolean unzipSuccess = new File(tempDir + "account.dat").exists();
        
        if (!unzipSuccess) {
            deleteRecursive(tempDirFile);
            return false;
        }
        
        // 5. Dateien zurückkopieren
        boolean success = true;
        
        // Required file
        File accountDatFile = new File(tempDir + "account.dat");
        if (accountDatFile.exists()) {
            String cpCommand = "cp " + escapeShellArg(tempDir + "account.dat") + " " + escapeShellArg(REQUIRED_FILE);
            String cpResult = RootManager.runRootCommand(cpCommand);
            success = !cpResult.contains("Error") && !cpResult.contains("cannot");
        } else {
            success = false;
        }
        
        // Optionale Dateien
        if (success) {
            restoreOptionalFiles(tempDir);
            setProperPermissions();
        }
        
        // 6. Aufräumen
        deleteRecursive(tempDirFile);
        
        return success;
    }
    
    /**
     * Hilfsmethode: Rekursives Löschen von Verzeichnissen
     */
    private static boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null || !fileOrDirectory.exists()) {
            return false;
        }
        
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        
        return fileOrDirectory.delete();
    }
    
    /**
     * Hilfsmethode: Dateiname generieren
     */
    private static String getFileName(String fullPath, int index) {
        if (fullPath.contains("device-id") && !fullPath.contains("internal")) {
            return "device-id.txt";
        } else if (fullPath.contains("internal-device-id")) {
            return "internal-device-id.txt";
        } else if (fullPath.contains("generatefid.lock")) {
            return "generatefid.lock";
        } else if (fullPath.contains("playerprefs.xml")) {
            return "playerprefs.xml";
        } else if (fullPath.contains("mys_mod_window_positions")) {
            return "window_positions.xml";
        } else if (fullPath.contains("mys_mod_feature_settings")) {
            return "feature_settings.xml";
        }
        return "file_" + index;
    }
    
    /**
     * Hilfsmethode: Dateiliste erstellen
     */
    private static void createFileList(String tempDir, List<String> files, boolean fbIncluded) {
        try {
            StringBuilder fileList = new StringBuilder();
            fileList.append("=== Backup File List ===\n");
            fileList.append("Date: ").append(new java.util.Date().toString()).append("\n");
            fileList.append("FB-Token included: ").append(fbIncluded ? "YES" : "NO").append("\n");
            fileList.append("\nFiles:\n");
            
            for (String file : files) {
                fileList.append("- ").append(file).append("\n");
            }
            
            java.io.FileWriter fw = new java.io.FileWriter(tempDir + "backup_info.txt");
            fw.write(fileList.toString());
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Hilfsmethode: Validate account name to prevent command injection
     */
    private static boolean isValidAccountName(String accountName) {
        if (accountName == null || accountName.isEmpty()) {
            return false;
        }
        // Prevent excessively long account names that could cause path issues
        if (accountName.length() > 100) {
            return false;
        }
        // Only allow alphanumeric characters, underscores, hyphens, and dots
        // This prevents shell metacharacters from being injected
        return accountName.matches("^[a-zA-Z0-9._-]+$");
    }
    
    /**
     * Helper method: Escape shell argument to prevent injection
     * Uses single quotes and escapes any single quotes in the argument
     */
    private static String escapeShellArg(String arg) {
        if (arg == null) {
            return "''";
        }
        // Replace single quotes with '\'' (end quote, escaped quote, start quote)
        return "'" + arg.replace("'", "'\\''") + "'";
    }
    
    /**
     * Hilfsmethode: Optionale Dateien wiederherstellen
     */
    private static void restoreOptionalFiles(String tempDir) {
        String[][] fileMappings = {
            {"device-id.txt", DATA_DIR + "files/device-id"},
            {"internal-device-id.txt", DATA_DIR + "files/internal-device-id"},
            {"generatefid.lock", DATA_DIR + "files/generatefid.lock"},
            {"playerprefs.xml", DATA_DIR + "shared_prefs/com.scopely.monopolygo.v2.playerprefs.xml"},
            {"window_positions.xml", DATA_DIR + "shared_prefs/mys_mod_window_positions.xml"},
            {"feature_settings.xml", DATA_DIR + "shared_prefs/mys_mod_feature_settings.xml"},
            {"fb_token.xml", DATA_DIR + "shared_prefs/com.facebook.AccessTokenManager.SharedPreferences.xml"}
        };
        
        for (String[] mapping : fileMappings) {
            String sourceFile = tempDir + mapping[0];
            String targetFile = mapping[1];
            
            // Check if source file exists using shell command
            String checkCommand = "[ -f " + escapeShellArg(sourceFile) + " ] && echo 'exists' || echo 'not found'";
            String checkResult = RootManager.runRootCommand(checkCommand);
            
            if (checkResult.contains("exists")) {
                String cpCommand = "cp " + escapeShellArg(sourceFile) + " " + escapeShellArg(targetFile);
                RootManager.runRootCommand(cpCommand);
            }
        }
    }
    
    /**
     * Hilfsmethode: Berechtigungen setzen
     */
    private static void setProperPermissions() {
        String[] commands = {
            "chmod 660 \"" + REQUIRED_FILE + "\"",
            "chown $(stat -c %u:%g " + DATA_DIR + ") \"" + REQUIRED_FILE + "\"",
            "chmod 660 \"" + DATA_DIR + "shared_prefs/*.xml\" 2>/dev/null || true",
            "chown $(stat -c %u:%g " + DATA_DIR + ") \"" + DATA_DIR + "shared_prefs/*.xml\" 2>/dev/null || true"
        };
        
        RootManager.runRootCommands(commands);
    }
    
    /**
     * Hilfsmethode: Datei existiert?
     */
    private static boolean fileExists(String path) {
        String result = RootManager.runRootCommand(
            "[ -f \"" + path + "\" ] && echo 'exists' || echo 'not found'"
        );
        return result.contains("exists");
    }
    
    /**
     * Backup Account - SIMPLIFIED VERSION (like original)
     * No complex checks, just copy
     */
    public static boolean backupAccountSimple(String accountName, boolean includeFbToken) {
        // Validate accountName to prevent command injection
        if (!isValidAccountName(accountName)) {
            android.util.Log.e("BabixGO", "Invalid account name: " + accountName);
            return false;
        }
        
        android.util.Log.d("BabixGO", "=== BACKUP START (Simple) ===");
        android.util.Log.d("BabixGO", "Account: " + accountName);
        android.util.Log.d("BabixGO", "FB-Token: " + includeFbToken);
        
        // 1. App stoppen
        forceStopApp();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 2. Temp-Verzeichnis erstellen
        String tempDir = TEMP_PATH + accountName + "/";
        File tempDirFile = new File(tempDir);
        
        if (tempDirFile.exists()) {
            deleteRecursive(tempDirFile);
        }
        
        if (!tempDirFile.mkdirs()) {
            android.util.Log.e("BabixGO", "Temp-Verzeichnis konnte nicht erstellt werden");
            return false;
        }
        
        android.util.Log.d("BabixGO", "Temp-Dir: " + tempDir);
        
        // 3. Finde Account-Datei
        String accountFile = findAccountFile();
        if (accountFile == null) {
            android.util.Log.e("BabixGO", "Account-Datei nicht gefunden");
            deleteRecursive(tempDirFile);
            return false;
        }
        
        android.util.Log.d("BabixGO", "Account-Datei: " + accountFile);
        
        // 4. Kopiere Account-Datei - SIMPEL wie Original
        String destFile = tempDir + "account.dat";
        String command = "cp " + escapeShellArg(accountFile) + " " + escapeShellArg(destFile);
        
        android.util.Log.d("BabixGO", "Executing: " + command);
        String result = RootManager.runRootCommand(command);
        android.util.Log.d("BabixGO", "Result: " + result);
        
        // Prüfe ob kopiert (kein fileExists, direkter File-Check)
        File copiedFile = new File(destFile);
        if (!copiedFile.exists() || copiedFile.length() == 0) {
            android.util.Log.e("BabixGO", "Kopieren fehlgeschlagen");
            deleteRecursive(tempDirFile);
            return false;
        }
        
        android.util.Log.d("BabixGO", "✓ Account-Datei kopiert (" + copiedFile.length() + " bytes)");
        
        // 5. Optionale Dateien kopieren (simple Version)
        List<String> copiedFiles = new ArrayList<>();
        copiedFiles.add("account.dat");
        
        copyOptionalFile("/data/data/" + PACKAGE_NAME + "/files/device-id", 
                         tempDir + "device-id.txt", copiedFiles);
        copyOptionalFile("/data/data/" + PACKAGE_NAME + "/files/internal-device-id", 
                         tempDir + "internal-device-id.txt", copiedFiles);
        copyOptionalFile("/data/data/" + PACKAGE_NAME + "/shared_prefs/" + PACKAGE_NAME + ".v2.playerprefs.xml", 
                         tempDir + "playerprefs.xml", copiedFiles);
        
        if (includeFbToken) {
            copyOptionalFile("/data/data/" + PACKAGE_NAME + "/shared_prefs/com.facebook.AccessTokenManager.SharedPreferences.xml", 
                             tempDir + "fb_token.xml", copiedFiles);
        }
        
        // 6. Backup-Info
        createFileList(tempDir, copiedFiles, includeFbToken);
        
        // 7. Berechtigungen (more restrictive than 777)
        RootManager.runRootCommand("chmod -R 755 " + escapeShellArg(tempDir));
        
        android.util.Log.d("BabixGO", "Erstelle ZIP...");
        
        // 8. ZIP erstellen (shell command)
        String zipFile = TEMP_PATH + accountName + ".zip";
        String zipCommand = "cd " + escapeShellArg(tempDir) + " && zip -r " + escapeShellArg(zipFile) + " . 2>&1";
        String zipResult = RootManager.runRootCommand(zipCommand);
        
        boolean zipSuccess = new File(zipFile).exists() && new File(zipFile).length() > 0;
        
        if (!zipSuccess) {
            android.util.Log.e("BabixGO", "ZIP-Erstellung fehlgeschlagen: " + zipResult);
            deleteRecursive(tempDirFile);
            return false;
        }
        
        android.util.Log.d("BabixGO", "✓ ZIP erstellt");
        
        // 9. Zielverzeichnis
        String targetDir = ACCOUNTS_EIGENE + accountName + "/";
        File target = new File(targetDir);
        if (!target.exists()) {
            target.mkdirs();
        }
        
        // 10. ZIP verschieben
        String finalZip = targetDir + accountName + ".zip";
        File zipFileObj = new File(zipFile);
        File finalZipObj = new File(finalZip);
        
        if (finalZipObj.exists()) {
            finalZipObj.delete();
        }
        
        boolean moved = zipFileObj.renameTo(finalZipObj);
        
        // 11. Cleanup
        deleteRecursive(tempDirFile);
        if (zipFileObj.exists()) {
            zipFileObj.delete();
        }
        
        // 12. Erfolgsprüfung
        boolean success = moved && finalZipObj.exists() && finalZipObj.length() > 0;
        
        android.util.Log.d("BabixGO", "=== BACKUP " + (success ? "ERFOLGREICH" : "FEHLGESCHLAGEN") + " ===");
        android.util.Log.d("BabixGO", "ZIP: " + finalZip);
        android.util.Log.d("BabixGO", "Größe: " + finalZipObj.length() + " bytes");
        
        return success;
    }
    
    /**
     * Helper method: Copy optional file
     */
    private static void copyOptionalFile(String source, String dest, List<String> fileList) {
        String command = "cp " + escapeShellArg(source) + " " + escapeShellArg(dest) + " 2>&1";
        String result = RootManager.runRootCommand(command);
        
        if (new File(dest).exists()) {
            fileList.add(new File(dest).getName());
            android.util.Log.d("BabixGO", "✓ Optional kopiert: " + new File(dest).getName());
        } else if (!result.isEmpty() && !result.contains("No such file")) {
            // Log unexpected errors, but expected "No such file" is fine for optional files
            android.util.Log.d("BabixGO", "Optional file not copied: " + source + " - " + result);
        }
    }
    
    /**
     * Find Account-File - SIMPLIFIED
     */
    private static String findAccountFile() {
        String[] possiblePaths = {
            "/data/data/" + PACKAGE_NAME + "/files/DiskBasedCacheDirectory/WithBuddies.Services.User.0Production.dat",
            "/data/data/" + PACKAGE_NAME + "/files/WithBuddies.Services.User.0Production.dat",
            "/data/user/0/" + PACKAGE_NAME + "/files/DiskBasedCacheDirectory/WithBuddies.Services.User.0Production.dat"
        };
        
        for (String path : possiblePaths) {
            // SIMPLE CHECK: Versuche zu lesen
            String cmd = "ls " + escapeShellArg(path) + " 2>&1";
            String result = RootManager.runRootCommand(cmd);
            
            if (!result.contains("No such file")) {
                android.util.Log.d("BabixGO", "Gefunden: " + path);
                return path;
            }
        }
        
        // Fallback: find (PACKAGE_NAME is safe, from constant)
        // Note: Wildcards must be outside quotes to be interpreted by shell
        String basePath = "/data/data/" + PACKAGE_NAME;
        String cmd = "find " + escapeShellArg(basePath) + " -name '*WithBuddies.Services.User*.dat' 2>/dev/null | head -n 1";
        String result = RootManager.runRootCommand(cmd);
        
        if (result != null && result.trim().length() > 0 && !result.contains("Error")) {
            return result.trim();
        }
        
        return null;
    }
    
    /**
     * Open a MonopolyGo friend link.
     * @param userId The user ID to add as friend
     */
    public static void openFriendLink(String userId) {
        String url = "monopolygo://add-friend/" + userId;
        RootManager.runRootCommand("am start -a android.intent.action.VIEW -d \"" + url + "\"");
    }
    
    public static String getAccountsEigenePath() {
        return ACCOUNTS_EIGENE;
    }
    
    public static String getAccountsKundenPath() {
        return ACCOUNTS_KUNDEN;
    }
    
    public static String getPartnereventsPath() {
        return PARTNEREVENTS_PATH;
    }
    
    public static String getBackupsPath() {
        return BACKUPS_PATH;
    }
    
    /**
     * Inner class to hold account information from CSV files.
     */
    public static class AccountInfo {
        public String internalId;
        public String userId;
        public String date;
        public String shortLink;
        public String note;
        public String customerId;
        public String username;
    }
    
    /**
     * Read account info from CSV file.
     * @param isOwnAccounts true for own accounts, false for customer accounts
     * @return List of AccountInfo objects
     */
    public static List<AccountInfo> readAccountInfos(boolean isOwnAccounts) {
        String csvPath = isOwnAccounts ? 
            ACCOUNTS_EIGENE + "Accountinfos.csv" : 
            ACCOUNTS_KUNDEN + "Kundeninfos.csv";
        
        List<AccountInfo> accounts = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            reader.skip(1); // Skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                // Validate array has minimum required length
                if (line == null || line.length == 0) {
                    continue;
                }
                
                AccountInfo info = new AccountInfo();
                if (isOwnAccounts) {
                    info.internalId = line[0];
                    info.userId = line.length > 1 ? line[1] : "";
                    info.date = line.length > 2 ? line[2] : "";
                    info.shortLink = line.length > 3 ? line[3] : "";
                    info.note = line.length > 4 ? line[4] : "";
                } else {
                    info.customerId = line[0];
                    info.username = line.length > 1 ? line[1] : "";
                    // Additional customer fields can be added here
                }
                accounts.add(info);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read account info from CSV: " + csvPath, e);
        }
        
        return accounts;
    }
}
