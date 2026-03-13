package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Navigation hub for testing US1-US4 activities.
 * Provides buttons to launch EventActivity, EventListsViewActivity,
 * and InvitationActivity with a test EVENT_ID.
 */
public class MainActivity extends AppCompatActivity {

    // Use this test event ID — make sure this document exists in Firestore
    private static final String TEST_EVENT_ID = "test_event_open";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // US1: Join Waiting List
        Button btnEventActivity = findViewById(R.id.btnGoToEventActivity);
        btnEventActivity.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventActivity.class);
            intent.putExtra("EVENT_ID", TEST_EVENT_ID);
            startActivity(intent);
        });

        // US2 & US3: Organizer view waiting + chosen lists
        Button btnEventLists = findViewById(R.id.btnGoToEventLists);
        btnEventLists.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventListsViewActivity.class);
            intent.putExtra("EVENT_ID", TEST_EVENT_ID);
            startActivity(intent);
        });

        // US4: Accept / Decline Invitation
        Button btnInvitation = findViewById(R.id.btnGoToInvitation);
        btnInvitation.setOnClickListener(v -> {
            Intent intent = new Intent(this, InvitationActivity.class);
            intent.putExtra("EVENT_ID", TEST_EVENT_ID);
            startActivity(intent);
        });
    }
}
