package com.example.eventmanager.repository;

import androidx.annotation.NonNull;

import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.Event;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Central repository for reading and writing entrant and event data in Firestore.
 */
public class FirebaseRepository {

    private final FirebaseFirestore db;

    public FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface RepoCallback<T> {
        void onSuccess(T result);
        void onError(@NonNull Exception e);
    }

    /**
     * Retrieves the entrant profile for a device identifier.
     *
     * @param deviceId unique device identifier used as the user document id
     * @param callback callback that receives the entrant or {@code null} when no profile exists
     */
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

    /**
     * Saves or overwrites an entrant profile in Firestore.
     *
     * @param entrant entrant profile to persist
     * @param callback callback notified when the write completes or fails
     */
    public void saveUser(Entrant entrant, RepoCallback<Void> callback) {
        db.collection("users")
                .document(entrant.getDeviceId())
                .set(entrant)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    /**
     * Loads an event document by its identifier.
     *
     * @param eventId event document id
     * @param callback callback that receives the event or {@code null} when it does not exist
     */
    public void getEventById(String eventId, RepoCallback<Event> callback) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Event event = snapshot.toObject(Event.class);
                        callback.onSuccess(event);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Adds an entrant to an event waiting list.
     *
     * @param eventId event document id
     * @param entrant entrant being added to the waiting list
     * @param callback callback notified when the sign-up write completes or fails
     */
    public void signUpForEvent(String eventId, Entrant entrant, RepoCallback<Void> callback) {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .document(entrant.getDeviceId())
                .set(entrant)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}
