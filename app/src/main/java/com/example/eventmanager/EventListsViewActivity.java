package com.example.eventmanager;

import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

/**
 * US2 & US3: Organizer-facing screen for managing event entrant lists.
 * Displays two tabs:
 *   - Waiting List (US2): all entrants currently on the waitlist
 *   - Chosen List  (US3): all entrants selected by the lottery
 * Uses a real-time Firestore listener so counts and lists update automatically.
 */
public class EventListsViewActivity extends AppCompatActivity {

    private FirebaseRepository repository;
    private ListenerRegistration eventListener;
    private String eventId;

    // Waiting list tab views
    private RecyclerView rvWaitingList;
    private TextView tvWaitingCount;
    private TextView tvWaitingEmpty;
    private EntrantAdapter waitingAdapter;

    // Chosen list tab views
    private RecyclerView rvChosenList;
    private TextView tvChosenCount;
    private TextView tvChosenEmpty;
    private EntrantAdapter chosenAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.makeshift_lists_activity);

        repository = new FirebaseRepository();
        eventId = getIntent().getStringExtra("EVENT_ID");

        setupTabs();
        setupRecyclerViews();
        listenToEvent();
    }

    private void setupTabs() {
        TabHost tabHost = findViewById(R.id.tabHost);
        tabHost.setup();

        // Tab 1: Waiting List
        TabHost.TabSpec waitingTab = tabHost.newTabSpec("waiting");
        waitingTab.setIndicator("Waiting List");
        waitingTab.setContent(R.id.tabWaiting);
        tabHost.addTab(waitingTab);

        // Tab 2: Chosen List
        TabHost.TabSpec chosenTab = tabHost.newTabSpec("chosen");
        chosenTab.setIndicator("Chosen List");
        chosenTab.setContent(R.id.tabChosen);
        tabHost.addTab(chosenTab);
    }

    private void setupRecyclerViews() {
        // Waiting list RecyclerView
        rvWaitingList   = findViewById(R.id.rvWaitingList);
        tvWaitingCount  = findViewById(R.id.tvWaitingCount);
        tvWaitingEmpty  = findViewById(R.id.tvWaitingEmpty);
        waitingAdapter  = new EntrantAdapter(new ArrayList<>());
        rvWaitingList.setLayoutManager(new LinearLayoutManager(this));
        rvWaitingList.setAdapter(waitingAdapter);

        // Chosen list RecyclerView
        rvChosenList  = findViewById(R.id.rvChosenList);
        tvChosenCount = findViewById(R.id.tvChosenCount);
        tvChosenEmpty = findViewById(R.id.tvChosenEmpty);
        chosenAdapter = new EntrantAdapter(new ArrayList<>());
        rvChosenList.setLayoutManager(new LinearLayoutManager(this));
        rvChosenList.setAdapter(chosenAdapter);
    }

    /**
     * Attaches a real-time listener to the event document.
     * When the event updates, fetches full Entrant profiles for both lists
     * and refreshes the UI.
     */
    private void listenToEvent() {
        if (eventId == null) return;

        eventListener = repository.listenToEvent(eventId, event -> {

            // --- US2: Update Waiting List ---
            int waitingCount = event.getWaitingList().size();
            tvWaitingCount.setText("Waiting: " + waitingCount);

            if (waitingCount == 0) {
                tvWaitingEmpty.setVisibility(View.VISIBLE);
                rvWaitingList.setVisibility(View.GONE);
            } else {
                tvWaitingEmpty.setVisibility(View.GONE);
                rvWaitingList.setVisibility(View.VISIBLE);
                repository.getEntrantProfiles(event.getWaitingList())
                        .addOnSuccessListener(waitingAdapter::updateList);
            }

            // --- US3: Update Chosen List ---
            int chosenCount = event.getChosenList().size();
            tvChosenCount.setText("Chosen: " + chosenCount);

            if (chosenCount == 0) {
                tvChosenEmpty.setVisibility(View.VISIBLE);
                rvChosenList.setVisibility(View.GONE);
            } else {
                tvChosenEmpty.setVisibility(View.GONE);
                rvChosenList.setVisibility(View.VISIBLE);
                repository.getEntrantProfiles(event.getChosenList())
                        .addOnSuccessListener(chosenAdapter::updateList);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventListener != null) {
            eventListener.remove();
        }
    }
}
