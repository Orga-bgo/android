package de.babixgo.monopolygo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.babixgo.monopolygo.R;
import de.babixgo.monopolygo.adapters.EventListAdapter;
import de.babixgo.monopolygo.database.EventRepository;
import de.babixgo.monopolygo.models.Event;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EventListActivity extends AppCompatActivity {
    
    private EventRepository eventRepository;
    private Button btnNewEvent;
    private RecyclerView rvEvents;
    private EventListAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);
        
        eventRepository = new EventRepository();
        
        initViews();
        loadEvents();
    }
    
    private void initViews() {
        btnNewEvent = findViewById(R.id.btn_new_event);
        rvEvents = findViewById(R.id.rv_events);
        
        btnNewEvent.setOnClickListener(v -> showNewEventDialog());
        
        setupRecyclerView();
    }
    
    private void setupRecyclerView() {
        adapter = new EventListAdapter(new EventListAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                openEventDetail(event);
            }
            
            @Override
            public void onEditClick(Event event) {
                showEditEventDialog(event);
            }
        });
        
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setAdapter(adapter);
    }
    
    private void loadEvents() {
        eventRepository.getAllEvents()
            .thenAccept(events -> runOnUiThread(() -> {
                adapter.setEvents(events);
            }))
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "Fehler beim Laden: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    private void showNewEventDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_event, null);
        EditText etEventName = dialogView.findViewById(R.id.et_event_name);
        EditText etDateRange = dialogView.findViewById(R.id.et_date_range);
        
        new AlertDialog.Builder(this)
            .setTitle("Neues Event")
            .setView(dialogView)
            .setPositiveButton("Erstellen", (dialog, which) -> {
                String name = etEventName.getText().toString().trim();
                String dateRange = etDateRange.getText().toString().trim();
                
                if (name.isEmpty() || dateRange.isEmpty()) {
                    Toast.makeText(this, "Name und Datum erforderlich", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                createEvent(name, dateRange);
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    private void showEditEventDialog(Event event) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_event, null);
        EditText etEventName = dialogView.findViewById(R.id.et_event_name);
        EditText etDateRange = dialogView.findViewById(R.id.et_date_range);
        
        etEventName.setText(event.getName());
        etDateRange.setText(event.getFormattedDateRange());
        
        new AlertDialog.Builder(this)
            .setTitle("Event bearbeiten")
            .setView(dialogView)
            .setPositiveButton("Speichern", (dialog, which) -> {
                String name = etEventName.getText().toString().trim();
                String dateRange = etDateRange.getText().toString().trim();
                
                if (name.isEmpty() || dateRange.isEmpty()) {
                    Toast.makeText(this, "Name und Datum erforderlich", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                updateEvent(event, name, dateRange);
            })
            .setNegativeButton("Abbrechen", null)
            .show();
    }
    
    private void createEvent(String name, String dateRange) {
        String[] dates = parseDateRange(dateRange);
        if (dates == null) {
            Toast.makeText(this, "Ungültiges Datumsformat", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Event event = new Event(name, dates[0], dates[1]);
        
        eventRepository.createEvent(event)
            .thenAccept(created -> runOnUiThread(() -> {
                Toast.makeText(this, "Event erstellt", Toast.LENGTH_SHORT).show();
                loadEvents(); // Refresh
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
    
    private void updateEvent(Event event, String name, String dateRange) {
        String[] dates = parseDateRange(dateRange);
        if (dates == null) {
            Toast.makeText(this, "Ungültiges Datumsformat", Toast.LENGTH_SHORT).show();
            return;
        }
        
        event.setName(name);
        event.setStartDate(dates[0]);
        event.setEndDate(dates[1]);
        
        eventRepository.updateEvent(event)
            .thenAccept(updated -> runOnUiThread(() -> {
                Toast.makeText(this, "Event aktualisiert", Toast.LENGTH_SHORT).show();
                loadEvents(); // Refresh
            }))
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "Fehler beim Aktualisieren: " + throwable.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    private void openEventDetail(Event event) {
        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.putExtra("event_id", event.getId());
        startActivity(intent);
    }
    
    /**
     * Parse date range "01.02 bis 05.02" to ISO format ["2024-02-01", "2024-02-05"]
     */
    private String[] parseDateRange(String dateRange) {
        try {
            // Extract dates from "01.02 bis 05.02" format
            String[] parts = dateRange.toLowerCase().split("\\s*bis\\s*");
            if (parts.length != 2) return null;
            
            String startStr = parts[0].trim();
            String endStr = parts[1].trim();
            
            // Get current year
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            
            // Parse DD.MM format and add current year
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd.MM", Locale.GERMAN);
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN);
            
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(inputFormat.parse(startStr));
            startCal.set(Calendar.YEAR, currentYear);
            
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(inputFormat.parse(endStr));
            endCal.set(Calendar.YEAR, currentYear);
            
            // If end date is before start date, assume it's in the next year
            if (endCal.before(startCal)) {
                endCal.set(Calendar.YEAR, currentYear + 1);
            }
            
            String startDate = outputFormat.format(startCal.getTime());
            String endDate = outputFormat.format(endCal.getTime());
            
            return new String[]{startDate, endDate};
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadEvents(); // Refresh on return
    }
}
