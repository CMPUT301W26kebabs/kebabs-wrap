package com.example.eventmanager.callbacks;

import java.util.List;

/**
 * Callback interface for waitlist join operations.
 * Implementations are notified when an entrant is successfully added to
 * an event's waiting list, or when the operation fails.
 */
public interface WaitlistCallback {
    /**
     * Called when the entrant is successfully added to the waitlist.
     */
    void onSuccess();

    /**
     * Called when the waitlist join operation fails.
     *
     * @param errorMessage a human-readable description of what went wrong
     */
    void onFailure(String errorMessage);
}

