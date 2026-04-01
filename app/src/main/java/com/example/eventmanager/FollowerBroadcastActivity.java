package com.example.eventmanager;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.managers.DeviceAuthManager;
import com.example.eventmanager.repository.FollowRepository;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class FollowerBroadcastActivity extends AppCompatActivity {

    private EditText etTitle, etBody;
    private MaterialButton btnSend;
    private TextView tvFollowerPreview;
    private FollowRepository followRepo;
    private String organizerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follower_broadcast);

        organizerId = new DeviceAuthManager().getDeviceId(this);
        followRepo = new FollowRepository();

        etTitle = findViewById(R.id.etBroadcastTitle);
        etBody = findViewById(R.id.etBroadcastBody);
        btnSend = findViewById(R.id.btnSendBroadcast);
        tvFollowerPreview = findViewById(R.id.tvFollowerPreview);

        findViewById(R.id.btnBackBroadcast).setOnClickListener(v -> finish());

        followRepo.getFollowerCount(organizerId, count ->
                runOnUiThread(() -> tvFollowerPreview.setText(
                        String.format(Locale.getDefault(), "Sending to %d follower%s",
                                count, count == 1 ? "" : "s"))));

        btnSend.setOnClickListener(v -> sendBroadcast());
    }

    private void sendBroadcast() {
        String title = etTitle.getText().toString().trim();
        String body = etBody.getText().toString().trim();

        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, R.string.broadcast_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);
        btnSend.setText("Sending…");

        followRepo.broadcastToFollowers(organizerId, title, body,
                new FollowRepository.FollowCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(FollowerBroadcastActivity.this,
                                    R.string.broadcast_sent, Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onFailure(@androidx.annotation.NonNull String message) {
                        runOnUiThread(() -> {
                            btnSend.setEnabled(true);
                            btnSend.setText(R.string.send_broadcast);
                            Toast.makeText(FollowerBroadcastActivity.this, message,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }
}
