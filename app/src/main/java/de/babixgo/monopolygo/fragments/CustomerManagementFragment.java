package de.babixgo.monopolygo.fragments;

import android.app.AlertDialog;
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
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.adapters.CustomerListAdapter;
import de.babixgo.monopolygo.database.CustomerRepository;
import de.babixgo.monopolygo.models.Customer;

/**
 * Fragment for Customer Management
 * Displays list of customers with RecyclerView
 * Allows creating new customers via FAB
 */
public class CustomerManagementFragment extends Fragment {
    private static final String TAG = "CustomerFragment";
    
    private RecyclerView rvCustomers;
    private CustomerListAdapter adapter;
    private CustomerRepository repository;
    private FloatingActionButton fabCreateCustomer;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                            @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_management, container, false);
        
        // Initialize Repository
        repository = new CustomerRepository();
        
        // Setup RecyclerView
        rvCustomers = view.findViewById(R.id.rv_customers);
        rvCustomers.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        adapter = new CustomerListAdapter(this::onCustomerClick);
        rvCustomers.setAdapter(adapter);
        
        // Setup FAB
        fabCreateCustomer = view.findViewById(R.id.fab_create_customer);
        fabCreateCustomer.setOnClickListener(v -> showCreateCustomerDialog());
        
        // Load Customers
        loadCustomers();
        
        return view;
    }
    
    /**
     * Load customers from repository
     */
    private void loadCustomers() {
        Log.d(TAG, "Loading customers");
        
        repository.getAllCustomers()
            .thenAccept(customers -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.d(TAG, "Loaded " + customers.size() + " customers");
                        adapter.setCustomers(customers);
                    });
                }
            })
            .exceptionally(throwable -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Failed to load customers", throwable);
                        Toast.makeText(requireContext(), 
                            "Fehler beim Laden: " + throwable.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    });
                }
                return null;
            });
    }
    
    /**
     * Show dialog to create new customer
     */
    private void showCreateCustomerDialog() {
        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_customer, null);
        
        EditText etCustomerName = dialogView.findViewById(R.id.et_customer_name);
        
        new AlertDialog.Builder(requireContext())
            .setTitle("Neuer Kunde")
            .setView(dialogView)
            .setPositiveButton("Erstellen", (dialog, which) -> {
                String name = etCustomerName.getText().toString().trim();
                
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), 
                        "Bitte Namen eingeben", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                createCustomer(name);
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    /**
     * Create a new customer
     */
    private void createCustomer(String name) {
        Log.d(TAG, "Creating customer: " + name);
        
        Customer customer = new Customer(name);
        
        repository.createCustomer(customer)
            .thenAccept(createdCustomer -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), 
                            "Kunde erstellt: " + createdCustomer.getName(), 
                            Toast.LENGTH_SHORT).show();
                        loadCustomers(); // Reload list
                    });
                }
            })
            .exceptionally(throwable -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.e(TAG, "Failed to create customer", throwable);
                        Toast.makeText(requireContext(), 
                            "Fehler beim Erstellen: " + throwable.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    });
                }
                return null;
            });
    }
    
    /**
     * Handle customer click
     * For now, just show a Toast
     */
    private void onCustomerClick(Customer customer) {
        Toast.makeText(requireContext(), 
            "Kunde: " + customer.getName(), 
            Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadCustomers();
    }
}
