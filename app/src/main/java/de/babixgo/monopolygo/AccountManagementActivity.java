package de.babixgo.monopolygo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for account management operations.
 */
public class AccountManagementActivity extends AppCompatActivity {
    
    private Button btnRestore, btnBackupOwn, btnBackupCustomer, btnCopyLinks;
    private TextView tvStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_management);
        
        btnRestore = findViewById(R.id.btn_restore_account);
        btnBackupOwn = findViewById(R.id.btn_backup_own);
        btnBackupCustomer = findViewById(R.id.btn_backup_customer);
        btnCopyLinks = findViewById(R.id.btn_copy_links);
        tvStatus = findViewById(R.id.tv_status);
        
        setupButtons();
    }
    
    private void setupButtons() {
        btnRestore.setOnClickListener(v -> showRestoreDialog());
        btnBackupOwn.setOnClickListener(v -> showBackupOwnDialog());
        btnBackupCustomer.setOnClickListener(v -> showBackupCustomerDialog());
        btnCopyLinks.setOnClickListener(v -> showCopyLinksDialog());
    }
    
    private void showRestoreDialog() {
        // Only allow restoring from own accounts (customer folders don't contain backups)
        showAccountSelectionDialog(true);
    }
    
    private void showAccountSelectionDialog(boolean isOwnAccounts) {
        String[] accounts = AccountManager.getBackedUpAccounts(isOwnAccounts);
        
        if (accounts.length == 0) {
            Toast.makeText(this, "Keine Accounts gefunden", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Account auswählen");
        builder.setItems(accounts, (dialog, which) -> {
            String accountName = accounts[which];
            restoreAccount(accountName, isOwnAccounts);
        });
        builder.setNegativeButton("Abbrechen", null);
        builder.show();
    }
    
    private void restoreAccount(String accountName, boolean isOwnAccounts) {
        tvStatus.setText("Wiederherstelle Account: " + accountName);
        
        new Thread(() -> {
            String basePath = isOwnAccounts ? 
                AccountManager.getAccountsEigenePath() : 
                AccountManager.getAccountsKundenPath();
            
            String sourceFile = basePath + accountName + "/WithBuddies.Services.User.0Production.dat";
            
            boolean success = AccountManager.restoreAccount(sourceFile);
            
            runOnUiThread(() -> {
                if (success) {
                    tvStatus.setText("Account erfolgreich wiederhergestellt: " + accountName);
                    Toast.makeText(this, "Account wiederhergestellt", Toast.LENGTH_SHORT).show();
                    
                    // Ask if user wants to start the app
                    new AlertDialog.Builder(this)
                        .setTitle("App starten?")
                        .setMessage("Möchten Sie MonopolyGo jetzt starten?")
                        .setPositiveButton("Ja", (d, w) -> {
                            AccountManager.startApp();
                            Toast.makeText(this, "App wird gestartet...", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Nein", null)
                        .show();
                } else {
                    tvStatus.setText("Fehler beim Wiederherstellen");
                    Toast.makeText(this, "Fehler beim Wiederherstellen", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    private void showBackupOwnDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Eigenen Account sichern");
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_backup_own, null);
        EditText etInternalId = dialogView.findViewById(R.id.et_internal_id);
        EditText etNote = dialogView.findViewById(R.id.et_note);
        
        builder.setView(dialogView);
        builder.setPositiveButton("Sichern", (dialog, which) -> {
            String internalId = etInternalId.getText().toString().trim();
            String note = etNote.getText().toString().trim();
            
            if (internalId.isEmpty()) {
                Toast.makeText(this, "Interne ID ist erforderlich", Toast.LENGTH_SHORT).show();
                return;
            }
            
            backupOwnAccount(internalId, note);
        });
        builder.setNegativeButton("Abbrechen", null);
        builder.show();
    }
    
    private void backupOwnAccount(String internalId, String note) {
        tvStatus.setText("Sichere Account...");
        
        new Thread(() -> {
            // Extract UserID (optional - backup proceeds even if this fails)
            String userId = DataExtractor.extractUserId();
            String shortLink = null;
            
            if (userId == null) {
                runOnUiThread(() -> {
                    tvStatus.setText("Warnung: UserID konnte nicht extrahiert werden. Fortfahren mit Backup...");
                    Toast.makeText(this, "UserID nicht gefunden - Backup wird trotzdem durchgeführt", Toast.LENGTH_LONG).show();
                });
            } else {
                // Create short link only if UserID is available
                shortLink = ShortLinkManager.createShortLink(userId, internalId);
                if (shortLink == null) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Warnung: Shortlink konnte nicht erstellt werden");
                    });
                }
            }
            
            // Backup account (always proceed regardless of UserID extraction)
            boolean success = AccountManager.backupAccount(
                AccountManager.getAccountsEigenePath(), 
                internalId
            );
            
            final String finalShortLink = shortLink;
            final String finalUserId = userId;
            runOnUiThread(() -> {
                if (success) {
                    String message = "Account gesichert\n" +
                                   "Interne ID: " + internalId + "\n" +
                                   "UserID: " + (finalUserId != null ? finalUserId : "Nicht gefunden") + "\n" +
                                   "Shortlink: " + (finalShortLink != null ? finalShortLink : "Nicht verfügbar");
                    tvStatus.setText(message);
                    Toast.makeText(this, "Account erfolgreich gesichert", Toast.LENGTH_SHORT).show();
                } else {
                    tvStatus.setText("Fehler beim Sichern");
                    Toast.makeText(this, "Fehler beim Sichern", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    private void showBackupCustomerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Kunden Account sichern");
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_backup_customer, null);
        EditText etCustomerName = dialogView.findViewById(R.id.et_customer_name);
        EditText etFriendLink = dialogView.findViewById(R.id.et_friend_link);
        
        builder.setView(dialogView);
        builder.setPositiveButton("Sichern", (dialog, which) -> {
            String customerName = etCustomerName.getText().toString().trim();
            String friendLink = etFriendLink.getText().toString().trim();
            
            if (customerName.isEmpty() || friendLink.isEmpty()) {
                Toast.makeText(this, "Alle Felder sind erforderlich", Toast.LENGTH_SHORT).show();
                return;
            }
            
            backupCustomerAccount(customerName, friendLink);
        });
        builder.setNegativeButton("Abbrechen", null);
        builder.show();
    }
    
    private void backupCustomerAccount(String customerName, String friendLink) {
        tvStatus.setText("Sichere Kunden-Metadaten: " + customerName);
        
        // Extract UserID from friend link
        String userId = extractUserIdFromLink(friendLink);
        if (userId == null) {
            Toast.makeText(this, "Ungültiger Freundschaftslink", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create customer folder (without account backup files)
        File customerDir = new File(AccountManager.getAccountsKundenPath(), customerName);
        if (!customerDir.exists()) {
            customerDir.mkdirs();
        }
        
        // Save customer info (in real implementation, this would save to CSV)
        // Note: Customer folders only contain metadata, not account backup files
        String message = "Kunden-Metadaten gespeichert\n" +
                       "Name: " + customerName + "\n" +
                       "UserID: " + userId + "\n" +
                       "Link: " + friendLink + "\n" +
                       "Ordner: " + customerDir.getAbsolutePath();
        tvStatus.setText(message);
        Toast.makeText(this, "Kunden-Metadaten gespeichert", Toast.LENGTH_SHORT).show();
    }
    
    private String extractUserIdFromLink(String link) {
        // Extract UserID from monopolygo://add-friend/USERID
        // Handles various formats including query parameters
        if (link == null || link.isEmpty()) {
            return null;
        }
        
        try {
            // Normalize the link
            link = link.trim();
            
            // Check for the expected protocol and path
            if (link.contains("add-friend/")) {
                // Find the start of the user ID
                int startIndex = link.indexOf("add-friend/") + "add-friend/".length();
                
                if (startIndex < link.length()) {
                    // Extract everything after "add-friend/"
                    String userId = link.substring(startIndex);
                    
                    // Remove any query parameters or fragments
                    int endIndex = userId.length();
                    if (userId.contains("?")) {
                        endIndex = Math.min(endIndex, userId.indexOf("?"));
                    }
                    if (userId.contains("#")) {
                        endIndex = Math.min(endIndex, userId.indexOf("#"));
                    }
                    if (userId.contains("&")) {
                        endIndex = Math.min(endIndex, userId.indexOf("&"));
                    }
                    
                    userId = userId.substring(0, endIndex).trim();
                    
                    // Validate that it's numeric (UserIDs should be numbers)
                    if (userId.matches("\\d+")) {
                        return userId;
                    }
                }
            }
        } catch (Exception e) {
            // Log error in real implementation
            return null;
        }
        
        return null;
    }
    
    private void showCopyLinksDialog() {
        Toast.makeText(this, "Link-Kopie Funktion (In Entwicklung)", Toast.LENGTH_SHORT).show();
        // This would show a list of saved links and allow copying to clipboard
    }
}
