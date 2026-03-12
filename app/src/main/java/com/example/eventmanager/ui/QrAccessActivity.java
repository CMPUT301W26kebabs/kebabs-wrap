package com.example.eventmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.MainActivity;
import com.example.eventmanager.R;

public class QrAccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_access);

        TextView skipButton = findViewById(R.id.qrSkip);
        TextView nextButton = findViewById(R.id.qrNext);

        skipButton.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        nextButton.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
    }
}
