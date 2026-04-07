package com.example.eventmanager.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom View that renders an animated grid of pulsing cells.
 * Cells radiate outward from the centre with staggered opacity animation,
 * producing a breathing / ripple effect similar to a DataGrid hero component.
 */
public class AnimatedGridView extends View {

    private static final int COLS = 28;
    private static final int ROWS = 50;
    private static final float OPACITY_MIN = 0.04f;
    private static final float OPACITY_MAX = 0.45f;
    private static final long CYCLE_MS = 4200L;

    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float[] delays;
    private int color = 0xFF4A43EC;
    private long startTime;
    private boolean running = true;

    public AnimatedGridView(Context context) {
        super(context);
        init();
    }

    public AnimatedGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        startTime = System.currentTimeMillis();
        int total = ROWS * COLS;
        delays = new float[total];
        int centerRow = ROWS / 2;
        int centerCol = COLS / 2;

        for (int i = 0; i < total; i++) {
            int r = i / COLS;
            int c = i % COLS;
            float dr = Math.abs(r - centerRow);
            float dc = Math.abs(c - centerCol);
            delays[i] = (float) Math.sqrt(dr * dr + dc * dc) * 0.18f;
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float spacing = 3f;
        float cellW = (w - (COLS - 1) * spacing) / COLS;
        float cellH = (h - (ROWS - 1) * spacing) / ROWS;

        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;
        float cycleSec = CYCLE_MS / 1000f;

        int total = ROWS * COLS;
        for (int i = 0; i < total; i++) {
            int r = i / COLS;
            int c = i % COLS;

            float t = (elapsed - delays[i]) / cycleSec;
            t = t - (float) Math.floor(t);
            float alpha = OPACITY_MIN + (OPACITY_MAX - OPACITY_MIN)
                    * (0.5f + 0.5f * (float) Math.sin(t * 2 * Math.PI));

            cellPaint.setColor(color);
            cellPaint.setAlpha((int) (alpha * 255));

            float left = c * (cellW + spacing);
            float top = r * (cellH + spacing);
            canvas.drawRoundRect(left, top, left + cellW, top + cellH, 3f, 3f, cellPaint);
        }

        if (running) {
            postInvalidateOnAnimation();
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
