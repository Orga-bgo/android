package de.babixgo.monopolygo;

import java.io.*;

/**
 * Manager class for handling root access and executing root commands.
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
     * Validate and sanitize command input to prevent command injection.
     * NOTE: This is a basic validation. Commands are constructed internally
     * in controlled contexts, not from direct user input.
     * @param command The command to validate
     * @return true if command appears safe, false otherwise
     */
    private static boolean isCommandSafe(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }
        
        // Basic checks - commands should not contain suspicious patterns
        // This is primarily for defensive programming as commands are
        // constructed internally, not from user input
        String[] dangerousPatterns = {
            ";rm -rf",
            "&& rm",
            "| rm",
            ">/dev/",
            "<(curl",
            "$(curl"
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
     * NOTE: Commands should be constructed programmatically, not from raw user input.
     * @param command The command to execute
     * @return The output of the command
     */
    public static String runRootCommand(String command) {
        // Basic safety check
        if (!isCommandSafe(command)) {
            return "Error: Command validation failed - potentially unsafe command";
        }
        
        StringBuilder output = new StringBuilder();
        
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));
            
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            // Also read error stream
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
     * Execute multiple commands with root privileges.
     * @param commands Array of commands to execute
     * @return The combined output of all commands
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
                os.writeBytes(command + "\n");
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
