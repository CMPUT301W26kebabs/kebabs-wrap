package com.example.eventmanager; // Ensure this matches your package name!

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MyEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private FirebaseRepository repository;
    private FloatingActionButton fabAddEvent;

    // We will use a mock device ID for testing until your DeviceAuthManager is built
    private final String MOCK_DEVICE_ID = "test-device-id-123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);

        repository = new FirebaseRepository();

        // Setup the Floating Action Button to open the Create Event screen
        fabAddEvent = findViewById(R.id.fab_add_event);
        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(MyEventsActivity.this, CreateEventActivity.class);
            startActivity(intent);
        });

        // Setup RecyclerView and Adapter
        recyclerView = findViewById(R.id.recycler_my_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(new ArrayList<>());
        recyclerView.setAdapter(eventAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fetch fresh data every time the user returns to this screen
        loadMyEvents();
    }

    private void loadMyEvents() {
        repository.getEventsByOrganizer(MOCK_DEVICE_ID,
                queryDocumentSnapshots -> {
                    List<Event> fetchedEvents = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            fetchedEvents.add(event);
                        }
                    }
                    // Send the new data to the adapter
                    eventAdapter.updateData(fetchedEvents);
                },
                e -> Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show()
        );
    }
}
