package com.example.eventmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.ui.HomeActivity;
import com.example.eventmanager.R;

/**
 * QR scanner onboarding entry point that introduces QR-based event access.
 * Provides skip/next navigation that routes the user to the home screen.
 */
public class QrAccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_access);

        TextView skipButton = findViewById(R.id.qrSkip);
        TextView nextButton = findViewById(R.id.qrNext);

        skipButton.setOnClickListener(v -> goToMain());
        nextButton.setOnClickListener(v -> goToMain());
    }

    private void goToMain() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
