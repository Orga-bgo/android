package de.babixgo.monopolygo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for friendship bar management.
 */
public class FriendshipActivity extends AppCompatActivity {
    
    private TextView tvStatus;
    private Button btnDownloadInstall;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friendship);
        
        tvStatus = findViewById(R.id.tv_status);
        btnDownloadInstall = findViewById(R.id.btn_download_install);
        
        setupButtons();
    }
    
    private void setupButtons() {
        btnDownloadInstall.setOnClickListener(v -> {
            Toast.makeText(this, "Download und Installation (In Entwicklung)", Toast.LENGTH_SHORT).show();
            tvStatus.setText("Funktion: Download und Installation für Freundschaftsbalken");
        });
    }
}
