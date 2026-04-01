package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.adapter.HomeEventAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Full-screen browse of all events. Opened from "See All" on the home screen.
 * US 01.01.04 — Filter by availability and capacity.
 * US 01.01.06 — Keyword search with filtering.
 */
public class BrowseEventsActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "browse_title";

    private RecyclerView rvEvents;
    private HomeEventAdapter adapter;
    private FirebaseRepository repo;

    private List<DocumentSnapshot> allEvents = new ArrayList<>();
    private String currentQuery = "";
    private boolean filterOpenReg = false;
    private boolean filterHasCapacity = false;

    private Button btnFilterOpen, btnFilterCapacity;

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

        // Search bar
        EditText etSearch = findViewById(R.id.et_browse_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentQuery = s != null ? s.toString().trim() : "";
                applyFilters();
            }
        });

        // Filter toggles
        btnFilterOpen = findViewById(R.id.btn_filter_open);
        btnFilterCapacity = findViewById(R.id.btn_filter_capacity);

        btnFilterOpen.setOnClickListener(v -> {
            filterOpenReg = !filterOpenReg;
            btnFilterOpen.setBackgroundResource(
                    filterOpenReg ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected);
            applyFilters();
        });

        btnFilterCapacity.setOnClickListener(v -> {
            filterHasCapacity = !filterHasCapacity;
            btnFilterCapacity.setBackgroundResource(
                    filterHasCapacity ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected);
            applyFilters();
        });

        // Load events
        repo = FirebaseRepository.getInstance();
        repo.fetchAllActiveEvents(new FirebaseRepository.OnDocumentsLoadedListener() {
            @Override
            public void onLoaded(List<DocumentSnapshot> docs) {
                allEvents = docs;
                applyFilters();
            }
            @Override
            public void onError(Exception e) { }
        });
    }

    /**
     * Applies keyword search + filter toggles together (intersection).
     * US 01.01.04 + US 01.01.06
     */
    private void applyFilters() {
        String q = currentQuery.toLowerCase();
        Date now = new Date();
        List<DocumentSnapshot> filtered = new ArrayList<>();

        for (DocumentSnapshot doc : allEvents) {
            // Keyword search: match name or description
            if (!q.isEmpty()) {
                String name = doc.getString("name");
                String desc = doc.getString("description");
                boolean nameMatch = name != null && name.toLowerCase().contains(q);
                boolean descMatch = desc != null && desc.toLowerCase().contains(q);
                if (!nameMatch && !descMatch) continue;
            }

            // Filter: open registration
            if (filterOpenReg) {
                Timestamp regStart = doc.getTimestamp("registrationStart");
                Timestamp regEnd = doc.getTimestamp("registrationEnd");
                boolean hasWindow = regStart != null || regEnd != null;
                if (!hasWindow) continue; // No registration window set — skip
                if (regStart != null && now.before(regStart.toDate())) continue; // Not open yet
                if (regEnd != null && now.after(regEnd.toDate())) continue; // Already closed
            }

            // Filter: has capacity (uses capacity field only — no subcollection query)
            if (filterHasCapacity) {
                Long capacity = doc.getLong("capacity");
                if (capacity != null && capacity <= 0) continue; // Zero capacity — skip
                // If capacity field is absent or > 0, we assume there's room
            }

            filtered.add(doc);
        }

        adapter.updateList(filtered);
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
