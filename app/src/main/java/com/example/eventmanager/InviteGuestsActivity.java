package com.example.eventmanager;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.adapter.GuestInviteAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * US 02.01.03 — Invite specific entrants to a private event's waiting list.
 * US 02.09.01 — Assign an entrant as a co-organizer.
 */
public class InviteGuestsActivity extends AppCompatActivity implements GuestInviteAdapter.OnGuestActionListener {

    private String eventId;
    private FirebaseFirestore db;
    private EditText searchInput;
    private Button btnFilterName, btnFilterEmail, btnFilterPhone;
    private RecyclerView guestsRecyclerView;
    private GuestInviteAdapter adapter;

    private List<DocumentSnapshot> allUsers = new ArrayList<>();
    private String currentFilter = "name"; // "name", "email", "phone"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_guests);

        eventId = getIntent().getStringExtra("EVENT_ID");
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.inviteGuestsToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        if (eventId == null) {
            Toast.makeText(this, "Missing event information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        searchInput = findViewById(R.id.searchGuestInput);
        btnFilterName = findViewById(R.id.nameFilterButton);
        btnFilterEmail = findViewById(R.id.emailFilterButton);
        btnFilterPhone = findViewById(R.id.phoneFilterButton);
        guestsRecyclerView = findViewById(R.id.guestsRecyclerView);

        adapter = new GuestInviteAdapter(this);
        guestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        guestsRecyclerView.setAdapter(adapter);

        // Filter button toggles
        setActiveFilter("name");
        btnFilterName.setOnClickListener(v -> setActiveFilter("name"));
        btnFilterEmail.setOnClickListener(v -> setActiveFilter("email"));
        btnFilterPhone.setOnClickListener(v -> setActiveFilter("phone"));

        // Search text listener
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s != null ? s.toString().trim() : "";
                filterUsers(query);
            }
        });

        // Load all users once
        loadAllUsers();
    }

    private void setActiveFilter(String filter) {
        currentFilter = filter;
        btnFilterName.setBackgroundResource(
                "name".equals(filter) ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected);
        btnFilterEmail.setBackgroundResource(
                "email".equals(filter) ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected);
        btnFilterPhone.setBackgroundResource(
                "phone".equals(filter) ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected);

        // Re-filter with current search text
        if (searchInput != null) {
            String query = searchInput.getText().toString().trim();
            filterUsers(query);
        }
    }

    private void loadAllUsers() {
        db.collection("users").get()
                .addOnSuccessListener(qs -> {
                    allUsers = qs.getDocuments();
                    // If the user already typed something while loading, apply it immediately.
                    // Otherwise, show nothing until they type.
                    String query = searchInput != null ? searchInput.getText().toString().trim() : "";
                    if (!query.isEmpty()) {
                        filterUsers(query);
                    } else {
                        adapter.updateList(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show());
    }

    private void filterUsers(String query) {
        if (query.isEmpty()) {
            adapter.updateList(new ArrayList<>());
            return;
        }

        String q = query.toLowerCase();
        List<DocumentSnapshot> filtered = new ArrayList<>();
        for (DocumentSnapshot doc : allUsers) {
            String value = null;
            switch (currentFilter) {
                case "name":
                    value = doc.getString("name");
                    break;
                case "email":
                    value = doc.getString("email");
                    break;
                case "phone":
                    value = doc.getString("phoneNumber");
                    break;
            }
            if (value != null && value.toLowerCase().contains(q)) {
                filtered.add(doc);
            }
        }
        adapter.updateList(filtered);
    }

    /**
     * US 02.01.03 — Invite entrant to the event's waiting list.
     * Checks for duplicates before adding.
     */
    @Override
    public void onInvite(DocumentSnapshot userDoc) {
        String targetDeviceId = userDoc.getId();
        if (targetDeviceId == null || targetDeviceId.isEmpty()) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if already on waiting list
        db.collection("events").document(eventId).collection("waitingList")
                .document(targetDeviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Toast.makeText(this, "Already on the waiting list", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Also check enrolled & selected to prevent duplicates
                    db.collection("events").document(eventId).collection("enrolled")
                            .document(targetDeviceId).get()
                            .addOnSuccessListener(enrolledDoc -> {
                                if (enrolledDoc.exists()) {
                                    Toast.makeText(this, "Already enrolled in this event", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                db.collection("events").document(eventId).collection("selected")
                                        .document(targetDeviceId).get()
                                        .addOnSuccessListener(selectedDoc -> {
                                            if (selectedDoc.exists()) {
                                                Toast.makeText(this, "Already selected for this event", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            // Safe to add — build waitlist entry
                                            Map<String, Object> data = new HashMap<>();
                                            data.put("deviceId", targetDeviceId);
                                            String name = userDoc.getString("name");
                                            String email = userDoc.getString("email");
                                            if (name != null) data.put("name", name);
                                            if (email != null) data.put("email", email);
                                            data.put("joinedAt", FieldValue.serverTimestamp());

                                            db.collection("events").document(eventId)
                                                    .collection("waitingList").document(targetDeviceId)
                                                    .set(data)
                                                    .addOnSuccessListener(aVoid ->
                                                            Toast.makeText(this, (name != null ? name : "User") + " invited to waiting list", Toast.LENGTH_SHORT).show())
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(this, "Failed to invite: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                        });
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error checking waitlist", Toast.LENGTH_SHORT).show());
    }

    /**
     * US 02.09.01 — Assign entrant as co-organizer.
     * Uses batched write: adds to coOrganizers array AND removes from waitingList.
     */
    @Override
    public void onAssignCoOrganizer(DocumentSnapshot userDoc) {
        String targetDeviceId = userDoc.getId();
        String targetName = userDoc.getString("name");
        if (targetDeviceId == null || targetDeviceId.isEmpty()) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if already a co-organizer
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    if (!eventDoc.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> coOrganizers = (List<String>) eventDoc.get("coOrganizers");
                    if (coOrganizers != null && coOrganizers.contains(targetDeviceId)) {
                        Toast.makeText(this, (targetName != null ? targetName : "User") + " is already a co-organizer", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check if they are the main organizer
                    String organizerId = eventDoc.getString("organizerId");
                    if (targetDeviceId.equals(organizerId)) {
                        Toast.makeText(this, "This user is already the main organizer", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Batched write: add co-organizer + remove from waitingList
                    WriteBatch batch = db.batch();

                    // Add to coOrganizers array on event document
                    batch.update(db.collection("events").document(eventId),
                            "coOrganizers", FieldValue.arrayUnion(targetDeviceId));

                    // Remove from waiting list if present (safe even if not there)
                    batch.delete(db.collection("events").document(eventId)
                            .collection("waitingList").document(targetDeviceId));

                    batch.commit()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this,
                                            (targetName != null ? targetName : "User") + " assigned as co-organizer",
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Failed to assign co-organizer: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }
}
