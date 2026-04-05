package com.example.eventmanager.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * One row in the entrant's registration history: an event plus their status
 * in that event's lottery sub-collections (waitingList, selected, enrolled, cancelled).
 */
public class RegistrationHistoryItem {

    public enum RegistrationStatus {
        WAITING_LIST,
        SELECTED,
        INVITED,
        ENROLLED,
        CANCELLED
    }

    private final Event event;
    private final RegistrationStatus status;

    public RegistrationHistoryItem(@NonNull Event event, @NonNull RegistrationStatus status) {
        this.event = event;
        this.status = status;
    }

    @NonNull
    public Event getEvent() {
        return event;
    }

    @NonNull
    public RegistrationStatus getStatus() {
        return status;
    }

    /**
     * Display name for the event, falling back to the document id when missing.
     */
    @NonNull
    public String getEventDisplayName() {
        String n = event.getName();
        if (n != null && !n.trim().isEmpty()) {
            return n;
        }
        String id = event.getEventId();
        return id != null ? id : "Event";
    }

    @Nullable
    public String getLocationDisplay() {
        String loc = event.getLocation();
        return loc != null && !loc.trim().isEmpty() ? loc : null;
    }
}
