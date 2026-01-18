package de.babixgo.monopolygo;

import android.os.Bundle;
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
        setContentView(R.layout.activity_account_management_new);
        
        // Initialize views
        btnRestore = findViewById(R.id.btn_restore_account);
        btnBackup = findViewById(R.id.btn_backup_account);
        btnDelete = findViewById(R.id.btn_delete_account);
        btnEdit = findViewById(R.id.btn_edit_account);
        tvAccountCount = findViewById(R.id.tv_account_count);
        tvSecurityStatus = findViewById(R.id.tv_security_status);
        
        // Update account count
        updateAccountCount();
        
        // Setup button listeners
        setupButtons();
    }
    
    private void setupButtons() {
        btnRestore.setOnClickListener(v -> showRestoreDialog());
        btnBackup.setOnClickListener(v -> showBackupDialog());
        btnDelete.setOnClickListener(v -> showDeleteDialog());
        btnEdit.setOnClickListener(v -> showEditDialog());
    }
    
    private void updateAccountCount() {
        String[] accounts = AccountManager.getBackedUpAccounts(true);
        int count = accounts.length;
        tvAccountCount.setText(count + " Accounts\nvorhanden");
        
        // Update security status
        boolean rootGranted = RootManager.hasRootAccess();
        tvSecurityStatus.setText("Sicherheitsstatus:\n" + 
            (rootGranted ? "Aktiv" : "Inaktiv"));
    }
    
    private void showRestoreDialog() {
        String[] accounts = AccountManager.getBackedUpAccounts(true);
        
        if (accounts.length == 0) {
            Toast.makeText(this, "Keine Accounts gefunden", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Account auswählen");
        builder.setItems(accounts, (dialog, which) -> {
            String accountName = accounts[which];
            restoreAccount(accountName);
        });
        builder.setNegativeButton("Abbrechen", null);
        builder.show();
    }
    
    private void restoreAccount(String accountName) {
        new Thread(() -> {
            String basePath = AccountManager.getAccountsEigenePath();
            String sourceFile = basePath + accountName + 
                "/WithBuddies.Services.User.0Production.dat";
            
            boolean success = AccountManager.restoreAccount(sourceFile);
            
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "✅ Account wiederhergestellt", 
                        Toast.LENGTH_SHORT).show();
                    
                    new AlertDialog.Builder(this)
                        .setTitle("App starten?")
                        .setMessage("MonopolyGo jetzt starten?")
                        .setPositiveButton("Ja", (d, w) -> AccountManager.startApp())
                        .setNegativeButton("Nein", null)
                        .show();
                } else {
                    Toast.makeText(this, "❌ Fehler beim Wiederherstellen", 
                        Toast.LENGTH_SHORT).show();
                }
                updateAccountCount();
            });
        }).start();
    }
    
    private void showBackupDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_backup_simple, null);
        EditText etInternalId = dialogView.findViewById(R.id.et_internal_id);
        EditText etNote = dialogView.findViewById(R.id.et_note);
        
        new AlertDialog.Builder(this)
            .setTitle("Account sichern")
            .setView(dialogView)
            .setPositiveButton("Sichern", (dialog, which) -> {
                String id = etInternalId.getText().toString().trim();
                String note = etNote.getText().toString().trim();
                
                if (id.isEmpty()) {
                    Toast.makeText(this, "ID erforderlich", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                backupAccount(id, note);
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    private void backupAccount(String internalId, String note) {
        new Thread(() -> {
            boolean success = AccountManager.backupAccount(
                AccountManager.getAccountsEigenePath(), 
                internalId
            );
            
            if (success) {
                saveMetadata(internalId, note);
            }
            
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "✅ Account gesichert", 
                        Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "❌ Fehler beim Sichern", 
                        Toast.LENGTH_SHORT).show();
                }
                updateAccountCount();
            });
        }).start();
    }
    
    private void saveMetadata(String id, String note) {
        try {
            String csvPath = AccountManager.getAccountsEigenePath() + "Accountinfos.csv";
            File csvFile = new File(csvPath);
            boolean writeHeader = !csvFile.exists();
            
            FileWriter fw = new FileWriter(csvFile, true);
            if (writeHeader) {
                fw.write("InterneID,Datum,Notiz\n");
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = sdf.format(new Date());
            
            fw.write(String.format("%s,%s,\"%s\"\n", id, date, note));
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showDeleteDialog() {
        Toast.makeText(this, "🚧 Funktion in Entwicklung", Toast.LENGTH_SHORT).show();
    }
    
    private void showEditDialog() {
        Toast.makeText(this, "🚧 Funktion in Entwicklung", Toast.LENGTH_SHORT).show();
    }
}
