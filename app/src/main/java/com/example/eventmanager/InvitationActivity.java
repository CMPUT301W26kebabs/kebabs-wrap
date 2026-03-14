package com.example.eventmanager;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class InvitationActivity extends AppCompatActivity {

    private AnasFirebaseRepo repository;
    private String eventId, currentDeviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_decline);

        repository = new AnasFirebaseRepo();
        eventId = getIntent().getStringExtra("EVENT_ID");
        currentDeviceId = android.provider.Settings.Secure.getString(
                getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        TextView tvEventName = findViewById(R.id.text_event_name);
        Button btnAccept = findViewById(R.id.btn_accept);
        Button btnDecline = findViewById(R.id.btn_decline);
        ImageButton btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        if (eventId != null) {
            repository.getEvent(eventId).addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    tvEventName.setText(name != null ? name : "Event");
                }
            });
        }

        btnAccept.setOnClickListener(v -> {
            repository.acceptInvitation(eventId, currentDeviceId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Accepted!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
        });

        btnDecline.setOnClickListener(v -> {
            repository.declineInvitation(eventId, currentDeviceId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Declined", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
        });
    }
}
