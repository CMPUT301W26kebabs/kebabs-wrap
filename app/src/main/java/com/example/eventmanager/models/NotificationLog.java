package com.example.eventmanager.models;

import com.google.firebase.Timestamp;

/**
 * Data model for the top-level {@code notificationLogs} collection in
 * Firestore.
 * Each document represents a single notification dispatch by an organizer.
 *
 * Unlike {@link Notification} (which is per-user), this is the admin audit
 * trail
 * that records WHO sent WHAT to WHICH group.
 *
 * US 03.08.01 — Admins can review all notification logs.
 */
public class NotificationLog {

    private String logId;
    private String organizerId;
    private String organizerName;
    private String eventId;
    private String eventName;
    private String recipientGroup; // e.g. "All Waiting List", "Selected Entrants"
    private int recipientCount;
    private String title;
    private String message;
    private Timestamp timestamp;

    /** Empty constructor required by Firestore for automatic data mapping. */
    public NotificationLog() {
    }

    public NotificationLog(String organizerId, String organizerName,
            String eventId, String eventName,
            String recipientGroup, int recipientCount,
            String title, String message) {
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.eventId = eventId;
        this.eventName = eventName;
        this.recipientGroup = recipientGroup;
        this.recipientCount = recipientCount;
        this.title = title;
        this.message = message;
        this.timestamp = Timestamp.now();
    }

    // ── Getters & Setters ──

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getRecipientGroup() {
        return recipientGroup;
    }

    public void setRecipientGroup(String recipientGroup) {
        this.recipientGroup = recipientGroup;
    }

    public int getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
