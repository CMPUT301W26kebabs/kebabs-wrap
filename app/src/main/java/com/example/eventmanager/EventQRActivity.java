package com.example.eventmanager;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmanager.QRCodeManager;

public class EventQRActivity extends AppCompatActivity {

    private ImageView qrImageView;
    private TextView eventNameTextView;
    private QRCodeManager qrManager;
    private Bitmap currentQrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_qr); // The XML we made earlier

        qrManager = new QRCodeManager();

        qrImageView = findViewById(R.id.image_generated_qr);
        eventNameTextView = findViewById(R.id.text_qr_event_name);
        Button btnSave = findViewById(R.id.btn_save_qr);
        Button btnShare = findViewById(R.id.btn_share_qr);

        // 1. Retrieve the Event data passed from CreateEventActivity
        String eventId = getIntent().getStringExtra("EVENT_ID");
        String eventName = getIntent().getStringExtra("EVENT_NAME");

        // 2. Update the UI with the Event Name
        if (eventName != null) {
            eventNameTextView.setText(eventName);
        }

        // 3. Generate the actual QR Code graphic using your Manager
        if (eventId != null) {
            currentQrBitmap = qrManager.generateEventQR(eventId);
            if (currentQrBitmap != null) {
                qrImageView.setImageBitmap(currentQrBitmap);
            } else {
                Toast.makeText(this, "Error generating QR Code", Toast.LENGTH_SHORT).show();
            }
        }

        // 4. Hook up the action buttons
        btnSave.setOnClickListener(v -> saveQrToGallery());
        btnShare.setOnClickListener(v -> shareQrCode());
    }

    private void saveQrToGallery() {
        // TODO: Implement Android MediaStore logic to save currentQrBitmap to photos
        Toast.makeText(this, "Save feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void shareQrCode() {
        // TODO: Implement Android FileProvider logic to open the share sheet
        Toast.makeText(this, "Share feature coming soon!", Toast.LENGTH_SHORT).show();
    }
}
