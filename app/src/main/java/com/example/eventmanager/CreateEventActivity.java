package com.example.eventmanager;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Event;
import com.example.eventmanager.repository.FollowRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
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
    private EditText registrationStartInput, registrationEndInput;
    private EditText locationInput, capacityInput, waitlistLimitInput;
    private FirebaseRepository repository;
    private Date startDate, endDate;
    private Date registrationStartDate, registrationEndDate;
    private Uri selectedImageUri;

    private ImageView imagePosterPreview;
    private View uploadPlaceholder;

    private MaterialButton btnCreateEvent;
    private MaterialButton btnSaveEvent;
    private SwitchMaterial switchPrivateEvent;
    private SwitchMaterial switchRequireGeolocation;

    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault());

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    if (imagePosterPreview != null) {
                        if (uploadPlaceholder != null) uploadPlaceholder.setVisibility(View.GONE);
                        imagePosterPreview.setVisibility(View.VISIBLE);
                        Glide.with(this).load(uri).centerCrop().into(imagePosterPreview);
                    }
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
        registrationStartInput = findViewById(R.id.editRegistrationStartDate);
        registrationEndInput = findViewById(R.id.editRegistrationEndDate);
        locationInput = findViewById(R.id.editLocation);
        capacityInput = findViewById(R.id.editCapacity);
        waitlistLimitInput = findViewById(R.id.editWaitlistLimit);

        // Poster preview + placeholder
        try {
            imagePosterPreview = findViewById(R.id.imagePosterPreview);
            uploadPlaceholder = findViewById(R.id.layoutUploadPlaceholder);
        } catch (Exception ignored) {}

        // Image upload - use try/catch since cardUploadImage might not be ImageView
        try { findViewById(R.id.cardUploadImage).setOnClickListener(v -> imagePickerLauncher.launch("image/*")); } catch (Exception e) {}

        // Date pickers
        startDateInput.setFocusable(false);
        startDateInput.setOnClickListener(v -> showDateTimePicker(true, false));
        endDateInput.setFocusable(false);
        endDateInput.setOnClickListener(v -> showDateTimePicker(false, false));

        registrationStartInput.setFocusable(false);
        registrationStartInput.setOnClickListener(v -> showDateTimePicker(true, true));
        registrationEndInput.setFocusable(false);
        registrationEndInput.setOnClickListener(v -> showDateTimePicker(false, true));

        // Buttons + private toggle
        try {
            btnCreateEvent = findViewById(R.id.btnCreateEvent);
            btnSaveEvent = findViewById(R.id.btnSaveEvent);
            switchPrivateEvent = findViewById(R.id.switchPrivateEvent);
            switchRequireGeolocation = findViewById(R.id.switchRequireGeolocation);

            btnCreateEvent.setOnClickListener(v -> saveEvent(true));
            btnSaveEvent.setOnClickListener(v -> saveEvent(false));

            updateBottomButtons(switchPrivateEvent.isChecked());
            switchPrivateEvent.setOnCheckedChangeListener((compoundButton, isChecked) -> updateBottomButtons(isChecked));
        } catch (Exception ignored) {
            // Fallback: keep existing behavior if some widgets are missing.
            findViewById(R.id.btnCreateEvent).setOnClickListener(v -> saveEvent(true));
        }
    }

    private void updateBottomButtons(boolean isPrivate) {
        if (btnCreateEvent == null || btnSaveEvent == null) return;
        btnCreateEvent.setVisibility(isPrivate ? View.GONE : View.VISIBLE);
        btnSaveEvent.setVisibility(isPrivate ? View.VISIBLE : View.GONE);
    }

    private void showDateTimePicker(boolean isStart, boolean isRegistrationWindow) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            new TimePickerDialog(this, (timeView, hour, minute) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, hour, minute);
                Date date = selected.getTime();
                if (isRegistrationWindow) {
                    if (isStart) {
                        registrationStartDate = date;
                        registrationStartInput.setText(sdf.format(date));
                    } else {
                        registrationEndDate = date;
                        registrationEndInput.setText(sdf.format(date));
                    }
                } else {
                    if (isStart) { startDate = date; startDateInput.setText(sdf.format(date)); }
                    else { endDate = date; endDateInput.setText(sdf.format(date)); }
                }
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveEvent(boolean generateQr) {
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

        // Registration window is stored in the Event model.
        Date regStart = registrationStartDate != null ? registrationStartDate : startDate;
        Date regEnd = registrationEndDate != null ? registrationEndDate : endDate;
        if (regStart != null) newEvent.setRegistrationStart(regStart);
        if (regEnd != null) newEvent.setRegistrationEnd(regEnd);

        String waitlistStr = waitlistLimitInput.getText().toString().trim();
        if (!waitlistStr.isEmpty()) newEvent.setMaxWaitlistCapacity(Integer.parseInt(waitlistStr));

        boolean geolocationRequired = switchRequireGeolocation != null && switchRequireGeolocation.isChecked();
        newEvent.setGeolocationRequired(geolocationRequired);

        Toast.makeText(this, "Creating event...", Toast.LENGTH_SHORT).show();

        if (selectedImageUri != null) {
            uploadImageThenSave(eventId, newEvent, generateQr);
        } else {
            saveEventToFirestore(newEvent, generateQr);
        }
    }

    private void uploadImageThenSave(String eventId, Event event, boolean generateQr) {
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("event_posters/" + eventId + ".jpg");
        ref.putFile(selectedImageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            event.setPosterUrl(uri.toString());
                            saveEventToFirestore(event, generateQr);
                        })
                        .addOnFailureListener(e -> saveEventToFirestore(event, generateQr)))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_LONG).show());
    }

    private void saveEventToFirestore(Event event, boolean generateQr) {
        repository.createEvent(event,
                aVoid -> {
                    Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show();

                    if (generateQr) {
                        new FollowRepository().notifyFollowersOfNewEvent(
                                event.getOrganizerId(), event.getName(), event.getEventId());

                        Intent intent = new Intent(this, EventQRActivity.class);
                        intent.putExtra("EVENT_ID", event.getEventId());
                        intent.putExtra("EVENT_NAME", event.getName());
                        startActivity(intent);
                    }
                    finish();
                },
                e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
