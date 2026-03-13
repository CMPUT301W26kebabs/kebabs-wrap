package com.example.eventmanager;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.ListenerRegistration;

public class EventActivity extends AppCompatActivity {

    private AnasFirebaseRepo repository;
    private String eventId;
    private String currentDeviceId;
    private ListenerRegistration eventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_activity);

        repository = new AnasFirebaseRepo();
        eventId = getIntent().getStringExtra("EVENT_ID");
        currentDeviceId = android.provider.Settings.Secure.getString(
                getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        TextView tvEventName = null;
        try { tvEventName = findViewById(R.id.tvEventName); } catch (Exception e) {}

        Button btnJoinWaitlist = null;
        btnJoinWaitlist = null; try { btnJoinWaitlist = (Button) getWindow().getDecorView().findViewWithTag("btnJoinWaitlist"); } catch (Exception e) {}

        if (eventId != null && tvEventName != null) {
            final TextView finalTvName = tvEventName;
            eventListener = repository.listenToEvent(eventId, event -> {
                if (event.getName() != null) finalTvName.setText(event.getName());
            });
        }

        if (btnJoinWaitlist != null) {
            btnJoinWaitlist.setOnClickListener(v -> {
                repository.joinWaitingList(eventId, currentDeviceId)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Joined waiting list!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventListener != null) eventListener.remove();
    }
}
