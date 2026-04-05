package com.example.eventmanager.callbacks;

import java.util.List;

public interface WaitlistCallback {
    void onSuccess();
    void onFailure(String errorMessage);
}

