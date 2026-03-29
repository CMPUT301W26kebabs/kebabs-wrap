package com.example.eventmanager;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class InviteGuestsActivity extends AppCompatActivity {

    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_guests);

        eventId = getIntent().getStringExtra("EVENT_ID");

        Toolbar toolbar = findViewById(R.id.inviteGuestsToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        if (eventId == null) {
            Toast.makeText(this, "Missing event information", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
