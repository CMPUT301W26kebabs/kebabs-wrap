package com.example.eventmanager;

import com.example.eventmanager.models.Entrant;
import java.util.List;

public interface EntrantListCallback {
    void onSuccess(List<Entrant> entrants);
    void onFailure(String errorMessage);
}