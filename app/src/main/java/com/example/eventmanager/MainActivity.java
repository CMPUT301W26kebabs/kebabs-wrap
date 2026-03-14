package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.admin.AdminEventsActivity;
import com.example.eventmanager.admin.AdminImagesActivity;
import com.example.eventmanager.admin.AdminProfilesActivity;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvEventCount, tvProfileCount, tvImageCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvEventCount   = findViewById(R.id.tv_event_count);
        tvProfileCount = findViewById(R.id.tv_profile_count);
        tvImageCount   = findViewById(R.id.tv_image_count);

        // Back button to Home Screen
        findViewById(R.id.btn_back_home).setOnClickListener(v -> finish());

        // Admin dashboard cards
        findViewById(R.id.card_events).setOnClickListener(v ->
                startActivity(new Intent(this, AdminEventsActivity.class)));
        findViewById(R.id.card_profiles).setOnClickListener(v ->
                startActivity(new Intent(this, AdminProfilesActivity.class)));
        findViewById(R.id.card_images).setOnClickListener(v ->
                startActivity(new Intent(this, AdminImagesActivity.class)));

        // Umar's test buttons
        Button btnRunLottery = findViewById(R.id.btnTestRunLottery);
        Button btnEnrolledList = findViewById(R.id.btnTestEnrolledList);

        btnRunLottery.setOnClickListener(v ->
                startActivity(new Intent(this, RunLotteryActivity.class)));
        btnEnrolledList.setOnClickListener(v -> {
            Intent intent = new Intent(this, EnrolledListActivity.class);
            intent.putExtra("EVENT_ID", "test_event_123");
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseRepository repo = FirebaseRepository.getInstance();
        try {
            repo.fetchAllActiveEvents(new FirebaseRepository.OnDocumentsLoadedListener() {
                public void onLoaded(List<DocumentSnapshot> docs) { tvEventCount.setText(docs.size() + " events"); }
                public void onError(Exception e) { tvEventCount.setText("0 events"); }
            });
            repo.fetchAllActiveProfiles(new FirebaseRepository.OnDocumentsLoadedListener() {
                public void onLoaded(List<DocumentSnapshot> docs) { tvProfileCount.setText(docs.size() + " users"); }
                public void onError(Exception e) { tvProfileCount.setText("0 users"); }
            });
            repo.fetchAllEvents(new FirebaseRepository.OnDocumentsLoadedListener() {
                public void onLoaded(List<DocumentSnapshot> docs) {
                    int count = 0;
                    for (DocumentSnapshot d : docs) {
                        String url = d.getString("posterUrl");
                        if (url != null && !url.isEmpty()) count++;
                    }
                    tvImageCount.setText(count + " posters");
                }
                public void onError(Exception e) { tvImageCount.setText("0 posters"); }
            });
        } catch (Exception e) { /* ignore */ }
    }
}
