package de.babixgo.monopolygo.utils;

import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper for AES-256 encryption of passwords
 */
public class EncryptionHelper {
    private static final String TAG = "EncryptionHelper";
    private static final String ALGORITHM = "AES";
    
    // IMPORTANT: In production, load this key from secure storage!
    // This key is 32 bytes (256 bits) for AES-256
    private static final String KEY = "BabixGO2026SecureKey1234567890"; // CHANGE THIS!
    
    /**
     * Encrypt text with AES-256
     * @param plainText Plain text
     * @return Encrypted text (Base64)
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
            
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Decrypt encrypted text
     * @param encryptedText Encrypted text (Base64)
     * @return Plain text
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return "";
        }
        
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            
            byte[] decoded = Base64.decode(encryptedText, Base64.NO_WRAP);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, "UTF-8");
            
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }
}
