package com.example.eventmanager;

import com.google.firebase.Timestamp;

/**
 * Data model representing a single notification document in Firestore.
 * Maps to: users/{deviceId}/notifications/{notificationId}
 *
 * Follows the same POJO pattern as Entrant and Event so Firestore
 * can automatically serialize/deserialize it.
 */
public class Notification {

    private String notificationId;
    private String title;
    private String body;
    private String eventName;
    private boolean isRead;
    private Timestamp timestamp;

    /**
     * Empty constructor required by Firestore for automatic data mapping.
     */
    public Notification() {}

    /**
     * Constructs a new Notification with all required fields.
     *
     * @param title     The notification headline (e.g. "You've been selected!")
     * @param body      The notification detail text.
     * @param eventName The name of the event this notification is about.
     */
    public Notification(String title, String body, String eventName) {
        this.title = title;
        this.body = body;
        this.eventName = eventName;
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

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}