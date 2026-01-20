package de.babixgo.monopolygo.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import de.babixgo.monopolygo.R;

/**
 * Tycoon Racers Activity - manages tycoon racer events and teams
 */
public class TycoonRacersActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create simple layout programmatically
        TextView textView = new TextView(this);
        textView.setText("Tycoon Racers\n\n(In Entwicklung)");
        textView.setTextSize(24);
        textView.setPadding(32, 32, 32, 32);
        textView.setGravity(android.view.Gravity.CENTER);
        
        setContentView(textView);
        
        // Set title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Tycoon Racers");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
