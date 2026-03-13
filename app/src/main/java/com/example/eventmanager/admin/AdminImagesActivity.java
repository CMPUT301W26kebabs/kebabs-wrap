package com.example.eventmanager.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmanager.FirebaseRepository;
import com.example.eventmanager.R;
import com.example.eventmanager.adapter.ImageAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminImagesActivity extends AppCompatActivity implements ImageAdapter.OnImageActionListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private ImageAdapter adapter;
    private FirebaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_images);
        repository = FirebaseRepository.getInstance();

        recyclerView = findViewById(R.id.rv_images);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        adapter = new ImageAdapter(this, new ArrayList<>(), this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        loadImages();
    }

    private void loadImages() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        repository.fetchAllEvents(new FirebaseRepository.OnDocumentsLoadedListener() {
            @Override
            public void onLoaded(List<DocumentSnapshot> documents) {
                // Filter only events that have a poster
                List<DocumentSnapshot> withPosters = new ArrayList<>();
                for (DocumentSnapshot doc : documents) {
                    String posterUrl = doc.getString("posterUrl");
                    Boolean isDeleted = doc.getBoolean("isDeleted");
                    if (posterUrl != null && !posterUrl.isEmpty()
                            && (isDeleted == null || !isDeleted)) {
                        withPosters.add(doc);
                    }
                }

                adapter.updateList(withPosters);
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(withPosters.isEmpty() ? View.GONE : View.VISIBLE);
                emptyState.setVisibility(withPosters.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AdminImagesActivity.this, "Failed to load images", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onImageRemoveClick(DocumentSnapshot eventDoc) {
        String eventName = eventDoc.getString("name");
        new AlertDialog.Builder(this)
                .setTitle("Remove Image")
                .setMessage("Remove the poster for \"" + eventName + "\"?\nThe image will be deleted from storage.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    // Remove poster URL from event document
                    repository.updateEventPosterUrl(eventDoc.getId(), null,
                            aVoid -> {
                                Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
                                loadImages();
                            },
                            e -> Toast.makeText(this, "Failed to remove image", Toast.LENGTH_LONG).show()
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
