package com.example.eventmanager.managers;

import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads poster images from Firebase Storage for display in the browse images flow.
 */
public class ImageManager {

    public interface ImageListCallback {
        void onSuccess(List<String> imageUrls);
        void onError(@NonNull Exception e);
    }

    private final FirebaseStorage storage;

    public ImageManager() {
        storage = FirebaseStorage.getInstance();
    }

    /**
     * Fetches download URLs for every poster stored in the shared posters folder.
     *
     * @param callback callback that receives the collected URLs or an error if listing fails
     */
    public void getAllPosterUrls(@NonNull ImageListCallback callback) {
        StorageReference postersRef = storage.getReference().child("posters");

        postersRef.listAll()
                .addOnSuccessListener(listResult -> {
                    List<StorageReference> items = listResult.getItems();
                    List<String> imageUrls = new ArrayList<>();

                    if (items.isEmpty()) {
                        callback.onSuccess(imageUrls);
                        return;
                    }

                    final int[] completed = {0};

                    for (StorageReference item : items) {
                        item.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    imageUrls.add(uri.toString());
                                    completed[0]++;

                                    // Wait until every storage item has responded before updating the UI.
                                    if (completed[0] == items.size()) {
                                        callback.onSuccess(imageUrls);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    completed[0]++;

                                    // Skip failed downloads but still finish once the full batch is processed.
                                    if (completed[0] == items.size()) {
                                        callback.onSuccess(imageUrls);
                                    }
                                });
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}
