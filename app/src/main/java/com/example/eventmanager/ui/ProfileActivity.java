package com.example.eventmanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.eventmanager.R;
import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.repository.FirebaseRepository;

public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_PROFILE = "edit_profile";

    private TextView tvName, tvEmail, tvInitial;
    private ImageView ivProfileAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvName          = findViewById(R.id.tv_profile_name);
        tvEmail         = findViewById(R.id.tv_profile_email);
        tvInitial       = findViewById(R.id.tv_profile_initial);
        ivProfileAvatar = findViewById(R.id.iv_profile_avatar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        loadProfile();
    }

    private void loadProfile() {
        String deviceId = new DeviceAuthManager().getDeviceId(this);
        new FirebaseRepository().getUser(deviceId,
                new FirebaseRepository.RepoCallback<Entrant>() {
                    @Override
                    public void onSuccess(Entrant result) {
                        if (result == null) {
                            showFallback("?");
                            tvName.setText("User");
                            tvEmail.setText("No profile yet");
                            return;
                        }

                        String name  = result.getName();
                        String email = result.getEmail();

                        tvName.setText(name  != null && !name.isEmpty()  ? name  : "User");
                        tvEmail.setText(email != null && !email.isEmpty() ? email : "No email");

                        String initial = (name != null && !name.isEmpty())
                                ? String.valueOf(name.charAt(0)).toUpperCase() : "?";

                        String photoUrl = result.getPhotoUrl();
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            // Show photo, hide initials
                            ivProfileAvatar.setVisibility(View.VISIBLE);
                            tvInitial.setVisibility(View.INVISIBLE);
                            Glide.with(ProfileActivity.this)
                                    .load(photoUrl)
                                    .centerCrop()
                                    .into(ivProfileAvatar);
                        } else {
                            showFallback(initial);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        showFallback("?");
                        tvName.setText("User");
                        tvEmail.setText("Unable to load profile");
                    }
                });
    }

    private void showFallback(String initial) {
        ivProfileAvatar.setVisibility(View.GONE);
        tvInitial.setVisibility(View.VISIBLE);
        tvInitial.setText(initial);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }
}