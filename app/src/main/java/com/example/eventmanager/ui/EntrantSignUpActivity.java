package com.example.eventmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eventmanager.ui.HomeActivity;
import com.example.eventmanager.R;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * New-user registration screen that collects the entrant's name, email, phone number,
 * and notification opt-in preference before creating their profile in Firestore.
 */
public class EntrantSignUpActivity extends AppCompatActivity {

    private TextInputLayout nameInputLayout, emailInputLayout;
    private TextInputEditText nameInput, emailInput;
    private FirebaseRepository repository;
    private DeviceAuthManager authManager;
    private String deviceId;
    private boolean isEditProfileMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_sign_up);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        isEditProfileMode = getIntent().getBooleanExtra(ProfileActivity.EXTRA_EDIT_PROFILE, false);

        nameInputLayout = findViewById(R.id.nameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);

        MaterialButton signUpButton = findViewById(R.id.signUpButton);
        TextView loginLink = findViewById(R.id.loginLink);
        TextView signUpTitle = findViewById(R.id.signUpTitle);
        TextView signUpSubtitle = findViewById(R.id.signUpSubtitle);

        repository = new FirebaseRepository();
        authManager = new DeviceAuthManager();
        deviceId = authManager.getDeviceId(this);

        if (isEditProfileMode) {
            signUpTitle.setText(R.string.edit_profile_title);
            signUpSubtitle.setText(R.string.edit_profile_subtitle);
            signUpButton.setText(R.string.save_profile);
            findViewById(R.id.termsText).setVisibility(android.view.View.GONE);
            findViewById(R.id.orSignUpWithDivider).setVisibility(android.view.View.GONE);
            findViewById(R.id.socialButtonsContainer).setVisibility(android.view.View.GONE);
            findViewById(R.id.loginLinkContainer).setVisibility(android.view.View.GONE);
            loadExistingProfile();
        } else {
            checkExistingUser();
        }

        signUpButton.setOnClickListener(v -> saveEntrantProfile());
        loginLink.setOnClickListener(v -> continueWithSavedProfile());
    }

    private void checkExistingUser() {
        repository.getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                if (result != null && result.getName() != null && !result.getName().isEmpty()) {
                    // User already exists — skip to main
                    goToMain();
                }
            }
            @Override
            public void onError(Exception e) { /* First time user, stay on sign up */ }
        });
    }

    private void loadExistingProfile() {
        repository.getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                if (result == null) return;
                if (result.getName() != null) nameInput.setText(result.getName());
                if (result.getEmail() != null) emailInput.setText(result.getEmail());
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(EntrantSignUpActivity.this, R.string.profile_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveEntrantProfile() {
        clearErrors();
        String name = getText(nameInput);
        String email = getText(emailInput);

        boolean hasError = false;
        if (name.isEmpty()) { nameInputLayout.setError(getString(R.string.name_required_error)); hasError = true; }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.email_invalid_error));
            hasError = true;
        }
        if (hasError) return;

        Entrant entrant = new Entrant(deviceId);
        entrant.setName(name);
        entrant.setEmail(email);

        repository.saveUser(entrant, new FirebaseRepository.RepoCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(EntrantSignUpActivity.this, R.string.profile_saved_message, Toast.LENGTH_SHORT).show();
                if (isEditProfileMode) {
                    startActivity(new Intent(EntrantSignUpActivity.this, ProfileActivity.class));
                    finish();
                } else {
                    openBookedEvents();
                }
            }
            @Override
            public void onError(Exception e) {
                String details = e != null && e.getMessage() != null ? e.getMessage() : "";
                Toast.makeText(
                        EntrantSignUpActivity.this,
                        getString(R.string.profile_save_error) + (details.isEmpty() ? "" : (": " + details)),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void continueWithSavedProfile() {
        repository.getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                if (result == null) {
                    Toast.makeText(EntrantSignUpActivity.this, R.string.profile_missing_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                goToMain();
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(EntrantSignUpActivity.this, R.string.profile_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearErrors() {
        nameInputLayout.setError(null);
        emailInputLayout.setError(null);
    }

    private String getText(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void openBookedEvents() {
        startActivity(new Intent(this, BookedEventsActivity.class));
    }

    private void goToMain() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
