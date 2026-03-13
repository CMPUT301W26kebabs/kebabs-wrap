package com.example.eventmanager.managers;

import android.content.Context;
import android.provider.Settings;

public class DeviceAuthManager {

    public String getDeviceId(Context context) {
        return Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

}