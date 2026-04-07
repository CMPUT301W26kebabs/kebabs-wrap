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

    /**
     * No-arg constructor required by Firestore deserialization.
     */
    public EventComment() {
    }

    /**
     * Full constructor for creating a comment with all fields.
     *
     * @param id         Firestore document ID of this comment.
     * @param deviceId   device ID of the comment author.
     * @param authorName display name of the author.
     * @param text       comment body text.
     * @param timestamp  server timestamp of when the comment was posted.
     * @param eventId    ID of the parent event (may be {@code null} for legacy docs).
     * @param eventName  denormalized name of the parent event.
     */
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

    /**
     * Extracts the parent event ID from the Firestore document reference path
     * ({@code events/{eventId}/comments/{commentId}}).
     *
     * @param doc the Firestore document snapshot.
     * @return the parent event ID, or {@code null} if the path cannot be resolved.
     */
    @Nullable
    private static String parentEventIdFromPath(DocumentSnapshot doc) {
        try {
            return doc.getReference().getParent().getParent().getId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Builds an {@link EventComment} from a Firestore document snapshot. Returns {@code null}
     * when the document is missing, doesn't exist, or has blank text.
     *
     * @param doc the Firestore document snapshot to convert.
     * @return a populated {@link EventComment}, or {@code null} if the doc is invalid.
     */
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

    /**
     * @return the Firestore document ID of this comment.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the Firestore document ID to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the device ID of the comment author.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * @param deviceId the device ID to set for the comment author.
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * @return the display name of the author, or {@code null} if anonymous.
     */
    public String getAuthorName() {
        return authorName;
    }

    /**
     * @param authorName the display name to set for the author.
     */
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    /**
     * @return the comment body text.
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the comment body text to set.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the server timestamp of when this comment was posted.
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the server timestamp to set.
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the parent event ID, or {@code null} for legacy documents.
     */
    @Nullable
    public String getEventId() {
        return eventId;
    }

    /**
     * @param eventId the parent event ID to set.
     */
    public void setEventId(@Nullable String eventId) {
        this.eventId = eventId;
    }

    /**
     * @return the denormalized event name, or {@code null} if not stored.
     */
    @Nullable
    public String getEventName() {
        return eventName;
    }

    /**
     * @param eventName the denormalized event name to set.
     */
    public void setEventName(@Nullable String eventName) {
        this.eventName = eventName;
    }
}
