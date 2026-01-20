package de.babixgo.monopolygo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import de.babixgo.monopolygo.adapters.AccountListAdapter;
import de.babixgo.monopolygo.database.AccountRepository;
import de.babixgo.monopolygo.models.Account;
import de.babixgo.monopolygo.activities.AccountDetailActivity;

/**
 * Main activity that serves as the entry point with account list and navigation drawer.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int MANAGE_STORAGE_REQUEST_CODE = 1002;
    
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView rvAccounts;
    private AccountListAdapter adapter;
    private AccountRepository repository;
    private FloatingActionButton fabNewAccount;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize directories
        AccountManager.initializeDirectories();
        
        // Initialize repository
        repository = new AccountRepository();
        
        // Check and request permissions
        checkAndRequestPermissions();
        
        // Check root access
        checkRootAccess();
        
        // Setup navigation drawer
        setupNavigationDrawer();
        
        // Setup account list
        setupAccountList();
        
        // Setup FAB
        setupFAB();
        
        // Load accounts
        loadAccounts();
    }
    
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+): MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                showManageStoragePermissionDialog();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10: Normale Storage-Berechtigungen
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                
                ActivityCompat.requestPermissions(this,
                    new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
            }
        }
    }
    
    private void showManageStoragePermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Speicherberechtigung erforderlich")
            .setMessage("Diese App benötigt Zugriff auf den vollständigen Speicher für Backups.\n\n" +
                      "Bitte aktivieren Sie 'Zugriff auf alle Dateien' in den Einstellungen.")
            .setPositiveButton("Einstellungen öffnen", (dialog, which) -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, MANAGE_STORAGE_REQUEST_CODE);
                }
            })
            .setNegativeButton("Abbrechen", (dialog, which) -> {
                Toast.makeText(this, 
                    "⚠️ Ohne Speicherberechtigung funktioniert Backup/Restore nicht!", 
                    Toast.LENGTH_LONG).show();
            })
            .setCancelable(false)
            .show();
    }
    
    private void checkRootAccess() {
        if (!RootManager.isRooted()) {
            showRootWarningDialog(false);
            return;
        }
        
        // Request root access
        new Thread(() -> {
            boolean hasRoot = RootManager.requestRoot();
            runOnUiThread(() -> {
                if (!hasRoot) {
                    showRootWarningDialog(true);
                } else {
                    Toast.makeText(this, "✅ Root-Zugriff gewährt", 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == MANAGE_STORAGE_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "✅ Speicherberechtigung erteilt", 
                        Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, 
                        "⚠️ Speicherberechtigung fehlt - Backup/Restore nicht möglich", 
                        Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    
    private void showRootWarningDialog(boolean rootedButDenied) {
        String message;
        if (rootedButDenied) {
            message = "Root-Zugriff wurde verweigert.\n\n" +
                     "Bitte gewähren Sie Root-Rechte.\n\n" +
                     "Ohne Root sind nur Basis-Funktionen verfügbar.";
        } else {
            message = "⚠️ Diese App benötigt Root-Zugriff\n\n" +
                     "Funktionen:\n" +
                     "✓ Account-Wiederherstellung\n" +
                     "✓ Account-Sicherung\n" +
                     "✓ Zugriff auf App-Daten\n\n" +
                     "Ohne Root nur Basis-Funktionen.";
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Root-Zugriff erforderlich")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setCancelable(true)
            .show();
    }
    
    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ImageButton btnMenu = findViewById(R.id.btn_menu);
        
        navigationView.setNavigationItemSelectedListener(this);
        
        // Set the first item as checked
        navigationView.setCheckedItem(R.id.nav_account_list);
        
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }
    
    private void setupAccountList() {
        rvAccounts = findViewById(R.id.rv_accounts);
        adapter = new AccountListAdapter(account -> showAccountOptionsDialog(account));
        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        rvAccounts.setAdapter(adapter);
    }
    
    private void setupFAB() {
        fabNewAccount = findViewById(R.id.fab_new_account);
        fabNewAccount.setOnClickListener(v -> showBackupDialog());
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
            "Mehr anzeigen",
            "Abbrechen"
        };
        
        new AlertDialog.Builder(this)
            .setTitle(account.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Wiederherstellen
                        restoreAccount(account);
                        break;
                    case 1: // Mehr anzeigen
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
        // Navigate to AccountManagementActivity and trigger restore
        Intent intent = new Intent(this, AccountManagementActivity.class);
        intent.putExtra("action", "restore");
        intent.putExtra("account_name", account.getName());
        startActivity(intent);
    }
    
    private void openAccountDetail(Account account) {
        Intent intent = new Intent(this, AccountDetailActivity.class);
        intent.putExtra("account_id", account.getId());
        startActivity(intent);
    }
    
    private void showBackupDialog() {
        // Navigate to AccountManagementActivity for backup
        Intent intent = new Intent(this, AccountManagementActivity.class);
        startActivity(intent);
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_account_list) {
            // Already on account list - just close drawer
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_tycoon_racers) {
            Toast.makeText(this, "Tycoon Racers (In Entwicklung)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_partner_event) {
            Intent intent = new Intent(this, PartnerEventActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_friendship) {
            Intent intent = new Intent(this, FriendshipActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_customers) {
            Toast.makeText(this, "Kunden (In Entwicklung)", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Einstellungen (In Entwicklung)", Toast.LENGTH_SHORT).show();
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadAccounts(); // Refresh on return
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, 
                    "⚠️ Berechtigungen erforderlich für volle Funktionalität", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}
