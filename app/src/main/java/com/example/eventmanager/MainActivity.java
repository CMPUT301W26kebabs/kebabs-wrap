package com.example.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.eventmanager.admin.AdminEventsActivity;
import com.example.eventmanager.admin.AdminProfilesActivity;
import com.example.eventmanager.repository.FirebaseRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvEventCount, tvProfileCount, tvImageCount;
    private FirebaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = FirebaseRepository.getInstance();
        tvEventCount   = findViewById(R.id.tv_event_count);
        tvProfileCount = findViewById(R.id.tv_profile_count);
        tvImageCount   = findViewById(R.id.tv_image_count);

        findViewById(R.id.card_events).setOnClickListener(v ->
                startActivity(new Intent(this, AdminEventsActivity.class)));
        findViewById(R.id.card_profiles).setOnClickListener(v ->
                startActivity(new Intent(this, AdminProfilesActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            repository.fetchAllActiveEvents(new FirebaseRepository.OnDocumentsLoadedListener() {
                public void onLoaded(List<DocumentSnapshot> docs) { tvEventCount.setText(docs.size() + " events"); }
                public void onError(Exception e) { tvEventCount.setText("0 events"); }
            });
            repository.fetchAllActiveProfiles(new FirebaseRepository.OnDocumentsLoadedListener() {
                public void onLoaded(List<DocumentSnapshot> docs) { tvProfileCount.setText(docs.size() + " users"); }
                public void onError(Exception e) { tvProfileCount.setText("0 users"); }
            });
            tvImageCount.setText("0 posters");
        } catch (Exception e) {
            tvEventCount.setText("0 events");
            tvProfileCount.setText("0 users");
            tvImageCount.setText("0 posters");
        }
    }
}
