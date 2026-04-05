package com.example.eventmanager.callbacks;

import com.example.eventmanager.models.Entrant;

import java.util.List;

/**
 * Callback interface for lottery draw operations.
 * Implementations receive the outcome of a lottery draw: either the list of
 * selected entrants on success, or an error message on failure.
 */
public interface LotteryCallback {
    /**
     * Called when a lottery draw completes successfully.
     *
     * @param selectedWinners the entrants randomly chosen in this draw
     * @param winnerDeviceIds canonical device IDs for this draw (same as written
     *                        to the {@code selected} sub-collection), in lockstep
     *                        with {@code selectedWinners}
     */
    void onSuccess(List<Entrant> selectedWinners, List<String> winnerDeviceIds);

    /**
     * Called when the lottery draw fails.
     *
     * @param errorMessage a human-readable description of what went wrong
     */
    void onFailure(String errorMessage);
}
