package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.adapter.HomeEventAdapter;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.ui.ProfileActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private RecyclerView rvUpcoming, rvNearby;
    private HomeEventAdapter upcomingAdapter, nearbyAdapter;
    private FirebaseRepository mainRepo;
    private EventRepository eventRepo;
    private View navEvents;
    private View lotteryBanner;
    private TextView tvLotteryEvent;
    private List<DocumentSnapshot> allEvents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        findViewById(R.id.btn_notifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        mainRepo = FirebaseRepository.getInstance();
        eventRepo = new EventRepository();
        tvWelcome = findViewById(R.id.tv_welcome);
        navEvents = findViewById(R.id.nav_events);
        rvUpcoming = findViewById(R.id.rv_upcoming_events);
        rvNearby = findViewById(R.id.rv_nearby_events);
        lotteryBanner = findViewById(R.id.lottery_banner);
        tvLotteryEvent = findViewById(R.id.tv_lottery_event);

        upcomingAdapter = new HomeEventAdapter(this, new ArrayList<>());
        rvUpcoming.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvUpcoming.setAdapter(upcomingAdapter);

        nearbyAdapter = new HomeEventAdapter(this, new ArrayList<>());
        rvNearby.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvNearby.setAdapter(nearbyAdapter);
        upcomingAdapter.setOnEventClickListener(this::openEventDetails);
        nearbyAdapter.setOnEventClickListener(this::openEventDetails);

        // Search
        EditText etSearch = findViewById(R.id.et_home_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                filterAndUpdateLists(s != null ? s.toString().trim() : "");
            }
        });

        // See All
        findViewById(R.id.see_all_upcoming).setOnClickListener(v -> openBrowse("Upcoming Events"));
        findViewById(R.id.see_all_nearby).setOnClickListener(v -> openBrowse("Nearby You"));

        // Filters
        findViewById(R.id.btn_filters).setOnClickListener(v ->
                Toast.makeText(this, "Filter by date or location coming soon", Toast.LENGTH_SHORT).show());

        // Lottery banner
        lotteryBanner.setOnClickListener(v -> openPendingInvite());

        // Admin shortcut in top-left
        findViewById(R.id.btn_admin_home).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });

        // Bottom nav
        findViewById(R.id.nav_explore).setOnClickListener(v -> { /* already here */ });

        navEvents.setOnClickListener(v -> {
            startActivity(new Intent(this, MyEventsActivity.class));
        });

        // QR Scanner - opens camera
        findViewById(R.id.nav_scan).setOnClickListener(v -> {
            startActivity(new Intent(this, QRScannerActivity.class));
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
        findViewById(R.id.btn_create_event).setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));

        loadUserName();
        loadEvents();
        loadPendingInvites();
    }

    private void openBrowse(String title) {
        Intent intent = new Intent(this, BrowseEventsActivity.class);
        intent.putExtra(BrowseEventsActivity.EXTRA_TITLE, title);
        startActivity(intent);
    }

    private EventRepository.PendingInvite pendingInvite;

    private void loadPendingInvites() {
        String deviceId = new DeviceAuthManager().getDeviceId(this);
        eventRepo.getEventsWhereUserIsSelected(deviceId, invites -> {
            runOnUiThread(() -> {
                if (invites != null && !invites.isEmpty()) {
                    pendingInvite = invites.get(0);
                    lotteryBanner.setVisibility(View.VISIBLE);
                    tvLotteryEvent.setText(pendingInvite.eventName + " - Tap to respond");
                } else {
                    pendingInvite = null;
                    lotteryBanner.setVisibility(View.GONE);
                }
            });
        });
    }

    private void openPendingInvite() {
        if (pendingInvite == null) return;
        Intent intent = new Intent(this, AcceptDeclineActivity.class);
        intent.putExtra("eventId", pendingInvite.eventId);
        intent.putExtra("eventName", pendingInvite.eventName);
        intent.putExtra("deviceId", new DeviceAuthManager().getDeviceId(this));
        startActivity(intent);
    }

    private void loadUserName() {
        String deviceId = new DeviceAuthManager().getDeviceId(this);
        com.example.eventmanager.repository.FirebaseRepository repo = new com.example.eventmanager.repository.FirebaseRepository();
        repo.getUser(deviceId, new com.example.eventmanager.repository.FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                if (result != null && result.getName() != null) {
                    tvWelcome.setText("Welcome back " + result.getName() + " \uD83D\uDC4B,");
                } else {
                    tvWelcome.setText("Welcome \uD83D\uDC4B,");
                }
            }
            @Override
            public void onError(Exception e) { tvWelcome.setText("Welcome \uD83D\uDC4B,"); }
        });
    }

    private void loadEvents() {
        mainRepo.fetchAllActiveEvents(new FirebaseRepository.OnDocumentsLoadedListener() {
            @Override
            public void onLoaded(List<DocumentSnapshot> docs) {
                allEvents = docs != null ? docs : new ArrayList<>();
                sortByDate(allEvents);
                filterAndUpdateLists("");
            }
            @Override
            public void onError(Exception e) { }
        });
    }

    private void sortByDate(List<DocumentSnapshot> docs) {
        Collections.sort(docs, (a, b) -> {
            Timestamp ta = a.getTimestamp("registrationStart");
            Timestamp tb = b.getTimestamp("registrationStart");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return ta.compareTo(tb);
        });
    }

    private void filterAndUpdateLists(String query) {
        List<DocumentSnapshot> filtered = new ArrayList<>();
        String q = query.toLowerCase();
        for (DocumentSnapshot doc : allEvents) {
            if (q.isEmpty()) {
                filtered.add(doc);
            } else {
                String name = doc.getString("name");
                String loc = doc.getString("location");
                if ((name != null && name.toLowerCase().contains(q))
                        || (loc != null && loc.toLowerCase().contains(q))) {
                    filtered.add(doc);
                }
            }
        }
        upcomingAdapter.updateList(filtered);
        nearbyAdapter.updateList(filtered);
    }

    private void openEventDetails(DocumentSnapshot doc) {
        if (doc == null) {
            return;
        }

        String eventId = doc.getString("eventId");
        if (eventId == null || eventId.trim().isEmpty()) {
            eventId = doc.getId();
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Unable to open this event.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }
}
