package de.babixgo.monopolygo;

import java.io.File;

/**
 * Manager class for MonopolyGo account operations (backup, restore, etc).
 */
public class AccountManager {
    private static final String PACKAGE_NAME = "com.scopely.monopolygo";
    private static final String BASE_PATH = "/storage/emulated/0/MonopolyGo/";
    private static final String ACCOUNTS_EIGENE = BASE_PATH + "Accounts/Eigene/";
    private static final String ACCOUNTS_KUNDEN = BASE_PATH + "Accounts/Kunden/";
    private static final String PARTNEREVENTS_PATH = BASE_PATH + "Partnerevents/";
    private static final String BACKUPS_PATH = BASE_PATH + "Backups/";
    
    private static final String DATA_FILE_PATH = "/data/data/" + PACKAGE_NAME + 
        "/files/DiskBasedCacheDirectory/WithBuddies.Services.User.0Production.dat";
    
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
     * Get the list of backed up accounts.
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
        
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].getName();
        }
        
        return names;
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
}
