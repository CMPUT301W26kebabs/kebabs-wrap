package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Log.d("DeviceID", "My device ID: " + new DeviceAuthManager().getDeviceId(this));
        findViewById(R.id.btn_open_notifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        // Register the user in Firestore using their device ID on every launch.
        // SetOptions.merge() in saveUser() ensures we never overwrite existing data.
        String deviceId = new DeviceAuthManager().getDeviceId(this);
        new FirebaseRepository().saveUser(deviceId);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}