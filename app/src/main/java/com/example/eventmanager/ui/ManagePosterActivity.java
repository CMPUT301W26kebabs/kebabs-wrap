package com.example.eventmanager.ui;

import com.example.eventmanager.R;

import com.example.eventmanager.managers.ImageManager;
import com.example.eventmanager.repository.FirebaseRepository;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class ManagePosterActivity extends AppCompatActivity {

    private ImageView posterPreview;
    private Button btnSelectImage, btnUpload, btnRemove;

    private ImageManager imageManager;
    private FirebaseRepository repository;

    private Uri selectedImageUri;
    private String currentEventId; // Assume this is passed via Intent

    // Modern way to launch the photo picker
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    posterPreview.setImageURI(selectedImageUri);
                    btnUpload.setEnabled(true);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_poster);

        // Assume we get the eventId from the Intent extra
        currentEventId = getIntent().getStringExtra("EVENT_ID");

        imageManager = new ImageManager();
        repository = new FirebaseRepository();

        posterPreview = findViewById(R.id.image_poster_preview);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnUpload = findViewById(R.id.btn_upload_poster);
        btnRemove = findViewById(R.id.btn_remove_poster);

        btnUpload.setEnabled(false); // Disable until an image is picked

        btnSelectImage.setOnClickListener(v -> openGallery());
        btnUpload.setOnClickListener(v -> uploadPoster());
        btnRemove.setOnClickListener(v -> removePoster());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadPoster() {
        if (selectedImageUri == null) return;

        imageManager.uploadEventPoster(selectedImageUri, currentEventId,
                downloadUri -> {
                    // Image uploaded to Storage, now update Firestore with the URL
                    repository.updateEventPosterUrl(currentEventId, downloadUri.toString(),
                            aVoid -> Toast.makeText(this, "Poster uploaded successfully!", Toast.LENGTH_SHORT).show(),
                            e -> Toast.makeText(this, "Failed to update database", Toast.LENGTH_SHORT).show()
                    );
                },
                e -> Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
        );
    }

    private void removePoster() {
        imageManager.deleteEventPoster(currentEventId,
                aVoid -> {
                    // Image deleted from Storage, now clear the URL in Firestore
                    repository.updateEventPosterUrl(currentEventId, null,
                            success -> {
                                posterPreview.setImageResource(R.drawable.placeholder_image); // Reset UI
                                selectedImageUri = null;
                                btnUpload.setEnabled(false);
                                Toast.makeText(this, "Poster removed successfully!", Toast.LENGTH_SHORT).show();
                            },
                            e -> Toast.makeText(this, "Failed to clear database reference", Toast.LENGTH_SHORT).show()
                    );
                },
                e -> Toast.makeText(this, "Failed to delete from storage", Toast.LENGTH_SHORT).show()
        );
    }
}
