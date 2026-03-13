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

import com.example.eventmanager.R;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Entrant sign-up activity for the entrant onboarding flow.
 *
 * <p>This screen presents the polished sign up / log in UI while preserving the
 * app's actual device-based identification and Firestore profile flow.</p>
 */
public class EntrantSignUpActivity extends AppCompatActivity {

    private TextInputLayout nameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;

    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;

    private FirebaseRepository repository;
    private DeviceAuthManager authManager;
    private String deviceId;

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

        nameInputLayout = findViewById(R.id.nameInputLayout);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);

        MaterialButton signUpButton = findViewById(R.id.signUpButton);
        TextView loginLink = findViewById(R.id.loginLink);

        repository = new FirebaseRepository();
        authManager = new DeviceAuthManager();
        deviceId = authManager.getDeviceId(this);

        loadExistingProfile();

        signUpButton.setOnClickListener(v -> saveEntrantProfile());
        loginLink.setOnClickListener(v -> continueWithSavedProfile());
    }

    private void loadExistingProfile() {
        repository.getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                if (result == null) {
                    return;
                }

                if (result.getName() != null) {
                    nameInput.setText(result.getName());
                }

                if (result.getEmail() != null) {
                    emailInput.setText(result.getEmail());
                }
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
        String password = getText(passwordInput);

        boolean hasError = false;

        if (name.isEmpty()) {
            nameInputLayout.setError(getString(R.string.name_required_error));
            hasError = true;
        }

        if (email.isEmpty()) {
            emailInputLayout.setError(getString(R.string.email_required_error));
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError(getString(R.string.email_invalid_error));
            hasError = true;
        }

        if (password.length() < 8) {
            passwordInputLayout.setError(getString(R.string.password_required_error));
            hasError = true;
        }

        if (hasError) {
            return;
        }

        Entrant entrant = new Entrant(deviceId);
        entrant.setName(name);
        entrant.setEmail(email);

        repository.saveUser(entrant, new FirebaseRepository.RepoCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(EntrantSignUpActivity.this, R.string.profile_saved_message, Toast.LENGTH_SHORT).show();
                openBookedEvents();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(EntrantSignUpActivity.this, R.string.profile_save_error, Toast.LENGTH_SHORT).show();
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

                openBookedEvents();
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
        passwordInputLayout.setError(null);
    }

    private String getText(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void openBookedEvents() {
        startActivity(new Intent(this, BookedEventsActivity.class));
    }
}