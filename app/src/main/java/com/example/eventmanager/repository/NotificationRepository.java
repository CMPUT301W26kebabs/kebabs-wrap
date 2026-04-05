package com.example.eventmanager.repository;

import com.example.eventmanager.models.Notification;
import com.example.eventmanager.ui.NotificationsActivity;
import com.example.eventmanager.repository.FirebaseRepository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all Firestore operations related to in-app notifications.
 * Operates on the sub-collection: users/{deviceId}/notifications
 *
 * Kept separate from FirebaseRepository to follow single responsibility —
 * FirebaseRepository handles users and events, this class handles notifications.
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // -------------------------------------------------------------------------
    // CALLBACK INTERFACES
    // These allow Firestore's async results to be passed back to the caller.
    // -------------------------------------------------------------------------

    /**
     * Callback interface for real-time notification list updates.
     * NotificationsActivity implements this to receive live data.
     */
    public interface NotificationListCallback {
        void onNotificationsUpdated(List<Notification> notifications);
    }

    // -------------------------------------------------------------------------
    // PUBLIC METHODS
    // -------------------------------------------------------------------------

    /**
     * Writes a new notification document to Firestore.
     * Called when the lottery runs and a winner is selected.
     *
     * Firestore path: users/{deviceId}/notifications/{auto-generated id}
     *
     * @param deviceId     The device ID of the user to notify.
     * @param notification The Notification object to store.
     */
    public void addNotification(@NonNull String deviceId,
                                @NonNull Notification notification) {

        // Let Firestore auto-generate the document ID, then store it
        // back into the document itself so we can reference it later.
        DocumentReference ref = db.collection("users")
                .document(deviceId)
                .collection("notifications")
                .document();

        String id = ref.getId();
        notification.setNotificationId(id);

        // Write explicit fields so documents match the team's schema and deserialize reliably
        // (toObject() breaks on alternate keys like eventID / isread / read).
        Timestamp ts = notification.getTimestamp() != null
                ? notification.getTimestamp()
                : Timestamp.now();
        Map<String, Object> data = new HashMap<>();
        data.put("notificationId", id);
        data.put("title", notification.getTitle());
        data.put("body", notification.getBody());
        data.put("eventName", notification.getEventName());
        String eid = notification.getEventId();
        data.put("eventId", eid);
        data.put("eventID", eid);
        boolean readFlag = notification.isRead();
        data.put("isRead", readFlag);
        data.put("isread", readFlag);
        data.put("read", false);
        data.put("timestamp", ts);

        ref.set(data)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Notification added for deviceId: " + deviceId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to add notification for deviceId: " + deviceId, e));
    }

    /**
     * Attaches a real-time Firestore listener to the user's notifications sub-collection.
     * The callback fires immediately with current data, then again on every change.
     * Results are sorted newest-first by timestamp on the client (no orderBy —
     * avoids index issues and includes docs while serverTimestamp is still resolving).
     *
     * The caller must store the returned ListenerRegistration and call .remove()
     * on it when the Activity is destroyed to avoid memory leaks.
     *
     * @param deviceId The device ID of the current user.
     * @param callback Called with the updated list whenever Firestore data changes.
     * @return A ListenerRegistration that the caller must remove in onStop/onDestroy.
     */
    public ListenerRegistration listenForNotifications(@NonNull String deviceId,
                                                       @NonNull NotificationListCallback callback) {

        return db.collection("users")
                .document(deviceId)
                .collection("notifications")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Notification listener error", error);
                        return;
                    }

                    if (snapshots == null) return;

                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        notifications.add(notificationFromDocument(doc));
                    }

                    notifications.sort((a, b) -> {
                        Timestamp ta = a.getTimestamp();
                        Timestamp tb = b.getTimestamp();
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        int cmp = Long.compare(tb.getSeconds(), ta.getSeconds());
                        if (cmp != 0) return cmp;
                        return Integer.compare(tb.getNanoseconds(), ta.getNanoseconds());
                    });

                    callback.onNotificationsUpdated(notifications);
                });
    }

    /**
     * Maps Firestore notification docs to {@link Notification}, tolerating field name variants
     * used across the project and in the database (eventId vs eventID, isRead vs isread, read).
     *
     * @param doc the Firestore document snapshot to deserialize
     * @return a fully populated {@link Notification} instance
     */
    static Notification notificationFromDocument(@NonNull DocumentSnapshot doc) {
        Notification n = new Notification();
        n.setNotificationId(doc.getId());

        String storedId = doc.getString("notificationId");
        if (storedId != null && !storedId.isEmpty()) {
            n.setNotificationId(storedId);
        }

        n.setTitle(doc.getString("title"));
        n.setBody(doc.getString("body"));
        n.setEventName(doc.getString("eventName"));

        String eventId = doc.getString("eventId");
        if (eventId == null) {
            eventId = doc.getString("eventID");
        }
        n.setEventId(eventId);

        boolean read = false;
        if (doc.contains("isRead")) {
            Boolean v = doc.getBoolean("isRead");
            read = Boolean.TRUE.equals(v);
        } else if (doc.contains("isread")) {
            Boolean v = doc.getBoolean("isread");
            read = Boolean.TRUE.equals(v);
        } else if (doc.contains("read")) {
            Boolean v = doc.getBoolean("read");
            read = Boolean.TRUE.equals(v);
        }
        n.setRead(read);

        Timestamp ts = doc.getTimestamp("timestamp");
        n.setTimestamp(ts);
        return n;
    }

    /**
     * Updates the isRead field of a single notification document to true.
     * Called when the user taps a notification card in the panel.
     *
     * @param deviceId       The device ID of the current user.
     * @param notificationId The ID of the notification document to mark as read.
     */
    public void markAsRead(@NonNull String deviceId,
                           @NonNull String notificationId) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("isRead", true);
        updates.put("isread", true);
        updates.put("read", true);

        db.collection("users")
                .document(deviceId)
                .collection("notifications")
                .document(notificationId)
                .update(updates)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to mark notification as read: " + notificationId, e));
    }
}