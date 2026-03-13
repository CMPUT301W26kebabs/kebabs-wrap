package com.example.eventmanager;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles core Firestore operations for users and events.
 * Notification-specific operations are handled by NotificationRepository.
 */
public class FirebaseRepository {

    private static final String TAG = "FirebaseRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Saves or updates a user document in Firestore using their device ID.
     * Called on first launch to register the user in the database.
     *
     * Firestore path: users/{deviceId}
     *
     * @param deviceId The unique hardware ID of the user's device.
     */
    public void saveUser(@NonNull String deviceId) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("deviceId", deviceId);

        db.collection("users")
                .document(deviceId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "User saved for deviceId: " + deviceId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to save user: " + deviceId, e));
    }
}