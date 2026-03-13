package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.UUID;

public class CreateEventActivity extends AppCompatActivity {

    private EditText nameInput, capacityInput;
    private FirebaseRepository repository;
    private String generatedEventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        repository = new FirebaseRepository();

        // 1. Link to the correct IDs from your updated Figma XML layout
        nameInput = findViewById(R.id.edit_event_name);
        capacityInput = findViewById(R.id.edit_capacity);

        // 2. Link to the correct Button ID
        Button btnCreateGenerate = findViewById(R.id.btn_create_generate);

        btnCreateGenerate.setOnClickListener(v -> saveEventAndShowQR());
    }

    private void saveEventAndShowQR() {
        String nameStr = nameInput.getText().toString().trim();
        String capacityStr = capacityInput.getText().toString().trim();

        // Validation check so the app doesn't crash if fields are empty
        if (nameStr.isEmpty() || capacityStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        generatedEventId = UUID.randomUUID().toString();

        // TODO: Replace with Fahad's DeviceAuthManager once his branch is merged
        String currentDeviceId = "temporary-device-id-123";

        Event newEvent = new Event();
        newEvent.setEventId(generatedEventId);
        newEvent.setName(nameStr);
        newEvent.setCapacity(Integer.parseInt(capacityStr));
        newEvent.setOrganizerId(currentDeviceId);

        repository.createEvent(newEvent,
                aVoid -> {
                    // 3. Move to the EventQRActivity screen to display the QR code!
                    Intent intent = new Intent(CreateEventActivity.this, EventQRActivity.class);
                    intent.putExtra("EVENT_ID", generatedEventId);
                    intent.putExtra("EVENT_NAME", newEvent.getName());
                    startActivity(intent);
                    finish();
                },
                e -> Toast.makeText(this, "Failed to create", Toast.LENGTH_SHORT).show()
        );
    }
}