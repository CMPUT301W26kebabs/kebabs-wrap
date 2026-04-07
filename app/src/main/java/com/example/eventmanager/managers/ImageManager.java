package com.example.eventmanager.managers;
import com.example.eventmanager.models.Event;

import android.net.Uri;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


/**
 * Manages upload and deletion of event poster images in Firebase Storage.
 * Poster images are stored under the {@code event_posters/} path, keyed by
 * event ID.
 */
public class ImageManager {
    private FirebaseStorage storage = FirebaseStorage.getInstance();

    /**
     * Uploads an event poster image to Firebase Storage and delivers the
     * resulting download URL on success.
     *
     * @param imageUri  the local URI of the image to upload
     * @param eventId   the event ID used to derive the storage path
     * @param onSuccess callback invoked with the download {@link Uri} on success
     * @param onFailure callback invoked if the upload or URL retrieval fails
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
     * Deletes the event poster image from Firebase Storage.
     *
     * @param eventId   the event ID whose poster should be removed
     * @param onSuccess callback invoked when the deletion succeeds
     * @param onFailure callback invoked if the deletion fails
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
