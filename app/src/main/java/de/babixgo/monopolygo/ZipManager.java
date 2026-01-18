package de.babixgo.monopolygo;

import java.io.*;
import java.util.zip.*;

/**
 * Pure Java ZIP implementation - no external tools needed
 */
public class ZipManager {
    
    /**
     * Create ZIP archive from directory
     * @param sourceDir Directory to zip
     * @param zipFilePath Output ZIP file path
     * @return true if successful
     */
    public static boolean zipDirectory(String sourceDir, String zipFilePath) {
        try {
            File sourceDirFile = new File(sourceDir);
            if (!sourceDirFile.exists() || !sourceDirFile.isDirectory()) {
                return false;
            }
            
            try (FileOutputStream fos = new FileOutputStream(zipFilePath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                zipDirectoryRecursive(sourceDirFile, sourceDirFile, zos);
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Recursive helper for directory zipping
     */
    private static void zipDirectoryRecursive(File rootDir, File sourceDir, 
                                             ZipOutputStream zos) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectoryRecursive(rootDir, file, zos);
            } else {
                String relativePath = getRelativePath(rootDir, file);
                ZipEntry zipEntry = new ZipEntry(relativePath);
                zos.putNextEntry(zipEntry);
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                
                zos.closeEntry();
            }
        }
    }
    
    /**
     * Get relative path for ZIP entry
     */
    private static String getRelativePath(File rootDir, File file) {
        String rootPath = rootDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        
        if (filePath.startsWith(rootPath)) {
            String relative = filePath.substring(rootPath.length());
            if (relative.startsWith(File.separator)) {
                relative = relative.substring(1);
            }
            return relative.replace(File.separator, "/");
        }
        
        return file.getName();
    }
    
    /**
     * Extract ZIP archive to directory
     * @param zipFilePath ZIP file to extract
     * @param destDir Destination directory
     * @return true if successful
     */
    public static boolean unzipArchive(String zipFilePath, String destDir) {
        try {
            File destDirFile = new File(destDir);
            if (!destDirFile.exists()) {
                destDirFile.mkdirs();
            }
            
            try (FileInputStream fis = new FileInputStream(zipFilePath);
                 ZipInputStream zis = new ZipInputStream(fis)) {
                ZipEntry zipEntry;
                
                while ((zipEntry = zis.getNextEntry()) != null) {
                    String fileName = zipEntry.getName();
                    
                    // Security: Prevent path traversal (Zip Slip vulnerability)
                    if (fileName.contains("..") || fileName.startsWith("/")) {
                        zis.closeEntry();
                        continue;
                    }
                    
                    File newFile = new File(destDir, fileName);
                    
                    // Additional security check: ensure file is within destDir
                    String canonicalDestPath = destDirFile.getCanonicalPath();
                    String canonicalFilePath = newFile.getCanonicalPath();
                    // Check if path starts with destination + separator (prevents bypass)
                    if (!canonicalFilePath.equals(canonicalDestPath) && 
                        !canonicalFilePath.startsWith(canonicalDestPath + File.separator)) {
                        zis.closeEntry();
                        continue;
                    }
                    
                    // Create parent directories
                    File parent = newFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    
                    if (!zipEntry.isDirectory()) {
                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            byte[] buffer = new byte[8192];
                            int length;
                            
                            while ((length = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, length);
                            }
                        }
                    }
                    
                    zis.closeEntry();
                }
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Copy file with root privileges
     * VERBESSERT: Nutzt cat-Umleitung statt cp
     * @param source Source file path
     * @param dest Destination file path
     * @return true if successful
     */
    public static boolean copyFileWithRoot(String source, String dest) {
        android.util.Log.d("BabixGO", "Kopiere mit Root: " + source + " -> " + dest);
        
        // Methode 1: cat > Umleitung (funktioniert besser auf manchen Geräten)
        String command = "cat \"" + source + "\" > \"" + dest + "\" 2>&1";
        String result = RootManager.runRootCommand(command);
        
        android.util.Log.d("BabixGO", "Copy result: " + result);
        
        // Prüfe ob Ziel existiert (mit Root)
        boolean destExists = fileExistsWithRoot(dest);
        
        if (!destExists) {
            // Fallback: Versuche mit cp
            android.util.Log.d("BabixGO", "Fallback: Versuche cp");
            command = "cp -f \"" + source + "\" \"" + dest + "\" 2>&1";
            result = RootManager.runRootCommand(command);
            android.util.Log.d("BabixGO", "CP result: " + result);
            destExists = fileExistsWithRoot(dest);
        }
        
        android.util.Log.d("BabixGO", "Datei kopiert: " + destExists);
        return destExists;
    }
    
    /**
     * Check if file exists (with root)
     * VERBESSERT: Mehrere Prüfmethoden
     */
    public static boolean fileExistsWithRoot(String path) {
        android.util.Log.d("BabixGO", "Prüfe Datei mit Root: " + path);
        
        // Methode 1: test -f
        String result1 = RootManager.runRootCommand(
            "test -f \"" + path + "\" && echo 'EXISTS' || echo 'NOT_FOUND'"
        );
        android.util.Log.d("BabixGO", "Test -f result: '" + result1 + "'");
        
        if (result1 != null && result1.trim().contains("EXISTS")) {
            android.util.Log.d("BabixGO", "✓ Datei existiert (test -f)");
            return true;
        }
        
        // Methode 2: ls (als Fallback)
        String result2 = RootManager.runRootCommand("ls -la \"" + path + "\" 2>&1");
        android.util.Log.d("BabixGO", "ls result: " + result2);
        
        boolean exists = result2 != null && 
                        !result2.contains("No such file") && 
                        !result2.contains("not found") &&
                        !result2.contains("cannot access") &&
                        result2.trim().length() > 0;
        
        android.util.Log.d("BabixGO", exists ? "✓ Datei existiert (ls)" : "✗ Datei nicht gefunden");
        return exists;
    }
}
