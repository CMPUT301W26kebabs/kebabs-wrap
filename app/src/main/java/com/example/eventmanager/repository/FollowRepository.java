package com.example.eventmanager.repository;

import com.example.eventmanager.models.Notification;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles all follow/unfollow logic between entrants and organizers.
 *
 * Firestore structure:
 *   users/{organizerId}/followers/{entrantId}  — { deviceId, name, followedAt }
 *   users/{entrantId}/following/{organizerId}   — { organizerId, name, followedAt }
 *   users/{organizerId}.followerCount           — atomic int counter
 */
public class FollowRepository {

    private static final String TAG = "FollowRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationRepository notificationRepo = new NotificationRepository();

    /**
     * Callback for follow/unfollow and broadcast operations.
     */
    public interface FollowCallback {
        void onSuccess();
        void onFailure(@NonNull String message);
    }

    /**
     * Callback that delivers a single boolean result (e.g. "is following?").
     */
    public interface BooleanCallback {
        void onResult(boolean value);
    }

    /**
     * Callback that delivers an integer count (e.g. follower count).
     */
    public interface CountCallback {
        void onResult(int count);
    }

    /**
     * Callback that delivers a list of organizer IDs the entrant follows.
     */
    public interface FollowingListCallback {
        void onResult(@NonNull List<String> organizerIds);
    }

    /**
     * Follow an organizer. Writes to both subcollections atomically and increments the counter.
     *
     * @param entrantId     device ID of the entrant who is following
     * @param entrantName   display name of the entrant (stored in the follower doc)
     * @param organizerId   device ID of the organizer being followed
     * @param organizerName display name of the organizer (stored in the following doc)
     * @param callback      notified on success or failure
     */
    public void follow(@NonNull String entrantId, @NonNull String entrantName,
                       @NonNull String organizerId, @NonNull String organizerName,
                       @NonNull FollowCallback callback) {

        Map<String, Object> followerDoc = new HashMap<>();
        followerDoc.put("deviceId", entrantId);
        followerDoc.put("name", entrantName);
        followerDoc.put("followedAt", FieldValue.serverTimestamp());

        Map<String, Object> followingDoc = new HashMap<>();
        followingDoc.put("organizerId", organizerId);
        followingDoc.put("name", organizerName);
        followingDoc.put("followedAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.set(db.collection("users").document(organizerId)
                .collection("followers").document(entrantId), followerDoc);
        batch.set(db.collection("users").document(entrantId)
                .collection("following").document(organizerId), followingDoc);
        batch.update(db.collection("users").document(organizerId),
                "followerCount", FieldValue.increment(1));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "follow failed", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Follow failed");
                });
    }

    /**
     * Unfollow an organizer. Removes both subcollection docs and decrements the counter.
     *
     * @param entrantId   device ID of the entrant who is unfollowing
     * @param organizerId device ID of the organizer being unfollowed
     * @param callback    notified on success or failure
     */
    public void unfollow(@NonNull String entrantId, @NonNull String organizerId,
                         @NonNull FollowCallback callback) {

        WriteBatch batch = db.batch();
        batch.delete(db.collection("users").document(organizerId)
                .collection("followers").document(entrantId));
        batch.delete(db.collection("users").document(entrantId)
                .collection("following").document(organizerId));
        batch.update(db.collection("users").document(organizerId),
                "followerCount", FieldValue.increment(-1));

        batch.commit()
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "unfollow failed", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Unfollow failed");
                });
    }

    /**
     * Toggle follow state: if currently following, unfollow; otherwise follow.
     *
     * @param entrantId     device ID of the entrant
     * @param entrantName   display name of the entrant
     * @param organizerId   device ID of the organizer
     * @param organizerName display name of the organizer
     * @param callback      notified on success or failure
     */
    public void toggleFollow(@NonNull String entrantId, @NonNull String entrantName,
                             @NonNull String organizerId, @NonNull String organizerName,
                             @NonNull FollowCallback callback) {
        isFollowing(entrantId, organizerId, following -> {
            if (following) {
                unfollow(entrantId, organizerId, callback);
            } else {
                follow(entrantId, entrantName, organizerId, organizerName, callback);
            }
        });
    }

    /**
     * Check if an entrant is following a specific organizer.
     *
     * @param entrantId   device ID of the entrant
     * @param organizerId device ID of the organizer
     * @param callback    receives {@code true} if the follow relationship exists
     */
    public void isFollowing(@NonNull String entrantId, @NonNull String organizerId,
                            @NonNull BooleanCallback callback) {
        db.collection("users").document(entrantId)
                .collection("following").document(organizerId)
                .get()
                .addOnSuccessListener(doc -> callback.onResult(doc.exists()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "isFollowing check failed", e);
                    callback.onResult(false);
                });
    }

    /**
     * Get the follower count for an organizer from the cached field.
     *
     * @param organizerId device ID of the organizer
     * @param callback    receives the follower count (0 if the field is absent)
     */
    public void getFollowerCount(@NonNull String organizerId, @NonNull CountCallback callback) {
        db.collection("users").document(organizerId).get()
                .addOnSuccessListener(doc -> {
                    Long count = doc.getLong("followerCount");
                    callback.onResult(count != null ? count.intValue() : 0);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getFollowerCount failed", e);
                    callback.onResult(0);
                });
    }

    /**
     * Get all organizer IDs that an entrant follows.
     *
     * @param entrantId device ID of the entrant
     * @param callback  receives the list of followed organizer IDs
     */
    public void getFollowingList(@NonNull String entrantId,
                                 @NonNull FollowingListCallback callback) {
        db.collection("users").document(entrantId)
                .collection("following").get()
                .addOnSuccessListener(qs -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String oid = doc.getString("organizerId");
                        if (oid != null) {
                            ids.add(oid);
                        } else {
                            ids.add(doc.getId());
                        }
                    }
                    callback.onResult(ids);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getFollowingList failed", e);
                    callback.onResult(new ArrayList<>());
                });
    }

    /**
     * Send a notification to all followers of an organizer about a new event.
     * Each follower's notification opt-in preference is checked before delivery.
     *
     * @param organizerId device ID of the organizer who published the event
     * @param eventName   human-readable event name shown in the notification
     * @param eventId     Firestore event document ID (attached so the notification is tappable)
     */
    public void notifyFollowersOfNewEvent(@NonNull String organizerId,
                                          @NonNull String eventName,
                                          @NonNull String eventId) {
        db.collection("users").document(organizerId)
                .collection("followers").get()
                .addOnSuccessListener(qs -> {
                    String title = "New Event from an organizer you follow!";
                    String body = eventName + " was just published. Check it out!";
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String followerId = doc.getString("deviceId");
                        if (followerId == null) followerId = doc.getId();
                        sendIfOptedIn(followerId, title, body, eventName, eventId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "notifyFollowersOfNewEvent failed", e));
    }

    /**
     * Send a custom broadcast notification to all followers.
     *
     * @param organizerId device ID of the organizer broadcasting
     * @param title       notification title
     * @param body        notification body text
     * @param callback    notified on success or failure
     */
    public void broadcastToFollowers(@NonNull String organizerId,
                                     @NonNull String title,
                                     @NonNull String body,
                                     @NonNull FollowCallback callback) {
        db.collection("users").document(organizerId)
                .collection("followers").get()
                .addOnSuccessListener(qs -> {
                    int count = qs.size();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String followerId = doc.getString("deviceId");
                        if (followerId == null) followerId = doc.getId();
                        sendIfOptedIn(followerId, title, body, null, null);
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "broadcastToFollowers failed", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Broadcast failed");
                });
    }

    /**
     * Mass-invite all followers to {@code events/{eventId}/inviteeList} with the same notification
     * and accept/decline flow as private organizer invites. Skips followers already on the
     * invitee list, waiting list, selected, or enrolled.
     *
     * @param eventId     Firestore event document ID
     * @param organizerId device ID of the organizer whose followers are being invited
     * @param callback    notified on success or failure
     */
    public void inviteAllFollowersToWaitlist(@NonNull String eventId,
                                             @NonNull String organizerId,
                                             @NonNull FollowCallback callback) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    if (!eventDoc.exists()) {
                        callback.onFailure("Event not found.");
                        return;
                    }
                    String rawName = eventDoc.getString("name");
                    final String safeEventName = (rawName != null && !rawName.trim().isEmpty())
                            ? rawName.trim() : "Event";

                    db.collection("users").document(organizerId).collection("followers").get()
                            .addOnSuccessListener(followersSnap -> {
                                if (followersSnap.isEmpty()) {
                                    callback.onFailure("You have no followers to invite.");
                                    return;
                                }

                                Task<QuerySnapshot> tInv = db.collection("events").document(eventId)
                                        .collection("inviteeList").get();
                                Task<QuerySnapshot> tWait = db.collection("events").document(eventId)
                                        .collection("waitingList").get();
                                Task<QuerySnapshot> tSel = db.collection("events").document(eventId)
                                        .collection("selected").get();
                                Task<QuerySnapshot> tEnr = db.collection("events").document(eventId)
                                        .collection("enrolled").get();

                                Tasks.whenAllComplete(tInv, tWait, tSel, tEnr)
                                        .addOnCompleteListener(done -> {
                                            if (!tInv.isSuccessful() || !tWait.isSuccessful()
                                                    || !tSel.isSuccessful() || !tEnr.isSuccessful()) {
                                                callback.onFailure("Could not load event lists.");
                                                return;
                                            }
                                            Set<String> excluded = new HashSet<>();
                                            addDeviceIdsFromSnapshot(tInv.getResult(), excluded);
                                            addDeviceIdsFromSnapshot(tWait.getResult(), excluded);
                                            addDeviceIdsFromSnapshot(tSel.getResult(), excluded);
                                            addDeviceIdsFromSnapshot(tEnr.getResult(), excluded);

                                            List<FollowerInvitee> eligible = new ArrayList<>();
                                            for (DocumentSnapshot doc : followersSnap.getDocuments()) {
                                                String followerId = doc.getString("deviceId");
                                                if (followerId == null || followerId.isEmpty()) {
                                                    followerId = doc.getId();
                                                }
                                                if (excluded.contains(followerId)) {
                                                    continue;
                                                }
                                                eligible.add(new FollowerInvitee(
                                                        followerId, doc.getString("name")));
                                            }

                                            if (eligible.isEmpty()) {
                                                callback.onFailure(
                                                        "All followers are already invited or registered for this event.");
                                                return;
                                            }

                                            commitInviteeListChunks(eligible, 0, eventId, safeEventName, callback);
                                        });
                            })
                            .addOnFailureListener(e -> callback.onFailure(
                                    e.getMessage() != null ? e.getMessage() : "Could not load followers"));
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Could not load event"));
    }

    private static void addDeviceIdsFromSnapshot(QuerySnapshot snap, Set<String> out) {
        if (snap == null) {
            return;
        }
        for (DocumentSnapshot d : snap.getDocuments()) {
            String id = d.getString("deviceId");
            if (id == null || id.trim().isEmpty()) {
                id = d.getId();
            }
            out.add(id);
        }
    }

    /**
     * Two writes per follower (invitee set + cancelled delete). Chunk to stay under 500/batch.
     */
    private void commitInviteeListChunks(@NonNull List<FollowerInvitee> eligible, int startIndex,
                                       @NonNull String eventId, @NonNull String safeEventName,
                                       @NonNull FollowCallback callback) {
        final int maxPairsPerBatch = 200;
        if (startIndex >= eligible.size()) {
            callback.onSuccess();
            return;
        }
        int end = Math.min(startIndex + maxPairsPerBatch, eligible.size());
        WriteBatch batch = db.batch();
        for (int i = startIndex; i < end; i++) {
            FollowerInvitee fe = eligible.get(i);
            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", fe.deviceId);
            if (fe.name != null) {
                data.put("name", fe.name);
            }
            data.put("invitedAt", FieldValue.serverTimestamp());
            data.put("inviteSource", "organizer");

            batch.set(db.collection("events").document(eventId)
                    .collection("inviteeList").document(fe.deviceId), data);
            batch.delete(db.collection("events").document(eventId)
                    .collection("cancelled").document(fe.deviceId));
        }
        batch.commit()
                .addOnSuccessListener(v -> {
                    for (int i = startIndex; i < end; i++) {
                        FollowerInvitee fe = eligible.get(i);
                        Notification notification = new Notification(
                                "You are invited!",
                                "Tap to accept or decline your invitation to " + safeEventName + ".",
                                safeEventName,
                                eventId
                        );
                        notificationRepo.addNotification(fe.deviceId, notification);
                    }
                    commitInviteeListChunks(eligible, end, eventId, safeEventName, callback);
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Invite failed"));
    }

    private static final class FollowerInvitee {
        final String deviceId;
        final String name;

        FollowerInvitee(String deviceId, String name) {
            this.deviceId = deviceId;
            this.name = name;
        }
    }

    private void sendIfOptedIn(String deviceId, String title, String body,
                               String eventName, String eventId) {
        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(userDoc -> {
                    Boolean optOut = userDoc.getBoolean("receiveNotifications");
                    if (optOut != null && !optOut) return;

                    Notification n = new Notification(title, body,
                            eventName != null ? eventName : "");
                    if (eventId != null) n.setEventId(eventId);
                    notificationRepo.addNotification(deviceId, n);
                })
                .addOnFailureListener(e -> {
                    Notification n = new Notification(title, body, "");
                    if (eventId != null) n.setEventId(eventId);
                    notificationRepo.addNotification(deviceId, n);
                });
    }
}
