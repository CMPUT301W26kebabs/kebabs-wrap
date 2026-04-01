package com.example.eventmanager;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates sending notifications to groups of entrants.
 * Sits between the organizer UI and the two repositories.
 *
 * The UI calls one method here — this class handles fetching
 * recipients from EventRepository and writing notifications
 * via NotificationRepository. The UI knows nothing about Firestore.
 *
 * Every successful dispatch also writes an audit entry to the
 * top-level {@code notificationLogs} collection so that admins
 * can review all notifications (US 03.08.01).
 *
 * Before sending each notification, the user's {@code receiveNotifications}
 * preference is checked (US 01.04.03). Users who have opted out are skipped.
 *
 * Covers:
 *   US 02.07.01 — notifyWaitingList()
 *   US 02.05.01 — notifyWinners()
 *   US 01.04.03 — respect notification opt-out
 *   US 03.08.01 — audit logging
 */
public class OrganizerNotificationManager {

    private static final String TAG = "OrganizerNotifManager";

    private final EventRepository eventRepository;
    private final NotificationRepository notificationRepository;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface NotificationDispatchCallback {
        void onSuccess(int recipientCount);
        void onFailure(@NonNull String message);
    }

    public OrganizerNotificationManager() {
        this.eventRepository = new EventRepository();
        this.notificationRepository = new NotificationRepository();
    }

    /**
     * Checks the user's {@code receiveNotifications} preference before sending.
     * If the field is missing or {@code true}, the notification is sent.
     * If explicitly {@code false}, the notification is silently skipped.
     *
     * US 01.04.03
     */
    private void sendIfOptedIn(@NonNull String deviceId,
                               @NonNull Notification notification) {
        db.collection("users").document(deviceId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        Boolean optIn = userDoc.getBoolean("receiveNotifications");
                        // null or true → send; false → skip
                        if (optIn != null && !optIn) {
                            Log.d(TAG, "Skipping notification for opted-out user: " + deviceId);
                            return;
                        }
                    }
                    notificationRepository.addNotification(deviceId, notification);
                })
                .addOnFailureListener(e -> {
                    // If we can't read the preference, send anyway (fail-open)
                    Log.w(TAG, "Could not check notification pref for " + deviceId, e);
                    notificationRepository.addNotification(deviceId, notification);
                });
    }

    /**
     * Sends an informational notification to every entrant on the waiting list.
     * These notifications have no eventId — tapping them only marks them as read.
     *
     * Flow: getWaitingList() → loop → addNotification() for each deviceId
     *
     * US 02.07.01
     *
     * @param eventId   The Firestore document ID of the event.
     * @param eventName The human-readable event name shown in the notification.
     */
    public void notifyWaitingList(@NonNull String eventId,
                                  @NonNull String eventName,
                                  @NonNull NotificationDispatchCallback callback) {
        eventRepository.getWaitingList(eventId, deviceIds -> {

            if (deviceIds.isEmpty()) {
                Log.d(TAG, "No entrants on waiting list for event: " + eventId);
                callback.onFailure("No entrants on the waiting list.");
                return;
            }

            String title = "📋 Update from " + eventName;
            String body = "The organizer has sent you a message about " + eventName + ".";

            // Informational notification — no eventId, no accept/decline flow
            for (String deviceId : deviceIds) {
                Notification notification = new Notification(title, body, eventName);
                sendIfOptedIn(deviceId, notification);
            }

            Log.d(TAG, "Waiting list notified: " + deviceIds.size() + " entrants");
            callback.onSuccess(deviceIds.size());

            // Audit log for admin review (US 03.08.01)
            writeNotificationLog(eventId, eventName,
                    "All Waiting List", deviceIds.size(), title, body);
        });
    }

    public void notifyWaitingList(@NonNull String eventId, @NonNull String eventName) {
        notifyWaitingList(eventId, eventName, new NotificationDispatchCallback() {
            @Override
            public void onSuccess(int recipientCount) { }

            @Override
            public void onFailure(@NonNull String message) { }
        });
    }

    /**
     * Sends a winner notification to every entrant in the winners sub-collection.
     * These notifications carry the eventId — tapping them launches AcceptDeclineActivity.
     *
     * Flow: getWinners() → loop → addNotification() for each deviceId
     *
     * US 02.05.01
     *
     * @param eventId   The Firestore document ID of the event.
     * @param eventName The human-readable event name shown in the notification.
     */
    public void notifySelected(@NonNull String eventId,
                               @NonNull String eventName,
                               @NonNull NotificationDispatchCallback callback) {
        eventRepository.getSelected(eventId, deviceIds -> {

            if (deviceIds.isEmpty()) {
                Log.d(TAG, "No selected entrants found for event: " + eventId);
                callback.onFailure("No selected entrants to notify.");
                return;
            }

            String title = "🎉 You've been selected!";
            String body = "You were chosen to attend " + eventName + ". Tap to respond.";

            // Winner notification — eventId is populated so tapping launches accept/decline
            for (String deviceId : deviceIds) {
                Notification notification = new Notification(title, body, eventName, eventId);
                sendIfOptedIn(deviceId, notification);
            }

            Log.d(TAG, "Selected entrants notified: " + deviceIds.size() + " entrants");
            callback.onSuccess(deviceIds.size());

            // Audit log for admin review (US 03.08.01)
            writeNotificationLog(eventId, eventName,
                    "Selected Entrants", deviceIds.size(), title, body);
        });
    }

    public void notifyWinners(@NonNull String eventId,
                              @NonNull String eventName,
                              @NonNull NotificationDispatchCallback callback) {
        notifySelected(eventId, eventName, callback);
    }

    public void notifyWinners(@NonNull String eventId, @NonNull String eventName) {
        notifySelected(eventId, eventName, new NotificationDispatchCallback() {
            @Override
            public void onSuccess(int recipientCount) { }

            @Override
            public void onFailure(@NonNull String message) { }
        });
    }

    public void notifyEnrolled(@NonNull String eventId,
                               @NonNull String eventName,
                               @NonNull NotificationDispatchCallback callback) {
        eventRepository.getEnrolled(eventId, deviceIds -> {
            if (deviceIds.isEmpty()) {
                Log.d(TAG, "No enrolled entrants found for event: " + eventId);
                callback.onFailure("No enrolled entrants to notify.");
                return;
            }

            String title = "Event update from " + eventName;
            String body = "The organizer has shared an update for enrolled attendees.";

            for (String deviceId : deviceIds) {
                Notification notification = new Notification(title, body, eventName);
                sendIfOptedIn(deviceId, notification);
            }

            Log.d(TAG, "Enrolled entrants notified: " + deviceIds.size() + " entrants");
            callback.onSuccess(deviceIds.size());

            // Audit log for admin review (US 03.08.01)
            writeNotificationLog(eventId, eventName,
                    "Enrolled Attendees", deviceIds.size(), title, body);
        });
    }

    /**
     * US 02.07.03 — notify everyone in {@code events/{eventId}/cancelled}.
     */
    public void notifyCancelled(@NonNull String eventId,
                                @NonNull String eventName,
                                @NonNull NotificationDispatchCallback callback) {
        eventRepository.getCancelled(eventId, deviceIds -> {
            if (deviceIds.isEmpty()) {
                Log.d(TAG, "No cancelled entrants for event: " + eventId);
                callback.onFailure("No cancelled entrants to notify.");
                return;
            }

            String title = "Update regarding " + eventName;
            String body = "The organizer has sent a message to cancelled entrants for " + eventName + ".";

            for (String deviceId : deviceIds) {
                Notification notification = new Notification(title, body, eventName);
                sendIfOptedIn(deviceId, notification);
            }
            Log.d(TAG, "Cancelled entrants notified: " + deviceIds.size());
            callback.onSuccess(deviceIds.size());

            // Audit log for admin review (US 03.08.01)
            writeNotificationLog(eventId, eventName,
                    "Cancelled Entrants", deviceIds.size(), title, body);
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  AUDIT LOGGING — US 03.08.01
    // ─────────────────────────────────────────────────────────────────

    /**
     * Writes a document to the top-level {@code notificationLogs} collection.
     * Resolves the organizer name from the event's {@code organizerId} field.
     *
     * This creates the audit trail that {@code AdminNotificationLogsActivity}
     * displays for the admin.
     */
    private void writeNotificationLog(@NonNull String eventId,
                                      @NonNull String eventName,
                                      @NonNull String recipientGroup,
                                      int recipientCount,
                                      @NonNull String title,
                                      @NonNull String message) {

        // First, fetch the event to get the organizerId
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    String organizerId = eventDoc.getString("organizerId");
                    if (organizerId == null || organizerId.isEmpty()) {
                        // If no organizerId, write log with "Unknown"
                        persistLog(eventId, eventName, "Unknown", "Unknown Organizer",
                                recipientGroup, recipientCount, title, message);
                        return;
                    }

                    // Resolve organizer name from users collection
                    db.collection("users").document(organizerId).get()
                            .addOnSuccessListener(userDoc -> {
                                String orgName = userDoc.getString("name");
                                if (orgName == null || orgName.trim().isEmpty()) {
                                    orgName = "Organizer";
                                }
                                persistLog(eventId, eventName, organizerId, orgName,
                                        recipientGroup, recipientCount, title, message);
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Could not resolve organizer name", e);
                                persistLog(eventId, eventName, organizerId, "Organizer",
                                        recipientGroup, recipientCount, title, message);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Could not fetch event for notification log", e);
                    persistLog(eventId, eventName, "", "Unknown Organizer",
                            recipientGroup, recipientCount, title, message);
                });
    }

    /** Actually writes the log document to Firestore. */
    private void persistLog(String eventId, String eventName,
                            String organizerId, String organizerName,
                            String recipientGroup, int recipientCount,
                            String title, String message) {
        DocumentReference ref = db.collection("notificationLogs").document();

        Map<String, Object> log = new HashMap<>();
        log.put("logId", ref.getId());
        log.put("organizerId", organizerId);
        log.put("organizerName", organizerName);
        log.put("eventId", eventId);
        log.put("eventName", eventName);
        log.put("recipientGroup", recipientGroup);
        log.put("recipientCount", recipientCount);
        log.put("title", title);
        log.put("message", message);
        log.put("timestamp", Timestamp.now());

        ref.set(log)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Notification log written: " + ref.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to write notification log", e));
    }
}