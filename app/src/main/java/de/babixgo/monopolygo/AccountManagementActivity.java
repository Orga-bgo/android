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
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for account management operations with modern UI.
 */
public class AccountManagementActivity extends AppCompatActivity {
    
    private MaterialButton btnRestore, btnBackup, btnDelete, btnEdit;
    private TextView tvAccountCount, tvSecurityStatus;
    
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
            tvAccountCount = findViewById(R.id.tv_account_count);
            tvSecurityStatus = findViewById(R.id.tv_security_status);
            
            // Null-Checks
            if (btnRestore == null || btnBackup == null || btnDelete == null || 
                btnEdit == null || tvAccountCount == null || tvSecurityStatus == null) {
                Toast.makeText(this, "Layout-Fehler: Views nicht gefunden", 
                    Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            updateAccountCount();
            setupButtons();
            
        } catch (Exception e) {
            Toast.makeText(this, "Fehler beim Laden: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }
    
    private void setupButtons() {
        btnRestore.setOnClickListener(v -> showRestoreDialog());
        btnBackup.setOnClickListener(v -> showBackupDialog());
        btnDelete.setOnClickListener(v -> showDeleteDialog());
        btnEdit.setOnClickListener(v -> showEditDialog());
    }
    
    private void updateAccountCount() {
        try {
            String[] accounts = AccountManager.getBackedUpAccounts(true);
            int count = accounts.length;
            tvAccountCount.setText(count + " Accounts\nvorhanden");
            
            boolean rootGranted = RootManager.hasRootAccess();
            tvSecurityStatus.setText("Sicherheitsstatus:\n" + 
                (rootGranted ? "Aktiv" : "Inaktiv"));
        } catch (Exception e) {
            tvAccountCount.setText("Fehler beim Laden");
            tvSecurityStatus.setText("Status: Unbekannt");
            e.printStackTrace();
        }
    }
    
    private void showRestoreDialog() {
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
        AlertDialog progressDialog = new AlertDialog.Builder(this)
            .setTitle("Sichern...")
            .setMessage("Account wird gesichert\nBitte warten...")
            .setCancelable(false)
            .create();
        progressDialog.show();
        
        new Thread(() -> {
            boolean success = AccountManager.backupAccountExtended(
                internalId, 
                includeFbToken
            );
            
            if (success) {
                saveMetadata(internalId, note, includeFbToken);
            }
            
            runOnUiThread(() -> {
                progressDialog.dismiss();
                
                if (success) {
                    String message = "✅ Account erfolgreich gesichert!\n\n" +
                                   "📝 ID: " + internalId + "\n" +
                                   "📅 Datum: " + getCurrentDate() + "\n" +
                                   "🔐 FB-Token: " + (includeFbToken ? "✓ gesichert" : "✗ nicht gesichert");
                    
                    new AlertDialog.Builder(this)
                        .setTitle("Backup erfolgreich")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show();
                } else {
                    new AlertDialog.Builder(this)
                        .setTitle("Fehler")
                        .setMessage("❌ Backup fehlgeschlagen")
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
            
            FileWriter fw = new FileWriter(csvFile, true);
            if (writeHeader) {
                fw.write("InterneID,Datum,FBToken,Notiz\n");
            }
            
            String date = getCurrentDate();
            String fb = fbIncluded ? "JA" : "NEIN";
            
            fw.write(String.format("%s,%s,%s,\"%s\"\n", id, date, fb, note));
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
