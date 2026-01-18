package de.babixgo.monopolygo;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for account management operations with modern UI.
 */
public class AccountManagementActivity extends AppCompatActivity {
    
    private MaterialButton btnRestore, btnBackup, btnDelete, btnEdit, btnSettings;
    private TextView tvAccountCount, tvSecurityStatus;
    private SettingsManager settingsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // WICHTIG: Verwende das NEUE Layout
        setContentView(R.layout.activity_account_management_new);
        
        // Views initialisieren
        try {
            btnRestore = findViewById(R.id.btn_restore_account);
            btnBackup = findViewById(R.id.btn_backup_account);
            btnDelete = findViewById(R.id.btn_delete_account);
            btnEdit = findViewById(R.id.btn_edit_account);
            btnSettings = findViewById(R.id.btn_settings);
            tvAccountCount = findViewById(R.id.tv_account_count);
            tvSecurityStatus = findViewById(R.id.tv_security_status);
            
            // Null-Checks
            if (btnRestore == null || btnBackup == null || btnDelete == null || 
                btnEdit == null || btnSettings == null || tvAccountCount == null || tvSecurityStatus == null) {
                Toast.makeText(this, "Layout-Fehler: Views nicht gefunden", 
                    Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            // Initialize SettingsManager
            settingsManager = new SettingsManager(this);
            
            // WICHTIG: Root-Zugriff sicherstellen BEVOR UI initialisiert wird
            // Auf Android 10+ kann der Root-Dialog länger dauern
            ensureRootAccess();
            
            updateAccountCount();
            setupButtons();
            
        } catch (Exception e) {
            Toast.makeText(this, "Fehler beim Laden: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }
    
    /**
     * Ensure root access is granted before any operations.
     * This fixes timing issues on Android 10+ where root dialog appears slower.
     */
    private void ensureRootAccess() {
        // If root already granted, we're done
        if (RootManager.hasRootAccess()) {
            updateSecurityStatus(true);
            return;
        }
        
        // Request root access in background
        requestRootInBackground(false);
    }
    
    /**
     * Request root access in background thread with proper lifecycle management.
     * @param showSuccessMessage whether to show success toast on grant
     */
    private void requestRootInBackground(boolean showSuccessMessage) {
        new Thread(() -> {
            boolean hasRoot = RootManager.requestRoot();
            // Check if activity is still alive before updating UI
            if (!isFinishing() && !isDestroyed()) {
                runOnUiThread(() -> {
                    updateSecurityStatus(hasRoot);
                    if (!hasRoot) {
                        Toast.makeText(this, 
                            "⚠️ Root-Zugriff erforderlich für Account-Operationen", 
                            Toast.LENGTH_LONG).show();
                    } else if (showSuccessMessage) {
                        Toast.makeText(this, "✅ Root-Zugriff gewährt", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }
    
    /**
     * Update security status display
     */
    private void updateSecurityStatus(boolean rootGranted) {
        if (tvSecurityStatus != null) {
            tvSecurityStatus.setText("Sicherheitsstatus:\n" + 
                (rootGranted ? "Aktiv" : "Inaktiv"));
        }
    }
    
    private void setupButtons() {
        btnRestore.setOnClickListener(v -> showRestoreDialog());
        btnBackup.setOnClickListener(v -> showBackupDialog());
        btnDelete.setOnClickListener(v -> showDeleteDialog());
        btnEdit.setOnClickListener(v -> showEditDialog());
        btnSettings.setOnClickListener(v -> showSettingsDialog());
    }
    
    private void updateAccountCount() {
        try {
            String[] accounts = AccountManager.getBackedUpAccounts(true);
            int count = accounts.length;
            tvAccountCount.setText(count + " Accounts\nvorhanden");
            
            boolean rootGranted = RootManager.hasRootAccess();
            updateSecurityStatus(rootGranted);
        } catch (Exception e) {
            tvAccountCount.setText("Fehler beim Laden");
            tvSecurityStatus.setText("Status: Unbekannt");
            e.printStackTrace();
        }
    }
    
    private void showRestoreDialog() {
        // Check root access before showing dialog
        if (!checkRootAccessWithPrompt()) {
            return;
        }
        
        try {
            String[] accounts = AccountManager.getBackedUpAccounts(true);
            
            if (accounts.length == 0) {
                Toast.makeText(this, "Keine Accounts gefunden", Toast.LENGTH_SHORT).show();
                return;
            }
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Account wiederherstellen");
            builder.setItems(accounts, (dialog, which) -> {
                String accountName = accounts[which];
                restoreAccount(accountName);
            });
            builder.setNegativeButton("Abbrechen", null);
            builder.show();
        } catch (Exception e) {
            Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    private void restoreAccount(String accountName) {
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setTitle("Wiederherstellen...")
            .setMessage("Account wird wiederhergestellt\nBitte warten...")
            .setCancelable(false)
            .create();
        progressDialog.show();
        
        new Thread(() -> {
            boolean success = AccountManager.restoreAccountExtended(accountName);
            
            runOnUiThread(() -> {
                progressDialog.dismiss();
                
                if (success) {
                    Toast.makeText(this, "✅ Account wiederhergestellt", 
                        Toast.LENGTH_LONG).show();
                    
                    new AlertDialog.Builder(this)
                        .setTitle("App starten?")
                        .setMessage("MonopolyGo jetzt starten?")
                        .setPositiveButton("Ja", (d, w) -> AccountManager.startApp())
                        .setNegativeButton("Nein", null)
                        .show();
                } else {
                    new AlertDialog.Builder(this)
                        .setTitle("Fehler")
                        .setMessage("❌ Account konnte nicht wiederhergestellt werden")
                        .setPositiveButton("OK", null)
                        .show();
                }
                updateAccountCount();
            });
        }).start();
    }
    
    private void showBackupDialog() {
        // Check root access before showing dialog
        if (!checkRootAccessWithPrompt()) {
            return;
        }
        
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_backup_extended, null);
            EditText etInternalId = dialogView.findViewById(R.id.et_internal_id);
            EditText etNote = dialogView.findViewById(R.id.et_note);
            CheckBox cbFbToken = dialogView.findViewById(R.id.cb_include_fb_token);
            
            new AlertDialog.Builder(this)
                .setTitle("Account sichern")
                .setView(dialogView)
                .setPositiveButton("Sichern", (dialog, which) -> {
                    String id = etInternalId.getText().toString().trim();
                    String note = etNote.getText().toString().trim();
                    boolean includeFb = cbFbToken.isChecked();
                    
                    if (id.isEmpty()) {
                        Toast.makeText(this, "Interne ID erforderlich", 
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    backupAccount(id, note, includeFb);
                })
                .setNegativeButton("Abbrechen", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "Fehler beim Öffnen: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    private void backupAccount(String internalId, String note, boolean includeFbToken) {
        // Apply name prefix
        String prefix = settingsManager.getNamePrefix();
        String accountName = prefix + internalId;
        
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setTitle("Sichern...")
            .setMessage("Bitte warten...")
            .setCancelable(false)
            .create();
        progressDialog.show();
        
        new Thread(() -> {
            // NUTZE SIMPLE VERSION with name prefix applied
            boolean success = AccountManager.backupAccountSimple(accountName, includeFbToken);
            
            if (success) {
                saveMetadata(accountName, note, includeFbToken);
            }
            
            runOnUiThread(() -> {
                progressDialog.dismiss();
                
                if (success) {
                    String message = "✅ Account gesichert!\n\n" +
                                   "📝 Name: " + accountName + "\n" +
                                   "📅 " + getCurrentDate() + "\n" +
                                   "🔐 FB-Token: " + (includeFbToken ? "✓" : "✗");
                    
                    new AlertDialog.Builder(this)
                        .setTitle("Backup erfolgreich")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show();
                } else {
                    new AlertDialog.Builder(this)
                        .setTitle("Fehler")
                        .setMessage("❌ Backup fehlgeschlagen\n\nSiehe Logcat für Details")
                        .setPositiveButton("OK", null)
                        .show();
                }
                updateAccountCount();
            });
        }).start();
    }
    
    private void saveMetadata(String id, String note, boolean fbIncluded) {
        try {
            String csvPath = AccountManager.getAccountsEigenePath() + "Accountinfos.csv";
            File csvFile = new File(csvPath);
            boolean writeHeader = !csvFile.exists();
            
            try (FileWriter fw = new FileWriter(csvFile, true)) {
                if (writeHeader) {
                    fw.write("Name,Datum,FBToken,Notiz\n");
                }
                
                String date = getCurrentDate();
                String fb = fbIncluded ? "JA" : "NEIN";
                
                // Sanitize CSV fields to prevent injection
                String sanitizedId = sanitizeCSV(id);
                String sanitizedDate = sanitizeCSV(date);
                String sanitizedFb = sanitizeCSV(fb);
                String sanitizedNote = sanitizeCSV(note);
                
                fw.write(String.format("%s,%s,%s,\"%s\"\n", 
                    sanitizedId, sanitizedDate, sanitizedFb, sanitizedNote));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sanitize CSV field to prevent CSV injection attacks
     */
    private String sanitizeCSV(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        
        // Remove dangerous characters that could trigger formula execution
        // or command injection in spreadsheet applications
        if (value.startsWith("=") || value.startsWith("+") || 
            value.startsWith("-") || value.startsWith("@") ||
            value.startsWith("\t") || value.startsWith("\r") ||
            value.startsWith("|") || value.startsWith(";")) {
            value = "'" + value; // Prefix with single quote to prevent formula execution
        }
        
        // Escape quotes
        value = value.replace("\"", "\"\"");
        
        return value;
    }
    
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    private void showDeleteDialog() {
        Toast.makeText(this, "🚧 Funktion in Entwicklung", Toast.LENGTH_SHORT).show();
    }
    
    private void showEditDialog() {
        Toast.makeText(this, "🚧 Funktion in Entwicklung", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show settings dialog for configuring paths and name prefix.
     */
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Einstellungen");
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        EditText etBackupPath = dialogView.findViewById(R.id.et_backup_output_path);
        EditText etRestorePath = dialogView.findViewById(R.id.et_restore_input_path);
        EditText etPrefix = dialogView.findViewById(R.id.et_name_prefix);
        
        // Aktuelle Werte laden
        etBackupPath.setText(settingsManager.getBackupOutputPath());
        etRestorePath.setText(settingsManager.getRestoreInputPath());
        etPrefix.setText(settingsManager.getNamePrefix());
        
        builder.setView(dialogView);
        builder.setPositiveButton("Speichern", (dialog, which) -> {
            String backupPath = etBackupPath.getText().toString().trim();
            String restorePath = etRestorePath.getText().toString().trim();
            String prefix = etPrefix.getText().toString().trim();
            
            // Pfade validieren (sollten mit / enden)
            if (!backupPath.isEmpty() && !backupPath.endsWith("/")) {
                backupPath += "/";
            }
            if (!restorePath.isEmpty() && !restorePath.endsWith("/")) {
                restorePath += "/";
            }
            
            // Einstellungen speichern
            settingsManager.setBackupOutputPath(backupPath);
            settingsManager.setRestoreInputPath(restorePath);
            settingsManager.setNamePrefix(prefix);
            
            Toast.makeText(this, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Abbrechen", null);
        builder.show();
    }
    
    /**
     * Check if root access is granted and prompt user if not.
     * This prevents operations from failing silently on newer Android versions.
     * @return true if root is granted, false otherwise
     */
    private boolean checkRootAccessWithPrompt() {
        if (!RootManager.hasRootAccess()) {
            new AlertDialog.Builder(this)
                .setTitle("Root-Zugriff erforderlich")
                .setMessage("Bitte gewähren Sie Root-Zugriff für diese Operation.\n\n" +
                           "Die App wird Root-Zugriff anfordern.")
                .setPositiveButton("Weiter", (dialog, which) -> {
                    // Request root using common method
                    requestRootInBackground(true);
                })
                .setNegativeButton("Abbrechen", null)
                .show();
            return false;
        }
        return true;
    }
}
