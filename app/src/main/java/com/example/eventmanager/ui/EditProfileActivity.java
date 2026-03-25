package com.example.eventmanager.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.example.eventmanager.adapter.RegistrationHistoryAdapter;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.Event;
import com.example.eventmanager.repository.FirebaseRepository;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EditProfileActivity extends AppCompatActivity {

    private ShapeableImageView ivAvatar;
    private TextInputEditText nameInput, emailInput;
    private RecyclerView rvRegistrationHistory;
    private RegistrationHistoryAdapter adapter;
    private List<Event> historyData = new ArrayList<>();
    private FirebaseRepository repository;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 1. Setup Firebase & Auth
        repository = new FirebaseRepository();
        deviceId = new DeviceAuthManager().getDeviceId(this);

        // 2. Bind Views
        ivAvatar = findViewById(R.id.ivAvatar);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        rvRegistrationHistory = findViewById(R.id.rvRegistrationHistory);

        // 3. Setup RecyclerView
        rvRegistrationHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RegistrationHistoryAdapter(historyData);
        rvRegistrationHistory.setAdapter(adapter);

        // 4. Listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvChangePhoto).setOnClickListener(v -> openGallery());
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnDeleteProfile).setOnClickListener(v -> {
            // Show the confirmation dialog first
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Profile")
                    .setMessage("This action is permanent. Are you sure you want to delete your profile?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // If they click "Delete", call your logic
                        deleteProfile();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // If they click "Cancel", just close the dialog
                        dialog.dismiss();
                    })
                    .show();
        });

        loadUserData();
        loadHistory();
    }

    private void openGallery() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    ivAvatar.setImageURI(uri);
                    // Update: You'll want to save this URI to Firebase later!
                }
            });

    private void loadUserData() {
        repository.getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                if (result != null) {
                    nameInput.setText(result.getName());
                    emailInput.setText(result.getEmail());

                    // Logic: If no profile URL in Firebase, set a random avatar
                    setRandomAvatar();
                }
            }
            @Override
            public void onError(Exception e) { /* Handle error */ }
        });
    }

    private void setRandomAvatar() {
       // int[] avatars = {R.drawable.ic_account_circle_24}; // Add more drawable resource IDs here
      //  int randomImg = avatars[new Random().nextInt(avatars.length)];
       // ivAvatar.setImageResource(randomImg);
    }

    private void saveProfile() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();

        // 1. Validation
        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Create the updated object
        Entrant entrant = new Entrant(deviceId);
        entrant.setName(name);
        entrant.setEmail(email);

        // 3. Save to Firebase
        repository.saveUser(entrant, new FirebaseRepository.RepoCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // SUCCESS: Show the Toast message
                Toast.makeText(getApplicationContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();

                // SUCCESS: Navigate back
                // finish() closes this activity and returns the user to ProfileActivity
                finish();
            }

            @Override
            public void onError(Exception e) {
                // FAILURE: Inform the user
                Toast.makeText(EditProfileActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteProfile() {
        repository.deleteUser(deviceId, new FirebaseRepository.RepoCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // 1. Success Toast
                Toast.makeText(getApplicationContext(), "Profile deleted successfully", Toast.LENGTH_SHORT).show();

                // 2. Navigate back to SplashActivity (The starting point)
                Intent intent = new Intent(EditProfileActivity.this, SplashActivity.class);

                // 3. Clear the whole app history
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(EditProfileActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadHistory() {
        // Temporary dummy data to test your new scrollable list
        historyData.add(new Event());
        historyData.add(new Event());
        adapter.notifyDataSetChanged();
    }
}