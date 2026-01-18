package de.babixgo.monopolygo;

import com.topjohnwu.superuser.Shell;
import java.util.List;

/**
 * Manager class for handling root access and executing root commands.
 * FIXED: Uses libsu library for proper root handling on all Android versions
 * 
 * This fixes issues on Android 10+ where manual su execution with sh -c wrapper
 * fails due to SELinux restrictions and compatibility issues with newer Magisk versions.
 */
public class RootManager {
    private static boolean hasRootAccess = false;
    private static boolean rootChecked = false;
    
    static {
        // Configure libsu Shell
        Shell.enableVerboseLogging = android.util.Log.isLoggable("BabixGO", android.util.Log.DEBUG);
        Shell.setDefaultBuilder(Shell.Builder.create()
            .setFlags(Shell.FLAG_REDIRECT_STDERR)
            .setTimeout(10));
    }

    /**
     * Check if the device is rooted.
     * Uses libsu's built-in root detection which is more reliable.
     */
    public static boolean isRooted() {
        // libsu handles root detection properly across all Android versions
        return Shell.isAppGrantedRoot() != null && Shell.isAppGrantedRoot();
    }

    /**
     * Request root access from SuperSU/Magisk/KernelSU.
     * Uses libsu which properly handles root requests on all Android versions.
     * @return true if root access is granted, false otherwise
     */
    public static boolean requestRoot() {
        if (rootChecked && hasRootAccess) {
            return true;
        }
        
        try {
            // libsu automatically handles root request dialog and compatibility
            // Works with Magisk 24+, KernelSU, and older SuperSU
            Boolean granted = Shell.isAppGrantedRoot();
            hasRootAccess = granted != null && granted;
            rootChecked = true;
            
            android.util.Log.d("BabixGO", "Root access: " + (hasRootAccess ? "granted" : "denied"));
            return hasRootAccess;
        } catch (Exception e) {
            android.util.Log.e("BabixGO", "Root request error: " + e.getMessage());
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
     * FIXED: Uses libsu Shell API which properly handles shell context on all Android versions.
     * This eliminates the need for manual sh -c wrapping and fixes compatibility issues.
     */
    public static String runRootCommand(String command) {
        if (!isCommandSafe(command)) {
            android.util.Log.e("BabixGO", "Command validation failed: " + command);
            return "Error: Command validation failed";
        }
        
        android.util.Log.d("BabixGO", "Executing root command: " + command);
        
        try {
            // libsu automatically provides proper shell context
            // No need for manual sh -c wrapping - libsu handles this internally
            Shell.Result result = Shell.cmd(command).exec();
            
            // Get output
            StringBuilder output = new StringBuilder();
            for (String line : result.getOut()) {
                output.append(line).append("\n");
            }
            
            // Log errors if any
            if (!result.getErr().isEmpty()) {
                StringBuilder errors = new StringBuilder();
                for (String line : result.getErr()) {
                    errors.append(line).append("\n");
                }
                android.util.Log.w("BabixGO", "Command stderr: " + errors.toString());
            }
            
            // Check if command succeeded
            if (!result.isSuccess()) {
                android.util.Log.e("BabixGO", "Command failed with code: " + result.getCode());
            }
            
            String outputStr = output.toString();
            android.util.Log.d("BabixGO", "Command output: '" + outputStr.trim() + "'");
            
            return outputStr;
            
        } catch (Exception e) {
            android.util.Log.e("BabixGO", "Command error: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Execute multiple commands with root privileges.
     * FIXED: Uses libsu for proper command execution on all Android versions.
     */
    public static String runRootCommands(String[] commands) {
        StringBuilder output = new StringBuilder();
        
        try {
            // Validate commands first
            for (String command : commands) {
                if (!isCommandSafe(command)) {
                    android.util.Log.e("BabixGO", "Command validation failed: " + command);
                    output.append("Error: Command validation failed for: ").append(command).append("\n");
                    return output.toString();
                }
            }
            
            // Execute all commands in a single shell session
            // libsu automatically maintains shell context between commands
            Shell.Result result = Shell.cmd(commands).exec();
            
            // Collect output
            for (String line : result.getOut()) {
                output.append(line).append("\n");
            }
            
            // Collect errors
            for (String line : result.getErr()) {
                output.append("ERROR: ").append(line).append("\n");
            }
            
            if (!result.isSuccess()) {
                android.util.Log.e("BabixGO", "Commands failed with code: " + result.getCode());
            }
            
        } catch (Exception e) {
            android.util.Log.e("BabixGO", "Commands error: " + e.getMessage());
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
