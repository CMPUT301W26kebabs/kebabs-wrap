package com.example.eventmanager;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;

public class AnasFirebaseRepo {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface StatusCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface EventListener {
        void onEvent(AnasEvent event);
    }

    public ListenerRegistration listenToEvent(String eventId, EventListener listener) {
        return db.collection("events").document(eventId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    AnasEvent event = snapshot.toObject(AnasEvent.class);
                    if (event != null) listener.onEvent(event);
                });
    }

    public Task<Void> joinWaitingList(String eventId, String deviceId) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("timestamp", FieldValue.serverTimestamp());
        return db.collection("events").document(eventId)
                .collection("waitingList").document(deviceId).set(data);
    }

    public Task<DocumentSnapshot> getEvent(String eventId) {
        return db.collection("events").document(eventId).get();
    }

    public Task<Void> acceptInvitation(String eventId, String deviceId) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("acceptedAt", FieldValue.serverTimestamp());
        return eventRef.collection("enrolled").document(deviceId).set(data)
                .continueWithTask(task -> eventRef.collection("selected").document(deviceId).delete());
    }

    public Task<Void> declineInvitation(String eventId, String deviceId) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("declinedAt", FieldValue.serverTimestamp());
        return eventRef.collection("cancelled").document(deviceId).set(data)
                .continueWithTask(task -> eventRef.collection("selected").document(deviceId).delete())
                .continueWithTask(task -> eventRef.collection("inviteeList").document(deviceId).delete());
    }

    public void getWaitingList(String eventId, StatusCallback callback) {
        db.collection("events").document(eventId).collection("waitingList").get()
                .addOnSuccessListener(qs -> callback.onSuccess("Loaded " + qs.size() + " entrants"))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getChosenEntrants(String eventId, StatusCallback callback) {
        db.collection("events").document(eventId).collection("selected").get()
                .addOnSuccessListener(qs -> callback.onSuccess("Loaded " + qs.size() + " chosen"))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
