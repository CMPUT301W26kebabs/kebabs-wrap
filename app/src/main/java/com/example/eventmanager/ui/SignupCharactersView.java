package com.example.eventmanager.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Draws 4 colorful geometric cartoon characters with idle eye animations.
 * Inspired by the animated-characters-login-page React component,
 * adapted for native Android with gentle pupil drift and random blinking.
 */
public class SignupCharactersView extends View {

    private static final int CHAR_COUNT = 4;

    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyeWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mouthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final int[] bodyColors = {0xFF4A43EC, 0xFF2D2D2D, 0xFFFF9063, 0xFFE8D754};

    private final long[] nextBlinkTime = new long[CHAR_COUNT];
    private final boolean[] isBlinking = new boolean[CHAR_COUNT];
    private final long[] blinkEndTime = new long[CHAR_COUNT];

    private long startTime;
    private boolean running = true;

    public SignupCharactersView(Context context) {
        super(context);
        init();
    }

    public SignupCharactersView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SignupCharactersView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        startTime = System.currentTimeMillis();
        eyeWhitePaint.setColor(0xFFFFFFFF);
        pupilPaint.setColor(0xFF2D2D2D);
        mouthPaint.setColor(0xFF2D2D2D);
        mouthPaint.setStrokeCap(Paint.Cap.ROUND);
        mouthPaint.setStrokeWidth(3f);

        long now = System.currentTimeMillis();
        for (int i = 0; i < CHAR_COUNT; i++) {
            nextBlinkTime[i] = now + 2000 + (long) (Math.random() * 4000);
            isBlinking[i] = false;
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;
        long now = System.currentTimeMillis();

        updateBlinks(now);

        float bottom = h;
        float centerX = w * 0.5f;

        drawPurpleChar(canvas, centerX, bottom, w, h, elapsed, 0);
        drawDarkChar(canvas, centerX, bottom, w, h, elapsed, 1);
        drawOrangeChar(canvas, centerX, bottom, w, h, elapsed, 2);
        drawYellowChar(canvas, centerX, bottom, w, h, elapsed, 3);

        if (running) {
            postInvalidateOnAnimation();
        }
    }

    private void updateBlinks(long now) {
        for (int i = 0; i < CHAR_COUNT; i++) {
            if (isBlinking[i]) {
                if (now >= blinkEndTime[i]) {
                    isBlinking[i] = false;
                    nextBlinkTime[i] = now + 3000 + (long) (Math.random() * 4000);
                }
            } else if (now >= nextBlinkTime[i]) {
                isBlinking[i] = true;
                blinkEndTime[i] = now + 150;
            }
        }
    }

    private float[] pupilOffset(float elapsed, int index) {
        float speed = 0.4f + index * 0.15f;
        float phase = index * 1.7f;
        float px = 2.5f * (float) Math.sin(elapsed * speed + phase);
        float py = 1.8f * (float) Math.cos(elapsed * speed * 0.7f + phase + 1.0f);
        return new float[]{px, py};
    }

    // Purple tall rectangle (back-left, tallest) -- z=1
    private void drawPurpleChar(Canvas canvas, float cx, float bottom, int w, int h, float elapsed, int idx) {
        float charW = w * 0.30f;
        float charH = h * 0.85f;
        float left = cx - w * 0.35f;

        float sway = 2f * (float) Math.sin(elapsed * 0.5f);

        bodyPaint.setColor(bodyColors[idx]);
        float[] radii = {14f, 14f, 14f, 14f, 0f, 0f, 0f, 0f};
        rect.set(left + sway, bottom - charH, left + charW + sway, bottom);
        canvas.save();
        android.graphics.Path path = new android.graphics.Path();
        path.addRoundRect(rect, radii, android.graphics.Path.Direction.CW);
        canvas.drawPath(path, bodyPaint);
        canvas.restore();

        float eyeCx = left + charW * 0.35f + sway;
        float eyeCx2 = left + charW * 0.70f + sway;
        float eyeCy = bottom - charH + charH * 0.18f;
        float eyeR = charW * 0.09f;
        float pupilR = eyeR * 0.5f;

        if (isBlinking[idx]) {
            eyeWhitePaint.setStrokeWidth(2f);
            canvas.drawLine(eyeCx - eyeR, eyeCy, eyeCx + eyeR, eyeCy, eyeWhitePaint);
            canvas.drawLine(eyeCx2 - eyeR, eyeCy, eyeCx2 + eyeR, eyeCy, eyeWhitePaint);
        } else {
            float[] pOff = pupilOffset(elapsed, idx);
            canvas.drawCircle(eyeCx, eyeCy, eyeR, eyeWhitePaint);
            canvas.drawCircle(eyeCx2, eyeCy, eyeR, eyeWhitePaint);
            canvas.drawCircle(eyeCx + pOff[0], eyeCy + pOff[1], pupilR, pupilPaint);
            canvas.drawCircle(eyeCx2 + pOff[0], eyeCy + pOff[1], pupilR, pupilPaint);
        }
    }

    // Dark tall rectangle (middle) -- z=2
    private void drawDarkChar(Canvas canvas, float cx, float bottom, int w, int h, float elapsed, int idx) {
        float charW = w * 0.22f;
        float charH = h * 0.65f;
        float left = cx - w * 0.08f;

        float sway = 1.5f * (float) Math.sin(elapsed * 0.6f + 1.0f);

        bodyPaint.setColor(bodyColors[idx]);
        float[] radii = {10f, 10f, 10f, 10f, 0f, 0f, 0f, 0f};
        rect.set(left + sway, bottom - charH, left + charW + sway, bottom);
        android.graphics.Path path = new android.graphics.Path();
        path.addRoundRect(rect, radii, android.graphics.Path.Direction.CW);
        canvas.drawPath(path, bodyPaint);

        float eyeCx = left + charW * 0.32f + sway;
        float eyeCx2 = left + charW * 0.68f + sway;
        float eyeCy = bottom - charH + charH * 0.16f;
        float eyeR = charW * 0.10f;
        float pupilR = eyeR * 0.45f;

        if (isBlinking[idx]) {
            eyeWhitePaint.setStrokeWidth(2f);
            canvas.drawLine(eyeCx - eyeR, eyeCy, eyeCx + eyeR, eyeCy, eyeWhitePaint);
            canvas.drawLine(eyeCx2 - eyeR, eyeCy, eyeCx2 + eyeR, eyeCy, eyeWhitePaint);
        } else {
            float[] pOff = pupilOffset(elapsed, idx);
            canvas.drawCircle(eyeCx, eyeCy, eyeR, eyeWhitePaint);
            canvas.drawCircle(eyeCx2, eyeCy, eyeR, eyeWhitePaint);
            canvas.drawCircle(eyeCx + pOff[0], eyeCy + pOff[1], pupilR, pupilPaint);
            canvas.drawCircle(eyeCx2 + pOff[0], eyeCy + pOff[1], pupilR, pupilPaint);
        }
    }

    // Orange semi-circle (front-left, shorter) -- z=3
    private void drawOrangeChar(Canvas canvas, float cx, float bottom, int w, int h, float elapsed, int idx) {
        float charW = w * 0.40f;
        float charH = h * 0.42f;
        float left = cx - w * 0.48f;

        float sway = 1.2f * (float) Math.sin(elapsed * 0.45f + 2.0f);

        bodyPaint.setColor(bodyColors[idx]);
        rect.set(left + sway, bottom - charH, left + charW + sway, bottom + charH);
        canvas.drawOval(rect, bodyPaint);
        bodyPaint.setColor(bodyColors[idx]);
        rect.set(left + sway, bottom, left + charW + sway, bottom + charH);
        canvas.save();
        canvas.clipRect(left + sway - 1, bottom, left + charW + sway + 1, bottom + charH + 1);
        canvas.drawColor(0x00000000);
        canvas.restore();

        rect.set(left + sway, bottom - charH, left + charW + sway, bottom + charH);
        canvas.save();
        canvas.clipRect(left + sway - 1, bottom - charH - 1, left + charW + sway + 1, bottom);
        canvas.drawOval(rect, bodyPaint);
        canvas.restore();

        float eyeCx = left + charW * 0.35f + sway;
        float eyeCx2 = left + charW * 0.62f + sway;
        float eyeCy = bottom - charH * 0.52f;
        float pupilR = charW * 0.035f;

        float[] pOff = pupilOffset(elapsed, idx);
        if (!isBlinking[idx]) {
            canvas.drawCircle(eyeCx + pOff[0], eyeCy + pOff[1], pupilR, pupilPaint);
            canvas.drawCircle(eyeCx2 + pOff[0], eyeCy + pOff[1], pupilR, pupilPaint);
        } else {
            pupilPaint.setStrokeWidth(2f);
            canvas.drawLine(eyeCx - pupilR * 1.5f, eyeCy, eyeCx + pupilR * 1.5f, eyeCy, pupilPaint);
            canvas.drawLine(eyeCx2 - pupilR * 1.5f, eyeCy, eyeCx2 + pupilR * 1.5f, eyeCy, pupilPaint);
        }
    }

    // Yellow rounded rect (front-right) -- z=4
    private void drawYellowChar(Canvas canvas, float cx, float bottom, int w, int h, float elapsed, int idx) {
        float charW = w * 0.26f;
        float charH = h * 0.50f;
        float left = cx + w * 0.14f;

        float sway = 1.8f * (float) Math.sin(elapsed * 0.55f + 3.0f);

        bodyPaint.setColor(bodyColors[idx]);
        float topRadius = charW * 0.5f;
        float[] radii = {topRadius, topRadius, topRadius, topRadius, 0f, 0f, 0f, 0f};
        rect.set(left + sway, bottom - charH, left + charW + sway, bottom);
        android.graphics.Path path = new android.graphics.Path();
        path.addRoundRect(rect, radii, android.graphics.Path.Direction.CW);
        canvas.drawPath(path, bodyPaint);

        float eyeCx = left + charW * 0.33f + sway;
        float eyeCx2 = left + charW * 0.67f + sway;
        float eyeCy = bottom - charH + charH * 0.22f;
        float pupilR = charW * 0.04f;

        float[] pOff = pupilOffset(elapsed, idx);
        if (!isBlinking[idx]) {
            canvas.drawCircle(eyeCx + pOff[0], eyeCy + pOff[1], pupilR, pupilPaint);
            canvas.drawCircle(eyeCx2 + pOff[0], eyeCy + pOff[1], pupilR, pupilPaint);
        } else {
            pupilPaint.setStrokeWidth(2f);
            canvas.drawLine(eyeCx - pupilR * 2, eyeCy, eyeCx + pupilR * 2, eyeCy, pupilPaint);
            canvas.drawLine(eyeCx2 - pupilR * 2, eyeCy, eyeCx2 + pupilR * 2, eyeCy, pupilPaint);
        }

        float mouthCx = left + charW * 0.5f + sway + pOff[0] * 0.3f;
        float mouthCy = bottom - charH + charH * 0.42f + pOff[1] * 0.2f;
        float mouthW = charW * 0.35f;
        if (!isBlinking[idx]) {
            canvas.drawLine(mouthCx - mouthW * 0.5f, mouthCy, mouthCx + mouthW * 0.5f, mouthCy, mouthPaint);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        startTime = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        running = false;
        super.onDetachedFromWindow();
    }
}
