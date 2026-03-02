package com.example.eventmanager;

import org.junit.Test;

import static org.junit.Assert.*;

import android.graphics.Bitmap;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
}

public class QRCodeManagerTest {
    @Test
    public void testQRGeneration_ReturnsBitmap() {
        QRCodeManager manager = new QRCodeManager();
        Bitmap result = manager.generateEventQR("test-event-123");
        assertNotNull("QR Bitmap should not be null", result);
        assertEquals(500, result.getWidth());
    }
}