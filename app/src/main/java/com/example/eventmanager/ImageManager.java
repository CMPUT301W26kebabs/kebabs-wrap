package com.example.eventmanager;

import android.net.Uri;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


public class ImageManager {
    private FirebaseStorage storage = FirebaseStorage.getInstance();

    /**
     * Uploads an image to Firebase Storage and returns the download URL.
     */
    public void uploadEventPoster(Uri imageUri, String eventId,
                                  OnSuccessListener<Uri> onSuccess,
                                  OnFailureListener onFailure) {
        // Create a reference to 'event_posters/<eventId>.jpg'
        StorageReference posterRef = storage.getReference().child("event_posters/" + eventId + ".jpg");

        UploadTask uploadTask = posterRef.putFile(imageUri);

        // Chain the upload task to retrieve the download URL
        uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return posterRef.getDownloadUrl();
                }).addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Deletes the event poster from Firebase Storage.
     */
    public void deleteEventPoster(String eventId,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        StorageReference posterRef = storage.getReference().child("event_posters/" + eventId + ".jpg");
        posterRef.delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}
