package de.babixgo.monopolygo;

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
        createDirectory(ACCOUNTS_EIGENE);
        createDirectory(ACCOUNTS_KUNDEN);
        createDirectory(PARTNEREVENTS_PATH);
        createDirectory(BACKUPS_PATH);
    }
    
    private static void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
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
     * Erweiterte Backup-Funktion mit ZIP-Archivierung
     * @param accountName Name des Accounts
     * @param includeFbToken Soll FB-Token gesichert werden?
     * @return true wenn erfolgreich
     */
    public static boolean backupAccountExtended(String accountName, boolean includeFbToken) {
        // Validate accountName to prevent command injection
        if (!isValidAccountName(accountName)) {
            Log.e(TAG, "Invalid account name: " + accountName);
            return false;
        }
        
        // 1. App stoppen für konsistente Daten
        forceStopApp();
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted during backup", e);
        }
        
        // 2. Temporäres Verzeichnis erstellen
        String tempDir = TEMP_PATH + accountName + "/";
        RootManager.runRootCommand("mkdir -p \"" + tempDir + "\"");
        
        // 3. REQUIRED FILE kopieren (muss existieren)
        if (!fileExists(REQUIRED_FILE)) {
            RootManager.runRootCommand("rm -rf \"" + tempDir + "\"");
            return false;
        }
        
        String result = RootManager.runRootCommand(
            "cp \"" + REQUIRED_FILE + "\" \"" + tempDir + "account.dat\""
        );
        
        if (result.contains("Error") || result.contains("cannot")) {
            RootManager.runRootCommand("rm -rf \"" + tempDir + "\"");
            return false;
        }
        
        // 4. Optionale Dateien kopieren (nur wenn vorhanden)
        List<String> copiedFiles = new ArrayList<>();
        copiedFiles.add("account.dat");
        
        for (int i = 0; i < OPTIONAL_FILES.length; i++) {
            String sourceFile = OPTIONAL_FILES[i];
            if (fileExists(sourceFile)) {
                String fileName = getFileName(sourceFile, i);
                String copyResult = RootManager.runRootCommand(
                    "cp \"" + sourceFile + "\" \"" + tempDir + fileName + "\""
                );
                
                if (!copyResult.contains("Error")) {
                    copiedFiles.add(fileName);
                }
            }
        }
        
        // 5. FB-Token kopieren (falls gewünscht und vorhanden)
        if (includeFbToken && fileExists(FB_TOKEN_FILE)) {
            String copyResult = RootManager.runRootCommand(
                "cp \"" + FB_TOKEN_FILE + "\" \"" + tempDir + "fb_token.xml\""
            );
            
            if (!copyResult.contains("Error")) {
                copiedFiles.add("fb_token.xml");
            }
        }
        
        // 6. Dateiliste für Dokumentation erstellen
        createFileList(tempDir, copiedFiles, includeFbToken);
        
        // 7. ZIP-Archiv erstellen
        String zipFile = TEMP_PATH + accountName + ".zip";
        String zipResult = RootManager.runRootCommand(
            "cd \"" + tempDir + "\" && zip -r \"" + zipFile + "\" ."
        );
        
        if (zipResult.contains("Error") || !fileExists(zipFile)) {
            RootManager.runRootCommand("rm -rf \"" + tempDir + "\"");
            return false;
        }
        
        // 8. Zielverzeichnis erstellen
        String targetDir = ACCOUNTS_EIGENE + accountName + "/";
        File target = new File(targetDir);
        if (!target.exists()) {
            target.mkdirs();
        }
        
        // 9. ZIP in finales Verzeichnis verschieben
        String finalZip = targetDir + accountName + ".zip";
        String moveResult = RootManager.runRootCommand(
            "cp \"" + zipFile + "\" \"" + finalZip + "\" && " +
            "chmod 644 \"" + finalZip + "\""
        );
        
        // 10. Aufräumen
        RootManager.runRootCommand("rm -rf \"" + tempDir + "\"");
        RootManager.runRootCommand("rm -f \"" + zipFile + "\"");
        
        // 11. Erfolgsprüfung
        File finalFile = new File(finalZip);
        return finalFile.exists() && finalFile.length() > 0;
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
        RootManager.runRootCommand("mkdir -p \"" + tempDir + "\"");
        
        // 4. ZIP entpacken
        String unzipResult = RootManager.runRootCommand(
            "unzip -o \"" + zipPath + "\" -d \"" + tempDir + "\""
        );
        
        if (unzipResult.contains("Error")) {
            RootManager.runRootCommand("rm -rf \"" + tempDir + "\"");
            return false;
        }
        
        // 5. Dateien zurückkopieren
        boolean success = true;
        
        // Required file
        if (fileExists(tempDir + "account.dat")) {
            String result = RootManager.runRootCommand(
                "cp \"" + tempDir + "account.dat\" \"" + REQUIRED_FILE + "\""
            );
            if (result.contains("Error")) {
                success = false;
            }
        } else {
            success = false;
        }
        
        // Optionale Dateien
        if (success) {
            restoreOptionalFiles(tempDir);
            
            // Berechtigungen setzen
            setProperPermissions();
        }
        
        // 6. Aufräumen
        RootManager.runRootCommand("rm -rf \"" + tempDir + "\"");
        
        return success;
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
        StringBuilder fileList = new StringBuilder();
        fileList.append("=== Backup File List ===\n");
        fileList.append("Date: ").append(new java.util.Date().toString()).append("\n");
        fileList.append("FB-Token included: ").append(fbIncluded ? "YES" : "NO").append("\n");
        fileList.append("\nFiles:\n");
        
        for (String file : files) {
            fileList.append("- ").append(file).append("\n");
        }
        
        // Write file using Java instead of shell echo to avoid command injection
        try {
            File infoFile = new File(tempDir + "backup_info.txt");
            java.io.FileWriter writer = new java.io.FileWriter(infoFile);
            writer.write(fileList.toString());
            writer.close();
            // Set permissions via root
            RootManager.runRootCommand("chmod 644 \"" + tempDir + "backup_info.txt\"");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create backup info file", e);
        }
    }
    
    /**
     * Hilfsmethode: Validate account name to prevent command injection
     */
    private static boolean isValidAccountName(String accountName) {
        if (accountName == null || accountName.isEmpty()) {
            return false;
        }
        // Only allow alphanumeric characters, underscores, hyphens, and dots
        // This prevents shell metacharacters from being injected
        return accountName.matches("^[a-zA-Z0-9._-]+$");
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
            
            if (fileExists(sourceFile)) {
                RootManager.runRootCommand(
                    "cp \"" + sourceFile + "\" \"" + targetFile + "\""
                );
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
