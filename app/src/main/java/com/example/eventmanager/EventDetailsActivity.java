package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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

public class EventDetailsActivity extends AppCompatActivity {

    private String eventId;
    private String deviceId;
    private FirebaseFirestore db;
    private TextView tvEventName, tvEventDate, tvEventLocation;
    private TextView tvEventDescription, tvOrganizerName;
    private Button btnJoinWaitlist;
    private ImageView ivPoster;
    private EditText commentInput;
    private ImageButton sendCommentButton;
    private RecyclerView commentsRecyclerView;
    private TextView tvCommentsEmpty;
    private EventCommentAdapter commentAdapter;
    private ListenerRegistration commentsListener;
    private boolean alreadyJoined = false;
    private Date registrationStart;
    private Date registrationEnd;

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
        btnJoinWaitlist = findViewById(R.id.leaveWaitlistButton);
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

        // Follow button from the new UI
        View followButton = findViewById(R.id.followButton);
        if (followButton != null) {
            followButton.setOnClickListener(v ->
                    Toast.makeText(this, "Follow coming soon", Toast.LENGTH_SHORT).show());
        }

        // Invite Guests button
        Button inviteGuestsButton = findViewById(R.id.inviteGuestsButton);
        if (inviteGuestsButton != null) {
            inviteGuestsButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, InviteGuestsActivity.class);
                intent.putExtra("EVENT_ID", eventId);
                startActivity(intent);
            });
        }

        if (btnJoinWaitlist != null) {
            btnJoinWaitlist.setOnClickListener(v -> joinWaitingList());
            setJoinButtonState("LEAVE WAITING LIST", true, R.drawable.bg_primary_button);
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

    private void loadOrganizerName(String organizerId) {
        db.collection("users").document(organizerId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        tvOrganizerName.setText(name);
                    } else {
                        tvOrganizerName.setText("Organizer");
                    }
                });
    }

    private void loadGoingCount() {
        // tvGoingCount view not present in current layout; no-op
    }

    private void checkIfAlreadyJoined() {
        db.collection("events").document(eventId).collection("waitingList")
                .document(deviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        alreadyJoined = true;
                        setJoinButtonState("ON WAITING LIST", false, R.drawable.bg_joined_button);
                    }
                });

        // Also check enrolled
        db.collection("events").document(eventId).collection("enrolled")
                .document(deviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        alreadyJoined = true;
                        setJoinButtonState("ENROLLED", false, R.drawable.bg_joined_button);
                    }
                });

        db.collection("events").document(eventId).collection("selected")
                .document(deviceId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        alreadyJoined = true;
                        setJoinButtonState("INVITED", false, R.drawable.bg_joined_button);
                    }
                });
    }

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
                                setJoinButtonState("ON WAITING LIST", false, R.drawable.bg_joined_button);
                                Toast.makeText(this, "You joined the waiting list!", Toast.LENGTH_SHORT).show();
                                loadGoingCount();
                            })
                            .addOnFailureListener(e -> {
                                setJoinButtonState("TRY AGAIN", true, R.drawable.bg_button_gradient_purple);
                                Toast.makeText(this, "Failed to join: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                });
    }

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

    private void setJoinButtonState(String label, boolean enabled, int backgroundRes) {
        if (btnJoinWaitlist == null) return;
        btnJoinWaitlist.setText(label);
        btnJoinWaitlist.setEnabled(enabled);
        btnJoinWaitlist.setClickable(enabled);
        btnJoinWaitlist.setAlpha(enabled ? 1f : 0.92f);
        btnJoinWaitlist.setBackgroundResource(backgroundRes);
    }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentsListener != null) {
            commentsListener.remove();
            commentsListener = null;
        }
    }
}
