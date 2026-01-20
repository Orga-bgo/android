// AccountListActivity.java
package de.babixgo.monopolygo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.adapters.AccountListAdapter;
import de.babixgo.monopolygo.database.AccountRepository;
import de.babixgo.monopolygo.models.Account;

public class AccountListActivity extends AppCompatActivity {
    
    private RecyclerView rvAccounts;
    private AccountListAdapter adapter;
    private AccountRepository repository;
    private Button btnBackup, btnSettings;
    private FloatingActionButton fabNewAccount;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);
        
        repository = new AccountRepository();
        
        initViews();
        setupRecyclerView();
        loadAccounts();
    }
    
    private void initViews() {
        rvAccounts = findViewById(R.id.rv_accounts);
        btnBackup = findViewById(R.id.btn_backup);
        btnSettings = findViewById(R.id.btn_settings);
        fabNewAccount = findViewById(R.id.fab_new_account);
        
        btnBackup.setOnClickListener(v -> {
            Toast.makeText(this, "Backup (In Entwicklung)", Toast.LENGTH_SHORT).show();
        });
        
        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Einstellungen (In Entwicklung)", Toast.LENGTH_SHORT).show();
        });
        
        fabNewAccount.setOnClickListener(v -> {
            // Navigate to AccountManagementActivity for backup
            Intent intent = new Intent(this, de.babixgo.monopolygo.AccountManagementActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupRecyclerView() {
        adapter = new AccountListAdapter(account -> showAccountOptionsDialog(account));
        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        rvAccounts.setAdapter(adapter);
    }
    
    private void loadAccounts() {
        repository.getAllAccounts()
            .thenAccept(accounts -> runOnUiThread(() -> {
                adapter.setAccounts(accounts);
            }))
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "Fehler beim Laden: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    private void showAccountOptionsDialog(Account account) {
        String[] options = {
            "Wiederherstellen",
            "Profil anzeigen",
            "Abbrechen"
        };
        
        new AlertDialog.Builder(this)
            .setTitle(account.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Wiederherstellen
                        restoreAccount(account);
                        break;
                    case 1: // Profil anzeigen
                        openAccountDetail(account);
                        break;
                    case 2: // Abbrechen
                        dialog.dismiss();
                        break;
                }
            })
            .show();
    }
    
    private void restoreAccount(Account account) {
        // TODO: Implement restore logic
        Toast.makeText(this, "Wiederherstellung von " + account.getName(), Toast.LENGTH_SHORT).show();
        
        // Navigate to AccountManagementActivity and trigger restore
        Intent intent = new Intent(this, de.babixgo.monopolygo.AccountManagementActivity.class);
        intent.putExtra("action", "restore");
        intent.putExtra("account_name", account.getName());
        startActivity(intent);
    }
    
    private void openAccountDetail(Account account) {
        Intent intent = new Intent(this, AccountDetailActivity.class);
        intent.putExtra("account_id", account.getId());
        startActivity(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadAccounts(); // Refresh on return
    }
}
