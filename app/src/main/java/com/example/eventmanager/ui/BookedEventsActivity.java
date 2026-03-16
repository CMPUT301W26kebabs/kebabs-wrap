package com.example.eventmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.R;

public class BookedEventsActivity extends AppCompatActivity {

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
