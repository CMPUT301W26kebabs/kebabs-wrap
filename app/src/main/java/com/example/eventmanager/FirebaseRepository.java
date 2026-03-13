package com.example.eventmanager;

import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.Event;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;
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
     * Attempts to add an entrant to an event's waiting list, respecting the
     * optional capacity limit.
     *
     * @param eventId  The ID of the event.
     * @param entrant  The entrant trying to join.
     * @param callback Handles the success or failure response.
     */
    public void joinWaitingList(String eventId, Entrant entrant, WaitlistCallback callback) {
        DocumentReference eventRef = db.collection("events").document(eventId);
        CollectionReference waitlistRef = eventRef.collection("waitingList");

        // Step 1: Fetch the Event document to check its rules
        eventRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Event event = documentSnapshot.toObject(Event.class);

                if (event != null) {
                    int maxCapacity = event.getMaxWaitlistCapacity();

                    // Step 2: Check if a limit exists (assuming > 0 means limited)
                    if (maxCapacity > 0) {

                        // Query the server for the current count of the sub-collection
                        waitlistRef.count().get(AggregateSource.SERVER).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                long currentSize = task.getResult().getCount();

                                // Step 3: Enforce the limit
                                if (currentSize >= maxCapacity) {
                                    callback.onFailure("The waiting list is currently full.");
                                } else {
                                    executeAddUser(waitlistRef, entrant, callback);
                                }
                            } else {
                                callback.onFailure("Failed to verify current waitlist size.");
                            }
                        });

                    } else {
                        // No limit set, add the user immediately
                        executeAddUser(waitlistRef, entrant, callback);
                    }
                }
            } else {
                callback.onFailure("Event does not exist.");
            }
        }).addOnFailureListener(e -> callback.onFailure("Database error: " + e.getMessage()));
    }

    /**
     * Helper method to perform the actual database write for joining the waiting list.
     */
    private void executeAddUser(CollectionReference waitlistRef, Entrant entrant, WaitlistCallback callback) {
        waitlistRef.document(entrant.getDeviceId()).set(entrant)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Failed to join waitlist: " + e.getMessage()));
    }

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
        if (deviceId.equals("5c7640b6f0f59181")) {
            userData.put("name", "OrgName");
        } else {
            userData.put("name", "UserName");
        }
        userData.put("email", "");
        userData.put("phoneNumber", "");
        if (deviceId.equals("5c7640b6f0f59181")) {
            userData.put("isOrganizer", true);
        } else {
            userData.put("isOrganizer", false);
        }
        userData.put("isAdmin", false);

        db.collection("users")
                .document(deviceId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "User saved for deviceId: " + deviceId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to save user: " + deviceId, e));
    }

    /**
     * Fetches the final list of entrants who are fully enrolled in an event.
     *
     * @param eventId  The ID of the event.
     * @param callback Handles the response containing the list of entrants.
     */
    public void getEnrolledEntrants(String eventId, EntrantListCallback callback) {
        db.collection("events").document(eventId).collection("enrolled")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Entrant> enrolledList = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        if (entrant != null) {
                            enrolledList.add(entrant);
                        }
                    }
                    callback.onSuccess(enrolledList);
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Failed to load enrolled list: " + e.getMessage()));
    }
}