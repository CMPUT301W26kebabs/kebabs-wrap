package com.example.eventmanager;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class OrganizerNotifyWaitlist extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_event);

        // Hardcode the test event ID for now
        String testEventId   = "Yx4SXWIOd64MvS2G6PmW";
        String testEventName = "event1";

        OrganizerNotificationManager manager = new OrganizerNotificationManager();

        findViewById(R.id.btn_notify).setOnClickListener(v ->
                manager.notifyWaitingList(testEventId, testEventName)
        );

        // Register the user in Firestore using their device ID on every launch.
        // SetOptions.merge() in saveUser() ensures we never overwrite existing data.
        String deviceId = new DeviceAuthManager().getDeviceId(this);
        new FirebaseRepository().saveUser(deviceId);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}