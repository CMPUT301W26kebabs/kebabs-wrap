package com.example.eventmanager;
import java.util.List;
import com.example.eventmanager.models.Entrant;

public interface LotteryCallback {
    void onSuccess(List<Entrant> selectedWinners);
    void onFailure(String errorMessage);
}
