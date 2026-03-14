package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ManageEventActivity extends AppCompatActivity {

    private String eventId, eventName;
    private TextView tvEventTitle, tvSubtitle, tvActiveSectionLabel;
    private TextView tvWaitingCount, tvChosenCount, tvEnrolledCount;
    private TextView tvEmptyState, tvPreviewHint;
    private RecyclerView rvAttendees;
    private Button btnRunLottery, btnNotify;
    private CardView cardWaiting, cardChosen, cardEnrolled;
    private FirebaseFirestore db;
    private OrganizerNotificationManager organizerNotificationManager;
    private String currentTab = "waiting";
    private int eventCapacity = Integer.MAX_VALUE;
    private boolean previewWaitingMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_event);

        db = FirebaseFirestore.getInstance();
        organizerNotificationManager = new OrganizerNotificationManager();
        eventId = getIntent().getStringExtra("EVENT_ID");
        eventName = getIntent().getStringExtra("EVENT_NAME");

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event information.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnViewQr).setOnClickListener(v -> openEventQr());

        // Header
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvActiveSectionLabel = findViewById(R.id.tvActiveSectionLabel);
        tvEventTitle.setText(eventName != null ? eventName : "Event");
        tvSubtitle.setText("Manage Event");

        // Counts
        tvWaitingCount = findViewById(R.id.tvWaitingCount);
        tvChosenCount = findViewById(R.id.tvChosenCount);
        tvEnrolledCount = findViewById(R.id.tvEnrolledCount);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvPreviewHint = findViewById(R.id.tvPreviewHint);

        // Cards (tabs)
        cardWaiting = findViewById(R.id.cardWaiting);
        cardChosen = findViewById(R.id.cardChosen);
        cardEnrolled = findViewById(R.id.cardEnrolled);

        // Attendee list
        rvAttendees = findViewById(R.id.rvAttendees);
        rvAttendees.setLayoutManager(new LinearLayoutManager(this));
        rvAttendees.setHasFixedSize(false);

        // Tab clicks
        cardWaiting.setOnClickListener(v -> { currentTab = "waiting"; loadAttendees(); highlightTab(); });
        cardWaiting.setOnLongClickListener(v -> {
            previewWaitingMode = !previewWaitingMode;
            currentTab = "waiting";
            loadCounts();
            loadAttendees();
            highlightTab();
            Toast.makeText(this,
                    previewWaitingMode
                            ? "Sample waiting list preview enabled."
                            : "Sample waiting list preview disabled.",
                    Toast.LENGTH_SHORT).show();
            return true;
        });
        cardChosen.setOnClickListener(v -> { currentTab = "selected"; loadAttendees(); highlightTab(); });
        cardEnrolled.setOnClickListener(v -> { currentTab = "enrolled"; loadAttendees(); highlightTab(); });

        // Run Lottery button
        btnRunLottery = findViewById(R.id.btnRunLottery);
        btnRunLottery.setOnClickListener(v -> {
            Intent intent = new Intent(this, RunLotteryActivity.class);
            intent.putExtra(RunLotteryActivity.EXTRA_EVENT_ID, eventId);
            intent.putExtra(RunLotteryActivity.EXTRA_EVENT_NAME, eventName);
            intent.putExtra(RunLotteryActivity.EXTRA_CAPACITY, eventCapacity);
            startActivity(intent);
        });

        // Notify button
        btnNotify = findViewById(R.id.btnNotify);
        btnNotify.setOnClickListener(v -> showNotifyAudienceChooser());

        loadEventMeta();
        loadCounts();
        loadAttendees();
        highlightTab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCounts();
        loadAttendees();
    }

    private void loadEventMeta() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        return;
                    }
                    String loadedName = doc.getString("name");
                    if (!TextUtils.isEmpty(loadedName)) {
                        eventName = loadedName;
                        tvEventTitle.setText(loadedName);
                    }
                    Long cap = doc.getLong("capacity");
                    if (cap != null && cap > 0) {
                        eventCapacity = cap.intValue();
                    }
                });
    }

    private void loadCounts() {
        if (previewWaitingMode) {
            tvWaitingCount.setText(String.valueOf(getPreviewEntrants().size()));
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            if (!previewWaitingMode) {
                Toast.makeText(this, "This event is missing its database id.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (!previewWaitingMode) {
            db.collection("events").document(eventId).collection("waitingList").get()
                    .addOnSuccessListener(qs -> tvWaitingCount.setText(String.valueOf(qs.size())));
        }

        db.collection("events").document(eventId).collection("selected").get()
                .addOnSuccessListener(qs -> tvChosenCount.setText(String.valueOf(qs.size())));

        db.collection("events").document(eventId).collection("enrolled").get()
                .addOnSuccessListener(qs -> tvEnrolledCount.setText(String.valueOf(qs.size())));
    }

    private void loadAttendees() {
        if (previewWaitingMode && "waiting".equals(currentTab)) {
            rvAttendees.setAdapter(new EntrantAdapter(getPreviewEntrants()));
            hideEmptyState();
            return;
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            rvAttendees.setAdapter(new EntrantAdapter(new ArrayList<>()));
            showEmptyState("This event could not be loaded correctly.");
            return;
        }

        String collection = currentTab.equals("waiting") ? "waitingList" :
                currentTab.equals("selected") ? "selected" : "enrolled";

        db.collection("events").document(eventId).collection(collection).get()
                .addOnSuccessListener(qs -> {
                    List<AnasEntrant> attendeeRows = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String deviceId = doc.getString("deviceId");
                        if (deviceId == null || deviceId.trim().isEmpty()) {
                            deviceId = doc.getId();
                        }
                        AnasEntrant e = new AnasEntrant(deviceId,
                                name != null ? name : deviceId,
                                email != null ? email : "",
                                currentTab);
                        attendeeRows.add(e);
                    }
                    Collections.sort(attendeeRows, Comparator.comparing(
                            entrant -> entrant.getName() == null ? "" : entrant.getName().toLowerCase(Locale.getDefault())
                    ));
                    List<AnasEntrant> groupedEntrants = buildGroupedEntrants(attendeeRows);
                    EntrantAdapter adapter = new EntrantAdapter(groupedEntrants);
                    rvAttendees.setAdapter(adapter);
                    if (groupedEntrants.isEmpty()) {
                        showEmptyState(getEmptyMessage());
                    } else {
                        hideEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    showEmptyState("Failed to load people for this section.");
                    Toast.makeText(this, "Failed to load attendees", Toast.LENGTH_SHORT).show();
                });
    }

    private void highlightTab() {
        float activeAlpha = 1.0f;
        float inactiveAlpha = 0.5f;
        cardWaiting.setAlpha(currentTab.equals("waiting") ? activeAlpha : inactiveAlpha);
        cardChosen.setAlpha(currentTab.equals("selected") ? activeAlpha : inactiveAlpha);
        cardEnrolled.setAlpha(currentTab.equals("enrolled") ? activeAlpha : inactiveAlpha);
        tvActiveSectionLabel.setText(currentTab.equals("waiting")
                ? "Waiting List"
                : currentTab.equals("selected")
                ? "Chosen Entrants"
                : "Enrolled Entrants");
        tvPreviewHint.setVisibility(currentTab.equals("waiting") ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(String message) {
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
        rvAttendees.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        tvEmptyState.setVisibility(View.GONE);
        rvAttendees.setVisibility(View.VISIBLE);
    }

    private String getEmptyMessage() {
        if ("selected".equals(currentTab)) {
            return "No chosen entrants yet.";
        }
        if ("enrolled".equals(currentTab)) {
            return "No enrolled entrants yet.";
        }
        return "No one is on the waiting list yet.";
    }

    private List<AnasEntrant> buildGroupedEntrants(List<AnasEntrant> attendees) {
        List<AnasEntrant> grouped = new ArrayList<>();
        String currentHeader = "";
        for (AnasEntrant attendee : attendees) {
            String displayName = attendee.getName() == null ? "" : attendee.getName().trim();
            String letter = displayName.isEmpty()
                    ? "#"
                    : String.valueOf(Character.toUpperCase(displayName.charAt(0)));
            if (!letter.equals(currentHeader)) {
                currentHeader = letter;
                AnasEntrant header = new AnasEntrant("", letter, "", "header");
                header.setSectionHeader(true);
                grouped.add(header);
            }
            grouped.add(attendee);
        }
        return grouped;
    }

    private void showNotifyAudienceChooser() {
        String[] options = {"Waiting List", "Chosen", "Enrolled"};
        new AlertDialog.Builder(this)
                .setTitle("Send notification")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            organizerNotificationManager.notifyWaitingList(
                                    eventId,
                                    eventName != null ? eventName : "Event",
                                    createNotificationCallback("waiting list")
                            );
                            break;
                        case 1:
                            organizerNotificationManager.notifySelected(
                                    eventId,
                                    eventName != null ? eventName : "Event",
                                    createNotificationCallback("chosen entrants")
                            );
                            break;
                        default:
                            organizerNotificationManager.notifyEnrolled(
                                    eventId,
                                    eventName != null ? eventName : "Event",
                                    createNotificationCallback("enrolled entrants")
                            );
                            break;
                    }
                })
                .show();
    }

    private OrganizerNotificationManager.NotificationDispatchCallback createNotificationCallback(String audienceLabel) {
        return new OrganizerNotificationManager.NotificationDispatchCallback() {
            @Override
            public void onSuccess(int recipientCount) {
                Toast.makeText(ManageEventActivity.this,
                        "Notification sent to " + recipientCount + " " + audienceLabel + ".",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(ManageEventActivity.this, message, Toast.LENGTH_LONG).show();
            }
        };
    }

    private void openEventQr() {
        Intent intent = new Intent(this, EventQRActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("EVENT_NAME", eventName);
        startActivity(intent);
    }

    private List<AnasEntrant> getPreviewEntrants() {
        List<String> previewNames = Arrays.asList(
                "Alex Jim", "Adrian Oliveira", "Adnan Sajid", "Bao Shu",
                "Bella Ahmed", "Chris Wong", "Dina Qureshi", "Ethan Cole",
                "Fatima Noor", "Hassan Ali", "Ivy Chen", "Jason Park",
                "Kira Singh", "Liam Brown", "Maya Patel", "Noah Smith"
        );
        List<AnasEntrant> entrants = new ArrayList<>();
        for (int i = 0; i < previewNames.size(); i++) {
            String name = previewNames.get(i);
            entrants.add(new AnasEntrant(
                    "preview-" + i,
                    name,
                    name.toLowerCase().replace(" ", ".") + "@spotly.test",
                    "waiting"
            ));
        }
        return entrants;
    }
}
