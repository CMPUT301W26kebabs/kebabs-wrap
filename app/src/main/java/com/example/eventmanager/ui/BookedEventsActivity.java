package com.example.eventmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.R;

/**
 * First filler screen in the polished entrant onboarding flow.
 *
 * <p>This screen is presentational and is used to preview booked events
 * before advancing to the QR access filler screen.</p>
 */
public class BookedEventsActivity extends AppCompatActivity {

    /**
     * Sets up the booked-events filler screen and advances the user to the
     * QR access filler screen when either Skip or Next is pressed.
     *
     * @param savedInstanceState previously saved activity state, if any
     */
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
    }
}