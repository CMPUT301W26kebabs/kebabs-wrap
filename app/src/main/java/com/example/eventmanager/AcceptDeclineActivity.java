package com.example.eventmanager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

        eventRepository = new EventRepository();

        // Populate the event name on screen
        TextView textEventName = findViewById(R.id.text_event_name);
        textEventName.setText(eventName);

        Button btnAccept  = findViewById(R.id.btn_accept);
        Button btnDecline = findViewById(R.id.btn_decline);

        // ── Accept ──────────────────────────────────────────────────────────
        btnAccept.setOnClickListener(v -> {

            // Disable both buttons immediately to prevent double-taps
            btnAccept.setEnabled(false);
            btnDecline.setEnabled(false);

            // Add to enrolled, remove from winners
            eventRepository.enrollUser(eventId, deviceId);
            eventRepository.removeFromWinners(eventId, deviceId);

            // Return to the notification panel
            finish();
        });

        // ── Decline ─────────────────────────────────────────────────────────
        btnDecline.setOnClickListener(v -> {

            btnAccept.setEnabled(false);
            btnDecline.setEnabled(false);

            // Remove from both winners and waitingList entirely
            eventRepository.removeFromWinners(eventId, deviceId);
            eventRepository.removeFromWaitingList(eventId, deviceId);

            finish();
        });
    }
}