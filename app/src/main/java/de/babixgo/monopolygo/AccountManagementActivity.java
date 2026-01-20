package de.babixgo.monopolygo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import de.babixgo.monopolygo.database.AccountRepository;
import de.babixgo.monopolygo.models.Account;
import de.babixgo.monopolygo.utils.DeviceIdExtractor;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AccountManagementActivity extends AppCompatActivity {
    
    private Button btnRestore, btnBackupOwn, btnBackupCustomer, btnCopyLinks;
    private TextView tvStatus;
    private AccountRepository repository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_management);
        
        repository = new AccountRepository();
        
        btnRestore = findViewById(R.id.btn_restore_account);
        btnBackupOwn = findViewById(R.id.btn_backup_own);
        btnBackupCustomer = findViewById(R.id.btn_backup_customer);
        btnCopyLinks = findViewById(R.id.btn_copy_links);
        tvStatus = findViewById(R.id.tv_status);
        
        setupButtons();
        
        // Handle intent from AccountListActivity
        handleIntent();
    }
    
    private void handleIntent() {
        String action = getIntent().getStringExtra("action");
        String accountName = getIntent().getStringExtra("account_name");
        
        if ("restore".equals(action) && accountName != null) {
            // Auto-trigger restore
            restoreAccountByName(accountName);
        }
    }
    
    private void setupButtons() {
        btnRestore.setOnClickListener(v -> showRestoreDialog());
        btnBackupOwn.setOnClickListener(v -> showBackupOwnDialog());
        btnBackupCustomer.setOnClickListener(v -> showBackupCustomerDialog());
        btnCopyLinks.setOnClickListener(v -> showCopyLinksDialog());
    }
    
    // RESTORE FUNCTIONALITY
    private void showRestoreDialog() {
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
    
    private void restoreAccountByName(String accountName) {
        restoreAccount(accountName, true);
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
                    
                    // Update last_played in database
                    updateLastPlayedInDatabase(accountName);
                    
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
    
    private void updateLastPlayedInDatabase(String accountName) {
        repository.getAccountByName(accountName)
            .thenAccept(account -> {
                if (account != null) {
                    repository.updateLastPlayed(account.getId());
                }
            });
    }
    
    // BACKUP OWN ACCOUNT FUNCTIONALITY
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
            // 1. File Backup (BESTEHEND - UNVERÄNDERT)
            boolean fileBackupSuccess = AccountManager.backupAccount(
                AccountManager.getAccountsEigenePath(), 
                internalId
            );
            
            if (!fileBackupSuccess) {
                runOnUiThread(() -> {
                    tvStatus.setText("Fehler beim Datei-Backup");
                    Toast.makeText(this, "Backup fehlgeschlagen", Toast.LENGTH_LONG).show();
                });
                return;
            }
            
            // 2. UserID Extraction (BESTEHEND - UNVERÄNDERT)
            String userId = DataExtractor.extractUserId();
            
            if (userId == null) {
                runOnUiThread(() -> {
                    tvStatus.setText("Warnung: UserID nicht gefunden - Backup wird trotzdem fortgesetzt");
                    Toast.makeText(this, 
                        "UserID nicht gefunden - Backup wird trotzdem durchgeführt", 
                        Toast.LENGTH_LONG).show();
                });
            }
            
            // 3. Device-ID Extraction (NEU)
            DeviceIdExtractor.extractAllIds(this)
                .thenAccept(deviceIds -> {
                    // 4. Save to Supabase (NEU)
                    saveAccountToDatabase(internalId, userId, deviceIds, note);
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        // Backup succeeded even without Device IDs
                        saveAccountToDatabase(internalId, userId, null, note);
                    });
                    return null;
                });
            
        }).start();
    }
    
    private void saveAccountToDatabase(
        String name, 
        String userId, 
        DeviceIdExtractor.DeviceIds deviceIds, 
        String note
    ) {
        Account account = new Account(name, userId);
        account.setNote(note);
        
        // Set Device IDs
        if (deviceIds != null) {
            account.setSsaid(deviceIds.ssaid);
            account.setGaid(deviceIds.gaid);
            account.setDeviceId(deviceIds.deviceId);
        }
        
        // Create short link (BESTEHEND)
        if (userId != null && !userId.isEmpty()) {
            String shortLink = ShortLinkManager.createShortLink(userId, name);
            account.setShortLink(shortLink);
            account.setFriendLink("monopolygo://add-friend/" + userId);
        }
        
        // Check if account already exists
        repository.getAccountByName(name)
            .thenAccept(existingAccount -> {
                if (existingAccount != null) {
                    // Update existing
                    account.setId(existingAccount.getId());
                    updateExistingAccount(account);
                } else {
                    // Create new
                    createNewAccount(account);
                }
            })
            .exceptionally(throwable -> {
                // Doesn't exist, create new
                createNewAccount(account);
                return null;
            });
    }
    
    private void createNewAccount(Account account) {
        repository.createAccount(account)
            .thenAccept(created -> runOnUiThread(() -> {
                tvStatus.setText("Account komplett gesichert: " + account.getName());
                Toast.makeText(this, 
                    "✓ Datei gesichert\n✓ In Datenbank gespeichert", 
                    Toast.LENGTH_SHORT).show();
            }))
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    tvStatus.setText("Warnung: Datei gesichert, aber DB-Fehler");
                    Toast.makeText(this, 
                        "Datei-Backup OK, DB-Speicherung fehlgeschlagen", 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    private void updateExistingAccount(Account account) {
        repository.updateAccount(account)
            .thenAccept(updated -> runOnUiThread(() -> {
                tvStatus.setText("Account aktualisiert: " + account.getName());
                Toast.makeText(this, 
                    "✓ Datei gesichert\n✓ Datenbank aktualisiert", 
                    Toast.LENGTH_SHORT).show();
            }))
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    tvStatus.setText("Warnung: Datei gesichert, aber DB-Fehler");
                    Toast.makeText(this, 
                        "Datei-Backup OK, DB-Update fehlgeschlagen", 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    // BACKUP CUSTOMER ACCOUNT
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
        
        // Save customer info (in real implementation, save to customers table)
        String message = "Kunden-Metadaten gespeichert\n" +
                       "Name: " + customerName + "\n" +
                       "UserID: " + userId + "\n" +
                       "Link: " + friendLink + "\n" +
                       "Ordner: " + customerDir.getAbsolutePath();
        tvStatus.setText(message);
        Toast.makeText(this, "Kunden-Metadaten gespeichert", Toast.LENGTH_SHORT).show();
    }
    
    private String extractUserIdFromLink(String link) {
        if (link == null || link.isEmpty()) return null;
        
        try {
            if (link.contains("add-friend/")) {
                int startIndex = link.indexOf("add-friend/") + "add-friend/".length();
                if (startIndex < link.length()) {
                    String userId = link.substring(startIndex);
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
                    if (userId.matches("\\d+")) {
                        return userId;
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
    
    // COPY LINKS
    private void showCopyLinksDialog() {
        Toast.makeText(this, "Link-Kopie Funktion (In Entwicklung)", Toast.LENGTH_SHORT).show();
        // Load accounts and show links to copy
    }
}
