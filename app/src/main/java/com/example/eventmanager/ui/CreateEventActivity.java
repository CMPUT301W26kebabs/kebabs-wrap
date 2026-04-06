package com.example.eventmanager.ui;

import com.example.eventmanager.R;
import com.example.eventmanager.repository.FirebaseRepository;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Form screen for creating a new event or editing an existing one. Organizers can set
 * the event name, description, start/end dates, capacity, waitlist limit, poster image,
 * geolocation requirement, and private-event toggle.
 *
 * <p>When {@link #EXTRA_EDIT_EVENT_ID} is supplied via the launching Intent, the activity
 * loads the existing event data and updates it instead of creating a new document.
 *
 * <p>Covers US 02.01.01 (create event), US 02.01.02 (edit event details),
 * US 02.07.01 (poster upload), US 02.02.01 (geolocation toggle).
 */
public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEventActivity";

    public static final String EXTRA_EDIT_EVENT_ID = "EDIT_EVENT_ID";

    /** When non-null, we update this event instead of creating a new one. */
    private String editEventId;
    private String editingOrganizerId;
    private String existingPosterUrl;
    /** Preserved when the waitlist limit field is left blank in edit mode. */
    private Integer persistedMaxWaitlist;

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

        String editId = getIntent().getStringExtra(EXTRA_EDIT_EVENT_ID);
        if (editId != null && !editId.trim().isEmpty()) {
            loadEventForEdit(editId.trim());
        }
    }

    private void loadEventForEdit(String eventId) {
        repository.fetchEventById(eventId, new FirebaseRepository.OnDocumentLoadedListener() {
            @Override
            public void onLoaded(DocumentSnapshot document) {
                if (document == null || !document.exists()) {
                    Toast.makeText(CreateEventActivity.this, "Event not found.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                Event ev = document.toObject(Event.class);
                String myId = new DeviceAuthManager().getDeviceId(CreateEventActivity.this);
                String orgId = ev != null && ev.getOrganizerId() != null
                        ? ev.getOrganizerId()
                        : document.getString("organizerId");
                if (orgId != null && !orgId.equals(myId)) {
                    Toast.makeText(CreateEventActivity.this,
                            "You can only edit your own events.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                editEventId = eventId;
                editingOrganizerId = orgId;

                android.widget.TextView tvTitle = findViewById(R.id.tv_create_event_title);
                if (tvTitle != null) {
                    tvTitle.setText("Edit Event");
                }

                String n = document.getString("name");
                if (n != null) nameInput.setText(n);
                String desc = document.getString("description");
                if (desc != null) descriptionInput.setText(desc);
                String loc = document.getString("location");
                if (loc != null) locationInput.setText(loc);
                Long cap = document.getLong("capacity");
                if (cap != null) capacityInput.setText(String.valueOf(cap));
                Long wl = document.getLong("maxWaitlistCapacity");
                persistedMaxWaitlist = wl != null ? wl.intValue() : null;
                if (wl != null && wl > 0) waitlistLimitInput.setText(String.valueOf(wl));

                Timestamp sd = document.getTimestamp("startDate");
                Timestamp ed = document.getTimestamp("endDate");
                if (sd != null) {
                    startDate = sd.toDate();
                    startDateInput.setText(sdf.format(startDate));
                }
                if (ed != null) {
                    endDate = ed.toDate();
                    endDateInput.setText(sdf.format(endDate));
                }

                Timestamp rs = document.getTimestamp("registrationStart");
                Timestamp re = document.getTimestamp("registrationEnd");
                if (rs != null) {
                    registrationStartDate = rs.toDate();
                    registrationStartInput.setText(sdf.format(registrationStartDate));
                }
                if (re != null) {
                    registrationEndDate = re.toDate();
                    registrationEndInput.setText(sdf.format(registrationEndDate));
                }

                if (switchPrivateEvent != null) {
                    switchPrivateEvent.setChecked(Boolean.TRUE.equals(document.getBoolean("privateEvent")));
                    updateBottomButtons(switchPrivateEvent.isChecked());
                }
                if (switchRequireGeolocation != null) {
                    switchRequireGeolocation.setChecked(
                            Boolean.TRUE.equals(document.getBoolean("geolocationRequired")));
                }

                existingPosterUrl = document.getString("posterUrl");
                if (existingPosterUrl != null && !existingPosterUrl.isEmpty()
                        && imagePosterPreview != null) {
                    if (uploadPlaceholder != null) uploadPlaceholder.setVisibility(View.GONE);
                    imagePosterPreview.setVisibility(View.VISIBLE);
                    Glide.with(CreateEventActivity.this).load(existingPosterUrl).centerCrop().into(imagePosterPreview);
                }

                if (btnCreateEvent != null) {
                    btnCreateEvent.setText("Save changes");
                    btnCreateEvent.setVisibility(View.VISIBLE);
                }
                if (btnSaveEvent != null) {
                    btnSaveEvent.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(CreateEventActivity.this,
                        "Failed to load event.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void updateBottomButtons(boolean isPrivate) {
        if (btnCreateEvent == null || btnSaveEvent == null) return;
        if (editEventId != null) {
            btnSaveEvent.setVisibility(View.GONE);
            btnCreateEvent.setVisibility(View.VISIBLE);
            return;
        }
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
        if (editEventId != null) {
            saveExistingEvent();
            return;
        }

        String nameStr = nameInput.getText().toString().trim();
        String capacityStr = capacityInput.getText().toString().trim();

        if (nameStr.isEmpty()) { Toast.makeText(this, "Enter event name", Toast.LENGTH_SHORT).show(); return; }
        if (capacityStr.isEmpty()) { Toast.makeText(this, "Enter capacity", Toast.LENGTH_SHORT).show(); return; }
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Set event start and end date/time", Toast.LENGTH_SHORT).show();
            return;
        }

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

        newEvent.setStartDate(startDate);
        newEvent.setEndDate(endDate);

        if (registrationStartDate != null) newEvent.setRegistrationStart(registrationStartDate);
        if (registrationEndDate != null) newEvent.setRegistrationEnd(registrationEndDate);

        String waitlistStr = waitlistLimitInput.getText().toString().trim();
        if (!waitlistStr.isEmpty()) newEvent.setMaxWaitlistCapacity(Integer.parseInt(waitlistStr));

        boolean geolocationRequired = switchRequireGeolocation != null && switchRequireGeolocation.isChecked();
        newEvent.setGeolocationRequired(geolocationRequired);

        boolean isPrivate = switchPrivateEvent != null && switchPrivateEvent.isChecked();
        newEvent.setPrivateEvent(isPrivate);

        Toast.makeText(this, "Creating event...", Toast.LENGTH_SHORT).show();

        if (selectedImageUri != null) {
            uploadImageThenSave(eventId, newEvent, generateQr);
        } else {
            saveEventToFirestore(newEvent, generateQr);
        }
    }

    private void saveExistingEvent() {
        String nameStr = nameInput.getText().toString().trim();
        String capacityStr = capacityInput.getText().toString().trim();

        if (nameStr.isEmpty()) { Toast.makeText(this, "Enter event name", Toast.LENGTH_SHORT).show(); return; }
        if (capacityStr.isEmpty()) { Toast.makeText(this, "Enter capacity", Toast.LENGTH_SHORT).show(); return; }
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Set event start and end date/time", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event = new Event();
        event.setEventId(editEventId);
        event.setOrganizerId(editingOrganizerId != null
                ? editingOrganizerId
                : new DeviceAuthManager().getDeviceId(this));
        event.setName(nameStr);
        event.setCapacity(Integer.parseInt(capacityStr));

        String desc = descriptionInput.getText().toString().trim();
        if (!desc.isEmpty()) event.setDescription(desc);
        String loc = locationInput.getText().toString().trim();
        if (!loc.isEmpty()) event.setLocation(loc);

        event.setStartDate(startDate);
        event.setEndDate(endDate);

        if (registrationStartDate != null) event.setRegistrationStart(registrationStartDate);
        if (registrationEndDate != null) event.setRegistrationEnd(registrationEndDate);

        String waitlistStr = waitlistLimitInput.getText().toString().trim();
        if (!waitlistStr.isEmpty()) {
            event.setMaxWaitlistCapacity(Integer.parseInt(waitlistStr));
        } else if (persistedMaxWaitlist != null) {
            event.setMaxWaitlistCapacity(persistedMaxWaitlist);
        }

        boolean geolocationRequired = switchRequireGeolocation != null && switchRequireGeolocation.isChecked();
        event.setGeolocationRequired(geolocationRequired);

        boolean isPrivate = switchPrivateEvent != null && switchPrivateEvent.isChecked();
        event.setPrivateEvent(isPrivate);

        if (selectedImageUri == null && existingPosterUrl != null && !existingPosterUrl.isEmpty()) {
            event.setPosterUrl(existingPosterUrl);
        }

        Toast.makeText(this, "Saving event…", Toast.LENGTH_SHORT).show();

        if (selectedImageUri != null) {
            uploadImageThenUpdate(editEventId, event);
        } else {
            persistEventUpdate(event);
        }
    }

    private void uploadImageThenUpdate(String eventId, Event event) {
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("event_posters/" + eventId + ".jpg");
        ref.putFile(selectedImageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            event.setPosterUrl(uri.toString());
                            persistEventUpdate(event);
                        })
                        .addOnFailureListener(e -> persistEventUpdate(event)))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_LONG).show());
    }

    private void persistEventUpdate(Event event) {
        repository.updateEvent(event,
                aVoid -> {
                    Toast.makeText(this, "Event updated!", Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
                        if (!event.isPrivateEvent()) {
                            new FollowRepository().notifyFollowersOfNewEvent(
                                    event.getOrganizerId(), event.getName(), event.getEventId());
                        }

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
