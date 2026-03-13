package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.models.Entrant;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ManageEventActivity extends AppCompatActivity {

    private String eventId, eventName;
    private TextView tvEventTitle, tvSubtitle;
    private TextView tvWaitingCount, tvChosenCount, tvEnrolledCount;
    private RecyclerView rvAttendees;
    private Button btnRunLottery, btnNotify;
    private CardView cardWaiting, cardChosen, cardEnrolled;
    private FirebaseFirestore db;
    private String currentTab = "waiting";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_event);

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("EVENT_ID");
        eventName = getIntent().getStringExtra("EVENT_NAME");

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Header
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvEventTitle.setText(eventName != null ? eventName : "Event");
        tvSubtitle.setText("Manage Event");

        // Counts
        tvWaitingCount = findViewById(R.id.tvWaitingCount);
        tvChosenCount = findViewById(R.id.tvChosenCount);
        tvEnrolledCount = findViewById(R.id.tvEnrolledCount);

        // Cards (tabs)
        cardWaiting = findViewById(R.id.cardWaiting);
        cardChosen = findViewById(R.id.cardChosen);
        cardEnrolled = findViewById(R.id.cardEnrolled);

        // Attendee list
        rvAttendees = findViewById(R.id.rvAttendees);
        rvAttendees.setLayoutManager(new LinearLayoutManager(this));

        // Tab clicks
        cardWaiting.setOnClickListener(v -> { currentTab = "waiting"; loadAttendees(); highlightTab(); });
        cardChosen.setOnClickListener(v -> { currentTab = "selected"; loadAttendees(); highlightTab(); });
        cardEnrolled.setOnClickListener(v -> { currentTab = "enrolled"; loadAttendees(); highlightTab(); });

        // Run Lottery button
        btnRunLottery = findViewById(R.id.btnRunLottery);
        btnRunLottery.setOnClickListener(v -> {
            Intent intent = new Intent(this, RunLotteryActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("EVENT_NAME", eventName);
            startActivity(intent);
        });

        // Notify button
        btnNotify = findViewById(R.id.btnNotify);
        btnNotify.setOnClickListener(v ->
                Toast.makeText(this, "Notifications sent!", Toast.LENGTH_SHORT).show());

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

    private void loadCounts() {
        if (eventId == null) return;

        db.collection("events").document(eventId).collection("waitingList").get()
                .addOnSuccessListener(qs -> tvWaitingCount.setText(String.valueOf(qs.size())));

        db.collection("events").document(eventId).collection("selected").get()
                .addOnSuccessListener(qs -> tvChosenCount.setText(String.valueOf(qs.size())));

        db.collection("events").document(eventId).collection("enrolled").get()
                .addOnSuccessListener(qs -> tvEnrolledCount.setText(String.valueOf(qs.size())));
    }

    private void loadAttendees() {
        if (eventId == null) return;

        String collection = currentTab.equals("waiting") ? "waitingList" :
                currentTab.equals("selected") ? "selected" : "enrolled";

        db.collection("events").document(eventId).collection(collection).get()
                .addOnSuccessListener(qs -> {
                    List<AnasEntrant> entrants = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String deviceId = doc.getId();
                        AnasEntrant e = new AnasEntrant(deviceId,
                                name != null ? name : deviceId,
                                email != null ? email : "",
                                currentTab);
                        entrants.add(e);
                    }
                    EntrantAdapter adapter = new EntrantAdapter(entrants);
                    rvAttendees.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load attendees", Toast.LENGTH_SHORT).show());
    }

    private void highlightTab() {
        float activeAlpha = 1.0f;
        float inactiveAlpha = 0.5f;
        cardWaiting.setAlpha(currentTab.equals("waiting") ? activeAlpha : inactiveAlpha);
        cardChosen.setAlpha(currentTab.equals("selected") ? activeAlpha : inactiveAlpha);
        cardEnrolled.setAlpha(currentTab.equals("enrolled") ? activeAlpha : inactiveAlpha);
    }
}
