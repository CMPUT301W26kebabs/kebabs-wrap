package com.example.eventmanager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class EventQRActivity extends AppCompatActivity {

    private ImageView ivQrCode;
    private TextView tvEventName, tvEventId;
    private String eventId, eventName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_qr);

        eventId = getIntent().getStringExtra("EVENT_ID");
        eventName = getIntent().getStringExtra("EVENT_NAME");

        ivQrCode = findViewById(R.id.ivQrCode);
        tvEventName = findViewById(R.id.tvEventName);
        tvEventId = findViewById(R.id.tvEventId);

        if (tvEventName != null) tvEventName.setText(eventName != null ? eventName : "Event");
        if (tvEventId != null) tvEventId.setText("ID: " + (eventId != null ? eventId : "N/A"));

        // Generate REAL QR code with event data
        if (eventId != null) {
            String qrContent = "eventmanager://event/" + eventId;
            generateQRCode(qrContent);
        }

        // Back button
        try {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        } catch (Exception e) { /* no back button in layout */ }

        // Done / Go to My Events button
        try {
            Button btnDone = findViewById(R.id.btnDone);
            if (btnDone != null) {
                btnDone.setOnClickListener(v -> {
                    startActivity(new Intent(this, MyEventsActivity.class));
                    finish();
                });
            }
        } catch (Exception e) { /* no done button */ }

        // Share button
        try {
            findViewById(R.id.btnShare).setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Join my event \"" + eventName + "\" on Event Manager!\n\nEvent ID: " + eventId);
                startActivity(Intent.createChooser(shareIntent, "Share Event"));
            });
        } catch (Exception e) { /* no share button */ }
    }

    private void generateQRCode(String content) {
        try {
            int size = 800;
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MyEventsActivity.class));
        finish();
    }
}
