package com.example.eventmanager;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final String COLLECTION_WAITING = "waitingList";
    private static final String COLLECTION_SELECTED = "selected";
    private static final String COLLECTION_ENROLLED = "enrolled";
    private static final String COLLECTION_CANCELLED = "cancelled";
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

    public interface OperationCallback {
        void onSuccess();
        void onFailure(@NonNull String message);
    }

    /** Returns device IDs moved from {@code selected} to {@code cancelled} (organizer non-enrollment). */
    public interface CancelNonEnrollmentCallback {
        void onSuccess(@NonNull List<String> movedDeviceIds);
        void onFailure(@NonNull String message);
    }

    /**
     * Result for events where the user is in the selected (pending accept/decline) list.
     */
    public static class PendingInvite {
        public final String eventId;
        public final String eventName;

        public PendingInvite(String eventId, String eventName) {
            this.eventId = eventId;
            this.eventName = eventName;
        }
    }

    public interface PendingInvitesCallback {
        void onResult(List<PendingInvite> invites);
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
        getWaitingList(eventId, Source.DEFAULT, callback);
    }

    /**
     * Same as {@link #getWaitingList(String, DeviceIdListCallback)} but allows choosing the read
     * source (e.g. {@link Source#SERVER} right after a lottery so the cache is not stale).
     */
    public void getWaitingList(@NonNull String eventId,
                               @NonNull Source source,
                               @NonNull DeviceIdListCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection(COLLECTION_WAITING)
                .get(source)
                .addOnSuccessListener(snapshots -> {
                    List<String> deviceIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String deviceId = extractDeviceId(doc);
                        if (deviceId != null) deviceIds.add(deviceId);
                    }
                    Log.d(TAG, "Fetched " + deviceIds.size() + " entrants from waitingList (source=" + source + ")");
                    callback.onResult(deviceIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch waitingList for event: " + eventId, e);
                    callback.onResult(new ArrayList<>());
                });
    }

    /**
     * Fetches all deviceIds from events/{eventId}/selected.
     * Called when the organizer sends selected/winner notifications.
     *
     * @param eventId  The event to fetch selected entrants for.
     * @param callback Returns the list of deviceIds on success.
     */
    public void getSelected(@NonNull String eventId,
                            @NonNull DeviceIdListCallback callback) {
        getSelected(eventId, Source.DEFAULT, callback);
    }

    /** Same as {@link #getSelected(String, DeviceIdListCallback)} with an explicit read source. */
    public void getSelected(@NonNull String eventId,
                            @NonNull Source source,
                            @NonNull DeviceIdListCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection(COLLECTION_SELECTED)
                .get(source)
                .addOnSuccessListener(snapshots -> {
                    List<String> deviceIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String deviceId = extractDeviceId(doc);
                        if (deviceId != null) deviceIds.add(deviceId);
                    }
                    Log.d(TAG, "Fetched " + deviceIds.size() + " selected entrants for event: " + eventId
                            + " (source=" + source + ")");
                    callback.onResult(deviceIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch selected entrants for event: " + eventId, e);
                    callback.onResult(new ArrayList<>());
                });
    }

    public void getWinners(@NonNull String eventId,
                           @NonNull DeviceIdListCallback callback) {
        getSelected(eventId, callback);
    }

    /**
     * Fetches events where the user (deviceId) is in the selected subcollection
     * (pending accept/decline). Used for the lottery banner on the home screen.
     */
    public void getEventsWhereUserIsSelected(@NonNull String deviceId,
                                             @NonNull PendingInvitesCallback callback) {
        db.collectionGroup(COLLECTION_SELECTED)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<String> eventIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        if (!doc.getId().equals(deviceId)) continue;
                        CollectionReference selectedCol = doc.getReference().getParent();
                        if (selectedCol == null) continue;
                        DocumentReference eventRef = selectedCol.getParent();
                        if (eventRef == null) continue;
                        eventIds.add(eventRef.getId());
                    }
                    if (eventIds.isEmpty()) {
                        callback.onResult(new ArrayList<>());
                        return;
                    }
                    fetchEventNamesAndInvoke(eventIds, 0, new ArrayList<>(), callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch pending invites", e);
                    callback.onResult(new ArrayList<>());
                });
    }

    private void fetchEventNamesAndInvoke(List<String> eventIds, int index,
                                          List<PendingInvite> accumulated,
                                          PendingInvitesCallback callback) {
        if (index >= eventIds.size()) {
            Log.d(TAG, "Found " + accumulated.size() + " pending invites");
            callback.onResult(accumulated);
            return;
        }
        String eventId = eventIds.get(index);
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    String eventName = doc != null && doc.exists() && doc.getString("name") != null
                            ? doc.getString("name") : "Event";
                    accumulated.add(new PendingInvite(eventId, eventName));
                    fetchEventNamesAndInvoke(eventIds, index + 1, accumulated, callback);
                })
                .addOnFailureListener(e -> {
                    accumulated.add(new PendingInvite(eventId, "Event"));
                    fetchEventNamesAndInvoke(eventIds, index + 1, accumulated, callback);
                });
    }

    public void getEnrolled(@NonNull String eventId,
                            @NonNull DeviceIdListCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection(COLLECTION_ENROLLED)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<String> deviceIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String deviceId = extractDeviceId(doc);
                        if (deviceId != null) deviceIds.add(deviceId);
                    }
                    Log.d(TAG, "Fetched " + deviceIds.size() + " enrolled entrants for event: " + eventId);
                    callback.onResult(deviceIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch enrolled entrants for event: " + eventId, e);
                    callback.onResult(new ArrayList<>());
                });
    }

    /**
     * Fetches deviceIds from {@code events/{eventId}/cancelled}.
     * US 02.07.03 — notify cancelled entrants.
     */
    public void getCancelled(@NonNull String eventId,
                             @NonNull DeviceIdListCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection(COLLECTION_CANCELLED)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<String> deviceIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String deviceId = extractDeviceId(doc);
                        if (deviceId != null) deviceIds.add(deviceId);
                    }
                    Log.d(TAG, "Fetched " + deviceIds.size() + " cancelled entrants for event: " + eventId);
                    callback.onResult(deviceIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch cancelled entrants for event: " + eventId, e);
                    callback.onResult(new ArrayList<>());
                });
    }

    /**
     * Moves every entrant in {@code selected} who is not in {@code enrolled} into {@code cancelled}
     * (invited via lottery but never completed enrollment). US 02.06.04.
     */
    public void cancelSelectedWithoutEnrollment(@NonNull String eventId,
                                                @NonNull CancelNonEnrollmentCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection(COLLECTION_SELECTED)
                .get()
                .addOnSuccessListener(selectedSnap -> {
                    db.collection("events")
                            .document(eventId)
                            .collection(COLLECTION_ENROLLED)
                            .get()
                            .addOnSuccessListener(enrolledSnap -> {
                                Set<String> enrolledIds = new HashSet<>();
                                for (QueryDocumentSnapshot d : enrolledSnap) {
                                    String id = extractDeviceId(d);
                                    if (id != null) enrolledIds.add(id);
                                }
                                WriteBatch batch = db.batch();
                                List<String> moved = new ArrayList<>();
                                for (QueryDocumentSnapshot selectedDoc : selectedSnap) {
                                    String deviceId = extractDeviceId(selectedDoc);
                                    if (deviceId == null) {
                                        continue;
                                    }
                                    if (enrolledIds.contains(deviceId)) {
                                        continue;
                                    }
                                    Map<String, Object> cancelledData = new HashMap<>();
                                    cancelledData.put("deviceId", deviceId);
                                    cancelledData.put("status", "organizer_no_signup");
                                    if (selectedDoc.getString("name") != null) {
                                        cancelledData.put("name", selectedDoc.getString("name"));
                                    }
                                    if (selectedDoc.getString("email") != null) {
                                        cancelledData.put("email", selectedDoc.getString("email"));
                                    }
                                    batch.set(db.collection("events").document(eventId)
                                                    .collection(COLLECTION_CANCELLED).document(deviceId),
                                            cancelledData);
                                    batch.delete(selectedDoc.getReference());
                                    moved.add(deviceId);
                                }
                                if (moved.isEmpty()) {
                                    callback.onFailure("No invited entrants without enrollment to cancel.");
                                    return;
                                }
                                final int cancelledCount = moved.size();
                                batch.commit()
                                        .addOnSuccessListener(unused -> {
                                            Log.d(TAG, "Organizer cancelled " + cancelledCount + " non-enrolled selected entrants");
                                            callback.onSuccess(moved);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Batch cancel non-signups failed", e);
                                            callback.onFailure("Could not cancel entrants.");
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to read enrolled for cancelSelectedWithoutEnrollment", e);
                                callback.onFailure("Could not load enrollment data.");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to read selected for cancelSelectedWithoutEnrollment", e);
                    callback.onFailure("Could not load invited entrants.");
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
    public void enrollUser(@NonNull String eventId,
                           @NonNull String deviceId,
                           @NonNull OperationCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection(COLLECTION_SELECTED)
                .document(deviceId)
                .get()
                .addOnSuccessListener(selectedDoc -> {
                    if (!selectedDoc.exists()) {
                        callback.onFailure("This invitation is no longer active.");
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("deviceId", deviceId);

                    if (selectedDoc.getString("name") != null) {
                        data.put("name", selectedDoc.getString("name"));
                    }
                    if (selectedDoc.getString("email") != null) {
                        data.put("email", selectedDoc.getString("email"));
                    }

                    WriteBatch batch = db.batch();
                    batch.set(db.collection("events").document(eventId)
                                    .collection(COLLECTION_ENROLLED).document(deviceId),
                            data);
                    batch.delete(db.collection("events").document(eventId)
                            .collection(COLLECTION_SELECTED).document(deviceId));

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "User enrolled: " + deviceId + " in event: " + eventId);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to enroll user: " + deviceId, e);
                                callback.onFailure("Failed to enroll entrant.");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load selected entrant before enrollment: " + deviceId, e);
                    callback.onFailure("Failed to load selected entrant.");
                });
    }

    /**
     * Deletes the user's document from events/{eventId}/winners/{deviceId}.
     * Called on both Accept (cleanup) and Decline.
     *
     * @param eventId  The event to remove the winner from.
     * @param deviceId The device ID of the user to remove.
     */
    public void removeFromWinners(@NonNull String eventId, @NonNull String deviceId) {
        removeFromSelected(eventId, deviceId);
    }

    public void removeFromSelected(@NonNull String eventId, @NonNull String deviceId) {
        db.collection("events")
                .document(eventId)
                .collection(COLLECTION_SELECTED)
                .document(deviceId)
                .delete()
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Removed from selected: " + deviceId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to remove from selected: " + deviceId, e));
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
                .collection(COLLECTION_WAITING)
                .document(deviceId)
                .delete()
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Removed from waitingList: " + deviceId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to remove from waitingList: " + deviceId, e));
    }

    public void declineInvitation(@NonNull String eventId,
                                  @NonNull String deviceId,
                                  @NonNull OperationCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection(COLLECTION_SELECTED)
                .document(deviceId)
                .get()
                .addOnSuccessListener(selectedDoc -> {
                    if (!selectedDoc.exists()) {
                        callback.onFailure("This invitation is no longer active.");
                        return;
                    }

                    Map<String, Object> cancelledData = new HashMap<>();
                    cancelledData.put("deviceId", deviceId);
                    cancelledData.put("status", "declined");

                    if (selectedDoc.getString("name") != null) {
                        cancelledData.put("name", selectedDoc.getString("name"));
                    }
                    if (selectedDoc.getString("email") != null) {
                        cancelledData.put("email", selectedDoc.getString("email"));
                    }

                    WriteBatch batch = db.batch();
                    batch.set(db.collection("events").document(eventId)
                                    .collection(COLLECTION_CANCELLED).document(deviceId),
                            cancelledData);
                    batch.delete(db.collection("events").document(eventId)
                            .collection(COLLECTION_SELECTED).document(deviceId));

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Declined invitation for: " + deviceId);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to decline invitation for: " + deviceId, e);
                                callback.onFailure("Failed to decline invitation.");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load selected entrant before decline: " + deviceId, e);
                    callback.onFailure("Failed to load invitation data.");
                });
    }

    private String extractDeviceId(@NonNull DocumentSnapshot doc) {
        String deviceId = doc.getString("deviceId");
        return deviceId != null ? deviceId : doc.getId();
    }
}