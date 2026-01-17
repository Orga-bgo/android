package de.babixgo.monopolygo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for partner event management.
 */
public class PartnerEventActivity extends AppCompatActivity {
    
    private TextView tvStatus;
    private Button btnAddCustomer, btnSelectAccounts, btnAssignment, btnTeamSetup;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partner_event);
        
        tvStatus = findViewById(R.id.tv_status);
        btnAddCustomer = findViewById(R.id.btn_add_customer);
        btnSelectAccounts = findViewById(R.id.btn_select_accounts);
        btnAssignment = findViewById(R.id.btn_assignment);
        btnTeamSetup = findViewById(R.id.btn_team_setup);
        
        setupButtons();
    }
    
    private void setupButtons() {
        btnAddCustomer.setOnClickListener(v -> {
            Toast.makeText(this, "Kunde hinzufügen (In Entwicklung)", Toast.LENGTH_SHORT).show();
            tvStatus.setText("Funktion: Kunde hinzufügen");
        });
        
        btnSelectAccounts.setOnClickListener(v -> {
            Toast.makeText(this, "Eigene Accounts wählen (In Entwicklung)", Toast.LENGTH_SHORT).show();
            tvStatus.setText("Funktion: Eigene Accounts wählen");
        });
        
        btnAssignment.setOnClickListener(v -> {
            Toast.makeText(this, "Zuweisung erstellen (In Entwicklung)", Toast.LENGTH_SHORT).show();
            tvStatus.setText("Funktion: Zuweisung erstellen");
        });
        
        btnTeamSetup.setOnClickListener(v -> {
            Toast.makeText(this, "Team zusammenstellen (In Entwicklung)", Toast.LENGTH_SHORT).show();
            tvStatus.setText("Funktion: Team zusammenstellen");
        });
    }
}
