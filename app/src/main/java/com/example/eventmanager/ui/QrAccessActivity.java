package com.example.eventmanager.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.R;

/**
 * Final filler screen in the polished entrant onboarding flow.
 *
 * <p>This screen is presentational only. Both actions end the temporary
 * onboarding flow for this branch without looping the user back to the
 * sign up screen.</p>
 */
public class QrAccessActivity extends AppCompatActivity {

    /**
     * Sets up the QR filler screen and ends the onboarding flow when the
     * user presses either Skip or Next.
     *
     * @param savedInstanceState previously saved activity state, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_access);

        TextView skipButton = findViewById(R.id.qrSkip);
        TextView nextButton = findViewById(R.id.qrNext);

        skipButton.setOnClickListener(v -> finishAffinity());
        nextButton.setOnClickListener(v -> finishAffinity());
    }
}