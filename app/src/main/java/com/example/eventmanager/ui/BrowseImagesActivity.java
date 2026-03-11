package com.example.eventmanager.ui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.R;
import com.example.eventmanager.managers.ImageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows all stored event poster images in a simple grid for browsing.
 */
public class BrowseImagesActivity extends AppCompatActivity {

    private RecyclerView imagesRecyclerView;
    private ImageAdapter adapter;
    private ImageManager imageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_images);

        imagesRecyclerView = findViewById(R.id.imagesRecyclerView);
        imagesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new ImageAdapter(new ArrayList<>());
        imagesRecyclerView.setAdapter(adapter);

        imageManager = new ImageManager();
        loadImages();
    }

    private void loadImages() {
        imageManager.getAllPosterUrls(new ImageManager.ImageListCallback() {
            @Override
            public void onSuccess(List<String> imageUrls) {
                // Replace the placeholder list once the storage query finishes.
                adapter.updateImages(imageUrls);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(BrowseImagesActivity.this, "Failed to load images", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
