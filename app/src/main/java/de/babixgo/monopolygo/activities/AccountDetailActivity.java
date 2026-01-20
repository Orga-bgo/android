// AccountDetailActivity.java
package de.babixgo.monopolygo.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.switchmaterial.SwitchMaterial;
import de.babixgo.monopolygo.AccountManager;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.database.AccountRepository;
import de.babixgo.monopolygo.models.Account;

public class AccountDetailActivity extends AppCompatActivity {
    
    private Account account;
    private AccountRepository repository;
    
    // Views
    private TextView tvAccountName, tvUserId, tvLastPlayed;
    private TextView tvShortLink, tvFriendCode;
    private TextView tvSuspensionStatus, tvErrorStatus;
    private TextView tvSsaid, tvGaid, tvDeviceId;
    private Button btnRestore, btnEdit, btnDelete;
    private TextView tvBack;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_detail);
        
        repository = new AccountRepository();
        
        initViews();
        loadAccount();
    }
    
    private void initViews() {
        tvAccountName = findViewById(R.id.tv_account_name);
        tvUserId = findViewById(R.id.tv_user_id);
        tvLastPlayed = findViewById(R.id.tv_last_played);
        tvShortLink = findViewById(R.id.tv_short_link);
        tvFriendCode = findViewById(R.id.tv_friend_code);
        tvSuspensionStatus = findViewById(R.id.tv_suspension_status);
        tvErrorStatus = findViewById(R.id.tv_error_status);
        tvSsaid = findViewById(R.id.tv_ssaid);
        tvGaid = findViewById(R.id.tv_gaid);
        tvDeviceId = findViewById(R.id.tv_device_id);
        
        btnRestore = findViewById(R.id.btn_restore);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        tvBack = findViewById(R.id.tv_back);
        
        btnRestore.setOnClickListener(v -> showRestoreConfirmation());
        btnEdit.setOnClickListener(v -> toggleEditMode());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        tvBack.setOnClickListener(v -> finish());
    }
    
    private void loadAccount() {
        long accountId = getIntent().getLongExtra("account_id", -1);
        if (accountId == -1) {
            Toast.makeText(this, "Fehler: Keine Account-ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        repository.getAccountById(accountId)
            .thenAccept(loadedAccount -> runOnUiThread(() -> {
                account = loadedAccount;
                displayAccount();
            }))
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "Fehler beim Laden: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show();
                    finish();
                });
                return null;
            });
    }
    
    private void displayAccount() {
        tvAccountName.setText(account.getName());
        tvUserId.setText("MoGo User ID: " + (account.getUserId() != null ? account.getUserId() : "N/A"));
        tvLastPlayed.setText(account.getFormattedLastPlayed());
        
        tvShortLink.setText(account.getShortLink() != null ? account.getShortLink() : "Nicht verfügbar");
        tvFriendCode.setText(account.getFriendCode() != null ? account.getFriendCode() : "---");
        
        tvSuspensionStatus.setText(account.getSuspensionSummary());
        tvErrorStatus.setText(account.getErrorStatusText());
        tvErrorStatus.setTextColor(ContextCompat.getColor(this,
            account.isHasError() ? R.color.error_red : R.color.text_dark));
        
        tvSsaid.setText(account.getSsaid() != null ? account.getSsaid() : "Nicht verfügbar");
        tvGaid.setText(account.getGaid() != null ? account.getGaid() : "Nicht verfügbar");
        tvDeviceId.setText(account.getDeviceId() != null ? account.getDeviceId() : "Nicht verfügbar");
    }
    
    private void showRestoreConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Account wiederherstellen")
            .setMessage("Möchten Sie " + account.getName() + " wiederherstellen?\n\nMonopolyGo wird gestoppt und der Account wird aktiviert.")
            .setPositiveButton("Ja", (dialog, which) -> performRestore())
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    private void performRestore() {
        Toast.makeText(this, "Wiederherstelle " + account.getName() + "...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            boolean success = AccountManager.restoreAccountExtended(account.getName());
            
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Account wiederhergestellt", Toast.LENGTH_SHORT).show();
                    
                    // Update last_played in database with proper ISO format
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                    String currentTimestamp = sdf.format(new java.util.Date());
                    account.setLastPlayed(currentTimestamp);
                    repository.updateLastPlayed(account.getId());
                    
                    // Ask to start app
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
                    Toast.makeText(this, "Fehler beim Wiederherstellen", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
    
    private void toggleEditMode() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_account, null);
        
        // Initialize views
        TextInputEditText etName = dialogView.findViewById(R.id.et_name);
        TextInputEditText etUserId = dialogView.findViewById(R.id.et_user_id);
        TextInputEditText etFriendCode = dialogView.findViewById(R.id.et_friend_code);
        TextInputEditText etSuspension0 = dialogView.findViewById(R.id.et_suspension_0);
        TextInputEditText etSuspension3 = dialogView.findViewById(R.id.et_suspension_3);
        TextInputEditText etSuspension7 = dialogView.findViewById(R.id.et_suspension_7);
        SwitchMaterial switchPermanent = dialogView.findViewById(R.id.switch_suspension_permanent);
        SwitchMaterial switchError = dialogView.findViewById(R.id.switch_has_error);
        TextInputEditText etNote = dialogView.findViewById(R.id.et_note);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);
        
        // Populate with current values
        etName.setText(account.getName());
        etUserId.setText(account.getUserId());
        etFriendCode.setText(account.getFriendCode());
        etSuspension0.setText(String.valueOf(account.getSuspension0Days()));
        etSuspension3.setText(String.valueOf(account.getSuspension3Days()));
        etSuspension7.setText(String.valueOf(account.getSuspension7Days()));
        switchPermanent.setChecked(account.isSuspensionPermanent());
        switchError.setChecked(account.isHasError());
        etNote.setText(account.getNote());
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Account bearbeiten")
            .setView(dialogView)
            .setCancelable(true)
            .create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            // Update account object
            account.setName(etName.getText().toString().trim());
            account.setUserId(etUserId.getText().toString().trim());
            account.setFriendCode(etFriendCode.getText().toString().trim());
            
            try {
                account.setSuspension0Days(Integer.parseInt(etSuspension0.getText().toString()));
            } catch (Exception e) {
                account.setSuspension0Days(0);
            }
            
            try {
                account.setSuspension3Days(Integer.parseInt(etSuspension3.getText().toString()));
            } catch (Exception e) {
                account.setSuspension3Days(0);
            }
            
            try {
                account.setSuspension7Days(Integer.parseInt(etSuspension7.getText().toString()));
            } catch (Exception e) {
                account.setSuspension7Days(0);
            }
            
            account.setSuspensionPermanent(switchPermanent.isChecked());
            account.setHasError(switchError.isChecked());
            account.setNote(etNote.getText().toString().trim());
            
            // Save to database
            saveAccount();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void saveAccount() {
        repository.updateAccount(account)
            .thenRun(() -> runOnUiThread(() -> {
                Toast.makeText(this, "Änderungen gespeichert", Toast.LENGTH_SHORT).show();
                displayAccount(); // Refresh UI
            }))
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "Fehler beim Speichern: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Account löschen")
            .setMessage("Möchten Sie " + account.getName() + " wirklich löschen?\n\nDieser Vorgang kann nicht rückgängig gemacht werden.")
            .setPositiveButton("Löschen", (dialog, which) -> performDelete())
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    private void performDelete() {
        repository.deleteAccount(account.getId())
            .thenRun(() -> runOnUiThread(() -> {
                Toast.makeText(this, "Account gelöscht", Toast.LENGTH_SHORT).show();
                finish();
            }))
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "Fehler beim Löschen: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
}
