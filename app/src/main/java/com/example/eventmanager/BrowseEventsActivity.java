package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.adapter.HomeEventAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen browse of all events. Opened from "See All" on the home screen.
 */
public class BrowseEventsActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "browse_title";

    private RecyclerView rvEvents;
    private HomeEventAdapter adapter;
    private FirebaseRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_events);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null) {
            ((TextView) findViewById(R.id.tv_title)).setText(title);
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rvEvents = findViewById(R.id.rv_events);
        rvEvents.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new HomeEventAdapter(this, new ArrayList<>());
        adapter.setOnEventClickListener(this::openEventDetails);
        rvEvents.setAdapter(adapter);

        repo = FirebaseRepository.getInstance();
        repo.fetchAllActiveEvents(new FirebaseRepository.OnDocumentsLoadedListener() {
            @Override
            public void onLoaded(List<DocumentSnapshot> docs) {
                adapter.updateList(docs);
            }
            @Override
            public void onError(Exception e) { }
        });
    }

    private void openEventDetails(DocumentSnapshot doc) {
        if (doc == null) return;
        String eventId = doc.getString("eventId");
        if (eventId == null || eventId.trim().isEmpty()) eventId = doc.getId();
        if (eventId == null || eventId.trim().isEmpty()) return;
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }
}
