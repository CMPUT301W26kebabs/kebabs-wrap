package com.example.eventmanager;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Shown when a user taps a winner notification.
 * Presents the event name and two choices: Accept or Decline.
 *
 * Accept flow:
 *   enrollUser() → removeFromWinners() → finish()
 *
 * Decline flow:
 *   removeFromWinners() → removeFromWaitingList() → finish()
 *
 * Covers US 01.05.02 (Accept) and US 01.05.03 (Decline)
 */
public class AcceptDeclineActivity extends AppCompatActivity {

    private EventRepository eventRepository;

    private String eventId;
    private String eventName;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_decline);

        // Unpack Intent extras passed from NotificationsActivity
        eventId   = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");
        deviceId  = getIntent().getStringExtra("deviceId");
        if (eventId == null || deviceId == null) {
            Toast.makeText(this, "Invitation data is missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        eventRepository = new EventRepository();

        // Populate the event name on screen
        TextView textEventName = findViewById(R.id.text_event_name);
        textEventName.setText(eventName != null ? eventName : "Event");
        ImageButton backButton = findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        Button btnAccept  = findViewById(R.id.btn_accept);
        Button btnDecline = findViewById(R.id.btn_decline);

        // ── Accept ──────────────────────────────────────────────────────────
        btnAccept.setOnClickListener(v -> {
            btnAccept.setEnabled(false);
            btnDecline.setEnabled(false);
            eventRepository.enrollUser(eventId, deviceId, new EventRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(AcceptDeclineActivity.this, "Invitation accepted.", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String message) {
                    btnAccept.setEnabled(true);
                    btnDecline.setEnabled(true);
                    Toast.makeText(AcceptDeclineActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });

        // ── Decline ─────────────────────────────────────────────────────────
        btnDecline.setOnClickListener(v -> {
            btnAccept.setEnabled(false);
            btnDecline.setEnabled(false);
            eventRepository.declineInvitation(eventId, deviceId, new EventRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(AcceptDeclineActivity.this, "Invitation declined.", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String message) {
                    btnAccept.setEnabled(true);
                    btnDecline.setEnabled(true);
                    Toast.makeText(AcceptDeclineActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}