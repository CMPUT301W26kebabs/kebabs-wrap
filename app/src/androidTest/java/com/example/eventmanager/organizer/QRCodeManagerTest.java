package com.example.eventmanager.organizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmanager.managers.QRCodeManager;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Organizer — US 02.01.01 (promotional QR bitmap generation). */
@RunWith(AndroidJUnit4.class)
public class QRCodeManagerTest {

    @Test
    public void testQRGeneration_ReturnsBitmap() {
        QRCodeManager manager = new QRCodeManager();
        Bitmap result = manager.generateEventQR("test-event-123");

        assertNotNull("QR Bitmap should not be null", result);
        assertEquals(500, result.getWidth());
    }
}
