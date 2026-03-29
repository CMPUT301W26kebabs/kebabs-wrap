package com.example.eventmanager.models;

import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * One comment on an event, stored under {@code events/{eventId}/comments/{commentId}}.
 */
public class EventComment {

    private String id;
    private String deviceId;
    private String authorName;
    private String text;
    private Timestamp timestamp;
    /** Denormalized for admin / filters; may be null on older documents (infer from document path). */
    private String eventId;
    private String eventName;

    public EventComment() {
    }

    public EventComment(String id, @Nullable String deviceId, @Nullable String authorName,
                        @Nullable String text, @Nullable Timestamp timestamp,
                        @Nullable String eventId, @Nullable String eventName) {
        this.id = id;
        this.deviceId = deviceId;
        this.authorName = authorName;
        this.text = text;
        this.timestamp = timestamp;
        this.eventId = eventId;
        this.eventName = eventName;
    }

    @Nullable
    private static String parentEventIdFromPath(DocumentSnapshot doc) {
        try {
            return doc.getReference().getParent().getParent().getId();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static EventComment fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        String text = doc.getString("text");
        if (text == null || text.trim().isEmpty()) return null;
        String eid = doc.getString("eventId");
        if (eid == null || eid.isEmpty()) {
            eid = parentEventIdFromPath(doc);
        }
        return new EventComment(
                doc.getId(),
                doc.getString("deviceId"),
                doc.getString("authorName"),
                text.trim(),
                doc.getTimestamp("timestamp"),
                eid,
                doc.getString("eventName")
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Nullable
    public String getEventId() {
        return eventId;
    }

    public void setEventId(@Nullable String eventId) {
        this.eventId = eventId;
    }

    @Nullable
    public String getEventName() {
        return eventName;
    }

    public void setEventName(@Nullable String eventName) {
        this.eventName = eventName;
    }
}
