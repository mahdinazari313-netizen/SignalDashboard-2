package com.example.signaldashboard.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

/**
 * A small circular countdown drawn purely with Canvas (no libraries). It
 * starts as a full filled circle and empties clockwise from the top as time
 * elapses between start and end times, like a pie chart shrinking.
 */
public class CountdownCircleView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private long startTime;
    private long endTime;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            invalidate();
            handler.postDelayed(this, 1000L);
        }
    };

    public CountdownCircleView(Context context) {
        super(context);
        init();
    }

    public CountdownCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(3f);
        trackPaint.setColor(Color.parseColor("#3A3A3A"));
        arcPaint.setStyle(Paint.Style.FILL);
    }

    /** color = ARGB int, e.g. from Color.parseColor("#4CAF50") for Buy. */
    public void setTimes(long startTime, long endTime, int color) {
        this.startTime = startTime;
        this.endTime = endTime;
        arcPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = resolveSize(dpToPx(20), widthMeasureSpec);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padding = 3f;
        arcRect.set(padding, padding, getWidth() - padding, getHeight() - padding);

        canvas.drawOval(arcRect, trackPaint);

        long total = endTime - startTime;
        long remaining = endTime - System.currentTimeMillis();
        float fraction = total > 0 ? (float) remaining / (float) total : 0f;
        if (fraction < 0f) fraction = 0f;
        if (fraction > 1f) fraction = 1f;

        // Full circle at fraction=1, sweeping clockwise from 12 o'clock as it empties.
        canvas.drawArc(arcRect, -90f, fraction * 360f, true, arcPaint);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
