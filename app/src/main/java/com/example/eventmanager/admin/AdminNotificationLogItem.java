package com.example.eventmanager.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * One row in the admin notification log list.
 * Backed by {@code notificationLogs/{logId}} documents in Firestore.
 *
 * Mirrors the pattern of {@link AdminCommentListItem} — a flat view-model
 * built from a Firestore snapshot, used directly by the adapter.
 */
public final class AdminNotificationLogItem {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

    public final String logId;
    public final String organizerName;
    public final String recipientGroup;
    public final String message;
    public final String formattedTimestamp;
    public final long timestampMillis;   // for sorting/filtering

    private AdminNotificationLogItem(String logId,
                                     String organizerName,
                                     String recipientGroup,
                                     String message,
                                     String formattedTimestamp,
                                     long timestampMillis) {
        this.logId = logId;
        this.organizerName = organizerName;
        this.recipientGroup = recipientGroup;
        this.message = message;
        this.formattedTimestamp = formattedTimestamp;
        this.timestampMillis = timestampMillis;
    }

    /**
     * Factory method that converts a Firestore {@link DocumentSnapshot}
     * into a display-ready list item.
     *
     * @return the item, or {@code null} if the snapshot is unusable.
     */
    @Nullable
    public static AdminNotificationLogItem fromSnapshot(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        String logId = doc.getId();

        // Organizer
        String organizerName = doc.getString("organizerName");
        if (organizerName == null || organizerName.trim().isEmpty()) {
            organizerName = "Unknown Organizer";
        }

        // Recipient group
        String recipientGroup = doc.getString("recipientGroup");
        if (recipientGroup == null || recipientGroup.trim().isEmpty()) {
            recipientGroup = "Recipients";
        }

        // Message body — fall back to title if body is empty
        String message = doc.getString("message");
        if (message == null || message.trim().isEmpty()) {
            message = doc.getString("title");
        }
        if (message == null || message.trim().isEmpty()) {
            message = "(No message)";
        }

        // Timestamp
        Timestamp ts = doc.getTimestamp("timestamp");
        String formattedTs = "";
        long tsMillis = 0;
        if (ts != null) {
            Date date = ts.toDate();
            formattedTs = DATE_FORMAT.format(date);
            tsMillis = date.getTime();
        }

        return new AdminNotificationLogItem(
                logId,
                organizerName.trim(),
                recipientGroup.trim(),
                message.trim(),
                formattedTs,
                tsMillis
        );
    }
}
