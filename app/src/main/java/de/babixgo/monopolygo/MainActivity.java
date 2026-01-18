package de.babixgo.monopolygo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize directories
        AccountManager.initializeDirectories();
        
        // Check and request permissions
        checkPermissions();
        
        // Check root access
        checkRootAccess();
        
        // Check ZIP tool availability
        checkZipTool();
        
        // Setup buttons
        setupButtons();
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                    Toast.makeText(this, "Root-Zugriff gewährt", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    private void showRootWarningDialog(boolean rootedButDenied) {
        String message;
        if (rootedButDenied) {
            message = "Root-Zugriff wurde verweigert.\n\n" +
                     "Bitte gewähren Sie Root-Rechte für diese App.\n\n" +
                     "Ohne Root sind nur Basis-Funktionen verfügbar.";
        } else {
            message = "⚠️ WARNUNG: Diese App benötigt Root-Zugriff\n\n" +
                     "Die folgenden Funktionen erfordern Root-Rechte:\n" +
                     "✓ Account-Wiederherstellung\n" +
                     "✓ Account-Sicherung\n" +
                     "✓ Zugriff auf App-Daten\n" +
                     "✓ Automatische Freundschaftsanfragen\n\n" +
                     "Ohne Root sind nur Basis-Funktionen verfügbar.";
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Root-Zugriff erforderlich")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setCancelable(true)
            .show();
    }
    
    private void checkZipTool() {
        new Thread(() -> {
            String result = RootManager.runRootCommand("which zip");
            boolean hasZip = !result.contains("not found") && !result.isEmpty();
            
            if (!hasZip) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(this)
                        .setTitle("ZIP-Tool fehlt")
                        .setMessage("⚠️ Das 'zip' Tool wurde nicht gefunden.\n\n" +
                                  "Backup/Restore funktioniert möglicherweise nicht.\n\n" +
                                  "Lösung: Installieren Sie 'zip' über Ihren Root-Manager oder Termux.")
                        .setPositiveButton("OK", null)
                        .show();
                });
            }
        }).start();
    }
    
    private void setupButtons() {
        Button btnAccountManagement = findViewById(R.id.btn_account_management);
        Button btnPartnerEvent = findViewById(R.id.btn_partner_event);
        Button btnFriendship = findViewById(R.id.btn_friendship);
        
        btnAccountManagement.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AccountManagementActivity.class);
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
                Toast.makeText(this, "Berechtigungen sind erforderlich", Toast.LENGTH_LONG).show();
            }
        }
    }
}
