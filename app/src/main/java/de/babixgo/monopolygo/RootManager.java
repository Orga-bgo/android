package de.babixgo.monopolygo;

import java.io.*;

/**
 * Manager class for handling root access and executing root commands.
 * VERBESSERT: Nutzt explizite Shell für alle Befehle
 */
public class RootManager {
    private static boolean hasRootAccess = false;
    private static boolean rootChecked = false;

    /**
     * Check if the device is rooted.
     */
    public static boolean isRooted() {
        String[] paths = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su"
        };
        
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Request root access from SuperSU/Magisk.
     * @return true if root access is granted, false otherwise
     */
    public static boolean requestRoot() {
        if (rootChecked && hasRootAccess) {
            return true;
        }
        
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("echo 'Root Granted'\n");
            os.writeBytes("exit\n");
            os.flush();
            
            process.waitFor();
            hasRootAccess = (process.exitValue() == 0);
            rootChecked = true;
            
            os.close();
            return hasRootAccess;
        } catch (Exception e) {
            hasRootAccess = false;
            rootChecked = true;
            return false;
        }
    }

    /**
     * Validate command (basic check)
     */
    private static boolean isCommandSafe(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }
        
        String[] dangerousPatterns = {
            ";rm -rf /",
            "&& rm -rf /",
            "| rm -rf /",
            ">/dev/sda"
        };
        
        for (String pattern : dangerousPatterns) {
            if (command.contains(pattern)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Execute a command with root privileges.
     * VERBESSERT: Nutzt explizite Shell (sh -c)
     */
    public static String runRootCommand(String command) {
        if (!isCommandSafe(command)) {
            android.util.Log.e("BabixGO", "Command validation failed: " + command);
            return "Error: Command validation failed";
        }
        
        android.util.Log.d("BabixGO", "Executing root command: " + command);
        
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        try {
            // WICHTIG: Nutze "su -c sh" für Shell-Context
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));
            
            // Führe Command in Shell aus
            os.writeBytes("sh -c '" + command.replace("'", "'\\''") + "'\n");
            os.writeBytes("exit\n");
            os.flush();
            
            // Lese Output
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            // Lese Fehler
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            process.waitFor();
            
            os.close();
            reader.close();
            errorReader.close();
            
            if (errorOutput.length() > 0) {
                android.util.Log.w("BabixGO", "Command stderr: " + errorOutput.toString());
            }
            
            String result = output.toString();
            android.util.Log.d("BabixGO", "Command output: '" + result.trim() + "'");
            
            return result;
            
        } catch (Exception e) {
            android.util.Log.e("BabixGO", "Command error: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Execute multiple commands with root privileges.
     */
    public static String runRootCommands(String[] commands) {
        StringBuilder output = new StringBuilder();
        
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));
            
            for (String command : commands) {
                os.writeBytes("sh -c '" + command.replace("'", "'\\''") + "'\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            while ((line = errorReader.readLine()) != null) {
                output.append("ERROR: ").append(line).append("\n");
            }
            
            process.waitFor();
            
            os.close();
            reader.close();
            errorReader.close();
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
        
        return output.toString();
    }

    /**
     * Check if root access has been granted.
     */
    public static boolean hasRootAccess() {
        return hasRootAccess;
    }
}
