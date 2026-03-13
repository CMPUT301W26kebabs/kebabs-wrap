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

/**
 * US1: Join Waiting List screen.
 * The Firestore real-time listener is the ONLY place that updates UI.
 * onJoinClicked() only disables the button immediately to prevent double-tap.
 * All badge/error/button-text decisions happen in updateUI() after Firestore responds.
 */
public class EventActivity extends AppCompatActivity {

    private FirebaseRepository repository;
    private ListenerRegistration eventListener;

    private String eventId;
    private String currentDeviceId;

    private Button    btnJoinWaitingList;
    private TextView  tvWaitingListStatus;   // green success badge
    private TextView  tvJoinErrorMessage;    // red error message
    private TextView  tvEventTitle;
    private TextView  tvEventDescription;
    private TextView  tvEventLocation;
    private TextView  tvEventDate;

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

    // ------------------------------------------------------------------
    // Data loading — real-time listener drives ALL UI updates
    // ------------------------------------------------------------------

    private void loadEventData() {
        if (eventId == null) return;
        eventListener = repository.listenToEvent(eventId, event -> {
            runOnUiThread(() -> updateUI(event));
        });
    }

    /**
     * Single source of truth for all UI state.
     * Called every time the Firestore document changes.
     *
     * Priority order:
     *   1. Already joined       → grey button + green badge
     *   2. Registration closed  → grey button + red error
     *   3. Waitlist full        → grey button + red error
     *   4. Eligible             → active button, no badge/error
     */
    private void updateUI(Event event) {
        // Populate info fields
        if (event.getName()        != null) tvEventTitle.setText(event.getName());
        if (event.getDescription() != null) tvEventDescription.setText(event.getDescription());
        if (event.getLocation()    != null) tvEventLocation.setText(event.getLocation());
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

    // ------------------------------------------------------------------
    // Join action
    // ------------------------------------------------------------------

    private void onJoinClicked() {
        if (eventId == null || currentDeviceId == null) return;

        // Immediately disable button to prevent double-tap.
        // Do NOT change badge/error here — updateUI() will handle that
        // once Firestore confirms the write via the real-time listener.
        setButton(false, "JOINING...");

        repository.joinWaitingList(eventId, currentDeviceId)
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    // On failure: restore button and show error so user can retry
                    setButton(true, "JOIN WAITING LIST");
                    showError(e.getMessage() != null
                            ? e.getMessage()
                            : "Something went wrong. Please try again.");
                }));
        // On success: do nothing here.
        // The real-time listener will fire automatically because we wrote to Firestore,
        // and updateUI() will set the button + badge to the correct final state.
    }

    // ------------------------------------------------------------------
    // UI helpers
    // ------------------------------------------------------------------

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