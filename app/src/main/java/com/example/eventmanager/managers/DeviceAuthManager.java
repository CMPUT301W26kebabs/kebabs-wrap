package com.example.eventmanager.managers;

import android.content.Context;
import android.provider.Settings;

/**
 * Retrieves the Android hardware device ID ({@code ANDROID_ID}) used as the
 * unique user identifier throughout the application. This ID persists across
 * app reinstalls on the same device and avoids the need for a traditional
 * login flow.
 */
public class DeviceAuthManager {

    /**
     * Returns the stable, per-device {@code ANDROID_ID} for the given context.
     *
     * @param context the Android context used to access secure settings
     * @return the device's unique {@code ANDROID_ID} string
     */
    public String getDeviceId(Context context) {
        return Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

}