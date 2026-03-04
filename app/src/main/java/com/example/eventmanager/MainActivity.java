package com.example.eventmanager; // Keep your package name!

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnRunLottery = findViewById(R.id.btnTestRunLottery);
        Button btnEnrolledList = findViewById(R.id.btnTestEnrolledList);

        // Navigate to the Lottery Engine screen
        btnRunLottery.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RunLotteryActivity.class);
            startActivity(intent);
        });

        // Navigate to the Enrolled List screen
        btnEnrolledList.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EnrolledListActivity.class);
            // We pass a dummy Event ID here for testing so the Activity doesn't crash
            intent.putExtra("EVENT_ID", "test_event_123");
            startActivity(intent);
        });
    }
}