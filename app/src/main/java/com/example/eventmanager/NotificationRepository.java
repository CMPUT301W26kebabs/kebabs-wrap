package com.example.eventmanager;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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

        notification.setNotificationId(ref.getId());

        ref.set(notification)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Notification added for deviceId: " + deviceId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to add notification for deviceId: " + deviceId, e));
    }

    /**
     * Attaches a real-time Firestore listener to the user's notifications sub-collection.
     * The callback fires immediately with current data, then again on every change.
     * Notifications are ordered newest-first by timestamp.
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
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Notification listener error", error);
                        return;
                    }

                    if (snapshots == null) return;

                    // Convert each Firestore document into a Notification object
                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Notification n = doc.toObject(Notification.class);
                        notifications.add(n);
                    }

                    callback.onNotificationsUpdated(notifications);
                });
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

        db.collection("users")
                .document(deviceId)
                .collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to mark notification as read: " + notificationId, e));
    }
}