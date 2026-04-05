package com.example.eventmanager;

import com.example.eventmanager.models.Entrant;

import java.util.List;

public interface LotteryCallback {
    /**
     * @param winnerDeviceIds Canonical device ids for this draw (same as written to {@code selected}),
     *                        in lockstep with {@code selectedWinners}.
     */
    void onSuccess(List<Entrant> selectedWinners, List<String> winnerDeviceIds);

    void onFailure(String errorMessage);
}
