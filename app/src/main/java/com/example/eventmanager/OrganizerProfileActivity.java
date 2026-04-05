package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.adapter.HomeEventAdapter;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.repository.FollowRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrganizerProfileActivity extends AppCompatActivity {

    private String organizerId;
    private String deviceId;
    private FirebaseFirestore db;
    private FollowRepository followRepo;

    private TextView tvAvatarLarge, tvOrganizerName, tvFollowerCount, tvNoEvents;
    private Button btnFollow;
    private RecyclerView rvEvents;
    private HomeEventAdapter eventsAdapter;
    private boolean isFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_profile);

        organizerId = getIntent().getStringExtra("ORGANIZER_ID");
        if (organizerId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        followRepo = new FollowRepository();
        deviceId = new DeviceAuthManager().getDeviceId(this);

        tvAvatarLarge = findViewById(R.id.tvAvatarLarge);
        tvOrganizerName = findViewById(R.id.tvOrganizerName);
        tvFollowerCount = findViewById(R.id.tvFollowerCount);
        tvNoEvents = findViewById(R.id.tvNoEvents);
        btnFollow = findViewById(R.id.btnFollowProfile);
        rvEvents = findViewById(R.id.rvOrganizerEvents);

        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());

        eventsAdapter = new HomeEventAdapter(this, new ArrayList<>());
        rvEvents.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvEvents.setAdapter(eventsAdapter);
        eventsAdapter.setOnEventClickListener(this::openEventDetails);

        btnFollow.setOnClickListener(v -> toggleFollow());

        loadOrganizerInfo();
        loadFollowerCount();
        checkFollowState();
        loadOrganizerEvents();
    }

    private void loadOrganizerInfo() {
        db.collection("users").document(organizerId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        tvOrganizerName.setText(name);
                        tvAvatarLarge.setText(String.valueOf(Character.toUpperCase(name.charAt(0))));
                    }
                });
    }

    private void loadFollowerCount() {
        followRepo.getFollowerCount(organizerId, count ->
                runOnUiThread(() -> tvFollowerCount.setText(
                        String.format(Locale.getDefault(), "%d %s", count,
                                count == 1 ? "Follower" : "Followers"))));
    }

    private void checkFollowState() {
        followRepo.isFollowing(deviceId, organizerId, following -> {
            isFollowing = following;
            runOnUiThread(this::updateFollowButtonUI);
        });
    }

    private void updateFollowButtonUI() {
        if (isFollowing) {
            btnFollow.setText(R.string.following);
            btnFollow.setBackgroundResource(R.drawable.bg_following_button);
            btnFollow.setTextColor(getResources().getColor(R.color.following_button_text, null));
        } else {
            btnFollow.setText(R.string.follow);
            btnFollow.setBackgroundResource(R.drawable.bg_avatar_large);
            btnFollow.setTextColor(getResources().getColor(R.color.follow_button_bg, null));
        }
    }

    private void toggleFollow() {
        btnFollow.setEnabled(false);
        String orgName = tvOrganizerName.getText().toString();

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
                                        loadFollowerCount();
                                        btnFollow.setEnabled(true);
                                        Toast.makeText(OrganizerProfileActivity.this,
                                                isFollowing ? getString(R.string.follow_success)
                                                        : getString(R.string.unfollow_success),
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onFailure(@NonNull String message) {
                                    runOnUiThread(() -> {
                                        btnFollow.setEnabled(true);
                                        Toast.makeText(OrganizerProfileActivity.this, message,
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                });
    }

    private void loadOrganizerEvents() {
        db.collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(qs -> {
                    List<DocumentSnapshot> active = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        Boolean deleted = doc.getBoolean("isDeleted");
                        if (deleted == null || !deleted) {
                            active.add(doc);
                        }
                    }
                    eventsAdapter.updateList(active);
                    tvNoEvents.setVisibility(active.isEmpty() ? View.VISIBLE : View.GONE);
                    rvEvents.setVisibility(active.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void openEventDetails(DocumentSnapshot doc) {
        String eventId = doc.getString("eventId");
        if (eventId == null || eventId.trim().isEmpty()) eventId = doc.getId();
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
    }
}
