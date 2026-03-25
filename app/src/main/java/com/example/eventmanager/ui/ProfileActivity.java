package com.example.eventmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.R;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.repository.FirebaseRepository;

/**
 * Profile screen for viewing and editing the current user's entrant profile.
 * US 01.02.01: Profile Setup / Edit Profile
 */
public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_PROFILE = "edit_profile";

    private TextView tvName, tvEmail, tvInitial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvName = findViewById(R.id.tv_profile_name);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvInitial = findViewById(R.id.tv_profile_initial);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_edit_profile).setOnClickListener(v -> openEditProfile());

        loadProfile();
    }

    private void loadProfile() {
        String deviceId = new DeviceAuthManager().getDeviceId(this);
        new FirebaseRepository().getUser(deviceId, new FirebaseRepository.RepoCallback<Entrant>() {
            @Override
            public void onSuccess(Entrant result) {
                if (result != null) {
                    String name = result.getName();
                    String email = result.getEmail();
                    tvName.setText(name != null && !name.isEmpty() ? name : "User");
                    tvEmail.setText(email != null && !email.isEmpty() ? email : "No email");
                    String initial = (name != null && !name.isEmpty())
                            ? String.valueOf(name.charAt(0)).toUpperCase()
                            : "?";
                    tvInitial.setText(initial);
                } else {
                    tvName.setText("User");
                    tvEmail.setText("No profile yet");
                    tvInitial.setText("?");
                }
            }
            @Override
            public void onError(Exception e) {
                tvName.setText("User");
                tvEmail.setText("Unable to load profile");
                tvInitial.setText("?");
            }
        });
    }

    private void openEditProfile() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        //intent.putExtra(EXTRA_EDIT_PROFILE, true);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }
}
