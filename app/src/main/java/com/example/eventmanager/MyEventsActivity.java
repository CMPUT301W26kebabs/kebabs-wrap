package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.managers.DeviceAuthManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyEventsActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private FirebaseRepository repository;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);

        repository = FirebaseRepository.getInstance();
        deviceId = new DeviceAuthManager().getDeviceId(this);

        // Back button
        findViewById(R.id.btn_back_my_events).setOnClickListener(v -> finish());

        // FAB → Create Event
        findViewById(R.id.fab_add_event).setOnClickListener(v -> {
            startActivity(new Intent(this, CreateEventActivity.class));
        });

        rvEvents = findViewById(R.id.recycler_my_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyEvents();
    }

    private void loadMyEvents() {
        repository.getEventsForOrganizerDashboard(deviceId,
                documents -> {
                    List<Event> events = new ArrayList<>();
                    if (documents != null) {
                        for (DocumentSnapshot doc : documents) {
                            Boolean deleted = doc.getBoolean("isDeleted");
                            if (Boolean.TRUE.equals(deleted)) {
                                continue;
                            }
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
                                    event.setEventId(doc.getId());
                                }
                                event.setDeleted(Boolean.TRUE.equals(doc.getBoolean("isDeleted")));
                                events.add(event);
                            }
                        }
                    }
                    if (events.isEmpty()) {
                        rvEvents.setAdapter(null);
                        return;
                    }
                    EventAdapter adapter = new EventAdapter(events);
                    adapter.setOnItemClickListener(event -> {
                        Intent intent = new Intent(this, ManageEventActivity.class);
                        intent.putExtra("EVENT_ID", event.getEventId());
                        intent.putExtra("EVENT_NAME", event.getName());
                        startActivity(intent);
                    });
                    rvEvents.setAdapter(adapter);
                },
                e -> Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show()
        );
    }
}
