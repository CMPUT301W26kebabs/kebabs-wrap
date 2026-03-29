package com.example.eventmanager;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Event;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEventActivity";

    private EditText nameInput, descriptionInput, startDateInput, endDateInput;
    private EditText locationInput, capacityInput, waitlistLimitInput;
    private FirebaseRepository repository;
    private Date startDate, endDate;
    private Uri selectedImageUri;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault());

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);
        repository = new FirebaseRepository();

        // Back button
        try { findViewById(R.id.btnBack).setOnClickListener(v -> finish()); } catch (Exception e) {}

        // Input fields
        nameInput = findViewById(R.id.editEventName);
        descriptionInput = findViewById(R.id.editDescription);
        startDateInput = findViewById(R.id.editStartDate);
        endDateInput = findViewById(R.id.editEndDate);
        locationInput = findViewById(R.id.editLocation);
        capacityInput = findViewById(R.id.editCapacity);
        waitlistLimitInput = findViewById(R.id.editWaitlistLimit);

        // Image upload - use try/catch since cardUploadImage might not be ImageView
        try { findViewById(R.id.cardUploadImage).setOnClickListener(v -> imagePickerLauncher.launch("image/*")); } catch (Exception e) {}

        // Date pickers
        startDateInput.setFocusable(false);
        startDateInput.setOnClickListener(v -> showDateTimePicker(true));
        endDateInput.setFocusable(false);
        endDateInput.setOnClickListener(v -> showDateTimePicker(false));

        // Create button
        findViewById(R.id.btnCreateEvent).setOnClickListener(v -> saveEvent());
    }

    private void showDateTimePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            new TimePickerDialog(this, (timeView, hour, minute) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, hour, minute);
                Date date = selected.getTime();
                if (isStart) { startDate = date; startDateInput.setText(sdf.format(date)); }
                else { endDate = date; endDateInput.setText(sdf.format(date)); }
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveEvent() {
        String nameStr = nameInput.getText().toString().trim();
        String capacityStr = capacityInput.getText().toString().trim();

        if (nameStr.isEmpty()) { Toast.makeText(this, "Enter event name", Toast.LENGTH_SHORT).show(); return; }
        if (capacityStr.isEmpty()) { Toast.makeText(this, "Enter capacity", Toast.LENGTH_SHORT).show(); return; }

        String eventId = UUID.randomUUID().toString();
        String deviceId = new DeviceAuthManager().getDeviceId(this);

        Event newEvent = new Event();
        newEvent.setEventId(eventId);
        newEvent.setName(nameStr);
        newEvent.setCapacity(Integer.parseInt(capacityStr));
        newEvent.setOrganizerId(deviceId);

        String desc = descriptionInput.getText().toString().trim();
        if (!desc.isEmpty()) newEvent.setDescription(desc);
        String loc = locationInput.getText().toString().trim();
        if (!loc.isEmpty()) newEvent.setLocation(loc);
        if (startDate != null) newEvent.setRegistrationStart(startDate);
        if (endDate != null) newEvent.setRegistrationEnd(endDate);

        String waitlistStr = waitlistLimitInput.getText().toString().trim();
        if (!waitlistStr.isEmpty()) newEvent.setMaxWaitlistCapacity(Integer.parseInt(waitlistStr));

        Toast.makeText(this, "Creating event...", Toast.LENGTH_SHORT).show();

        if (selectedImageUri != null) {
            uploadImageThenSave(eventId, newEvent);
        } else {
            saveEventToFirestore(newEvent);
        }
    }

    private void uploadImageThenSave(String eventId, Event event) {
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("event_posters/" + eventId + ".jpg");
        ref.putFile(selectedImageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            event.setPosterUrl(uri.toString());
                            saveEventToFirestore(event);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Poster uploaded but failed to fetch download URL", e);
                            Toast.makeText(this,
                                    "Uploaded poster, but failed to get URL. Creating event without poster.",
                                    Toast.LENGTH_LONG).show();
                            saveEventToFirestore(event);
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Poster upload failed", e);
                    Toast.makeText(this,
                            "Image upload failed: " + e.getMessage() + ". Creating event without poster.",
                            Toast.LENGTH_LONG).show();
                    saveEventToFirestore(event);
                });
    }

    private void saveEventToFirestore(Event event) {
        repository.createEvent(event,
                aVoid -> {
                    Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, EventQRActivity.class);
                    intent.putExtra("EVENT_ID", event.getEventId());
                    intent.putExtra("EVENT_NAME", event.getName());
                    startActivity(intent);
                    finish();
                },
                e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
