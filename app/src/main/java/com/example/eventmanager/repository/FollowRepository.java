package com.example.eventmanager.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.eventmanager.Notification;
import com.example.eventmanager.NotificationRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public interface FollowCallback {
        void onSuccess();
        void onFailure(@NonNull String message);
    }

    public interface BooleanCallback {
        void onResult(boolean value);
    }

    public interface CountCallback {
        void onResult(int count);
    }

    public interface FollowingListCallback {
        void onResult(@NonNull List<String> organizerIds);
    }

    /**
     * Follow an organizer. Writes to both subcollections atomically and increments the counter.
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
     * Mass-invite all followers to a specific event's waiting list.
     */
    public void inviteAllFollowersToWaitlist(@NonNull String eventId,
                                             @NonNull String organizerId,
                                             @NonNull FollowCallback callback) {
        db.collection("users").document(organizerId)
                .collection("followers").get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) {
                        callback.onFailure("You have no followers to invite.");
                        return;
                    }
                    WriteBatch batch = db.batch();
                    int count = 0;
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        String followerId = doc.getString("deviceId");
                        if (followerId == null) followerId = doc.getId();
                        String name = doc.getString("name");

                        Map<String, Object> data = new HashMap<>();
                        data.put("deviceId", followerId);
                        data.put("joinedAt", FieldValue.serverTimestamp());
                        if (name != null) data.put("name", name);
                        data.put("invitedByOrganizer", true);

                        batch.set(db.collection("events").document(eventId)
                                .collection("waitingList").document(followerId), data);
                        count++;

                        if (count % 450 == 0) {
                            batch.commit();
                            batch = db.batch();
                        }
                    }
                    batch.commit()
                            .addOnSuccessListener(v -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure(
                                    e.getMessage() != null ? e.getMessage() : "Invite failed"));
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Could not load followers"));
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
