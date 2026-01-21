package de.babixgo.monopolygo.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.adapters.TeamListAdapter;
import de.babixgo.monopolygo.database.AccountRepository;
import de.babixgo.monopolygo.database.EventRepository;
import de.babixgo.monopolygo.database.TeamRepository;
import de.babixgo.monopolygo.database.CustomerRepository;
import de.babixgo.monopolygo.models.Account;
import de.babixgo.monopolygo.models.Event;
import de.babixgo.monopolygo.models.Team;
import de.babixgo.monopolygo.models.Customer;
import java.util.ArrayList;
import java.util.List;

public class EventDetailActivity extends AppCompatActivity {
    
    private Event event;
    private EventRepository eventRepository;
    private TeamRepository teamRepository;
    private CustomerRepository customerRepository;
    
    private TextView tvEventTitle;
    private Button btnAddTeam, btnAddCustomer;
    private RecyclerView rvTeams;
    private TeamListAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        
        eventRepository = new EventRepository();
        teamRepository = new TeamRepository();
        customerRepository = new CustomerRepository();
        
        initViews();
        loadEvent();
    }
    
    private void initViews() {
        tvEventTitle = findViewById(R.id.tv_event_title);
        btnAddTeam = findViewById(R.id.btn_add_team);
        btnAddCustomer = findViewById(R.id.btn_add_customer);
        rvTeams = findViewById(R.id.rv_teams);
        
        btnAddTeam.setOnClickListener(v -> showAddTeamDialog());
        btnAddCustomer.setOnClickListener(v -> showAddCustomerDialog());
        
        setupRecyclerView();
    }
    
    private void setupRecyclerView() {
        adapter = new TeamListAdapter(team -> openTeamEdit(team));
        rvTeams.setLayoutManager(new LinearLayoutManager(this));
        rvTeams.setAdapter(adapter);
    }
    
    private void loadEvent() {
        long eventId = getIntent().getLongExtra("event_id", -1);
        if (eventId == -1) {
            Toast.makeText(this, "Fehler: Keine Event-ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        eventRepository.getEventById(eventId)
            .thenAccept(loadedEvent -> runOnUiThread(() -> {
                event = loadedEvent;
                tvEventTitle.setText("Event: " + event.getName());
                loadTeams();
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
    
    private void loadTeams() {
        teamRepository.getTeamsByEventId(event.getId())
            .thenAccept(teams -> runOnUiThread(() -> {
                adapter.setTeams(teams);
            }))
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "Fehler beim Laden der Teams: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    private void showAddTeamDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_team, null);
        EditText etTeamName = dialogView.findViewById(R.id.et_team_name);
        
        new AlertDialog.Builder(this)
            .setTitle("Team hinzufügen")
            .setView(dialogView)
            .setPositiveButton("Speichern", (dialog, which) -> {
                String teamName = etTeamName.getText().toString().trim();
                if (teamName.isEmpty()) {
                    Toast.makeText(this, "Name erforderlich", Toast.LENGTH_SHORT).show();
                    return;
                }
                createTeam(teamName);
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    private void createTeam(String teamName) {
        Team team = new Team(event.getId(), teamName);
        
        teamRepository.createTeam(team)
            .thenAccept(created -> runOnUiThread(() -> {
                Toast.makeText(this, "Team erstellt", Toast.LENGTH_SHORT).show();
                loadTeams(); // Refresh
            }))
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "Fehler beim Erstellen: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    private void showAddCustomerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_customer, null);
        EditText etName = dialogView.findViewById(R.id.et_customer_name);
        EditText etFriendLink = dialogView.findViewById(R.id.et_friend_link);
        EditText etFriendCode = dialogView.findViewById(R.id.et_friend_code);
        
        new AlertDialog.Builder(this)
            .setTitle("Kunde hinzufügen")
            .setView(dialogView)
            .setPositiveButton("Speichern", (dialog, which) -> {
                String name = etName.getText().toString().trim();
                String friendLink = etFriendLink.getText().toString().trim();
                String friendCode = etFriendCode.getText().toString().trim();
                
                if (name.isEmpty() || friendLink.isEmpty()) {
                    Toast.makeText(this, "Name und Link erforderlich", Toast.LENGTH_SHORT).show();
                    return;
                }
                createCustomer(name, friendLink, friendCode);
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    private void createCustomer(String name, String friendLink, String friendCode) {
        // Create customer with just name (new structure)
        Customer customer = new Customer(name);
        
        // Note: In the new structure, friend_link and friend_code belong to CustomerAccount
        // For now, we'll create a simple customer. To add friend links, create a CustomerAccount separately.
        
        customerRepository.createCustomer(customer)
            .thenAccept(created -> runOnUiThread(() -> {
                Toast.makeText(this, "Kunde erstellt: " + created.getName(), Toast.LENGTH_SHORT).show();
            }))
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "Fehler beim Erstellen: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    private String extractUserIdFromLink(String link) {
        if (link == null || !link.contains("add-friend/")) return null;
        try {
            int startIndex = link.indexOf("add-friend/") + "add-friend/".length();
            String userId = link.substring(startIndex);
            int endIndex = userId.length();
            if (userId.contains("?")) endIndex = Math.min(endIndex, userId.indexOf("?"));
            if (userId.contains("#")) endIndex = Math.min(endIndex, userId.indexOf("#"));
            userId = userId.substring(0, endIndex).trim();
            return userId.matches("\\d+") ? userId : null;
        } catch (Exception e) {
            return null;
        }
    }
    
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
            
            team.setSlot1Name(slot1 != null && slot1.id != -1 ? slot1.name : null);
            team.setSlot2Name(slot2 != null && slot2.id != -1 ? slot2.name : null);
            team.setSlot3Name(slot3 != null && slot3.id != -1 ? slot3.name : null);
            team.setSlot4Name(slot4 != null && slot4.id != -1 ? slot4.name : null);
            
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
    
    @Override
    protected void onResume() {
        super.onResume();
        if (event != null) {
            loadTeams(); // Refresh on return
        }
    }
}
