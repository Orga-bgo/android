package de.babixgo.monopolygo.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.adapters.CustomerAccountDetailAdapter;
import de.babixgo.monopolygo.adapters.CustomerActivityAdapter;
import de.babixgo.monopolygo.database.CustomerActivityRepository;
import de.babixgo.monopolygo.database.CustomerRepository;
import de.babixgo.monopolygo.models.Customer;
import de.babixgo.monopolygo.models.CustomerActivity;

/**
 * Comprehensive customer detail view
 * Displays:
 * - Customer information
 * - All accounts with services
 * - Activity history/audit trail
 */
public class CustomerDetailActivity extends AppCompatActivity {
    private static final String TAG = "CustomerDetailActivity";
    public static final String EXTRA_CUSTOMER_ID = "customer_id";
    
    private CustomerRepository customerRepository;
    private CustomerActivityRepository activityRepository;
    
    private Customer customer;
    private long customerId;
    
    // UI Elements - Customer Info
    private TextView tvCustomerName;
    private TextView tvCustomerNotes;
    private TextView tvAccountCount;
    private TextView tvServicesDisplay;
    
    // UI Elements - Accounts
    private RecyclerView rvAccounts;
    private CustomerAccountDetailAdapter accountsAdapter;
    private TextView tvNoAccounts;
    
    // UI Elements - Activity History
    private RecyclerView rvActivities;
    private CustomerActivityAdapter activitiesAdapter;
    private TextView tvNoActivities;
    
    // Actions
    private FloatingActionButton fabEdit;
    private FloatingActionButton fabAddAccount;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);
        
        // Get customer ID from intent
        customerId = getIntent().getLongExtra(EXTRA_CUSTOMER_ID, -1);
        if (customerId == -1) {
            Toast.makeText(this, "Fehler: Kunde nicht gefunden", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize repositories
        customerRepository = new CustomerRepository();
        activityRepository = new CustomerActivityRepository();
        
        // Initialize views
        initViews();
        
        // Setup adapters
        setupAdapters();
        
        // Load data
        loadCustomerDetails();
    }
    
    private void initViews() {
        // Customer Info Section
        tvCustomerName = findViewById(R.id.tv_customer_name);
        tvCustomerNotes = findViewById(R.id.tv_customer_notes);
        tvAccountCount = findViewById(R.id.tv_account_count);
        tvServicesDisplay = findViewById(R.id.tv_services_display);
        
        // Accounts Section
        rvAccounts = findViewById(R.id.rv_customer_accounts);
        tvNoAccounts = findViewById(R.id.tv_no_accounts);
        
        // Activities Section
        rvActivities = findViewById(R.id.rv_customer_activities);
        tvNoActivities = findViewById(R.id.tv_no_activities);
        
        // Actions
        fabEdit = findViewById(R.id.fab_edit_customer);
        fabAddAccount = findViewById(R.id.fab_add_account);
        
        fabEdit.setOnClickListener(v -> showEditCustomerDialog());
        fabAddAccount.setOnClickListener(v -> showAddAccountDialog());
        
        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Kundendetails");
        }
    }
    
    private void setupAdapters() {
        // Accounts adapter
        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        accountsAdapter = new CustomerAccountDetailAdapter(this::onAccountClick);
        rvAccounts.setAdapter(accountsAdapter);
        
        // Activities adapter
        rvActivities.setLayoutManager(new LinearLayoutManager(this));
        activitiesAdapter = new CustomerActivityAdapter();
        rvActivities.setAdapter(activitiesAdapter);
    }
    
    /**
     * Load customer details with accounts and activities
     */
    private void loadCustomerDetails() {
        Log.d(TAG, "Loading customer details for ID: " + customerId);
        
        // Load customer with accounts
        customerRepository.getCustomerById(customerId, true)
            .thenAccept(loadedCustomer -> {
                runOnUiThread(() -> {
                    this.customer = loadedCustomer;
                    displayCustomerInfo();
                    displayAccounts();
                });
                
                // Load activities in parallel
                loadActivities();
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Log.e(TAG, "Failed to load customer", throwable);
                    Toast.makeText(this, 
                        "Fehler beim Laden: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    /**
     * Display customer information
     */
    private void displayCustomerInfo() {
        if (customer == null) return;
        
        tvCustomerName.setText(customer.getName());
        tvCustomerNotes.setText(customer.getNotes() != null && !customer.getNotes().isEmpty() 
            ? customer.getNotes() 
            : "Keine Notizen");
        tvAccountCount.setText(String.valueOf(customer.getAccountCount()));
        tvServicesDisplay.setText(customer.getServicesDisplay());
        
        // Update toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(customer.getName());
        }
    }
    
    /**
     * Display customer accounts
     */
    private void displayAccounts() {
        if (customer == null) return;
        
        if (customer.getAccounts().isEmpty()) {
            rvAccounts.setVisibility(View.GONE);
            tvNoAccounts.setVisibility(View.VISIBLE);
        } else {
            rvAccounts.setVisibility(View.VISIBLE);
            tvNoAccounts.setVisibility(View.GONE);
            accountsAdapter.setAccounts(customer.getAccounts());
        }
    }
    
    /**
     * Load and display customer activities
     */
    private void loadActivities() {
        activityRepository.getActivitiesByCustomerId(customerId)
            .thenAccept(activities -> {
                runOnUiThread(() -> {
                    if (activities.isEmpty()) {
                        rvActivities.setVisibility(View.GONE);
                        tvNoActivities.setVisibility(View.VISIBLE);
                    } else {
                        rvActivities.setVisibility(View.VISIBLE);
                        tvNoActivities.setVisibility(View.GONE);
                        activitiesAdapter.setActivities(activities);
                    }
                });
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Log.e(TAG, "Failed to load activities", throwable);
                    tvNoActivities.setVisibility(View.VISIBLE);
                    tvNoActivities.setText("Fehler beim Laden der Aktivitäten");
                });
                return null;
            });
    }
    
    /**
     * Show dialog to edit customer information
     */
    private void showEditCustomerDialog() {
        if (customer == null) return;
        
        View dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_edit_customer, null);
        
        EditText etName = dialogView.findViewById(R.id.et_customer_name);
        EditText etNotes = dialogView.findViewById(R.id.et_customer_notes);
        
        etName.setText(customer.getName());
        etNotes.setText(customer.getNotes());
        
        new AlertDialog.Builder(this)
            .setTitle("Kunde bearbeiten")
            .setView(dialogView)
            .setPositiveButton("Speichern", (dialog, which) -> {
                String newName = etName.getText().toString().trim();
                String newNotes = etNotes.getText().toString().trim();
                
                if (newName.isEmpty()) {
                    Toast.makeText(this, "Name darf nicht leer sein", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                updateCustomer(newName, newNotes);
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    /**
     * Update customer information
     */
    private void updateCustomer(String name, String notes) {
        customer.setName(name);
        customer.setNotes(notes);
        
        customerRepository.updateCustomer(customer)
            .thenAccept(updatedCustomer -> {
                runOnUiThread(() -> {
                    this.customer = updatedCustomer;
                    displayCustomerInfo();
                    Toast.makeText(this, "Kunde aktualisiert", Toast.LENGTH_SHORT).show();
                });
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Log.e(TAG, "Failed to update customer", throwable);
                    Toast.makeText(this, 
                        "Fehler beim Aktualisieren: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    /**
     * Show dialog to add new account for this customer
     */
    private void showAddAccountDialog() {
        Toast.makeText(this, "Account hinzufügen - Feature in Entwicklung", Toast.LENGTH_SHORT).show();
        // TODO: Implement add account dialog
    }
    
    /**
     * Handle account click
     */
    private void onAccountClick(de.babixgo.monopolygo.models.CustomerAccount account) {
        // Show account details or edit dialog
        Toast.makeText(this, "Account: " + account.getIngameName(), Toast.LENGTH_SHORT).show();
        // TODO: Implement account detail/edit dialog
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerDetails();
    }
}
