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
import de.babixgo.monopolygo.DataExtractor;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.activities.AccountDetailActivity;
import de.babixgo.monopolygo.adapters.AccountListAdapter;
import de.babixgo.monopolygo.database.AccountRepository;
import de.babixgo.monopolygo.models.Account;
import de.babixgo.monopolygo.utils.DeviceIdExtractor;
import de.babixgo.monopolygo.ShortLinkManager;
import java.util.ArrayList;
import java.util.List;

public class AccountListFragment extends Fragment {
    private static final String TAG = "AccountListFragment";
    
    private RecyclerView rvAccounts;
    private AccountListAdapter adapter;
    private AccountRepository repository;
    private FloatingActionButton fabBackup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                            @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account_list, container, false);

        // Initialize Repository
        repository = new AccountRepository();
        
        // Initialize Views
        rvAccounts = view.findViewById(R.id.rv_accounts);
        fabBackup = view.findViewById(R.id.fab_backup);
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup FAB
        fabBackup.setOnClickListener(v -> showBackupDialog());
        
        // Load accounts
        loadAccounts();
        
        return view;
    }
    
    private void setupRecyclerView() {
        adapter = new AccountListAdapter(account -> showAccountOptionsDialog(account));
        rvAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAccounts.setAdapter(adapter);
    }
    
    private void loadAccounts() {
        repository.getAllAccounts()
            .thenAccept(accounts -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
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
    
    private void showAccountOptionsDialog(Account account) {
        String[] options = {
            "Wiederherstellen",
            "Mehr anzeigen",
            "Abbrechen"
        };
        
        new AlertDialog.Builder(requireContext())
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
        new Thread(() -> {
            try {
                // Force Stop MonopolyGo
                AccountManager.forceStopApp();
                Thread.sleep(1000);
                
                // Restore Account
                String sourceFile = AccountManager.getAccountsEigenePath() + 
                                   account.getName() + "/WithBuddies.Services.User.0Production.dat";
                boolean success = AccountManager.restoreAccount(sourceFile);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (success) {
                            // Update last_played in Supabase
                            repository.updateLastPlayed(account.getId())
                                .thenRun(() -> {
                                    if (getActivity() != null) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(requireContext(), 
                                                "✅ Account wiederhergestellt", 
                                                Toast.LENGTH_SHORT).show();
                                            loadAccounts(); // Refresh list
                                        });
                                    }
                                })
                                .exceptionally(e -> {
                                    Log.e(TAG, "Failed to update last_played", e);
                                    return null;
                                });
                        } else {
                            Toast.makeText(requireContext(), 
                                "❌ Wiederherstellung fehlgeschlagen", 
                                Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Restore failed", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), 
                            "❌ Fehler: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }
    
    private void openAccountDetail(Account account) {
        Intent intent = new Intent(requireContext(), AccountDetailActivity.class);
        intent.putExtra("account_id", account.getId());
        startActivity(intent);
    }
    
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
                        "❌ Bitte Account-Namen eingeben", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                backupAccount(accountName, note);
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    private void backupAccount(String accountName, String note) {
        new Thread(() -> {
            try {
                // 1. File Backup
                boolean fileBackupSuccess = AccountManager.backupAccount(
                    AccountManager.getAccountsEigenePath(), 
                    accountName
                );
                
                if (!fileBackupSuccess) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), 
                                "❌ File-Backup fehlgeschlagen", 
                                Toast.LENGTH_LONG).show();
                        });
                    }
                    return;
                }
                
                // 2. Extract UserID
                String userId = DataExtractor.extractUserId();
                
                // 3. Extract all Device IDs
                DeviceIdExtractor.extractAllIds(requireContext())
                    .thenAccept(deviceIds -> {
                        // 4. Create Account object
                        Account account = new Account();
                        account.setName(accountName);
                        account.setUserId(userId);
                        account.setSsaid(deviceIds.ssaid);
                        account.setGaid(deviceIds.gaid);
                        account.setDeviceId(deviceIds.deviceId);
                        account.setNote(note);
                        
                        // Create short link if userId is available
                        if (userId != null && !userId.isEmpty()) {
                            String shortLink = ShortLinkManager.createShortLink(userId, accountName);
                            account.setShortLink(shortLink);
                            account.setFriendLink("monopolygo://add-friend/" + userId);
                        }
                        
                        // 5. Save to Supabase
                        repository.createAccount(account)
                            .thenRun(() -> {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), 
                                            "✅ Backup komplett", 
                                            Toast.LENGTH_SHORT).show();
                                        loadAccounts(); // Refresh list
                                    });
                                }
                            })
                            .exceptionally(e -> {
                                Log.e(TAG, "Supabase save failed", e);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), 
                                            "⚠️ Supabase-Fehler: " + e.getMessage(), 
                                            Toast.LENGTH_LONG).show();
                                    });
                                }
                                return null;
                            });
                    });
                
            } catch (Exception e) {
                Log.e(TAG, "Backup failed", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), 
                            "❌ Fehler: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    });
                }
            }
        }).start();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadAccounts(); // Refresh on return
    }
}
