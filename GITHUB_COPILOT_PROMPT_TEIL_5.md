# GitHub Copilot Prompt - Teil 5: Team Management & Event Execution

## 👥 TEAM EDIT DIALOG

### Layout: dialog_edit_team.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">
        
        <!-- Team Name -->
        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Team Name"
            android:layout_marginBottom="16dp"
            style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox">
            
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/et_team_name"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:singleLine="true" />
        </com.google.android.material.textfield.TextInputLayout>
        
        <!-- Customer Selection -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Kunde auswählen"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="@color/text_dark"
            android:layout_marginBottom="8dp" />
        
        <Spinner
            android:id="@+id/spinner_customer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp" />
        
        <!-- Slot Assignments -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Account-Zuweisungen (max. 4)"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="@color/text_dark"
            android:layout_marginBottom="12dp" />
        
        <!-- Slot 1 -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Slot 1"
            style="@style/LabelText"
            android:layout_marginBottom="4dp" />
        
        <Spinner
            android:id="@+id/spinner_slot_1"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="12dp" />
        
        <!-- Slot 2 -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Slot 2"
            style="@style/LabelText"
            android:layout_marginBottom="4dp" />
        
        <Spinner
            android:id="@+id/spinner_slot_2"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="12dp" />
        
        <!-- Slot 3 -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Slot 3"
            style="@style/LabelText"
            android:layout_marginBottom="4dp" />
        
        <Spinner
            android:id="@+id/spinner_slot_3"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="12dp" />
        
        <!-- Slot 4 -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Slot 4"
            style="@style/LabelText"
            android:layout_marginBottom="4dp" />
        
        <Spinner
            android:id="@+id/spinner_slot_4"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp" />
        
        <!-- Buttons -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">
            
            <Button
                android:id="@+id/btn_cancel"
                style="@style/Widget.MaterialComponents.Button.TextButton"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Abbrechen"
                android:textColor="@color/text_gray" />
            
            <Button
                android:id="@+id/btn_save"
                style="@style/BabixButton.Blue"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Speichern" />
        </LinearLayout>
    </LinearLayout>
</ScrollView>
```

### Team Edit Implementation

```java
// In EventDetailActivity.java - Add Team Edit Method

private void openTeamEdit(Team team) {
    View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_team, null);
    
    // Initialize views
    TextInputEditText etTeamName = dialogView.findViewById(R.id.et_team_name);
    Spinner spinnerCustomer = dialogView.findViewById(R.id.spinner_customer);
    Spinner spinnerSlot1 = dialogView.findViewById(R.id.spinner_slot_1);
    Spinner spinnerSlot2 = dialogView.findViewById(R.id.spinner_slot_2);
    Spinner spinnerSlot3 = dialogView.findViewById(R.id.spinner_slot_3);
    Spinner spinnerSlot4 = dialogView.findViewById(R.id.spinner_slot_4);
    Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
    Button btnSave = dialogView.findViewById(R.id.btn_save);
    
    // Set current team name
    etTeamName.setText(team.getName());
    
    // Load customers for spinner
    loadCustomersIntoSpinner(spinnerCustomer, team.getCustomerId());
    
    // Load accounts for slot spinners
    loadAccountsIntoSlotSpinners(
        spinnerSlot1, spinnerSlot2, spinnerSlot3, spinnerSlot4,
        team.getSlot1AccountId(), team.getSlot2AccountId(), 
        team.getSlot3AccountId(), team.getSlot4AccountId()
    );
    
    AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle("Team bearbeiten")
        .setView(dialogView)
        .setCancelable(true)
        .create();
    
    btnCancel.setOnClickListener(v -> dialog.dismiss());
    
    btnSave.setOnClickListener(v -> {
        // Update team
        team.setName(etTeamName.getText().toString().trim());
        
        // Get selected customer
        CustomerSpinnerItem selectedCustomer = (CustomerSpinnerItem) spinnerCustomer.getSelectedItem();
        if (selectedCustomer != null && selectedCustomer.id != -1) {
            team.setCustomerId(selectedCustomer.id);
        } else {
            team.setCustomerId(null);
        }
        
        // Get selected accounts
        AccountSpinnerItem slot1 = (AccountSpinnerItem) spinnerSlot1.getSelectedItem();
        AccountSpinnerItem slot2 = (AccountSpinnerItem) spinnerSlot2.getSelectedItem();
        AccountSpinnerItem slot3 = (AccountSpinnerItem) spinnerSlot3.getSelectedItem();
        AccountSpinnerItem slot4 = (AccountSpinnerItem) spinnerSlot4.getSelectedItem();
        
        team.setSlot1AccountId(slot1 != null && slot1.id != -1 ? slot1.id : null);
        team.setSlot2AccountId(slot2 != null && slot2.id != -1 ? slot2.id : null);
        team.setSlot3AccountId(slot3 != null && slot3.id != -1 ? slot3.id : null);
        team.setSlot4AccountId(slot4 != null && slot4.id != -1 ? slot4.id : null);
        
        team.setSlot1Name(slot1 != null ? slot1.name : null);
        team.setSlot2Name(slot2 != null ? slot2.name : null);
        team.setSlot3Name(slot3 != null ? slot3.name : null);
        team.setSlot4Name(slot4 != null ? slot4.name : null);
        
        // Save to database
        saveTeam(team);
        dialog.dismiss();
    });
    
    dialog.show();
}

// Spinner Item Classes
private static class CustomerSpinnerItem {
    long id;
    String name;
    
    CustomerSpinnerItem(long id, String name) {
        this.id = id;
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}

private static class AccountSpinnerItem {
    long id;
    String name;
    
    AccountSpinnerItem(long id, String name) {
        this.id = id;
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}

private void loadCustomersIntoSpinner(Spinner spinner, Long selectedCustomerId) {
    customerRepository.getAllCustomers()
        .thenAccept(customers -> runOnUiThread(() -> {
            List<CustomerSpinnerItem> items = new ArrayList<>();
            items.add(new CustomerSpinnerItem(-1, "-- Kein Kunde --"));
            
            int selectedPosition = 0;
            for (int i = 0; i < customers.size(); i++) {
                Customer customer = customers.get(i);
                items.add(new CustomerSpinnerItem(customer.getId(), customer.getName()));
                if (selectedCustomerId != null && customer.getId() == selectedCustomerId) {
                    selectedPosition = i + 1;
                }
            }
            
            ArrayAdapter<CustomerSpinnerItem> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setSelection(selectedPosition);
        }))
        .exceptionally(throwable -> {
            runOnUiThread(() -> {
                Toast.makeText(this, 
                    "Fehler beim Laden der Kunden", 
                    Toast.LENGTH_SHORT).show();
            });
            return null;
        });
}

private void loadAccountsIntoSlotSpinners(
    Spinner slot1, Spinner slot2, Spinner slot3, Spinner slot4,
    Long selectedId1, Long selectedId2, Long selectedId3, Long selectedId4
) {
    AccountRepository accountRepo = new AccountRepository();
    accountRepo.getAllAccounts()
        .thenAccept(accounts -> runOnUiThread(() -> {
            List<AccountSpinnerItem> items = new ArrayList<>();
            items.add(new AccountSpinnerItem(-1, "-- Leer --"));
            
            for (Account account : accounts) {
                items.add(new AccountSpinnerItem(account.getId(), account.getName()));
            }
            
            // Setup each spinner
            setupAccountSpinner(slot1, items, selectedId1);
            setupAccountSpinner(slot2, items, selectedId2);
            setupAccountSpinner(slot3, items, selectedId3);
            setupAccountSpinner(slot4, items, selectedId4);
        }))
        .exceptionally(throwable -> {
            runOnUiThread(() -> {
                Toast.makeText(this, 
                    "Fehler beim Laden der Accounts", 
                    Toast.LENGTH_SHORT).show();
            });
            return null;
        });
}

private void setupAccountSpinner(
    Spinner spinner, 
    List<AccountSpinnerItem> items, 
    Long selectedId
) {
    ArrayAdapter<AccountSpinnerItem> adapter = new ArrayAdapter<>(
        this, android.R.layout.simple_spinner_item, items);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
    
    // Set selection
    if (selectedId != null) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == selectedId) {
                spinner.setSelection(i);
                break;
            }
        }
    }
}

private void saveTeam(Team team) {
    teamRepository.updateTeam(team)
        .thenRun(() -> runOnUiThread(() -> {
            Toast.makeText(this, "Team gespeichert", Toast.LENGTH_SHORT).show();
            loadTeams(); // Refresh
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
```

---

## 🚀 EVENT EXECUTION (AUTOMATISCHE FREUNDSCHAFTSANFRAGEN)

### Event Executor Manager

```java
// EventExecutor.java
package de.babixgo.monopolygo.utils;

import android.content.Context;
import de.babixgo.monopolygo.AccountManager;
import de.babixgo.monopolygo.database.AccountRepository;
import de.babixgo.monopolygo.database.CustomerRepository;
import de.babixgo.monopolygo.database.TeamRepository;
import de.babixgo.monopolygo.models.Account;
import de.babixgo.monopolygo.models.Customer;
import de.babixgo.monopolygo.models.Team;
import java.util.List;

/**
 * Führt Event-Sequenz automatisch aus:
 * 1. Account wiederherstellen
 * 2. MonopolyGo starten
 * 3. Warten (10 Sekunden)
 * 4. Freundschaftslinks öffnen (Slot 1-4)
 * 5. Nächster Account
 */
public class EventExecutor {
    
    public interface ExecutionListener {
        void onStepComplete(String message);
        void onTeamComplete(Team team);
        void onExecutionComplete();
        void onError(String error);
    }
    
    private Context context;
    private TeamRepository teamRepository;
    private AccountRepository accountRepository;
    private CustomerRepository customerRepository;
    private ExecutionListener listener;
    
    public EventExecutor(Context context, ExecutionListener listener) {
        this.context = context;
        this.listener = listener;
        this.teamRepository = new TeamRepository();
        this.accountRepository = new AccountRepository();
        this.customerRepository = new CustomerRepository();
    }
    
    /**
     * Führe Event für alle Teams aus
     */
    public void executeEvent(long eventId) {
        teamRepository.getTeamsByEventId(eventId)
            .thenAccept(teams -> {
                executeTeamsSequentially(teams, 0);
            })
            .exceptionally(throwable -> {
                listener.onError("Fehler beim Laden der Teams: " + throwable.getMessage());
                return null;
            });
    }
    
    private void executeTeamsSequentially(List<Team> teams, int index) {
        if (index >= teams.size()) {
            listener.onExecutionComplete();
            return;
        }
        
        Team team = teams.get(index);
        
        executeTeam(team)
            .thenRun(() -> {
                listener.onTeamComplete(team);
                // Continue with next team
                executeTeamsSequentially(teams, index + 1);
            })
            .exceptionally(throwable -> {
                listener.onError("Fehler bei Team " + team.getName() + ": " + throwable.getMessage());
                // Continue anyway
                executeTeamsSequentially(teams, index + 1);
                return null;
            });
    }
    
    /**
     * Führe ein einzelnes Team aus
     */
    private java.util.concurrent.CompletableFuture<Void> executeTeam(Team team) {
        return java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // Get customer info
                if (team.getCustomerId() == null) {
                    throw new Exception("Team hat keinen Kunden zugewiesen");
                }
                
                Customer customer = customerRepository.getCustomerById(team.getCustomerId())
                    .get(); // Blocking call
                
                if (customer == null || customer.getFriendLink() == null) {
                    throw new Exception("Kunde-Daten nicht vollständig");
                }
                
                // Process each slot
                processSlot(team, 1, team.getSlot1AccountId(), customer);
                processSlot(team, 2, team.getSlot2AccountId(), customer);
                processSlot(team, 3, team.getSlot3AccountId(), customer);
                processSlot(team, 4, team.getSlot4AccountId(), customer);
                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    private void processSlot(Team team, int slotNumber, Long accountId, Customer customer) 
        throws Exception {
        
        if (accountId == null) {
            listener.onStepComplete("Slot " + slotNumber + " ist leer - überspringe");
            return;
        }
        
        // Get account
        Account account = accountRepository.getAccountById(accountId).get();
        if (account == null) {
            throw new Exception("Account nicht gefunden: ID " + accountId);
        }
        
        listener.onStepComplete("Wiederherstelle Account: " + account.getName());
        
        // 1. Force stop MonopolyGo
        AccountManager.forceStopApp();
        Thread.sleep(1000);
        
        // 2. Restore account
        String sourceFile = AccountManager.getAccountsEigenePath() + account.getName() + 
                           "/WithBuddies.Services.User.0Production.dat";
        boolean restoreSuccess = AccountManager.restoreAccount(sourceFile);
        
        if (!restoreSuccess) {
            throw new Exception("Restore fehlgeschlagen für " + account.getName());
        }
        
        listener.onStepComplete("Account wiederhergestellt: " + account.getName());
        
        // 3. Start MonopolyGo
        AccountManager.startApp();
        listener.onStepComplete("MonopolyGo gestartet - warte 10 Sekunden...");
        Thread.sleep(10000); // Wait 10 seconds
        
        // 4. Open friend link
        listener.onStepComplete("Öffne Freundschaftslink für Kunde: " + customer.getName());
        AccountManager.openFriendLink(customer.getUserId());
        Thread.sleep(2000); // Wait 2 seconds for link to open
        
        listener.onStepComplete("Slot " + slotNumber + " abgeschlossen");
    }
}
```

### Execute Event from EventDetailActivity

```java
// In EventDetailActivity.java - Add Execution Button

private Button btnExecuteEvent;

private void initViews() {
    // ... existing code ...
    btnExecuteEvent = findViewById(R.id.btn_execute_event);
    btnExecuteEvent.setOnClickListener(v -> showExecuteConfirmation());
}

private void showExecuteConfirmation() {
    new AlertDialog.Builder(this)
        .setTitle("Event ausführen")
        .setMessage("Möchten Sie das Event automatisch ausführen?\n\n" +
                   "Die App wird nacheinander:\n" +
                   "1. Jeden Account wiederherstellen\n" +
                   "2. MonopolyGo starten\n" +
                   "3. Freundschaftslinks öffnen\n\n" +
                   "Dies kann mehrere Minuten dauern.")
        .setPositiveButton("Ja, starten", (dialog, which) -> executeEvent())
        .setNegativeButton("Abbrechen", null)
        .show();
}

private void executeEvent() {
    // Show progress dialog
    AlertDialog progressDialog = new AlertDialog.Builder(this)
        .setTitle("Event wird ausgeführt...")
        .setMessage("Bitte warten...")
        .setCancelable(false)
        .create();
    progressDialog.show();
    
    TextView messageView = new TextView(this);
    messageView.setPadding(24, 24, 24, 24);
    messageView.setTextSize(14);
    progressDialog.setContentView(messageView);
    
    EventExecutor executor = new EventExecutor(this, new EventExecutor.ExecutionListener() {
        @Override
        public void onStepComplete(String message) {
            runOnUiThread(() -> {
                messageView.setText(messageView.getText() + "\n" + message);
            });
        }
        
        @Override
        public void onTeamComplete(Team team) {
            runOnUiThread(() -> {
                messageView.setText(messageView.getText() + "\n\n✓ Team " + team.getName() + " abgeschlossen\n");
            });
        }
        
        @Override
        public void onExecutionComplete() {
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(EventDetailActivity.this, 
                    "Event vollständig ausgeführt!", 
                    Toast.LENGTH_LONG).show();
            });
        }
        
        @Override
        public void onError(String error) {
            runOnUiThread(() -> {
                messageView.setText(messageView.getText() + "\n❌ FEHLER: " + error);
                Toast.makeText(EventDetailActivity.this, 
                    "Fehler: " + error, 
                    Toast.LENGTH_LONG).show();
            });
        }
    });
    
    executor.executeEvent(event.getId());
}
```

### Update activity_event_detail.xml

```xml
<!-- Add to existing layout after btnAddCustomer -->

<Button
    android:id="@+id/btn_execute_event"
    style="@style/BabixButton.Green"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:text="Event ausführen"
    android:drawableStart="@android:drawable/ic_media_play" />
```

---

## ✅ ZUSAMMENFASSUNG TEIL 5

**Implementiert:**
- ✅ Team Edit Dialog mit Customer-Auswahl
- ✅ Account-Slot-Zuweisungen (Spinner)
- ✅ Event Executor für automatische Ausführung
- ✅ Sequentielle Team-Verarbeitung
- ✅ Account Restore → Start App → Open Links
- ✅ Progress Dialog mit Live-Updates

**Workflow Automatische Ausführung:**
1. User klickt "Event ausführen"
2. App lädt alle Teams für Event
3. Für jedes Team:
   - Für jeden belegten Slot (1-4):
     - Stop MonopolyGo
     - Restore Account
     - Start MonopolyGo
     - Warte 10 Sekunden
     - Öffne Freundschaftslink
     - Warte 2 Sekunden
4. Progress wird live angezeigt
5. Fertigmeldung nach Abschluss

**Nächste Schritte (Teil 6):**
- Supabase SQL Schema (vollständig)
- Setup-Anleitung
- Testing Checkliste
- Abschluss & Deployment

**Fortsetzung in Teil 6: Supabase Setup & Finale Schritte**
