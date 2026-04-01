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
 * Activity for inviting specific entrants to a private event's waiting list
 * and assigning top-level system entrants as co-organizers.
 * Supports User Stories: US 02.01.03 (Invite to Waiting List) and US 02.09.01 (Co-organizer Assignment).
 */
public class InviteGuestsActivity extends AppCompatActivity implements GuestInviteAdapter.OnGuestActionListener {

    private String eventId;
    private String eventName;
    private FirebaseFirestore db;
    private NotificationRepository notificationRepository;
    private EditText searchInput;
    private Button btnFilterName, btnFilterEmail, btnFilterPhone;
    private RecyclerView guestsRecyclerView;
    private GuestInviteAdapter adapter;

    private List<DocumentSnapshot> allUsers = new ArrayList<>();
    private String currentFilter = "name"; // "name", "email", "phone"

    /**
     * Initializes the activity, establishes the view hierarchy, and loads all system users
     * into memory for real-time search filtering.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_guests);

        eventId = getIntent().getStringExtra("EVENT_ID");
        eventName = getIntent().getStringExtra("EVENT_NAME");
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();

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

    /**
     * Updates the active search filter flag and toggles the aesthetic selected states
     * on the filter buttons. This dynamically changes what fields the search matches against.
     *
     * @param filter The filter category chosen (e.g., "name", "email", or "phone").
     */
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

    /**
     * Performs an asynchronous Firestore query to load every single registered user pattern.
     * Binds the initial empty query state after loading to prevent race conditions during typing.
     */
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

    /**
     * Filters the cached list of users against the textual search query.
     * Executes dynamically in real-time. Matches target strings via the active filter mode.
     *
     * @param query The input string from the search bar to evaluate.
     */
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
     * Invites the requested user directly to the event's localized waiting list securely.
     * Conducts deep cross-validation to prevent inviting already-enrolled participants.
     *
     * @param userDoc A Firestore DocumentSnapshot containing the exact user's data object.
     */
    @Override
    public void onInvite(DocumentSnapshot userDoc) {
        String targetDeviceId = userDoc.getId();
        if (targetDeviceId == null || targetDeviceId.isEmpty()) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if already selected (already invited and pending response)
        db.collection("events").document(eventId).collection("selected")
                .document(targetDeviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Toast.makeText(this, "Already invited for this event", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check enrolled to prevent duplicate acceptance states.
                    db.collection("events").document(eventId).collection("enrolled")
                            .document(targetDeviceId).get()
                            .addOnSuccessListener(enrolledDoc -> {
                                if (enrolledDoc.exists()) {
                                    Toast.makeText(this, "Already enrolled in this event", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Re-invite flow: move user to selected, remove stale waiting/cancelled docs.
                                Map<String, Object> data = new HashMap<>();
                                data.put("deviceId", targetDeviceId);
                                String name = userDoc.getString("name");
                                String email = userDoc.getString("email");
                                if (name != null) data.put("name", name);
                                if (email != null) data.put("email", email);
                                data.put("invitedAt", FieldValue.serverTimestamp());
                                data.put("inviteSource", "organizer");

                                WriteBatch batch = db.batch();
                                batch.set(db.collection("events").document(eventId)
                                        .collection("selected").document(targetDeviceId), data);
                                batch.delete(db.collection("events").document(eventId)
                                        .collection("waitingList").document(targetDeviceId));
                                batch.delete(db.collection("events").document(eventId)
                                        .collection("cancelled").document(targetDeviceId));

                                batch.commit()
                                        .addOnSuccessListener(aVoid -> {
                                            String safeEventName = (eventName != null && !eventName.trim().isEmpty()) ? eventName : "Event";
                                            Notification notification = new Notification(
                                                    "You are invited!",
                                                    "Tap to accept or decline your invitation to " + safeEventName + ".",
                                                    safeEventName,
                                                    eventId
                                            );
                                            notificationRepository.addNotification(targetDeviceId, notification);
                                            Toast.makeText(this, (name != null ? name : "User") + " invited successfully", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Failed to invite: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error checking invite status", Toast.LENGTH_SHORT).show());
    }
    /**
     * Grants the target user full co-organizer permissions by atomically adding their ID to
     * the event document array via a Firestore Batch Write. Also removes them implicitly from the waiting list.
     *
     * @param userDoc The Firestore DocumentSnapshot of the user to upgrade.
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

                    // Batched write: add co-organizer + clear entrant-role subcollections for this user
                    WriteBatch batch = db.batch();

                    batch.update(db.collection("events").document(eventId),
                            "coOrganizers", FieldValue.arrayUnion(targetDeviceId));

                    batch.delete(db.collection("events").document(eventId)
                            .collection("waitingList").document(targetDeviceId));
                    batch.delete(db.collection("events").document(eventId)
                            .collection("selected").document(targetDeviceId));
                    batch.delete(db.collection("events").document(eventId)
                            .collection("cancelled").document(targetDeviceId));

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                String safeEventName = (eventName != null && !eventName.trim().isEmpty())
                                        ? eventName.trim() : "Event";
                                Notification organizerNotification = new Notification(
                                        "You are a co-organizer",
                                        "You now have organizer access to " + safeEventName
                                                + ". Open My Events to manage it.",
                                        safeEventName
                                );
                                notificationRepository.addNotification(targetDeviceId, organizerNotification);
                                Toast.makeText(this,
                                        (targetName != null ? targetName : "User") + " assigned as co-organizer",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Failed to assign co-organizer: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }
}
