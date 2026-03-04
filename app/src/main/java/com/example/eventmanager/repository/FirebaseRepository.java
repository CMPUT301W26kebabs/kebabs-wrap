package com.example.eventmanager.repository;

import androidx.annotation.NonNull;

import com.example.eventmanager.models.Entrant;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseRepository {

    private final FirebaseFirestore db;

    public FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface RepoCallback<T> {
        void onSuccess(T result);
        void onError(@NonNull Exception e);
    }

    public void getUser(String deviceId, RepoCallback<Entrant> callback) {

        db.collection("users")
                .document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.exists()) {
                        Entrant entrant = snapshot.toObject(Entrant.class);
                        callback.onSuccess(entrant);
                    } else {
                        callback.onSuccess(null);
                    }

                })
                .addOnFailureListener(callback::onError);
    }

    public void saveUser(Entrant entrant, RepoCallback<Void> callback) {

        db.collection("users")
                .document(entrant.getDeviceId())
                .set(entrant)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}