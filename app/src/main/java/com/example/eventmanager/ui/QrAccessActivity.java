package com.example.eventmanager.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.R;

/**
 * QR scanner onboarding entry point that introduces QR-based event access.
 * Features an aurora background, glass phone illustration with animated
 * scan line, and staggered entrance animations.
 */
public class QrAccessActivity extends AppCompatActivity {

    private ObjectAnimator scanLineAnimator;
    private ObjectAnimator glowPulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_access);

        TextView skipButton = findViewById(R.id.qrSkip);
        TextView nextButton = findViewById(R.id.qrNext);

        skipButton.setOnClickListener(v -> goToMain());
        nextButton.setOnClickListener(v -> goToMain());

        animateEntrance();
        startScanLineAnimation();
        startGlowPulse();
    }

    private void animateEntrance() {
        View illustration = findViewById(R.id.qrIllustration);
        View message = findViewById(R.id.qrMessageText);
        View subtitle = findViewById(R.id.qrSubtitle);
        View navBar = findViewById(R.id.navBar);

        DecelerateInterpolator decelerate = new DecelerateInterpolator(2.5f);
        OvershootInterpolator overshoot = new OvershootInterpolator(1.2f);

        illustration.setAlpha(0f);
        illustration.setTranslationY(80f);
        illustration.setScaleX(0.9f);
        illustration.setScaleY(0.9f);
        illustration.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(900)
                .setStartDelay(200)
                .setInterpolator(overshoot)
                .start();

        message.setAlpha(0f);
        message.setTranslationY(50f);
        message.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(550)
                .setInterpolator(decelerate)
                .start();

        subtitle.setAlpha(0f);
        subtitle.setTranslationY(30f);
        subtitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(750)
                .setInterpolator(decelerate)
                .start();

        navBar.setAlpha(0f);
        navBar.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(1000)
                .start();
    }

    private void startScanLineAnimation() {
        View scanLine = findViewById(R.id.scanLine);
        scanLine.setAlpha(0.8f);

        scanLineAnimator = ObjectAnimator.ofFloat(scanLine, "translationY", -40f, 40f);
        scanLineAnimator.setDuration(2000);
        scanLineAnimator.setRepeatCount(ValueAnimator.INFINITE);
        scanLineAnimator.setRepeatMode(ValueAnimator.REVERSE);
        scanLineAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scanLineAnimator.setStartDelay(1100);
        scanLineAnimator.start();
    }

    private void startGlowPulse() {
        View glow = findViewById(R.id.phoneGlow);
        glowPulseAnimator = ObjectAnimator.ofFloat(glow, "alpha", 0.6f, 1f);
        glowPulseAnimator.setDuration(3000);
        glowPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        glowPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        glowPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        glowPulseAnimator.start();
    }

    @Override
    protected void onDestroy() {
        if (scanLineAnimator != null) scanLineAnimator.cancel();
        if (glowPulseAnimator != null) glowPulseAnimator.cancel();
        super.onDestroy();
    }

    private void goToMain() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
