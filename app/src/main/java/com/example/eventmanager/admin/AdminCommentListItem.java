package com.example.eventmanager.admin;
import com.example.eventmanager.models.Entrant;
import com.example.eventmanager.models.Event;

import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Locale;

/**
 * One row in the admin comment log, backed by {@code events/{eventId}/comments/{commentId}}.
 */
public final class AdminCommentListItem {

    private static final int[] AVATAR_COLORS = {
            0xFF6C8CCF, 0xFFE0A787, 0xFF8E7CE6, 0xFF8B623D, 0xFF5CB87A, 0xFFD67A9E
    };

    public final DocumentReference ref;
    public final String authorName;
    public final String commentText;
    public final String eventLine;
    public final CharSequence timeAgo;
    public final String initials;
    public final int avatarColor;

    private AdminCommentListItem(
            DocumentReference ref,
            String authorName,
            String commentText,
            String eventLine,
            CharSequence timeAgo,
            String initials,
            int avatarColor) {
        this.ref = ref;
        this.authorName = authorName;
        this.commentText = commentText;
        this.eventLine = eventLine;
        this.timeAgo = timeAgo;
        this.initials = initials;
        this.avatarColor = avatarColor;
    }

    @Nullable
    public static AdminCommentListItem fromSnapshot(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        String text = doc.getString("text");
        if (text == null || text.trim().isEmpty()) return null;

        String author = doc.getString("authorName");
        if (author == null || author.isEmpty()) {
            author = "Entrant";
        } else {
            author = author.trim();
        }

        String eventName = doc.getString("eventName");
        String eventId = doc.getString("eventId");
        if (eventId == null || eventId.isEmpty()) {
            try {
                eventId = doc.getReference().getParent().getParent().getId();
            } catch (Exception ignored) {
                eventId = null;
            }
        }

        final String eventLine;
        if (eventName != null && !eventName.trim().isEmpty()) {
            eventLine = "Event: " + eventName.trim();
        } else if (eventId != null && !eventId.isEmpty()) {
            eventLine = "Event ID: " + eventId;
        } else {
            eventLine = "Event: —";
        }

        Timestamp ts = doc.getTimestamp("timestamp");
        CharSequence timeAgo = ts != null
                ? DateUtils.getRelativeTimeSpanString(
                ts.toDate().getTime(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE)
                : "";

        return new AdminCommentListItem(
                doc.getReference(),
                author,
                text.trim(),
                eventLine,
                timeAgo,
                initialsFor(author),
                avatarColorFor(author));
    }

    @NonNull
    private static String initialsFor(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2
                && !parts[0].isEmpty()
                && !parts[1].isEmpty()) {
            String a = parts[0].substring(0, 1);
            String b = parts[1].substring(0, 1);
            return (a + b).toUpperCase(Locale.getDefault());
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase(Locale.getDefault());
    }

    private static int avatarColorFor(String name) {
        int h = name != null ? name.hashCode() : 0;
        return AVATAR_COLORS[Math.floorMod(h, AVATAR_COLORS.length)];
    }
}
