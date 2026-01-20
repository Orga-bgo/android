package de.babixgo.monopolygo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Main activity that serves as the entry point and module selector.
 */
public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int MANAGE_STORAGE_REQUEST_CODE = 1002;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize directories
        AccountManager.initializeDirectories();
        
        // Check and request permissions
        checkAndRequestPermissions();
        
        // Check root access
        checkRootAccess();
        
        // Setup buttons
        setupButtons();
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
    
    // ZIP tool check removed - we use Java-based ZIP now
    // No external 'zip' command needed anymore
    
    private void setupButtons() {
        Button btnAccountManagement = findViewById(R.id.btn_account_management);
        Button btnAccountList = findViewById(R.id.btn_account_list);
        Button btnPartnerEvent = findViewById(R.id.btn_partner_event);
        Button btnFriendship = findViewById(R.id.btn_friendship);
        
        btnAccountManagement.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AccountManagementActivity.class);
            startActivity(intent);
        });
        
        btnAccountList.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, de.babixgo.monopolygo.activities.AccountListActivity.class);
            startActivity(intent);
        });
        
        btnPartnerEvent.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PartnerEventActivity.class);
            startActivity(intent);
        });
        
        btnFriendship.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FriendshipActivity.class);
            startActivity(intent);
        });
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
