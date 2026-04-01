package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventmanager.adapters.EventCommentAdapter;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.EventComment;
import com.example.eventmanager.repository.FollowRepository;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity spanning comprehensive event interaction including viewing details,
 * engaging with the waiting list, reviewing lottery guidelines, and browsing live comments.
 * Supports User Stories: US 01.01.01 (View Details), US 01.01.02 (Leave Waitlist), US 01.05.05 (Guidelines).
 */
public class EventDetailsActivity extends AppCompatActivity {

    private String eventId;
    private String deviceId;
    private FirebaseFirestore db;
    private TextView tvEventName, tvEventDate, tvEventLocation;
    private TextView tvEventDescription, tvOrganizerName;
    private TextView tvGoingCount;
    private TextView tvLotteryGuidelines;
    private Button btnJoinWaitlist;
    private ImageView ivPoster;
    private EditText commentInput;
    private ImageButton sendCommentButton;
    private RecyclerView commentsRecyclerView;
    private TextView tvCommentsEmpty;
    private EventCommentAdapter commentAdapter;
    private ListenerRegistration commentsListener;
    private boolean alreadyJoined = false;
    private boolean isOnWaitingList = false;
    private Date registrationStart;
    private Date registrationEnd;
    private int eventCapacity = 0;
    private int eventMaxWaitlist = 0;

    private Button followButton;
    private LinearLayout organizerRow;
    private TextView organizerAvatarInitial;
    private FollowRepository followRepo;
    private String organizerId;
    private boolean isFollowing = false;

    /**
     * Bootstraps the visual layout and maps intent parameters to Firestore data loads.
     * Hooks all button interactions and listeners.
     *
     * @param savedInstanceState Persisted bundle state during configuration changes.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("EVENT_ID");
        deviceId = new DeviceAuthManager().getDeviceId(this);

        // Views
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.eventDetailsToolbar);
        ivPoster = findViewById(R.id.eventPosterImage);
        tvEventName = findViewById(R.id.eventTitleText);
        tvEventDate = findViewById(R.id.eventDateText);
        tvEventLocation = findViewById(R.id.eventLocationText);
        tvEventDescription = findViewById(R.id.aboutEventText);
        tvOrganizerName = findViewById(R.id.eventOrganizerText);
        tvGoingCount = findViewById(R.id.tvGoingCount);
        btnJoinWaitlist = findViewById(R.id.leaveWaitlistButton);
        tvLotteryGuidelines = findViewById(R.id.tvLotteryGuidelines);
        commentInput = findViewById(R.id.commentInput);
        sendCommentButton = findViewById(R.id.sendCommentButton);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        tvCommentsEmpty = findViewById(R.id.tvCommentsEmpty);
        if (commentsRecyclerView != null) {
            commentAdapter = new EventCommentAdapter();
            commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            commentsRecyclerView.setNestedScrollingEnabled(false);
            commentsRecyclerView.setAdapter(commentAdapter);
        }
        if (sendCommentButton != null) {
            sendCommentButton.setOnClickListener(v -> submitComment());
        }

        // Back button through toolbar navigation icon
        toolbar.setNavigationOnClickListener(v -> finish());

        followRepo = new FollowRepository();
        followButton = findViewById(R.id.followButton);
        organizerRow = findViewById(R.id.organizerRow);
        organizerAvatarInitial = findViewById(R.id.organizerAvatarInitial);

        if (followButton != null) {
            followButton.setOnClickListener(v -> toggleFollow());
        }
        if (organizerRow != null) {
            organizerRow.setOnClickListener(v -> openOrganizerProfile());
        }

        if (btnJoinWaitlist != null) {
            btnJoinWaitlist.setOnClickListener(v -> onWaitlistButtonClicked());
            setJoinButtonState("LOADING...", false, R.drawable.bg_join_button);
        }

        if (eventId != null) {
            loadEventDetails();
            checkIfAlreadyJoined();
            attachCommentsListener();
        } else {
            Toast.makeText(this, "Missing event information", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Executes the primary remote query fetching the overarching event document data.
     * Configures visuals such as the poster, description string, date layout, and guidelines.
     */
    private void loadEventDetails() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Name
                    String name = doc.getString("name");
                    tvEventName.setText(name != null ? name : "Event");

                    // Description
                    String desc = doc.getString("description");
                    tvEventDescription.setText(desc != null && !desc.isEmpty() ? desc :
                            "Experience this amazing event! Join the waiting list to secure your spot.");

                    // Registration date and time window
                    Timestamp regStart = doc.getTimestamp("registrationStart");
                    Timestamp regEnd = doc.getTimestamp("registrationEnd");
                    registrationStart = regStart != null ? regStart.toDate() : null;
                    registrationEnd = regEnd != null ? regEnd.toDate() : null;
                    if (registrationStart != null) {
                        tvEventDate.setText(new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
                                .format(registrationStart));
                    } else {
                        tvEventDate.setText("Date TBA");
                    }

                    // Capacity fields for guidelines
                    Long cap = doc.getLong("capacity");
                    Long maxWl = doc.getLong("maxWaitlistCapacity");
                    eventCapacity = cap != null ? cap.intValue() : 0;
                    eventMaxWaitlist = maxWl != null ? maxWl.intValue() : 0;

                    // US 01.05.05 — Dynamic lottery guidelines
                    updateLotteryGuidelines();

                    // Location
                    String location = doc.getString("location");
                    tvEventLocation.setText(location != null && !location.trim().isEmpty()
                            ? location
                            : "University Of Alberta");

                    // Poster
                    String posterUrl = doc.getString("posterUrl");
                    if (posterUrl != null && !posterUrl.isEmpty()) {
                        Glide.with(this)
                                .load(posterUrl)
                                .placeholder(R.drawable.ic_event_placeholder)
                                .centerCrop()
                                .into(ivPoster);
                    } else {
                        ivPoster.setImageResource(R.drawable.ic_event_placeholder);
                    }

                    // Organizer
                    String orgId = doc.getString("organizerId");
                    if (orgId != null) {
                        loadOrganizerName(orgId);
                    } else {
                        tvOrganizerName.setText("Organizer");
                    }

                    // Going count
                    loadGoingCount();
                    updateJoinButtonForRegistrationWindow();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Asynchronously discovers and loads the exact semantic name of the underlying organizer.
     *
     * @param organizerId The device ID of the user hosting this specific event.
     */
    private void loadOrganizerName(String orgId) {
        this.organizerId = orgId;
        db.collection("users").document(orgId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        tvOrganizerName.setText(name);
                        if (organizerAvatarInitial != null) {
                            organizerAvatarInitial.setText(
                                    String.valueOf(Character.toUpperCase(name.charAt(0))));
                        }
                    } else {
                        tvOrganizerName.setText("Organizer");
                    }
                });
        checkFollowState(orgId);
    }

    private void checkFollowState(String orgId) {
        if (followRepo == null || followButton == null) return;
        followRepo.isFollowing(deviceId, orgId, following -> {
            isFollowing = following;
            runOnUiThread(() -> updateFollowButtonUI());
        });
    }

    private void updateFollowButtonUI() {
        if (followButton == null) return;
        if (isFollowing) {
            followButton.setText(R.string.following);
            followButton.setBackgroundResource(R.drawable.bg_following_button);
            followButton.setTextColor(getResources().getColor(R.color.following_button_text, null));
        } else {
            followButton.setText(R.string.follow);
            followButton.setBackgroundResource(R.drawable.bg_follow_button_active);
            followButton.setTextColor(getResources().getColor(android.R.color.white, null));
        }
    }

    private void toggleFollow() {
        if (organizerId == null || followRepo == null) return;
        followButton.setEnabled(false);
        String entrantName = "Entrant";
        String orgName = tvOrganizerName.getText() != null ? tvOrganizerName.getText().toString() : "Organizer";

        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(doc -> {
                    String myName = doc.getString("name");
                    String finalName = (myName != null && !myName.isEmpty()) ? myName : "Entrant";

                    followRepo.toggleFollow(deviceId, finalName, organizerId, orgName,
                            new FollowRepository.FollowCallback() {
                                @Override
                                public void onSuccess() {
                                    runOnUiThread(() -> {
                                        isFollowing = !isFollowing;
                                        updateFollowButtonUI();
                                        followButton.setEnabled(true);
                                        Toast.makeText(EventDetailsActivity.this,
                                                isFollowing ? getString(R.string.follow_success)
                                                        : getString(R.string.unfollow_success),
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onFailure(@androidx.annotation.NonNull String message) {
                                    runOnUiThread(() -> {
                                        followButton.setEnabled(true);
                                        Toast.makeText(EventDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    followButton.setEnabled(true);
                    Toast.makeText(this, "Could not load your profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void openOrganizerProfile() {
        if (organizerId == null) return;
        Intent intent = new Intent(this, OrganizerProfileActivity.class);
        intent.putExtra("ORGANIZER_ID", organizerId);
        startActivity(intent);
    }

    /**
     * Ascertains the aggregated sum of entrants populating the waiting list sub-collection.
     * Evaluates independently and attaches result strings into the UI counter.
     */
    private void loadGoingCount() {
        if (eventId == null || tvGoingCount == null) return;
        db.collection("events").document(eventId).collection("waitingList").get()
                .addOnSuccessListener(qs -> {
                    int count = qs.size();
                    tvGoingCount.setText("CURRENTLY IN POOL: " + count + " ENTRANTS");
                })
                .addOnFailureListener(e -> {
                    // Keep existing text; optionally surface a small hint.
                });
    }

    /**
     * Performs synchronized multi-query validation to ensure button states reflect
     * correct contexts (e.g. Joined, Enrolled, Selected, Leaves).
     */
    private void checkIfAlreadyJoined() {
        db.collection("events").document(eventId).collection("waitingList")
                .document(deviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        alreadyJoined = true;
                        isOnWaitingList = true;
                        setJoinButtonState("LEAVE WAITING LIST", true, R.drawable.bg_primary_button);
                    }
                });

        // Also check enrolled
        db.collection("events").document(eventId).collection("enrolled")
                .document(deviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        alreadyJoined = true;
                        isOnWaitingList = false;
                        setJoinButtonState("ENROLLED", false, R.drawable.bg_joined_button);
                    }
                });

        db.collection("events").document(eventId).collection("selected")
                .document(deviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        alreadyJoined = true;
                        isOnWaitingList = false;
                        setJoinButtonState("INVITED", false, R.drawable.bg_joined_button);
                    }
                });
    }

    /**
     * Handles the waitlist button click — delegates to join or leave based on current state.
     */
    private void onWaitlistButtonClicked() {
        if (isOnWaitingList) {
            leaveWaitingList();
        } else {
            joinWaitingList();
        }
    }

    /**
     * US 01.01.02 — Removes the current entrant from the event's waiting list.
     */
    private void leaveWaitingList() {
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        setJoinButtonState("LEAVING...", false, R.drawable.bg_join_button);

        db.collection("events").document(eventId).collection("waitingList")
                .document(deviceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    alreadyJoined = false;
                    isOnWaitingList = false;
                    Toast.makeText(this, "You left the waiting list.", Toast.LENGTH_SHORT).show();
                    loadGoingCount();
                    updateJoinButtonForRegistrationWindow();
                })
                .addOnFailureListener(e -> {
                    // Restore leave button on failure
                    setJoinButtonState("LEAVE WAITING LIST", true, R.drawable.bg_primary_button);
                    Toast.makeText(this, "Failed to leave: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Executes robust pre-condition logic asserting the legitimacy of a user joining
     * the event pool. Handles max waitlist scenarios and missing requirements.
     */
    private void joinWaitingList() {
        if (eventId == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (alreadyJoined) {
            Toast.makeText(this, "You have already joined this event.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isRegistrationOpen()) {
            Toast.makeText(this, buildRegistrationClosedMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        setJoinButtonState("JOINING...", false, R.drawable.bg_join_button);

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    Boolean geolocationRequired = eventDoc.getBoolean("geolocationRequired");
                    if (Boolean.TRUE.equals(geolocationRequired) || Boolean.TRUE.equals(eventDoc.getBoolean("isGeolocationRequired"))) {
                        setJoinButtonState("LOCATION REQUIRED", false, R.drawable.bg_join_button);
                        Toast.makeText(this, "This event requires geolocation verification before joining.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Long maxWaitlist = eventDoc.getLong("maxWaitlistCapacity");

                    if (maxWaitlist != null && maxWaitlist > 0) {
                        db.collection("events").document(eventId).collection("waitingList").get()
                                .addOnSuccessListener(qs -> {
                                    if (qs.size() >= maxWaitlist) {
                                        setJoinButtonState("WAITLIST FULL", false, R.drawable.bg_join_button);
                                        Toast.makeText(this, "Waiting list is full!", Toast.LENGTH_LONG).show();
                                    } else {
                                        executeJoin();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    setJoinButtonState("TRY AGAIN", true, R.drawable.bg_button_gradient_purple);
                                    Toast.makeText(this, "Failed to verify waitlist size.", Toast.LENGTH_LONG).show();
                                });
                    } else {
                        executeJoin();
                    }
                })
                .addOnFailureListener(e -> {
                    setJoinButtonState("TRY AGAIN", true, R.drawable.bg_button_gradient_purple);
                    Toast.makeText(this, "Failed to check event status.", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Carries out the definitive Firestore update attaching the user payload to the waitlist sub-collection.
     * Extracts and passes semantic textual identifiers for admin convenience.
     */
    private void executeJoin() {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("joinedAt", FieldValue.serverTimestamp());

        // Also fetch user name/email
        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");
                        String email = userDoc.getString("email");
                        if (name != null) data.put("name", name);
                        if (email != null) data.put("email", email);
                    }

                    db.collection("events").document(eventId)
                            .collection("waitingList").document(deviceId)
                            .set(data)
                            .addOnSuccessListener(aVoid -> {
                                alreadyJoined = true;
                                isOnWaitingList = true;
                                setJoinButtonState("LEAVE WAITING LIST", true, R.drawable.bg_primary_button);
                                Toast.makeText(this, "You joined the waiting list!", Toast.LENGTH_SHORT).show();
                                loadGoingCount();
                            })
                            .addOnFailureListener(e -> {
                                setJoinButtonState("TRY AGAIN", true, R.drawable.bg_button_gradient_purple);
                                Toast.makeText(this, "Failed to join: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                });
    }

    /**
     * Re-renders the core visual state of the waitlist button conditionally based upon
     * overarching temporal constraints matching the registration period boundaries.
     */
    private void updateJoinButtonForRegistrationWindow() {
        if (alreadyJoined) {
            return;
        }
        if (isRegistrationOpen()) {
            setJoinButtonState("JOIN WAITING LIST", true, R.drawable.bg_button_gradient_purple);
        } else {
            setJoinButtonState("REGISTRATION CLOSED", false, R.drawable.bg_join_button);
        }
    }

    /**
     * Extrapolates time metrics against the localized operating system clock to decide
     * if the event spans its designated availability window.
     *
     * @return TRUE if the current moment lands within the event configuration dates.
     */
    private boolean isRegistrationOpen() {
        Date now = new Date();
        if (registrationStart != null && now.before(registrationStart)) {
            return false;
        }
        if (registrationEnd != null && now.after(registrationEnd)) {
            return false;
        }
        return registrationStart != null || registrationEnd != null;
    }

    /**
     * Composes specialized textual rationale clarifying why a user is prevented
     * from engaging during periods of registration closure.
     *
     * @return Specific closure messaging matching the clock variance.
     */
    private String buildRegistrationClosedMessage() {
        Date now = new Date();
        if (registrationStart != null && now.before(registrationStart)) {
            return "Registration has not opened yet.";
        }
        if (registrationEnd != null && now.after(registrationEnd)) {
            return "Registration for this event has closed.";
        }
        return "Registration is not available right now.";
    }

    /**
     * Programmatic mutator enforcing consistent button theming via a direct resource switch.
     *
     * @param label         String descriptor inside the button face.
     * @param enabled       Whether click events are routed through the view.
     * @param backgroundRes Expected drawn background identifier.
     */
    private void setJoinButtonState(String label, boolean enabled, int backgroundRes) {
        if (btnJoinWaitlist == null) return;
        btnJoinWaitlist.setText(label);
        btnJoinWaitlist.setEnabled(enabled);
        btnJoinWaitlist.setClickable(enabled);
        btnJoinWaitlist.setAlpha(enabled ? 1f : 0.92f);
        btnJoinWaitlist.setBackgroundResource(backgroundRes);
    }

    /**
     * US 01.05.05 — Builds dynamic lottery criteria/guidelines text from event data.
     */
    private void updateLotteryGuidelines() {
        if (tvLotteryGuidelines == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("\u2022 Attendees are selected via random lottery from the waiting list.\n");
        sb.append("\u2022 Joining the waiting list does not guarantee entry.\n");

        if (eventCapacity > 0) {
            sb.append("\u2022 Event capacity: ").append(eventCapacity).append(" spots.\n");
        }
        if (eventMaxWaitlist > 0) {
            sb.append("\u2022 Waiting list limit: ").append(eventMaxWaitlist).append(" entrants.\n");
        }

        SimpleDateFormat fmt = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        if (registrationStart != null && registrationEnd != null) {
            sb.append("\u2022 Registration window: ")
              .append(fmt.format(registrationStart))
              .append(" \u2013 ")
              .append(fmt.format(registrationEnd)).append(".\n");
        } else if (registrationEnd != null) {
            sb.append("\u2022 Registration closes: ").append(fmt.format(registrationEnd)).append(".\n");
        }

        sb.append("\u2022 If a selected entrant declines, a replacement may be drawn.\n");
        sb.append("\u2022 Winners will be notified via the app.");

        tvLotteryGuidelines.setText(sb.toString());
    }

    /**
     * Triggers active realtime synchronization polling the "comments" node beneath the event graph.
     * Dispatches list parsing dynamically to update user impressions instantaneously.
     */
    private void attachCommentsListener() {
        if (eventId == null || commentsRecyclerView == null || commentAdapter == null) {
            return;
        }
        if (commentsListener != null) {
            return;
        }
        commentsListener = db.collection("events").document(eventId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) {
                        return;
                    }
                    List<EventComment> list = new ArrayList<>(snap.size());
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        EventComment c = EventComment.fromDocument(doc);
                        if (c != null) {
                            list.add(c);
                        }
                    }
                    commentAdapter.updateData(list);
                    if (tvCommentsEmpty != null) {
                        boolean empty = list.isEmpty();
                        tvCommentsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    }
                });
    }

    /**
     * Prepares and launches the structured data package capturing a textual review or inquiry
     * about the event, directly attaching it securely into the sub-collection tree.
     */
    private void submitComment() {
        if (eventId == null || commentInput == null) {
            return;
        }
        String text = commentInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Write a comment first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sendCommentButton != null) {
            sendCommentButton.setEnabled(false);
        }

        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(userDoc -> {
                    String authorName = "Entrant";
                    if (userDoc.exists()) {
                        String n = userDoc.getString("name");
                        if (n != null && !n.trim().isEmpty()) {
                            authorName = n.trim();
                        }
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("deviceId", deviceId);
                    data.put("authorName", authorName);
                    data.put("text", text);
                    data.put("timestamp", FieldValue.serverTimestamp());
                    data.put("eventId", eventId);
                    String eventTitle = tvEventName != null && tvEventName.getText() != null
                            ? tvEventName.getText().toString().trim() : "";
                    if (!eventTitle.isEmpty()) {
                        data.put("eventName", eventTitle);
                    }

                    db.collection("events").document(eventId).collection("comments").add(data)
                            .addOnSuccessListener(docRef -> {
                                commentInput.setText("");
                                if (sendCommentButton != null) {
                                    sendCommentButton.setEnabled(true);
                                }
                            })
                            .addOnFailureListener(err -> {
                                if (sendCommentButton != null) {
                                    sendCommentButton.setEnabled(true);
                                }
                                Toast.makeText(this, "Failed to post comment", Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(err -> {
                    if (sendCommentButton != null) {
                        sendCommentButton.setEnabled(true);
                    }
                    Toast.makeText(this, "Could not load profile for comment", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Shuts down UI processes cleanly and detaches realtime snapshot polling listeners,
     * maintaining high operational memory standards.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentsListener != null) {
            commentsListener.remove();
            commentsListener = null;
        }
    }
}
