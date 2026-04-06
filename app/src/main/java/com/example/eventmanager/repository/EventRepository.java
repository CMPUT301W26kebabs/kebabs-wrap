package com.example.eventmanager.repository;

import com.example.eventmanager.managers.LotteryManager;
import com.example.eventmanager.managers.OrganizerNotificationManager;
import com.example.eventmanager.ui.AcceptDeclineActivity;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.Event;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles all Firestore operations on event sub-collections.
 * Operates on: events/{eventId}/waitingList, selected, inviteeList, enrolled, cancelled
 *
 * Called by:
 *   - OrganizerNotificationManager (reads waitingList and winners)
 *   - AcceptDeclineActivity (writes to enrolled, deletes from winners/waitingList)
 */
public class EventRepository {

    private static final String TAG = "EventRepository";
    private static final String COLLECTION_WAITING = "waitingList";
    private static final String COLLECTION_SELECTED = "selected";
    /** Private-event organizer invites (pending accept/decline). Not used for lottery winners. */
    private static final String COLLECTION_INVITEE_LIST = "inviteeList";
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

    /**
     * Callback for write/delete operations that either succeed or fail with a message.
     */
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
     * Lightweight value object representing an event where the user has a pending
     * invitation (lottery winner awaiting accept/decline, or private-event invite).
     */
    public static class PendingInvite {
        public final String eventId;
        public final String eventName;

        /**
         * @param eventId   Firestore document ID of the event
         * @param eventName human-readable event name displayed in the invitation banner
         */
        public PendingInvite(String eventId, String eventName) {
            this.eventId = eventId;
            this.eventName = eventName;
        }
    }

    /**
     * Callback that delivers a list of {@link PendingInvite} objects for events
     * where the user has an outstanding invitation.
     */
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

    /**
     * Alias for {@link #getSelected(String, DeviceIdListCallback)}.
     * Kept for backward compatibility with callers that use the "winners" terminology.
     *
     * @param eventId  Firestore event document ID
     * @param callback receives the list of selected/winner device IDs
     */
    public void getWinners(@NonNull String eventId,
                           @NonNull DeviceIdListCallback callback) {
        getSelected(eventId, callback);
    }

    /**
     * Fetches events where the user (deviceId) has a pending invitation: either in
     * {@code selected} (lottery winner pending enrollment) or {@code inviteeList}
     * (private-event invite pending response). Used for the invitation banner on the home screen.
     *
     * @param deviceId the entrant's device identifier
     * @param callback receives the list of {@link PendingInvite} objects
     */
    public void getEventsWhereUserIsSelected(@NonNull String deviceId,
                                             @NonNull PendingInvitesCallback callback) {
        Task<QuerySnapshot> taskSelected = db.collectionGroup(COLLECTION_SELECTED).get();
        Task<QuerySnapshot> taskInvitee = db.collectionGroup(COLLECTION_INVITEE_LIST).get();
        Tasks.whenAllComplete(taskSelected, taskInvitee)
                .addOnCompleteListener(done -> {
                    if (!taskSelected.isSuccessful() || !taskInvitee.isSuccessful()) {
                        Exception err = taskSelected.getException() != null
                                ? taskSelected.getException() : taskInvitee.getException();
                        Log.e(TAG, "Failed to fetch pending invites", err);
                        callback.onResult(new ArrayList<>());
                        return;
                    }
                    Set<String> eventIds = new LinkedHashSet<>();
                    addEventIdsFromGroup(taskSelected.getResult(), deviceId, eventIds);
                    addEventIdsFromGroup(taskInvitee.getResult(), deviceId, eventIds);
                    if (eventIds.isEmpty()) {
                        callback.onResult(new ArrayList<>());
                        return;
                    }
                    fetchEventNamesAndInvoke(new ArrayList<>(eventIds), 0, new ArrayList<>(), callback);
                });
    }

    private static void addEventIdsFromGroup(@NonNull QuerySnapshot snapshots,
                                             @NonNull String deviceId,
                                             @NonNull Set<String> eventIds) {
        for (QueryDocumentSnapshot doc : snapshots) {
            if (!doc.getId().equals(deviceId)) continue;
            CollectionReference col = doc.getReference().getParent();
            if (col == null) continue;
            DocumentReference eventRef = col.getParent();
            if (eventRef == null) continue;
            eventIds.add(eventRef.getId());
        }
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

    /**
     * Fetches all device IDs from {@code events/{eventId}/enrolled}.
     *
     * @param eventId  Firestore event document ID
     * @param callback receives the list of enrolled device IDs
     */
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
     *
     * @param eventId  Firestore event document ID
     * @param callback receives the list of cancelled device IDs
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
     *
     * @param eventId  Firestore event document ID
     * @param callback receives the list of moved device IDs on success, or an error message
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
     * Writes the user into events/{eventId}/enrolled/{deviceId} and removes them from
     * {@code selected}. Used when a lottery winner (in {@code selected}) accepts.
     * Private-event invites use {@link #acceptInviteToWaitingList} instead.
     *
     * @param eventId  Firestore event document ID
     * @param deviceId device identifier of the entrant to enroll
     * @param callback notified on success or failure
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
     * Accepts a private-event invitation: moves the user from {@code inviteeList} to
     * {@code waitingList}.
     *
     * @param eventId  Firestore event document ID
     * @param deviceId device identifier of the entrant accepting the invite
     * @param callback notified on success or failure
     */
    public void acceptInviteToWaitingList(@NonNull String eventId,
                                          @NonNull String deviceId,
                                          @NonNull OperationCallback callback) {
        db.collection("events")
                .document(eventId)
                .collection(COLLECTION_INVITEE_LIST)
                .document(deviceId)
                .get()
                .addOnSuccessListener(inviteeDoc -> {
                    if (!inviteeDoc.exists()) {
                        callback.onFailure("This invitation is no longer active.");
                        return;
                    }
                    Map<String, Object> waitData = new HashMap<>();
                    waitData.put("deviceId", deviceId);
                    if (inviteeDoc.getString("name") != null) {
                        waitData.put("name", inviteeDoc.getString("name"));
                    }
                    if (inviteeDoc.getString("email") != null) {
                        waitData.put("email", inviteeDoc.getString("email"));
                    }

                    WriteBatch batch = db.batch();
                    batch.set(db.collection("events").document(eventId)
                                    .collection(COLLECTION_WAITING).document(deviceId),
                            waitData);
                    batch.delete(db.collection("events").document(eventId)
                            .collection(COLLECTION_INVITEE_LIST).document(deviceId));

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Invite accepted to waiting list: " + deviceId + " event: " + eventId);
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to accept invite to waiting list: " + deviceId, e);
                                callback.onFailure("Could not join waiting list.");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load invitee doc: " + deviceId, e);
                    callback.onFailure("Failed to load invitation.");
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

    /**
     * Deletes the user's document from {@code events/{eventId}/selected/{deviceId}}.
     *
     * @param eventId  Firestore event document ID
     * @param deviceId device identifier of the entrant to remove
     */
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

    /**
     * Declines a pending invitation (lottery or private-event). Moves the user from
     * {@code inviteeList} and/or {@code selected} into {@code cancelled} with a
     * "declined" status, then deletes the source documents atomically.
     *
     * @param eventId  Firestore event document ID
     * @param deviceId device identifier of the entrant declining
     * @param callback notified on success or failure
     */
    public void declineInvitation(@NonNull String eventId,
                                  @NonNull String deviceId,
                                  @NonNull OperationCallback callback) {
        DocumentReference inviteeRef = db.collection("events")
                .document(eventId)
                .collection(COLLECTION_INVITEE_LIST)
                .document(deviceId);
        DocumentReference selectedRef = db.collection("events")
                .document(eventId)
                .collection(COLLECTION_SELECTED)
                .document(deviceId);

        Task<DocumentSnapshot> t1 = inviteeRef.get();
        Task<DocumentSnapshot> t2 = selectedRef.get();
        Tasks.whenAllComplete(t1, t2).addOnCompleteListener(done -> {
            if (!t1.isSuccessful() || !t2.isSuccessful()) {
                callback.onFailure("Failed to load invitation data.");
                return;
            }
            DocumentSnapshot inviteeDoc = t1.getResult();
            DocumentSnapshot selectedDoc = t2.getResult();
            DocumentSnapshot source = (inviteeDoc != null && inviteeDoc.exists())
                    ? inviteeDoc
                    : (selectedDoc != null && selectedDoc.exists() ? selectedDoc : null);
            if (source == null) {
                callback.onFailure("This invitation is no longer active.");
                return;
            }

            Map<String, Object> cancelledData = new HashMap<>();
            cancelledData.put("deviceId", deviceId);
            cancelledData.put("status", "declined");
            if (source.getString("name") != null) {
                cancelledData.put("name", source.getString("name"));
            }
            if (source.getString("email") != null) {
                cancelledData.put("email", source.getString("email"));
            }

            WriteBatch batch = db.batch();
            batch.set(db.collection("events").document(eventId)
                            .collection(COLLECTION_CANCELLED).document(deviceId),
                    cancelledData);
            if (inviteeDoc != null && inviteeDoc.exists()) {
                batch.delete(inviteeRef);
            }
            if (selectedDoc != null && selectedDoc.exists()) {
                batch.delete(selectedRef);
            }

            batch.commit()
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Declined invitation for: " + deviceId);
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to decline invitation for: " + deviceId, e);
                        callback.onFailure("Failed to decline invitation.");
                    });
        });
    }

    /**
     * Resolves canonical device id for sub-collection docs. Matches {@link LotteryManager}
     * ({@code deviceIdFromWaitlistDoc}): trim non-empty field, otherwise document id.
     */
    private String extractDeviceId(@NonNull DocumentSnapshot doc) {
        String fromField = doc.getString("deviceId");
        if (fromField != null && !fromField.trim().isEmpty()) {
            return fromField.trim();
        }
        return doc.getId();
    }
}