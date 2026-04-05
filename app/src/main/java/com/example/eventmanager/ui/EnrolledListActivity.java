package com.example.eventmanager.ui;

import com.example.eventmanager.R;

import com.example.eventmanager.callbacks.EntrantListCallback;
import com.example.eventmanager.adapter.EnrolledEntrantAdapter;
import com.example.eventmanager.repository.FirebaseRepository;
import com.example.eventmanager.models.Event;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.adapter.EnrolledEntrantAdapter;
import com.example.eventmanager.models.Entrant;

import java.util.ArrayList;
import java.util.List;

public class EnrolledListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EnrolledEntrantAdapter adapter;
    private FirebaseRepository repository;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This links to the activity_enrolled_list.xml file we made earlier
        setContentView(R.layout.activity_enrolled_list);

        // 1. Initialize UI and Repository
        recyclerView = findViewById(R.id.recyclerViewEnrolled);
        repository = new FirebaseRepository();

        // 2. Set up the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EnrolledEntrantAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // 3. Get the Event ID passed from the previous screen (e.g., Organizer Dashboard)
        // For testing purposes, you can hardcode a string here if you don't have navigation set up yet
        eventId = getIntent().getStringExtra("EVENT_ID");

        if (eventId != null && !eventId.isEmpty()) {
            loadEnrolledEntrants();
        } else {
            Toast.makeText(this, "Error: No Event ID provided", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadEnrolledEntrants() {
        repository.getEnrolledEntrants(eventId, new EntrantListCallback() {
            @Override
            public void onSuccess(List<Entrant> entrants) {
                if (entrants.isEmpty()) {
                    Toast.makeText(EnrolledListActivity.this, "No entrants enrolled yet.", Toast.LENGTH_SHORT).show();
                } else {
                    // Feed the data to our adapter to display on the screen
                    adapter.updateData(entrants);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(EnrolledListActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}