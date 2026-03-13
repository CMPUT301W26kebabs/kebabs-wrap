package com.example.eventmanager;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * US4: Entrant-facing invitation screen.
 * Shown when a user has been selected by the lottery (chosen list).
 * Displays the event name and two action buttons: Accept and Decline.
 *
 * Accepting moves the user from chosenList → attendees.
 * Declining moves the user from chosenList → declinedList,
 * triggering replacement draw eligibility.
 */
public class InvitationActivity extends AppCompatActivity {

    private FirebaseRepository repository;
    private String eventId;
    private String currentDeviceId;

    private TextView tvEventName;
    private TextView tvStatusMessage;
    private Button btnAccept;
    private Button btnDecline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.makeshift_invitation_activity);

        repository = new FirebaseRepository();
        currentDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        eventId = getIntent().getStringExtra("EVENT_ID");

        setupUI();
        loadEventName();
    }

    private void setupUI() {
        tvEventName    = findViewById(R.id.tvInvitationEventName);
        tvStatusMessage = findViewById(R.id.tvInvitationStatus);
        btnAccept      = findViewById(R.id.btnAcceptInvitation);
        btnDecline     = findViewById(R.id.btnDeclineInvitation);

        btnAccept.setOnClickListener(v -> onAcceptClicked());
        btnDecline.setOnClickListener(v -> onDeclineClicked());
    }

    private void loadEventName() {
        if (eventId == null) return;
        repository.getEvent(eventId).addOnSuccessListener(event -> {
            if (event != null) {
                tvEventName.setText(event.getName());
            }
        });
    }

    /**
     * Handles Accept button click.
     * Calls repository to atomically move user from chosenList to attendees.
     * Disables both buttons after tap to prevent double submission.
     */
    private void onAcceptClicked() {
        setButtonsEnabled(false);

        repository.acceptInvitation(eventId, currentDeviceId)
                .addOnSuccessListener(aVoid -> {
                    tvStatusMessage.setVisibility(View.VISIBLE);
                    tvStatusMessage.setText("You're enrolled! See you there.");
                    Toast.makeText(this, "Invitation accepted!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setButtonsEnabled(true);
                    tvStatusMessage.setVisibility(View.VISIBLE);
                    tvStatusMessage.setText("Error: " + e.getMessage());
                });
    }

    /**
     * Handles Decline button click.
     * Calls repository to move user from chosenList to declinedList.
     */
    private void onDeclineClicked() {
        setButtonsEnabled(false);

        repository.declineInvitation(eventId, currentDeviceId)
                .addOnSuccessListener(aVoid -> {
                    tvStatusMessage.setVisibility(View.VISIBLE);
                    tvStatusMessage.setText("You've declined the invitation.");
                    Toast.makeText(this, "Invitation declined.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setButtonsEnabled(true);
                    tvStatusMessage.setVisibility(View.VISIBLE);
                    tvStatusMessage.setText("Error: " + e.getMessage());
                });
    }

    private void setButtonsEnabled(boolean enabled) {
        btnAccept.setEnabled(enabled);
        btnDecline.setEnabled(enabled);
    }
}
