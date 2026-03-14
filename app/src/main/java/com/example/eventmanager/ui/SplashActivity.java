package com.example.eventmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eventmanager.HomeActivity;
import com.example.eventmanager.R;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1700L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private View logoPanel;
    private MaterialButton getStartedButton;
    private View loginRow;
    private TextView checkingText;
    private FirebaseRepository repository;
    private String deviceId;
    private boolean hasNavigated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new FirebaseRepository();
        deviceId = new DeviceAuthManager().getDeviceId(this);

        logoPanel = findViewById(R.id.logo_panel);
        getStartedButton = findViewById(R.id.btn_get_started);
        loginRow = findViewById(R.id.login_row);
        checkingText = findViewById(R.id.tv_checking_status);

        getStartedButton.setOnClickListener(v -> openProfileSetup());
        findViewById(R.id.btn_log_in).setOnClickListener(v -> continueWithSavedProfile());

        prepareAnimationState();
        playEntranceAnimation();
        checkDeviceProfile();
    }

    private void prepareAnimationState() {
        logoPanel.setAlpha(0f);
        logoPanel.setScaleX(0.82f);
        logoPanel.setScaleY(0.82f);
        logoPanel.setTranslationY(32f);

        getStartedButton.setAlpha(0f);
        getStartedButton.setTranslationY(28f);
        getStartedButton.setEnabled(false);
        loginRow.setAlpha(0f);
        loginRow.setTranslationY(28f);
        findViewById(R.id.btn_log_in).setEnabled(false);
    }

    private void playEntranceAnimation() {
        logoPanel.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(700L)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .start();
    }

    private void checkDeviceProfile() {
        repository.getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                if (result != null && result.getName() != null && !result.getName().trim().isEmpty()) {
                    checkingText.setText(R.string.splash_welcome_back);
                    handler.postDelayed(() -> navigateToHome(), SPLASH_DELAY_MS);
                } else {
                    showNewUserActions();
                }
            }

            @Override
            public void onError(Exception e) {
                showNewUserActions();
            }
        });
    }

    private void showNewUserActions() {
        if (isFinishing()) {
            return;
        }

        checkingText.setText(R.string.splash_new_user_prompt);

        handler.postDelayed(() -> {
            if (isFinishing()) {
                return;
            }
            getStartedButton.setEnabled(true);
            findViewById(R.id.btn_log_in).setEnabled(true);
            getStartedButton.animate().alpha(1f).translationY(0f).setDuration(320L).start();
            loginRow.animate().alpha(1f).translationY(0f).setDuration(320L).start();
        }, 600L);
    }

    private void continueWithSavedProfile() {
        repository.getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                if (result != null && result.getName() != null && !result.getName().trim().isEmpty()) {
                    navigateToHome();
                } else {
                    Toast.makeText(SplashActivity.this, R.string.profile_missing_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(SplashActivity.this, R.string.profile_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openProfileSetup() {
        if (hasNavigated) {
            return;
        }
        hasNavigated = true;
        Intent intent = new Intent(this, EntrantSignUpActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToHome() {
        if (hasNavigated || isFinishing()) {
            return;
        }
        hasNavigated = true;
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
