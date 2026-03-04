package com.example.eventmanager.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.R;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.repository.FirebaseRepository;

public class ProfileActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private Button saveButton;

    private FirebaseRepository repository;
    private DeviceAuthManager authManager;

    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        saveButton = findViewById(R.id.saveButton);

        repository = new FirebaseRepository();
        authManager = new DeviceAuthManager();

        deviceId = authManager.getDeviceId(this);

        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {

        String name = nameInput.getText().toString();
        String email = emailInput.getText().toString();
        String phone = phoneInput.getText().toString();

        Entrant entrant = new Entrant();

        entrant.setDeviceId(deviceId);
        entrant.setName(name);
        entrant.setEmail(email);
        entrant.setPhoneNumber(phone);

        repository.saveUser(entrant, new FirebaseRepository.RepoCallback<Void>() {

            @Override
            public void onSuccess(Void result) {
                Toast.makeText(ProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ProfileActivity.this, "Error saving profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}