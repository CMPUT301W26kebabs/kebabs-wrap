package com.example.eventmanager;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class InvitationActivity extends AppCompatActivity {

    private AnasFirebaseRepo repository;
    private String eventId, currentDeviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.makeshift_invitation_activity);

        repository = new AnasFirebaseRepo();
        eventId = getIntent().getStringExtra("EVENT_ID");
        currentDeviceId = android.provider.Settings.Secure.getString(
                getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        TextView tvEventName = findViewById(R.id.tvInvitationEventName);
        TextView tvStatus = findViewById(R.id.tvInvitationStatus);
        Button btnAccept = findViewById(R.id.btnAcceptInvitation);
        Button btnDecline = findViewById(R.id.btnDeclineInvitation);

        if (eventId != null) {
            repository.getEvent(eventId).addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    tvEventName.setText(name != null ? name : "Event");
                    tvStatus.setText("You've been selected! Accept or decline.");
                }
            });
        }

        btnAccept.setOnClickListener(v -> {
            repository.acceptInvitation(eventId, currentDeviceId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Accepted!", Toast.LENGTH_SHORT).show();
                        tvStatus.setText("You are enrolled!");
                        btnAccept.setEnabled(false);
                        btnDecline.setEnabled(false);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
        });

        btnDecline.setOnClickListener(v -> {
            repository.declineInvitation(eventId, currentDeviceId)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Declined", Toast.LENGTH_SHORT).show();
                        tvStatus.setText("You declined.");
                        btnAccept.setEnabled(false);
                        btnDecline.setEnabled(false);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show());
        });
    }
}
