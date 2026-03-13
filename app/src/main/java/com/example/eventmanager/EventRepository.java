package com.example.eventmanager;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all Firestore operations on event sub-collections.
 * Operates on: events/{eventId}/waitingList
 *              events/{eventId}/winners
 *              events/{eventId}/enrolled
 *
 * Called by:
 *   - OrganizerNotificationManager (reads waitingList and winners)
 *   - AcceptDeclineActivity (writes to enrolled, deletes from winners/waitingList)
 */
public class EventRepository {

    private static final String TAG = "EventRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // -------------------------------------------------------------------------
    // CALLBACK INTERFACE
    // -------------------------------------------------------------------------

    /**
     * Returns a list of deviceIds from a sub-collection.
     * Used by OrganizerNotificationManager to get notification recipients.
     */
    public interface DeviceIdListCallback {
        void onResult(List<String> deviceIds);
    }

    // -------------------------------------------------------------------------
    // READ METHODS — used by OrganizerNotificationManager
    // -------------------------------------------------------------------------

    /**
     * Fetches all deviceIds from events/{eventId}/waitingList.
     * Called when the organizer sends a notification to the entire waiting list.
     *
     * @param eventId  The event to fetch the waiting list for.
     * @param callback Returns the list of deviceIds on success.
     */
    public void getWaitingList(@NonNull String eventId,
                               @NonNull DeviceIdListCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<String> deviceIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String deviceId = doc.getString("deviceId");
                        if (deviceId != null) deviceIds.add(deviceId);
                    }
                    Log.d(TAG, "Fetched " + deviceIds.size() + " entrants from waitingList");
                    callback.onResult(deviceIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch waitingList for event: " + eventId, e);
                    callback.onResult(new ArrayList<>());
                });
    }

    /**
     * Fetches all deviceIds from events/{eventId}/winners.
     * Called when the organizer sends winner notifications.
     *
     * @param eventId  The event to fetch winners for.
     * @param callback Returns the list of deviceIds on success.
     */
    public void getWinners(@NonNull String eventId,
                           @NonNull DeviceIdListCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection("winnersList")
                .get()
                .addOnSuccessListener(/*snapshots -> {
                    Log.d(TAG, "Raw snapshot size: " + snapshots.size());
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Log.d(TAG, "Doc ID: " + doc.getId() + " | data: " + doc.getData());
                    }
                    List<String> deviceIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String deviceId = doc.getString("deviceId");
                        if (deviceId != null) deviceIds.add(deviceId);
                    }
                    Log.d(TAG, "Fetched " + deviceIds.size() + " winners for event: " + eventId);
                    callback.onResult(deviceIds);
                })*/
                        snapshots -> {
                    List<String> deviceIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String deviceId = doc.getString("deviceId");
                        if (deviceId != null) deviceIds.add(deviceId);
                    }
                    Log.d(TAG, "Fetched " + deviceIds.size() + " winners for event: " + eventId);
                    callback.onResult(deviceIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch winners for event: " + eventId, e);
                    callback.onResult(new ArrayList<>());
                });
    }

    // -------------------------------------------------------------------------
    // WRITE/DELETE METHODS — used by AcceptDeclineActivity
    // -------------------------------------------------------------------------

    /**
     * Writes the user into events/{eventId}/enrolled/{deviceId}.
     * Called when the entrant accepts their winner invitation.
     *
     * @param eventId  The event the user is enrolling in.
     * @param deviceId The device ID of the enrolling user.
     */
    public void enrollUser(@NonNull String eventId, @NonNull String deviceId) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);

        db.collection("events")
                .document(eventId)
                .collection("enrolled")
                .document(deviceId)
                .set(data)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "User enrolled: " + deviceId + " in event: " + eventId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to enroll user: " + deviceId, e));
    }

    /**
     * Deletes the user's document from events/{eventId}/winners/{deviceId}.
     * Called on both Accept (cleanup) and Decline.
     *
     * @param eventId  The event to remove the winner from.
     * @param deviceId The device ID of the user to remove.
     */
    public void removeFromWinners(@NonNull String eventId, @NonNull String deviceId) {
        db.collection("events")
                .document(eventId)
                .collection("winnersList")
                .document(deviceId)
                .delete()
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Removed from winners: " + deviceId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to remove from winners: " + deviceId, e));
    }

    /**
     * Deletes the user's document from events/{eventId}/waitingList/{deviceId}.
     * Called only on Decline — removes the user from the event entirely.
     *
     * @param eventId  The event to remove the user from.
     * @param deviceId The device ID of the user to remove.
     */
    public void removeFromWaitingList(@NonNull String eventId, @NonNull String deviceId) {
        db.collection("events")
                .document(eventId)
                .collection("waitingList")
                .document(deviceId)
                .delete()
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Removed from waitingList: " + deviceId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to remove from waitingList: " + deviceId, e));
    }
}