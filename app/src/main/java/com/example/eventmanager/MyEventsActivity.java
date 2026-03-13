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

        // FAB create event
        findViewById(R.id.fab_add_event).setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));

        rvEvents = findViewById(R.id.recycler_my_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        loadMyEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyEvents();
    }

    private void loadMyEvents() {
        repository.getEventsByOrganizer(deviceId,
                querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        List<Event> events = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) events.add(event);
                        }
                        EventAdapter adapter = new EventAdapter(events);
                        rvEvents.setAdapter(adapter);
                    }
                },
                e -> Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show()
        );
    }
}
