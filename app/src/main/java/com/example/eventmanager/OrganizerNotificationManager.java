package com.example.eventmanager;

import android.util.Log;
import androidx.annotation.NonNull;

/**
 * Orchestrates sending notifications to groups of entrants.
 * Sits between the organizer UI and the two repositories.
 *
 * The UI calls one method here — this class handles fetching
 * recipients from EventRepository and writing notifications
 * via NotificationRepository. The UI knows nothing about Firestore.
 *
 * Covers:
 *   US 02.07.01 — notifyWaitingList()
 *   US 02.05.01 — notifyWinners()
 */
public class OrganizerNotificationManager {

    private static final String TAG = "OrganizerNotifManager";

    private final EventRepository eventRepository;
    private final NotificationRepository notificationRepository;

    public OrganizerNotificationManager() {
        this.eventRepository = new EventRepository();
        this.notificationRepository = new NotificationRepository();
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
    public void notifyWaitingList(@NonNull String eventId, @NonNull String eventName) {
        eventRepository.getWaitingList(eventId, deviceIds -> {

            if (deviceIds.isEmpty()) {
                Log.d(TAG, "No entrants on waiting list for event: " + eventId);
                return;
            }

            // Informational notification — no eventId, no accept/decline flow
            for (String deviceId : deviceIds) {
                Notification notification = new Notification(
                        "📋 Update from " + eventName,
                        "The organizer has sent you a message about " + eventName + ".",
                        eventName
                );
                notificationRepository.addNotification(deviceId, notification);
            }

            Log.d(TAG, "Waiting list notified: " + deviceIds.size() + " entrants");
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
    public void notifyWinners(@NonNull String eventId, @NonNull String eventName) {
        eventRepository.getWinners(eventId, deviceIds -> {

            if (deviceIds.isEmpty()) {
                Log.d(TAG, "No winners found for event: " + eventId);
                return;
            }

            // Winner notification — eventId is populated so tapping launches accept/decline
            for (String deviceId : deviceIds) {
                Notification notification = new Notification(
                        "🎉 You've been selected!",
                        "You were chosen to attend " + eventName + ". Tap to respond.",
                        eventName,
                        eventId    // this is what makes it a winner notification
                );
                notificationRepository.addNotification(deviceId, notification);
            }

            Log.d(TAG, "Winners notified: " + deviceIds.size() + " entrants");
        });
    }
}