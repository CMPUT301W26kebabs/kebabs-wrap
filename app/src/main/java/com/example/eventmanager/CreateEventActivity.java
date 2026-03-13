package com.example.eventmanager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.UUID;
import com.example.eventmanager.models.Event;
public class CreateEventActivity extends AppCompatActivity {
    private EditText nameInput, capacityInput;
    private FirebaseRepository repository;
    private String generatedEventId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);
        repository = new FirebaseRepository();
        nameInput = findViewById(R.id.editEventName);
        capacityInput = findViewById(R.id.editCapacity);
        Button btnCreateGenerate = findViewById(R.id.btnCreateEvent);
        btnCreateGenerate.setOnClickListener(v -> saveEventAndShowQR());
    }
    private void saveEventAndShowQR() {
        String nameStr = nameInput.getText().toString().trim();
        String capacityStr = capacityInput.getText().toString().trim();
        if (nameStr.isEmpty() || capacityStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        generatedEventId = UUID.randomUUID().toString();
        String currentDeviceId = "temporary-device-id-123";
        Event newEvent = new Event();
        newEvent.setEventId(generatedEventId);
        newEvent.setName(nameStr);
        newEvent.setCapacity(Integer.parseInt(capacityStr));
        newEvent.setOrganizerId(currentDeviceId);
        repository.createEvent(newEvent,
                aVoid -> {
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
