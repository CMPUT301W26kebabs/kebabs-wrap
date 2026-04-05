package com.example.eventmanager.managers;

import com.example.eventmanager.models.Notification;
import com.example.eventmanager.repository.EventRepository;
import com.example.eventmanager.repository.NotificationRepository;
import com.example.eventmanager.ui.AcceptDeclineActivity;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.Event;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * <p>
     * <b>Note:</b> Running the lottery already sends the same template to new winners via
     * {@link #notifyAfterLotteryDraw}. Using this right after a draw will duplicate those
     * in-app notifications for everyone still in {@code selected}.
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

    /**
     * Sends the winner (accept/decline) notification only to the given device IDs.
     * Used after a lottery draw so we notify exactly who was drawn in this run, not everyone
     * currently in {@code selected} (which can include stale reads or prior invitees).
     */
    private void notifySelectedDeviceIds(@NonNull String eventId,
                                         @NonNull String eventName,
                                         @NonNull List<String> deviceIds,
                                         @NonNull NotificationDispatchCallback callback) {
        if (deviceIds.isEmpty()) {
            Log.d(TAG, "No winner device IDs to notify for event: " + eventId);
            callback.onFailure("No selected entrants to notify.");
            return;
        }

        String title = "🎉 You've been selected!";
        String body = "You were chosen to attend " + eventName + ". Tap to respond.";

        for (String deviceId : deviceIds) {
            if (deviceId == null || deviceId.trim().isEmpty()) {
                continue;
            }
            Notification notification = new Notification(title, body, eventName, eventId);
            sendIfOptedIn(deviceId.trim(), notification);
        }

        Log.d(TAG, "Selected entrants notified (explicit list): " + deviceIds.size());
        callback.onSuccess(deviceIds.size());

        writeNotificationLog(eventId, eventName,
                "Selected Entrants", deviceIds.size(), title, body);
    }

    /**
     * Notifies everyone still on the waiting list that they were not selected in the lottery.
     * {@code excludeWinnerDeviceIds} skips devices who were just chosen (safety if waitlist data overlaps).
     */
    public void notifyNotChosenAfterLottery(@NonNull String eventId,
                                            @NonNull String eventName,
                                            @Nullable Set<String> excludeWinnerDeviceIds,
                                            @NonNull NotificationDispatchCallback callback) {
        // Read from server so we see the post-lottery waitlist (not a stale cache with winners still on it).
        eventRepository.getWaitingList(eventId, Source.SERVER, deviceIds -> {
            String safeName = eventName != null && !eventName.trim().isEmpty() ? eventName.trim() : "Event";
            String title = "Unfortunately, you weren't chosen for " + safeName + ".";
            String body = "";

            int sent = 0;
            for (String deviceId : deviceIds) {
                if (deviceId == null || deviceId.trim().isEmpty()) {
                    continue;
                }
                String id = deviceId.trim();
                if (excludeWinnerDeviceIds != null && excludeWinnerDeviceIds.contains(id)) {
                    Log.d(TAG, "Skipping not-chosen notification for current draw winner: " + id);
                    continue;
                }
                Notification notification = new Notification(title, body, eventName);
                sendIfOptedIn(id, notification);
                sent++;
            }

            if (sent == 0) {
                callback.onSuccess(0);
                return;
            }

            Log.d(TAG, "Not-chosen (waitlist) notified: " + sent);
            callback.onSuccess(sent);

            writeNotificationLog(eventId, eventName,
                    "Not selected (after lottery)", sent, title, body);
        });
    }

    /**
     * After running the lottery: notify this draw's winners (exact device IDs from {@link LotteryManager})
     * and everyone still on the waiting list that they were not chosen.
     * <p>
     * Does not re-query {@code selected}: that would also match prior invitees and duplicate winner alerts.
     */
    public void notifyAfterLotteryDraw(@NonNull String eventId,
                                       @NonNull String eventName,
                                       @NonNull List<String> winnerDeviceIds,
                                       @NonNull NotificationDispatchCallback callback) {
        List<String> validWinners = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (String id : winnerDeviceIds) {
            if (id != null && !id.trim().isEmpty() && seen.add(id.trim())) {
                validWinners.add(id.trim());
            }
        }

        if (validWinners.isEmpty()) {
            Log.e(TAG, "Lottery notify: empty winner id list — skipping winner invites (not re-querying selected).");
            notifyNotChosenAfterLottery(eventId, eventName, null, callback);
            return;
        }

        runLotteryWinnerThenNotChosen(eventId, eventName, validWinners, callback);
    }

    private void runLotteryWinnerThenNotChosen(@NonNull String eventId,
                                               @NonNull String eventName,
                                               @NonNull List<String> winnerDeviceIds,
                                               @NonNull NotificationDispatchCallback callback) {
        HashSet<String> excludeLosersForWinners = new HashSet<>();
        for (String id : winnerDeviceIds) {
            if (id != null && !id.trim().isEmpty()) {
                excludeLosersForWinners.add(id.trim());
            }
        }

        // Notify waitlist losers first, then winners. Same total sends as before, but the
        // winner invite is written last so it sorts to the top (newest-first) and avoids an
        // extra "selected" doc feeling like the primary update when something else also writes.
        notifyNotChosenAfterLottery(eventId, eventName, excludeLosersForWinners,
                new NotificationDispatchCallback() {
                    @Override
                    public void onSuccess(int loserCount) {
                        notifySelectedDeviceIds(eventId, eventName, winnerDeviceIds,
                                new NotificationDispatchCallback() {
                                    @Override
                                    public void onSuccess(int winnerCount) {
                                        callback.onSuccess(winnerCount + loserCount);
                                    }

                                    @Override
                                    public void onFailure(@NonNull String message) {
                                        callback.onSuccess(loserCount);
                                    }
                                });
                    }

                    @Override
                    public void onFailure(@NonNull String message) {
                        notifySelectedDeviceIds(eventId, eventName, winnerDeviceIds, callback);
                    }
                });
    }

    /**
     * Organizer removed chosen entrants who never completed enrollment — informational only
     * (no {@code eventId} on notification so accept/decline does not open).
     */
    public void notifyOrganizerRevokedNonEnrollment(@NonNull String eventId,
                                                    @NonNull String eventName,
                                                    @NonNull List<String> deviceIds,
                                                    @NonNull NotificationDispatchCallback callback) {
        if (deviceIds.isEmpty()) {
            callback.onSuccess(0);
            return;
        }
        String title = "Invitation cancelled";
        String body = "The organizer removed you from the chosen list for \"" + eventName
                + "\" because you did not complete enrollment.";

        for (String deviceId : deviceIds) {
            Notification notification = new Notification(title, body, eventName);
            sendIfOptedIn(deviceId, notification);
        }

        Log.d(TAG, "Organizer revoked non-enrollment for " + deviceIds.size() + " entrants");
        callback.onSuccess(deviceIds.size());

        writeNotificationLog(eventId, eventName,
                "Revoked (no enrollment)", deviceIds.size(), title, body);
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

            String title = "Registration update";
            String body = "You cancelled from \"" + eventName + "\".";

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
