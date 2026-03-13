package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.adapter.HomeEventAdapter;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.ui.EntrantSignUpActivity;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private RecyclerView rvUpcoming, rvNearby;
    private HomeEventAdapter upcomingAdapter, nearbyAdapter;
    private FirebaseRepository mainRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mainRepo = FirebaseRepository.getInstance();
        tvWelcome = findViewById(R.id.tv_welcome);
        rvUpcoming = findViewById(R.id.rv_upcoming_events);
        rvNearby = findViewById(R.id.rv_nearby_events);

        // Horizontal RecyclerViews
        upcomingAdapter = new HomeEventAdapter(this, new ArrayList<>());
        rvUpcoming.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvUpcoming.setAdapter(upcomingAdapter);

        nearbyAdapter = new HomeEventAdapter(this, new ArrayList<>());
        rvNearby.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvNearby.setAdapter(nearbyAdapter);

        // Role chips
        findViewById(R.id.chip_entrant).setOnClickListener(v ->
                Toast.makeText(this, "Entrant mode active", Toast.LENGTH_SHORT).show());

        // Organizer → Create Event
        findViewById(R.id.chip_organizer).setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));

        // Admin → Admin Dashboard
        findViewById(R.id.chip_admin).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        // Bottom nav
        findViewById(R.id.nav_explore).setOnClickListener(v -> { /* already here */ });

        // Events → My Events
        findViewById(R.id.nav_events).setOnClickListener(v ->
                startActivity(new Intent(this, MyEventsActivity.class)));

        // Scan → QR Scanner (using ZXing)
        findViewById(R.id.nav_scan).setOnClickListener(v -> {
            try {
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, 100);
            } catch (Exception e) {
                Toast.makeText(this, "QR Scanner not available. Install a QR scanner app.", Toast.LENGTH_LONG).show();
            }
        });

        // Profile
        findViewById(R.id.nav_profile).setOnClickListener(v ->
                startActivity(new Intent(this, EntrantSignUpActivity.class)));

        loadUserName();
        loadEvents();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            String scannedContent = data.getStringExtra("SCAN_RESULT");
            if (scannedContent != null && scannedContent.startsWith("eventmanager://event/")) {
                String eventId = scannedContent.replace("eventmanager://event/", "");
                Intent intent = new Intent(this, EventActivity.class);
                intent.putExtra("EVENT_ID", eventId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadUserName() {
        String deviceId = new DeviceAuthManager().getDeviceId(this);
        com.example.eventmanager.repository.FirebaseRepository repo = new com.example.eventmanager.repository.FirebaseRepository();
        repo.getUser(deviceId, new com.example.eventmanager.repository.FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                if (result != null && result.getName() != null) {
                    tvWelcome.setText("Welcome back " + result.getName() + " 👋,");
                } else {
                    tvWelcome.setText("Welcome 👋,");
                }
            }
            @Override
            public void onError(Exception e) { tvWelcome.setText("Welcome 👋,"); }
        });
    }

    private void loadEvents() {
        mainRepo.fetchAllActiveEvents(new FirebaseRepository.OnDocumentsLoadedListener() {
            @Override
            public void onLoaded(List<DocumentSnapshot> docs) {
                upcomingAdapter.updateList(docs);
                nearbyAdapter.updateList(docs);
            }
            @Override
            public void onError(Exception e) { }
        });
    }
}
