package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.adapter.HomeEventAdapter;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.repository.FollowRepository;
import com.example.eventmanager.ui.ProfileActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private String currentSearchQuery = "";
    private boolean filterOpenReg = false;
    private boolean filterHasCapacity = false;
    private boolean filterFollowing = false;
    private ListenerRegistration eventsListener;
    private FollowRepository followRepo;
    private Set<String> followingOrganizerIds = new HashSet<>();
    private TextView chipAllEvents, chipFollowing, tvNoFollowing;

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
                currentSearchQuery = s != null ? s.toString().trim() : "";
                filterAndUpdateLists(currentSearchQuery);
            }
        });

        // See All
        findViewById(R.id.see_all_upcoming).setOnClickListener(v -> openBrowse("Upcoming Events"));
        findViewById(R.id.see_all_nearby).setOnClickListener(v -> openBrowse("Nearby You"));

        // Filters — US 01.01.04: dialog with Open Registration + Has Capacity
        findViewById(R.id.btn_filters).setOnClickListener(v -> showFilterDialog());

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

        followRepo = new FollowRepository();
        chipAllEvents = findViewById(R.id.chip_all_events);
        chipFollowing = findViewById(R.id.chip_following);
        tvNoFollowing = findViewById(R.id.tv_no_following);

        chipAllEvents.setOnClickListener(v -> {
            filterFollowing = false;
            updateChipUI();
            filterAndUpdateLists(currentSearchQuery);
        });
        chipFollowing.setOnClickListener(v -> {
            filterFollowing = true;
            updateChipUI();
            filterAndUpdateLists(currentSearchQuery);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserName();
        loadPendingInvites();
        loadFollowingList();
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachEventsListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }
    }

    private void loadFollowingList() {
        String devId = new DeviceAuthManager().getDeviceId(this);
        followRepo.getFollowingList(devId, organizerIds -> runOnUiThread(() -> {
            followingOrganizerIds = new HashSet<>(organizerIds);
            if (filterFollowing) {
                filterAndUpdateLists(currentSearchQuery);
            }
        }));
    }

    private void updateChipUI() {
        if (filterFollowing) {
            chipFollowing.setBackgroundResource(R.drawable.bg_chip_selected);
            chipFollowing.setTextColor(0xFFFFFFFF);
            chipAllEvents.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipAllEvents.setTextColor(0xFF4A43EC);
        } else {
            chipAllEvents.setBackgroundResource(R.drawable.bg_chip_selected);
            chipAllEvents.setTextColor(0xFFFFFFFF);
            chipFollowing.setBackgroundResource(R.drawable.bg_chip_unselected);
            chipFollowing.setTextColor(0xFF4A43EC);
        }
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

    private void attachEventsListener() {
        if (eventsListener != null) return;
        eventsListener = mainRepo.getDb().collection("events")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        return;
                    }
                    List<DocumentSnapshot> active = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Boolean isDeleted = doc.getBoolean("isDeleted");
                        if (isDeleted == null || !isDeleted) {
                            active.add(doc);
                        }
                    }
                    allEvents = active;
                    sortByDate(allEvents);
                    filterAndUpdateLists(currentSearchQuery);
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
        Date now = new Date();

        for (DocumentSnapshot doc : allEvents) {
            // Keyword search: match name, location, or description
            if (!q.isEmpty()) {
                String name = doc.getString("name");
                String loc = doc.getString("location");
                String desc = doc.getString("description");
                boolean match = (name != null && name.toLowerCase().contains(q))
                        || (loc != null && loc.toLowerCase().contains(q))
                        || (desc != null && desc.toLowerCase().contains(q));
                if (!match) continue;
            }

            // Following filter
            if (filterFollowing) {
                String orgId = doc.getString("organizerId");
                if (orgId == null || !followingOrganizerIds.contains(orgId)) continue;
            }

            // Filter: open registration
            if (filterOpenReg) {
                Timestamp regStart = doc.getTimestamp("registrationStart");
                Timestamp regEnd = doc.getTimestamp("registrationEnd");
                boolean hasWindow = regStart != null || regEnd != null;
                if (!hasWindow) continue;
                if (regStart != null && now.before(regStart.toDate())) continue;
                if (regEnd != null && now.after(regEnd.toDate())) continue;
            }

            // Filter: has capacity
            if (filterHasCapacity) {
                Long capacity = doc.getLong("capacity");
                if (capacity != null && capacity <= 0) continue;
            }

            filtered.add(doc);
        }
        upcomingAdapter.updateList(filtered);
        nearbyAdapter.updateList(filtered);

        if (tvNoFollowing != null) {
            boolean showEmpty = filterFollowing && filtered.isEmpty();
            tvNoFollowing.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * US 01.01.04 — Shows a filter dialog with checkboxes for Open Registration and Has Capacity.
     */
    private void showFilterDialog() {
        String[] filterLabels = {"Open Registration", "Has Capacity"};
        boolean[] checked = {filterOpenReg, filterHasCapacity};

        new AlertDialog.Builder(this)
                .setTitle("Filter Events")
                .setMultiChoiceItems(filterLabels, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    filterOpenReg = checked[0];
                    filterHasCapacity = checked[1];
                    filterAndUpdateLists(currentSearchQuery);
                })
                .setNegativeButton("Reset", (dialog, which) -> {
                    filterOpenReg = false;
                    filterHasCapacity = false;
                    filterAndUpdateLists(currentSearchQuery);
                })
                .show();
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
