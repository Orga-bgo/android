package de.babixgo.monopolygo.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.adapters.TeamListAdapter;
import de.babixgo.monopolygo.database.EventRepository;
import de.babixgo.monopolygo.database.TeamRepository;
import de.babixgo.monopolygo.database.CustomerRepository;
import de.babixgo.monopolygo.models.Event;
import de.babixgo.monopolygo.models.Team;
import de.babixgo.monopolygo.models.Customer;

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
        Customer customer = new Customer(name, friendLink, 4); // Default 4 slots
        customer.setFriendCode(friendCode);
        
        // Extract UserID from link
        String userId = extractUserIdFromLink(friendLink);
        if (userId != null) {
            customer.setUserId(userId);
        }
        
        customerRepository.createCustomer(customer)
            .thenAccept(created -> runOnUiThread(() -> {
                Toast.makeText(this, "Kunde erstellt", Toast.LENGTH_SHORT).show();
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
        // TODO: Implement team editing
        Toast.makeText(this, "Team bearbeiten: " + team.getName(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (event != null) {
            loadTeams(); // Refresh on return
        }
    }
}
