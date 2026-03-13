package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AnasMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.anas_activity_main);

        try {
            Button btnEvent = null;
            if (btnEvent != null) {
                btnEvent.setOnClickListener(v -> {
                    Intent intent = new Intent(this, EventActivity.class);
                    intent.putExtra("EVENT_ID", "test_event_123");
                    startActivity(intent);
                });
            }
        } catch (Exception e) { /* button may not exist */ }
    }
}
