package com.example.eventmanager.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventmanager.R;
import com.example.eventmanager.adapter.RegistrationHistoryAdapter;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.RegistrationHistoryItem;
import com.example.eventmanager.repository.FirebaseRepository;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class EditProfileActivity extends AppCompatActivity {

    private ShapeableImageView ivAvatar;
    private TextInputLayout nameInputLayout, emailInputLayout, phoneInputLayout;
    private TextInputEditText nameInput, emailInput, phoneInput;
    private SwitchMaterial switchReceiveNotifications;
    private RecyclerView rvRegistrationHistory;
    private RegistrationHistoryAdapter adapter;
    private final List<RegistrationHistoryItem> historyData = new ArrayList<>();
    private FirebaseRepository repository;
    private String deviceId;

    /** Loaded profile — reused on save so admin/organizer flags are not cleared. */
    private Entrant loadedEntrant;

    /**
     * URI of the newly selected avatar image.
     * Null means no change — keep whatever is already saved.
     * This is a temporary content:// URI valid only for this session.
     * On save we upload it to Firebase Storage and store the download URL.
     */
    @Nullable
    private Uri pendingAvatarUri;

    @Nullable
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Must register before super.onCreate per Activity Result API contract
        pickMedia = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri == null) return;
                    pendingAvatarUri = uri;
                    // Show the new photo immediately using Glide
                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .into(ivAvatar);
                });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        repository = new FirebaseRepository();
        deviceId   = new DeviceAuthManager().getDeviceId(this);

        ivAvatar                   = findViewById(R.id.ivAvatar);
        nameInputLayout            = findViewById(R.id.nameInputLayout);
        emailInputLayout           = findViewById(R.id.emailInputLayout);
        phoneInputLayout           = findViewById(R.id.phoneInputLayout);
        nameInput                  = findViewById(R.id.nameInput);
        emailInput                 = findViewById(R.id.emailInput);
        phoneInput                 = findViewById(R.id.phoneInput);
        switchReceiveNotifications = findViewById(R.id.switchReceiveNotifications);
        rvRegistrationHistory      = findViewById(R.id.rvRegistrationHistory);

        rvRegistrationHistory.setNestedScrollingEnabled(false);
        rvRegistrationHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RegistrationHistoryAdapter(historyData);
        rvRegistrationHistory.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvChangePhoto).setOnClickListener(v -> openGallery());
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnDeleteProfile).setOnClickListener(v -> confirmDeleteProfile());

        loadUserData();
        loadRegistrationHistory();
    }

    private void openGallery() {
        if (pickMedia == null) return;
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void loadUserData() {
        repository.getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                loadedEntrant = result;
                if (result == null) {
                    switchReceiveNotifications.setChecked(true);
                    return;
                }
                if (result.getName() != null)        nameInput.setText(result.getName());
                if (result.getEmail() != null)       emailInput.setText(result.getEmail());
                if (result.getPhoneNumber() != null) phoneInput.setText(result.getPhoneNumber());
                switchReceiveNotifications.setChecked(result.isReceiveNotifications());

                // Load saved avatar using Glide — handles expired URIs and http URLs safely
                loadAvatar(result.getPhotoUrl());
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(EditProfileActivity.this,
                        R.string.profile_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Loads the avatar image safely using Glide.
     * Falls back to the default icon if the URL is null, empty, or expired.
     */
    private void loadAvatar(@Nullable String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            // No photo saved — just show the default avatar drawable
            ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }
        Glide.with(this)
                .load(photoUrl)
                .centerCrop()
                .into(ivAvatar);
    }

    private void loadRegistrationHistory() {
        repository.getRegistrationHistoryForEntrant(deviceId,
                new FirebaseRepository.RepoCallback<List<RegistrationHistoryItem>>() {
                    @Override
                    public void onSuccess(List<RegistrationHistoryItem> result) {
                        historyData.clear();
                        if (result != null) historyData.addAll(result);
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(EditProfileActivity.this,
                                R.string.registration_history_load_error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveProfile() {
        String name  = nameInput.getText()  != null ? nameInput.getText().toString().trim()  : "";
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String phone = phoneInput.getText() != null ? phoneInput.getText().toString().trim() : "";

        // Clear previous errors
        nameInputLayout.setError(null);
        emailInputLayout.setError(null);
        phoneInputLayout.setError(null);

        boolean hasError = false;

        if (name.isEmpty()) {
            nameInputLayout.setError("Name is required");
            hasError = true;
        }
        if (email.isEmpty()) {
            emailInputLayout.setError("Email is required");
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address");
            hasError = true;
        }
        if (!phone.isEmpty() && !isValidPhone(phone)) {
            phoneInputLayout.setError("Valid phone: digits only, 7–15 digits");
            hasError = true;
        }
        if (hasError) return;

        Entrant entrant = loadedEntrant != null ? loadedEntrant : new Entrant(deviceId);
        entrant.setDeviceId(deviceId);
        entrant.setName(name);
        entrant.setEmail(email);
        entrant.setPhoneNumber(phone.isEmpty() ? null : phone);
        entrant.setReceiveNotifications(switchReceiveNotifications.isChecked());

        if (pendingAvatarUri != null) {
            // Upload to Firebase Storage then save the download URL
            uploadAvatarAndSave(entrant);
        } else {
            // No photo change — save profile directly
            persistEntrant(entrant);
        }
    }

    /**
     * Uploads the selected image to Firebase Storage under users/{deviceId}/avatar.jpg
     * then saves the download URL in the entrant profile so it persists across sessions.
     */
    private void uploadAvatarAndSave(Entrant entrant) {
        com.google.firebase.storage.FirebaseStorage storage =
                com.google.firebase.storage.FirebaseStorage.getInstance();
        com.google.firebase.storage.StorageReference ref =
                storage.getReference().child("users/" + deviceId + "/avatar.jpg");

        ref.putFile(pendingAvatarUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    entrant.setPhotoUrl(downloadUri.toString());
                                    persistEntrant(entrant);
                                })
                                .addOnFailureListener(e -> {
                                    // Upload worked but URL fetch failed — save without photo URL
                                    Toast.makeText(this,
                                            "Photo saved locally but URL unavailable",
                                            Toast.LENGTH_SHORT).show();
                                    persistEntrant(entrant);
                                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Photo upload failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Still save the rest of the profile
                    persistEntrant(entrant);
                });
    }

    private void persistEntrant(Entrant entrant) {
        repository.saveUser(entrant, new FirebaseRepository.RepoCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadedEntrant = entrant;
                pendingAvatarUri = null;
                Toast.makeText(getApplicationContext(),
                        "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(EditProfileActivity.this,
                        "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isValidPhone(String phone) {
        String digits = phone.replaceAll("[\\s\\-().+]", "");
        return digits.matches("\\d{7,15}");
    }

    private void confirmDeleteProfile() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("This action is permanent. All your data, registrations, and enrollments will be erased. Are you sure?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteProfile() {
        repository.deleteUserAndAllRegistrations(deviceId,
                new FirebaseRepository.RepoCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Also delete the avatar from Storage
                        com.google.firebase.storage.FirebaseStorage.getInstance()
                                .getReference()
                                .child("users/" + deviceId + "/avatar.jpg")
                                .delete(); // best-effort, ignore result

                        Toast.makeText(getApplicationContext(),
                                "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(EditProfileActivity.this, SplashActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(EditProfileActivity.this,
                                "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}