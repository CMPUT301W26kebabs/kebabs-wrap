package com.example.eventmanager.ui;

import com.example.eventmanager.R;
import com.example.eventmanager.models.Event;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Displays a generated QR code for a specific event, allowing organizers to share it.
 * Supports saving the QR image locally and sharing it via Android's share sheet.
 */
public class EventQRActivity extends AppCompatActivity {

    private static final String QR_SCHEME = "eventmanager://event/";

    private ImageView ivQrCode;
    private TextView tvEventName, tvEventId;
    private String eventId, eventName;
    private Bitmap qrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        eventId = getIntent().getStringExtra("EVENT_ID");
        eventName = getIntent().getStringExtra("EVENT_NAME");

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Event ID missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && Boolean.TRUE.equals(doc.getBoolean("privateEvent"))) {
                        Toast.makeText(this,
                                "QR codes are not available for private events.",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    inflateAndShowQrUi();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not load event.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void inflateAndShowQrUi() {
        setContentView(R.layout.activity_event_qr);

        ivQrCode = findViewById(R.id.ivQrCode);
        tvEventName = findViewById(R.id.tvEventName);
        tvEventId = findViewById(R.id.tvEventId);

        if (tvEventName != null) tvEventName.setText(eventName != null ? eventName : "Event");
        if (tvEventId != null) tvEventId.setText("ID: " + (eventId != null ? eventId : "N/A"));

        // Generate real QR code with eventmanager://event/{eventId} - scannable by our app
        String qrContent = QR_SCHEME + eventId;
        qrBitmap = generateQRCode(qrContent);
        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap);
        }

        // Back button
        try {
            findViewById(R.id.btnBack).setOnClickListener(v -> openMyEvents());
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

        // Share button - shares the actual QR code image so recipients can scan it
        try {
            findViewById(R.id.btnShare).setOnClickListener(v -> shareQRCode());
        } catch (Exception e) { /* no share button */ }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                openMyEvents();
            }
        });
    }

    private Bitmap generateQRCode(String content) {
        try {
            int targetSize = 512;
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    content, BarcodeFormat.QR_CODE, targetSize, targetSize, hints);

            int w = bitMatrix.getWidth();
            int h = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, false);
        } catch (WriterException e) {
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void shareQRCode() {
        if (qrBitmap == null) {
            Toast.makeText(this, "QR code not ready to share", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File cacheDir = getCacheDir();
            File qrFile = new File(cacheDir, "event_qr_" + (eventId != null ? eventId : "share") + ".png");
            try (FileOutputStream out = new FileOutputStream(qrFile)) {
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", qrFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Join my event \"" + (eventName != null ? eventName : "Event") + "\"! Scan this QR code with Event Manager.");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Event QR Code"));
        } catch (IOException e) {
            Toast.makeText(this, "Failed to share QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void openMyEvents() {
        startActivity(new Intent(this, MyEventsActivity.class));
        finish();
    }
}
