package com.example.eventmanager.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Renders animated aurora borealis-style gradient bands.
 * Multiple semi-transparent gradient layers drift at different speeds,
 * producing a flowing, organic light show behind content.
 */
public class AuroraBackgroundView extends View {

    private static final int BAND_COUNT = 4;
    private static final float BASE_ALPHA = 0.45f;

    private final Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int[][] bandColors = {
            {0xFF4A43EC, 0xFF6C63FF, 0xFF3D36C4},   // brand accent band
            {0xFF7C3AED, 0xFFA78BFA, 0xFF5B21B6},   // violet band
            {0xFF3B82F6, 0xFF93C5FD, 0xFF1D4ED8},   // blue band
            {0xFF8B5CF6, 0xFFDDD6FE, 0xFF6D28D9},   // lavender band
    };

    private final float[] bandSpeeds = {0.35f, 0.25f, 0.45f, 0.18f};
    private final float[] bandPhases = {0f, 1.5f, 3.0f, 4.5f};
    private final float[] bandHeights = {0.55f, 0.40f, 0.50f, 0.35f};

    private long startTime;
    private boolean running = true;

    public AuroraBackgroundView(Context context) {
        super(context);
        init();
    }

    public AuroraBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuroraBackgroundView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        startTime = System.currentTimeMillis();
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;

        canvas.drawColor(0xFF0D0B2E);

        for (int i = 0; i < BAND_COUNT; i++) {
            drawAuroraBand(canvas, w, h, elapsed, i);
        }

        drawCenterGlow(canvas, w, h, elapsed);

        if (running) {
            postInvalidateOnAnimation();
        }
    }

    private void drawAuroraBand(Canvas canvas, int w, int h, float time, int index) {
        float speed = bandSpeeds[index];
        float phase = bandPhases[index];
        float bandH = h * bandHeights[index];

        float t = time * speed + phase;

        float yCenter = h * (0.25f + 0.2f * (float) Math.sin(t * 0.7));
        float xOffset = w * 0.3f * (float) Math.sin(t * 0.5 + index);

        float waveAmplitude = h * 0.08f;
        float wave1 = waveAmplitude * (float) Math.sin(t * 1.3 + index * 2.0);
        float wave2 = waveAmplitude * 0.5f * (float) Math.sin(t * 2.1 + index * 1.5);

        float top = yCenter - bandH * 0.5f + wave1;
        float bottom = yCenter + bandH * 0.5f + wave2;

        float angle = 15f * (float) Math.sin(t * 0.3 + index);
        float rad = (float) Math.toRadians(angle);
        float dx = (float) Math.sin(rad) * bandH * 0.5f;

        int[] colors = bandColors[index];
        float alphaMultiplier = BASE_ALPHA + 0.12f * (float) Math.sin(t * 0.8 + index * 1.7);

        int c0 = applyAlpha(colors[0], alphaMultiplier * 0.0f);
        int c1 = applyAlpha(colors[0], alphaMultiplier * 0.6f);
        int c2 = applyAlpha(colors[1], alphaMultiplier);
        int c3 = applyAlpha(colors[2], alphaMultiplier * 0.6f);
        int c4 = applyAlpha(colors[2], alphaMultiplier * 0.0f);

        LinearGradient gradient = new LinearGradient(
                xOffset + dx, top,
                xOffset - dx + w, bottom,
                new int[]{c0, c1, c2, c3, c4},
                new float[]{0f, 0.2f, 0.5f, 0.8f, 1f},
                Shader.TileMode.CLAMP
        );

        bandPaint.setShader(gradient);
        canvas.drawRect(-w * 0.2f, top, w * 1.2f, bottom, bandPaint);
    }

    private void drawCenterGlow(Canvas canvas, int w, int h, float time) {
        float pulse = 0.8f + 0.2f * (float) Math.sin(time * 0.6);
        float radius = Math.min(w, h) * 0.6f * pulse;
        float cx = w * 0.5f + w * 0.05f * (float) Math.sin(time * 0.3);
        float cy = h * 0.3f + h * 0.03f * (float) Math.cos(time * 0.4);

        RadialGradient glow = new RadialGradient(
                cx, cy, radius,
                new int[]{
                        applyAlpha(0xFF4A43EC, 0.18f * pulse),
                        applyAlpha(0xFF7C3AED, 0.08f * pulse),
                        0x00000000
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        glowPaint.setShader(glow);
        canvas.drawCircle(cx, cy, radius, glowPaint);
    }

    private int applyAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (color & 0x00FFFFFF);
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
