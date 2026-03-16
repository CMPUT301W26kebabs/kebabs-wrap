package com.example.eventmanager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import android.graphics.Bitmap;

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