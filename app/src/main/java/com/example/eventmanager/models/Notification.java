package com.example.eventmanager.models;

import com.example.eventmanager.ui.AcceptDeclineActivity;
import com.example.eventmanager.models.Event;

import com.google.firebase.Timestamp;

/**
 * Data model representing a single notification document in Firestore.
 * Maps to: users/{deviceId}/notifications/{notificationId}
 *
 * eventId is nullable:
 *   - null    → informational notification (waiting list message), tapping only marks as read
 *   - non-null → winner notification, tapping launches AcceptDeclineActivity
 */
public class Notification {

    private String notificationId;
    private String title;
    private String body;
    private String eventName;
    private String eventId;       // null for waitingList notifications, populated for winner notifications
    private boolean isRead;
    private Timestamp timestamp;

    /** Empty constructor required by Firestore for automatic data mapping. */
    public Notification() {}

    /**
     * Constructor for informational notifications (e.g. waiting list messages).
     * eventId is left null — tapping this notification only marks it as read.
     */
    public Notification(String title, String body, String eventName) {
        this.title = title;
        this.body = body;
        this.eventName = eventName;
        this.eventId = null;
        this.isRead = false;
        this.timestamp = Timestamp.now();
    }

    /**
     * Constructor for winner notifications.
     * eventId is populated — tapping this notification launches AcceptDeclineActivity.
     *
     * @param eventId The Firestore document ID of the event the user won.
     */
    public Notification(String title, String body, String eventName, String eventId) {
        this.title = title;
        this.body = body;
        this.eventName = eventName;
        this.eventId = eventId;
        this.isRead = false;
        this.timestamp = Timestamp.now();
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}