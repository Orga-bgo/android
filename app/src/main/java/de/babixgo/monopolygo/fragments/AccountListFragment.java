package de.babixgo.monopolygo.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import de.babixgo.monopolygo.AccountManager;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.RootManager;
import de.babixgo.monopolygo.activities.AccountDetailActivity;
import de.babixgo.monopolygo.adapters.AccountListAdapter;
import de.babixgo.monopolygo.database.AccountRepository;
import de.babixgo.monopolygo.models.Account;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountListFragment extends Fragment {
    private static final String TAG = "AccountListFragment";
    
    private RecyclerView rvAccounts;
    private AccountListAdapter adapter;
    private AccountRepository repository;
    private FloatingActionButton fabBackup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_list, container, false);

        // Initialize Repository
        repository = new AccountRepository();

        // Setup RecyclerView
        rvAccounts = view.findViewById(R.id.rv_accounts);
        rvAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        adapter = new AccountListAdapter(this::showAccountOptions);
        rvAccounts.setAdapter(adapter);

        // Setup FAB
        fabBackup = view.findViewById(R.id.fab_backup);
        fabBackup.setOnClickListener(v -> showBackupDialog());

        // Load Accounts
        loadAccounts();

        return view;
    }

    // ==================== LOAD ACCOUNTS ====================
    
    private void loadAccounts() {
        Log.d(TAG, "Loading accounts from Supabase");
        
        repository.getAllAccounts()
            .thenAccept(accounts -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.d(TAG, "Loaded " + accounts.size() + " accounts");
                        adapter.setAccounts(accounts);
                    });
                }
            })
            .exceptionally(throwable -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Failed to load accounts", throwable);
                        Toast.makeText(requireContext(), 
                            "Fehler beim Laden: " + throwable.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    });
                }
                return null;
            });
    }

    // ==================== BACKUP DIALOG ====================
    
    private void showBackupDialog() {
        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_backup_own, null);
        
        EditText etInternalId = dialogView.findViewById(R.id.et_internal_id);
        EditText etNote = dialogView.findViewById(R.id.et_note);

        new AlertDialog.Builder(requireContext())
            .setTitle("Account sichern")
            .setView(dialogView)
            .setPositiveButton("Sichern", (dialog, which) -> {
                String accountName = etInternalId.getText().toString().trim();
                String note = etNote.getText().toString().trim();
                
                if (accountName.isEmpty()) {
                    Toast.makeText(requireContext(), 
                        "Bitte Interne ID eingeben", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                backupAccount(accountName, note);
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }

    // ==================== BACKUP ACCOUNT ====================
    
    private void backupAccount(String accountName, String note) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting backup for: " + accountName);
                
                // 1. MonopolyGo beenden
                AccountManager.forceStopApp();
                Thread.sleep(1000);
                
                // 2. Backup-Verzeichnis erstellen
                String backupDir = requireContext().getFilesDir().getAbsolutePath() + 
                                   "/backup/" + accountName + "/";
                RootManager.runRootCommand("mkdir -p '" + backupDir + "'");
                
                // 3. Dateien kopieren
                String monopolyGoData = "/data/data/com.scopely.monopolygo/";
                
                // DiskBasedCacheDirectory
                boolean diskCopySuccess = RootManager.copyDirectory(
                    monopolyGoData + "files/DiskBasedCacheDirectory/",
                    backupDir + "DiskBasedCacheDirectory/"
                );
                Log.d(TAG, "DiskBasedCacheDirectory copy: " + diskCopySuccess);
                
                // shared_prefs
                boolean prefsCopySuccess = RootManager.copyDirectory(
                    monopolyGoData + "shared_prefs/",
                    backupDir + "shared_prefs/"
                );
                Log.d(TAG, "shared_prefs copy: " + prefsCopySuccess);
                
                // settings_ssaid.xml
                boolean ssaidCopySuccess = RootManager.copyFile(
                    "/data/system/users/0/settings_ssaid.xml",
                    backupDir + "settings_ssaid.xml"
                );
                Log.d(TAG, "settings_ssaid.xml copy: " + ssaidCopySuccess);
                
                // 4. IDs extrahieren
                String prefsFile = backupDir + "shared_prefs/com.scopely.monopolygo.v2.playerprefs.xml";
                String xmlContent = RootManager.readFile(prefsFile);
                
                String userId = extractXmlValue(xmlContent, "Scopely.Attribution.UserId");
                String gaid = extractXmlValue(xmlContent, "GoogleAdId");
                String deviceToken = extractXmlValue(xmlContent, "LastOpenedDeviceToken");
                String appSetId = extractXmlValue(xmlContent, "AppSetId");
                
                Log.d(TAG, "Extracted - UserID: " + userId + ", GAID: " + gaid + 
                           ", DeviceToken: " + deviceToken + ", AppSetID: " + appSetId);
                
                // 5. SSAID extrahieren
                String ssaidContent = RootManager.readFile(backupDir + "settings_ssaid.xml");
                String ssaid = extractSSAID(ssaidContent);
                
                Log.d(TAG, "Extracted SSAID: " + ssaid);
                
                // 6. Account-Objekt erstellen (OHNE Friend Link)
                Account account = new Account();
                account.setName(accountName);
                account.setUserId(userId);
                account.setGaid(gaid);
                account.setDeviceToken(deviceToken);
                account.setAppSetId(appSetId);
                account.setSsaid(ssaid);
                account.setNote(note);
                account.setAccountStatus("active");
                account.setSuspensionStatus("0"); // Default: Keine Suspension
                account.setLastPlayed(getCurrentTimestamp());
                
                // WICHTIG: KEIN Friend Link generieren!
                // Friend Link wird manuell später hinzugefügt
                
                // 7. In Supabase speichern
                repository.createAccount(account)
                    .thenRun(() -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), 
                                    "Backup erfolgreich erstellt", 
                                    Toast.LENGTH_SHORT).show();
                                loadAccounts();
                            });
                        }
                    })
                    .exceptionally(throwable -> {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Log.e(TAG, "Supabase save failed", throwable);
                                Toast.makeText(requireContext(), 
                                    "Fehler beim Speichern: " + throwable.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                            });
                        }
                        return null;
                    });
                    
            } catch (Exception e) {
                Log.e(TAG, "Backup failed", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), 
                            "Backup-Fehler: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show()
                    );
                }
            }
        }).start();
    }

    // ==================== ACCOUNT OPTIONS ====================
    
    private void showAccountOptions(Account account) {
        String[] options = {"Wiederherstellen", "Mehr anzeigen", "Abbrechen"};

        new AlertDialog.Builder(requireContext())
            .setTitle(account.getName())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: 
                        restoreAccount(account); 
                        break;
                    case 1: 
                        openAccountDetail(account); 
                        break;
                }
            })
            .show();
    }

    // ==================== RESTORE ACCOUNT ====================
    
    private void restoreAccount(Account account) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Account wiederherstellen")
            .setMessage("Möchtest du den Account '" + account.getName() + "' wiederherstellen?")
            .setPositiveButton("Ja", (dialog, which) -> {
                new Thread(() -> {
                    try {
                        Log.d(TAG, "Restoring account: " + account.getName());
                        
                        // 1. MonopolyGo schließen
                        AccountManager.forceStopApp();
                        Thread.sleep(1000);
                        
                        // 2. Dateien zurückkopieren
                        String backupDir = requireContext().getFilesDir().getAbsolutePath() + 
                                           "/backup/" + account.getName() + "/";
                        String monopolyGoData = "/data/data/com.scopely.monopolygo/";
                        
                        // shared_prefs zurückkopieren
                        boolean prefsRestored = RootManager.copyDirectory(
                            backupDir + "shared_prefs/",
                            monopolyGoData + "shared_prefs/"
                        );
                        Log.d(TAG, "shared_prefs restored: " + prefsRestored);
                        
                        // DiskBasedCacheDirectory zurückkopieren
                        boolean diskRestored = RootManager.copyDirectory(
                            backupDir + "DiskBasedCacheDirectory/",
                            monopolyGoData + "files/DiskBasedCacheDirectory/"
                        );
                        Log.d(TAG, "DiskBasedCacheDirectory restored: " + diskRestored);
                        
                        // Berechtigungen setzen
                        RootManager.setPermissions(monopolyGoData + "shared_prefs/", "660");
                        RootManager.setPermissions(monopolyGoData + "files/DiskBasedCacheDirectory/", "771");
                        
                        // 3. Fragen ob App gestartet werden soll
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                new AlertDialog.Builder(requireContext())
                                    .setTitle("Account wiederhergestellt")
                                    .setMessage("Möchtest du MonopolyGo jetzt starten?")
                                    .setPositiveButton("Ja", (d, w) -> {
                                        AccountManager.startApp();
                                    })
                                    .setNegativeButton("Nein", null)
                                    .show();
                            });
                        }
                        
                        // 4. Last-Played aktualisieren
                        repository.updateLastPlayed(account.getId())
                            .thenRun(() -> {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        loadAccounts();
                                    });
                                }
                            });
                            
                    } catch (Exception e) {
                        Log.e(TAG, "Restore failed", e);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(requireContext(), 
                                    "Restore-Fehler: " + e.getMessage(), 
                                    Toast.LENGTH_LONG).show()
                            );
                        }
                    }
                }).start();
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }

    // ==================== HELPER METHODS ====================
    
    private String extractXmlValue(String xmlContent, String key) {
        String pattern = "<string name=\"" + key + "\">([^<]+)</string>";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(xmlContent);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractSSAID(String ssaidContent) {
        String pattern = "com\\.scopely\\.monopolygo[^/]*/[^/]*/[^/]*/([0-9a-f]{16})";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(ssaidContent);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void openAccountDetail(Account account) {
        Intent intent = new Intent(requireContext(), AccountDetailActivity.class);
        intent.putExtra("account_id", account.getId());
        startActivity(intent);
    }
    
    /**
     * Get current timestamp in ISO 8601 format
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAccounts();
    }
}
