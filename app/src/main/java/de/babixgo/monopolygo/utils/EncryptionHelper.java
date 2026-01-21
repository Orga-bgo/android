package de.babixgo.monopolygo.utils;

import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * Helper for AES-256 encryption of passwords
 * Uses CBC mode with random IV for security
 * 
 * SECURITY NOTE: This implementation uses a hard-coded key for simplicity.
 * For production use, consider:
 * - Storing the key in Android Keystore
 * - Using per-user encryption keys
 * - Implementing key rotation
 */
public class EncryptionHelper {
    private static final String TAG = "EncryptionHelper";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding"; // CBC mode with IV
    
    // IMPORTANT: In production, load this key from secure storage (Android Keystore)!
    // This key is 32 bytes (256 bits) for AES-256
    private static final String KEY = "BabixGO2026SecureKey1234567890"; // CHANGE THIS!
    
    /**
     * Encrypt text with AES-256 CBC mode
     * Uses random IV prepended to ciphertext
     * @param plainText Plain text
     * @return Encrypted text with IV (Base64)
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        
        try {
            // Generate random IV
            byte[] iv = new byte[16]; // AES block size is 16 bytes
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Encrypt
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            
            // Prepend IV to encrypted data
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            
            return Base64.encodeToString(combined, Base64.NO_WRAP);
            
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Decrypt encrypted text
     * Extracts IV from beginning of ciphertext
     * @param encryptedText Encrypted text with IV (Base64)
     * @return Plain text
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return "";
        }
        
        try {
            byte[] combined = Base64.decode(encryptedText, Base64.NO_WRAP);
            
            // Extract IV from beginning
            byte[] iv = new byte[16]; // AES block size is 16 bytes
            System.arraycopy(combined, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Extract encrypted data
            byte[] encrypted = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);
            
            // Decrypt
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, "UTF-8");
            
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }
}
