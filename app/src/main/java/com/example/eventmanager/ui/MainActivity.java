package com.example.eventmanager.ui;

import com.example.eventmanager.R;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.admin.AdminCommentsActivity;
import com.example.eventmanager.admin.AdminEventsActivity;
import com.example.eventmanager.admin.AdminImagesActivity;
import com.example.eventmanager.admin.AdminNotificationLogsActivity;
import com.example.eventmanager.admin.AdminProfilesActivity;
import com.example.eventmanager.utils.AdminGuard;

/**
 * Admin dashboard screen providing navigation to all administrative functions.
 * Presents cards for managing events, profiles, images, notification logs, and comments.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AdminGuard.guardActivity(this);

        // Back button to Home Screen
        findViewById(R.id.btn_back_home).setOnClickListener(v -> finish());

        // Admin dashboard cards
        findViewById(R.id.card_events).setOnClickListener(v ->
                startActivity(new Intent(this, AdminEventsActivity.class)));
        findViewById(R.id.card_profiles).setOnClickListener(v ->
                startActivity(new Intent(this, AdminProfilesActivity.class)));
        findViewById(R.id.card_images).setOnClickListener(v ->
                startActivity(new Intent(this, AdminImagesActivity.class)));
        findViewById(R.id.card_comments).setOnClickListener(v ->
                startActivity(new Intent(this, AdminCommentsActivity.class)));
        findViewById(R.id.card_notification_logs).setOnClickListener(v ->
                startActivity(new Intent(this, AdminNotificationLogsActivity.class)));
    }
}
