package com.example.eventmanager.ui;

import com.example.eventmanager.R;
import com.example.eventmanager.models.Event;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final String EVENT_QR_PREFIX = "eventmanager://event/";
    private DecoratedBarcodeView barcodeView;
    private TextView tvScanInstruction;
    private TextView tvScanHint;
    private TextView tvScanStatus;
    private boolean hasScanned = false;
    private boolean scannerInitialized = false;

    private final Runnable resetScannerRunnable = new Runnable() {
        @Override
        public void run() {
            hasScanned = false;
            if (!isFinishing()) {
                updateScannerText(
                        "Point the camera at an organizer QR code",
                        "Align the QR code inside the frame",
                        "Camera ready. Looking for an event QR code."
                );
                if (barcodeView != null) {
                    barcodeView.resume();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        barcodeView = findViewById(R.id.barcode_scanner);
        tvScanInstruction = findViewById(R.id.tv_scan_instruction);
        tvScanHint = findViewById(R.id.tv_scan_hint);
        tvScanStatus = findViewById(R.id.tv_scan_status);
        barcodeView.setStatusText("");

        findViewById(R.id.btn_back_scanner).setOnClickListener(v -> finish());
        updateScannerText(
                "Point the camera at an organizer QR code",
                "Align the QR code inside the frame",
                "Preparing camera access..."
        );

        checkCameraPermissionAndStart();
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            updateScannerText(
                    "Camera access required",
                    "Grant permission to scan the event QR",
                    "Camera permission is required for US 01.06.01."
            );
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    private void startScanning() {
        updateScannerText(
                "Point the camera at an organizer QR code",
                "Align the QR code inside the frame",
                "Camera ready. Looking for an event QR code."
        );

        if (scannerInitialized) {
            barcodeView.resume();
            return;
        }

        scannerInitialized = true;
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (hasScanned || result == null) {
                    return;
                }

                String scannedContent = result.getText() != null ? result.getText().trim() : "";
                if (scannedContent.isEmpty()) {
                    Toast.makeText(QRScannerActivity.this, "Empty QR code", Toast.LENGTH_SHORT).show();
                    scheduleRetry(
                            "QR code unreadable",
                            "Hold the code steady and try again",
                            "The scan result was empty."
                    );
                    return;
                }

                hasScanned = true;
                barcodeView.pause();

                String eventId = extractEventId(scannedContent);
                if (eventId != null) {
                    updateScannerText(
                            "Event QR detected",
                            "Opening event details",
                            "Success. Routing you to the event details page."
                    );
                    barcodeView.postDelayed(() -> openEventDetails(eventId), 450);
                } else {
                    Toast.makeText(QRScannerActivity.this,
                            "Invalid QR code. Please scan an event QR code.", Toast.LENGTH_LONG).show();
                    scheduleRetry(
                            "Invalid QR code",
                            "Only organizer event QR codes are supported",
                            "This code does not match the Event Manager QR format."
                    );
                }
            }
        });
    }

    private String extractEventId(String scannedContent) {
        if (!scannedContent.startsWith(EVENT_QR_PREFIX)) {
            return null;
        }

        String eventId = scannedContent.substring(EVENT_QR_PREFIX.length()).trim();
        return eventId.isEmpty() ? null : eventId;
    }

    private void openEventDetails(String eventId) {
        Intent intent = new Intent(QRScannerActivity.this, EventDetailsActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        startActivity(intent);
        finish();
    }

    private void scheduleRetry(String instruction, String hint, String status) {
        updateScannerText(instruction, hint, status);
        hasScanned = true;
        barcodeView.removeCallbacks(resetScannerRunnable);
        barcodeView.postDelayed(resetScannerRunnable, 1800);
    }

    private void updateScannerText(String instruction, String hint, String status) {
        tvScanInstruction.setText(instruction);
        tvScanHint.setText(hint);
        tvScanStatus.setText(status);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                updateScannerText(
                        "Camera access denied",
                        "Enable camera permission and reopen the scanner",
                        "Camera permission is required to scan event QR codes."
                );
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeView != null && !hasScanned
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeView != null) {
            barcodeView.pause();
            barcodeView.removeCallbacks(resetScannerRunnable);
        }
    }
}
