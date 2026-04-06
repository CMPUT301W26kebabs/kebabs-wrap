package com.example.eventmanager.entrant;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmanager.managers.DeviceAuthManager;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Entrant — US 01.07.01 (device identification). */
@RunWith(AndroidJUnit4.class)
public class DeviceAuthManagerTest {

    @Test
    public void getDeviceId_returnsNonEmptyString() {
        Context context = ApplicationProvider.getApplicationContext();
        DeviceAuthManager manager = new DeviceAuthManager();

        String deviceId = manager.getDeviceId(context);

        assertNotNull(deviceId);
        assertFalse(deviceId.trim().isEmpty());
    }
}
