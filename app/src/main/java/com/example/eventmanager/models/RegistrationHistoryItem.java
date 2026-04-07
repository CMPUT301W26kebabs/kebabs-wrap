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

    /**
     * Constructs a history item pairing an event with the entrant's status in it.
     *
     * @param event  the event the entrant interacted with.
     * @param status the entrant's current registration status for the event.
     */
    public RegistrationHistoryItem(@NonNull Event event, @NonNull RegistrationStatus status) {
        this.event = event;
        this.status = status;
    }

    /**
     * Returns the event associated with this history item.
     *
     * @return the event, never {@code null}.
     */
    @NonNull
    public Event getEvent() {
        return event;
    }

    /**
     * Returns the entrant's registration status for the associated event.
     *
     * @return the registration status, never {@code null}.
     */
    @NonNull
    public RegistrationStatus getStatus() {
        return status;
    }

    /**
     * Display name for the event, falling back to the document id when missing.
     *
     * @return the event name, its document ID, or {@code "Event"} as a last resort.
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

    /**
     * Returns a displayable location string, or {@code null} if the event has no location set.
     *
     * @return the trimmed location string, or {@code null} when absent or blank.
     */
    @Nullable
    public String getLocationDisplay() {
        String loc = event.getLocation();
        return loc != null && !loc.trim().isEmpty() ? loc : null;
    }
}
