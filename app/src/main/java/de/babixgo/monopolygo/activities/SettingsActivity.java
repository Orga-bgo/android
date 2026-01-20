package de.babixgo.monopolygo.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import de.babixgo.monopolygo.R;

/**
 * Settings Activity - manages app settings and configuration
 */
public class SettingsActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create simple layout programmatically
        TextView textView = new TextView(this);
        textView.setText("Einstellungen\n\n(In Entwicklung)");
        textView.setTextSize(24);
        textView.setPadding(32, 32, 32, 32);
        textView.setGravity(android.view.Gravity.CENTER);
        
        setContentView(textView);
        
        // Set title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Einstellungen");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
