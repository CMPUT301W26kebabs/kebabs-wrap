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
 * Displays the list of events the current user is enrolled in.
 * Features an aurora background with a floating glass phone card,
 * and staggered entrance animations matching the onboarding flow.
 */
public class BookedEventsActivity extends AppCompatActivity {

    private ObjectAnimator glowPulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booked_events);

        TextView skipButton = findViewById(R.id.bookedEventsSkip);
        TextView nextButton = findViewById(R.id.bookedEventsNext);

        skipButton.setOnClickListener(v ->
                startActivity(new Intent(this, QrAccessActivity.class)));
        nextButton.setOnClickListener(v ->
                startActivity(new Intent(this, QrAccessActivity.class)));

        animateEntrance();
        startGlowPulse();
    }

    private void animateEntrance() {
        View illustration = findViewById(R.id.calendarIllustration);
        View title = findViewById(R.id.bookedEventsTitle);
        View subtitle = findViewById(R.id.bookedEventsSubtitle);
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

        title.setAlpha(0f);
        title.setTranslationY(50f);
        title.animate()
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

    private void startGlowPulse() {
        View glow = findViewById(R.id.calendarGlow);
        glowPulseAnimator = ObjectAnimator.ofFloat(glow, "alpha", 0.6f, 1f);
        glowPulseAnimator.setDuration(3000);
        glowPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        glowPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        glowPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        glowPulseAnimator.start();
    }

    @Override
    protected void onDestroy() {
        if (glowPulseAnimator != null) glowPulseAnimator.cancel();
        super.onDestroy();
    }
}
