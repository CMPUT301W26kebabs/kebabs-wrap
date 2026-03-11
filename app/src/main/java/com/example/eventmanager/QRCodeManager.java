package com.example.eventmanager;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.qrcode.QRCodeWriter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
public class QRCodeManager {
    private static final int QR_SIZE = 500;

    /**
     * Generates a QR code Bitmap from an Event ID.
     */
    public Bitmap generateEventQR(String eventId) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(eventId, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}