package com.example.eventmanager;

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

import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.repository.FirebaseRepository;
import com.example.eventmanager.ui.BookedEventsActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Main entry activity for the entrant onboarding flow.
 *
 * <p>This screen presents the polished sign up / log in UI while preserving the
 * app's actual device-based identification and Firestore profile flow.</p>
 *
 * <p>Behavior:
 * <ul>
 *     <li>Loads the current device ID using {@link DeviceAuthManager}.</li>
 *     <li>Attempts to preload any saved entrant profile into the form.</li>
 *     <li>Saves a profile to Firestore when the user presses Sign Up.</li>
 *     <li>Allows continuing only if a saved profile already exists when Log in is pressed.</li>
 * </ul>
 * </p>
 */
public class MainActivity extends AppCompatActivity {

    private TextInputLayout nameInputLayout;
    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;

    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;

    private FirebaseRepository repository;
    private DeviceAuthManager authManager;
    private String deviceId;

    /**
     * Initializes the polished entrant screen, sets up edge-to-edge layout behavior,
     * binds form views, loads any existing saved profile, and attaches button actions.
     *
     * @param savedInstanceState previously saved activity state, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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

    /**
     * Loads an existing entrant profile for the current device, if one exists,
     * and pre-fills the visible form fields.
     */
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
                Toast.makeText(MainActivity.this, R.string.profile_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Validates the form inputs, creates an entrant profile tied to the current device ID,
     * saves it to Firestore, and proceeds to the next polished onboarding screen on success.
     */
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
                Toast.makeText(MainActivity.this, R.string.profile_saved_message, Toast.LENGTH_SHORT).show();
                openBookedEvents();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, R.string.profile_save_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Checks whether a saved entrant profile already exists for this device.
     * If so, proceeds to the next polished onboarding screen. Otherwise, shows an error.
     */
    private void continueWithSavedProfile() {
        repository.getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                if (result == null) {
                    Toast.makeText(MainActivity.this, R.string.profile_missing_error, Toast.LENGTH_SHORT).show();
                    return;
                }

                openBookedEvents();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, R.string.profile_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Clears any visible validation errors from the sign up form.
     */
    private void clearErrors() {
        nameInputLayout.setError(null);
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
    }

    /**
     * Returns trimmed text from a Material input field.
     *
     * @param input the input field to read from
     * @return trimmed text, or an empty string if the field has no value
     */
    private String getText(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /**
     * Opens the polished booked-events onboarding screen.
     */
    private void openBookedEvents() {
        startActivity(new Intent(this, BookedEventsActivity.class));
    }
}