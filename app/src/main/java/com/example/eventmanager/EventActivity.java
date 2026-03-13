package com.example.eventmanager;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class EventActivity extends AppCompatActivity {

    private FirebaseRepository repository;
    private ListenerRegistration eventListener;

    private String eventId;
    private String currentDeviceId;

    private Button   btnJoinWaitingList;
    private TextView tvWaitingListStatus;
    private TextView tvJoinErrorMessage;
    private TextView tvEventTitle;
    private TextView tvEventDescription;
    private TextView tvEventLocation;
    private TextView tvEventDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_activity);

        repository      = new FirebaseRepository();
        currentDeviceId = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ANDROID_ID);
        eventId = getIntent().getStringExtra("EVENT_ID");

        btnJoinWaitingList  = findViewById(R.id.btnJoinWaitingList);
        tvWaitingListStatus = findViewById(R.id.tvWaitingListStatus);
        tvJoinErrorMessage  = findViewById(R.id.tvJoinErrorMessage);
        tvEventTitle        = findViewById(R.id.tvEventTitle);
        tvEventDescription  = findViewById(R.id.tvEventDescription);
        tvEventLocation     = findViewById(R.id.tvEventLocation);
        tvEventDate         = findViewById(R.id.tvEventDate);

        btnJoinWaitingList.setOnClickListener(v -> onJoinClicked());
        loadEventData();
    }

    private void loadEventData() {
        if (eventId == null) return;
        eventListener = repository.listenToEvent(eventId, anasEvent -> {
            runOnUiThread(() -> updateUI(anasEvent));
        });
    }

    private void updateUI(AnasEvent event) {
        if (event.getName()             != null) tvEventTitle.setText(event.getName());
        if (event.getDescription()      != null) tvEventDescription.setText(event.getDescription());
        if (event.getLocation()         != null) tvEventLocation.setText(event.getLocation());
        if (event.getRegistrationStart() != null) {
            tvEventDate.setText(new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
                    .format(event.getRegistrationStart()));
        }

        boolean isJoined     = event.getWaitingList() != null
                && event.getWaitingList().contains(currentDeviceId);
        boolean windowOpen   = event.isRegistrationWindowActive();
        boolean waitlistFull = event.getWaitlistLimit() > 0
                && event.getWaitingList() != null
                && event.getWaitingList().size() >= event.getWaitlistLimit();

        if (isJoined) {
            setButton(false, "ALREADY ON WAITING LIST");
            showBadge("✓ You're on the waiting list!");
            hideError();
        } else if (!windowOpen) {
            setButton(false, "REGISTRATION CLOSED");
            hideBadge();
            showError("Registration is currently closed.");
        } else if (waitlistFull) {
            setButton(false, "WAITLIST FULL");
            hideBadge();
            showError("This event's waiting list is full.");
        } else {
            setButton(true, "JOIN WAITING LIST");
            hideBadge();
            hideError();
        }
    }

    private void onJoinClicked() {
        if (eventId == null || currentDeviceId == null) return;
        setButton(false, "JOINING...");
        repository.joinWaitingList(eventId, currentDeviceId)
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    setButton(true, "JOIN WAITING LIST");
                    showError(e.getMessage() != null
                            ? e.getMessage()
                            : "Something went wrong. Please try again.");
                }));
    }

    private void setButton(boolean enabled, String text) {
        btnJoinWaitingList.setEnabled(enabled);
        btnJoinWaitingList.setText(text);
        btnJoinWaitingList.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void showBadge(String message) {
        tvWaitingListStatus.setText(message);
        tvWaitingListStatus.setVisibility(View.VISIBLE);
    }

    private void hideBadge() {
        tvWaitingListStatus.setVisibility(View.GONE);
    }

    private void showError(String message) {
        tvJoinErrorMessage.setText(message);
        tvJoinErrorMessage.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvJoinErrorMessage.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventListener != null) eventListener.remove();
    }
}