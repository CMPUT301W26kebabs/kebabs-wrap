package com.example.eventmanager.callbacks;

import com.example.eventmanager.models.Entrant;
import java.util.List;

public interface EntrantListCallback {
    void onSuccess(List<Entrant> entrants);
    void onFailure(String errorMessage);
}