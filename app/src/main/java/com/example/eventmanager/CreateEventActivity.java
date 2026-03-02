package com.example.eventmanager;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class CreateEventActivity extends AppCompatActivity {
    private EditText nameInput, capacityInput;
    private ImageView qrPreview;
    private FirebaseRepository repository;
    private QRCodeManager qrManager;
    private String generatedEventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        repository = new FirebaseRepository();
        qrManager = new QRCodeManager();

        nameInput = findViewById(R.id.edit_event_name);
        capacityInput = findViewById(R.id.edit_capacity);
        qrPreview = findViewById(R.id.image_qr_preview);
        Button btnSave = findViewById(R.id.btn_save_event);

        btnSave.setOnClickListener(v -> saveEvent());
    }

    private void saveEvent() {
        generatedEventId = UUID.randomUUID().toString();
        Bitmap qrBitmap = qrManager.generateEventQR(generatedEventId);
        qrPreview.setImageBitmap(qrBitmap);

        Event newEvent = new Event();
        newEvent.setEventId(generatedEventId);
        newEvent.setName(nameInput.getText().toString());
        newEvent.setCapacity(Integer.parseInt(capacityInput.getText().toString()));
        // Assume organizerId is fetched from DeviceAuthManager

        repository.createEvent(newEvent,
                aVoid -> Toast.makeText(this, "Event Created!", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(this, "Failed to create", Toast.LENGTH_SHORT).show()
        );
    }
}