package com.example.eventmanager.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.eventmanager.R;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.Event;
import com.example.eventmanager.repository.FirebaseRepository;

/**
 * Displays a selected event and lets the current entrant join its waiting list.
 */
public class EventDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";

    private TextView eventNameText;
    private TextView eventDescriptionText;
    private TextView eventDatesText;
    private ImageView eventPosterImage;
    private Button signUpButton;

    private FirebaseRepository repository;
    private DeviceAuthManager authManager;

    private String eventId;
    private Entrant currentEntrant;
    private Event currentEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        eventNameText = findViewById(R.id.eventNameText);
        eventDescriptionText = findViewById(R.id.eventDescriptionText);
        eventDatesText = findViewById(R.id.eventDatesText);
        eventPosterImage = findViewById(R.id.eventPosterImage);
        signUpButton = findViewById(R.id.signUpButton);

        repository = new FirebaseRepository();
        authManager = new DeviceAuthManager();

        eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        if (TextUtils.isEmpty(eventId)) {
            Toast.makeText(this, "Missing event id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Entrant and event data load independently so the screen can populate as each call finishes.
        loadEntrant();
        loadEvent();

        signUpButton.setOnClickListener(v -> signUpForEvent());
    }

    private void loadEntrant() {
        String deviceId = authManager.getDeviceId(this);

        repository.getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                currentEntrant = result;
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(EventDetailsActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEvent() {
        repository.getEventById(eventId, new FirebaseRepository.RepoCallback<Event>() {
            @Override
            public void onSuccess(Event result) {
                if (result == null) {
                    Toast.makeText(EventDetailsActivity.this, "Event not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                currentEvent = result;
                bindEvent(result);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(EventDetailsActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindEvent(Event event) {
        eventNameText.setText(event.getName());
        eventDescriptionText.setText(event.getDescription());

        // Keep the registration window in one label so the timeline is easy to scan.
        String dateText = "Registration: "
                + String.valueOf(event.getRegistrationStart())
                + " to "
                + String.valueOf(event.getRegistrationEnd());
        eventDatesText.setText(dateText);

        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(this)
                    .load(event.getPosterUrl())
                    .into(eventPosterImage);
        }
    }

    private void signUpForEvent() {
        if (currentEntrant == null) {
            Toast.makeText(this, "Complete your profile first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the already loaded entrant profile so the waiting-list document matches the signed-in device.
        repository.signUpForEvent(eventId, currentEntrant, new FirebaseRepository.RepoCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(EventDetailsActivity.this, "Signed up for waiting list", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(EventDetailsActivity.this, "Sign up failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
