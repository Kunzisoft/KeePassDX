package com.kunzisoft.keepass.colorpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

public class Scale extends View {
    private final String TAG = "Scale";

    private final int SCALE_SIZE;
    private final float SCALE_RANGE;

    private float BORDER_WIDTH = 1;
    private float PANEL_HEIGHT = 30f;
    private float PANEL_START_END_PADDING = 16f;
    private float PANEL_TOP_BOTTOM_PADDING = 2f;
    private float PANEL_CORNER_RADIUS = 6f;
    private int BORDER_COLOR = 0xfffafafa;

    private float TRACKER_WIDTH = 4f;
    private float TRACKER_OFFSET = 2f;
    private float TRACKER_STROKE = 3.5f;
    private int TRACKER_COLOR = 0xfff9f9f9;

    private OnValueUpdateListener mListener;

    private float mValue;
    private int[] mValues;
    private ScaleType mType;

    private Paint mPaint;
    private Paint mTrackerPaint;
    private Paint mBorderPaint;
    private Shader mShader;

    private RectF mRect;
    private RectF mTrackerRect;
    private RectF mBorderRect;

    private Point mStartTouchPoint = null;

    public interface OnValueUpdateListener {
        void onValueUpdate(ScaleType type, float val);
    }

    public enum ScaleType {
        HUE, SAT, VAL
    }

    public Scale(Context context, int[] values, ScaleType type, float size) {
        super(context);
        mValues = values;
        SCALE_SIZE = mValues.length;
        SCALE_RANGE = size;
        mType = type;
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //convert px to dp
        float density = getContext().getResources().getDisplayMetrics().density;
        PANEL_HEIGHT *= density;
        BORDER_WIDTH *= density;
        PANEL_START_END_PADDING *= density;
        PANEL_TOP_BOTTOM_PADDING *= density;
        PANEL_CORNER_RADIUS *= density;
        TRACKER_OFFSET *= density;
        TRACKER_WIDTH *= density;
        TRACKER_STROKE *= density;

        //add padding to scale
        setPadding(
                (int) PANEL_START_END_PADDING,
                (int) PANEL_TOP_BOTTOM_PADDING,
                (int) PANEL_START_END_PADDING,
                (int) PANEL_TOP_BOTTOM_PADDING
        );

        mPaint = new Paint();
        mTrackerPaint = new Paint();
        mBorderPaint = new Paint();

        mPaint.setAntiAlias(true);

        mTrackerPaint.setColor(TRACKER_COLOR);
        mTrackerPaint.setShadowLayer(1, 0, 0, Color.BLACK);
        mTrackerPaint.setStyle(Paint.Style.STROKE);
        mTrackerPaint.setStrokeWidth(TRACKER_STROKE);
        mTrackerPaint.setAntiAlias(true);

        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setColor(BORDER_COLOR);

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mRect.width() <= 0 || mRect.height() <= 0) return;

        if (mShader == null) {
            mShader = new LinearGradient(
                    mRect.left, mRect.top, mRect.right, mRect.bottom,
                    mValues, null, Shader.TileMode.CLAMP);
            mPaint.setShader(mShader);
        }

        canvas.drawRoundRect(mBorderRect, PANEL_CORNER_RADIUS, PANEL_CORNER_RADIUS, mBorderPaint);
        canvas.drawRoundRect(mRect, PANEL_CORNER_RADIUS, PANEL_CORNER_RADIUS, mPaint);

        //Draw tracker
        float x = mRect.left + (mValue * mRect.width() / SCALE_SIZE);

        mTrackerRect.left = x - TRACKER_WIDTH;
        mTrackerRect.right = x + TRACKER_WIDTH;
        mTrackerRect.top = mRect.top - TRACKER_OFFSET;
        mTrackerRect.bottom = mRect.bottom + TRACKER_OFFSET;

        canvas.drawRoundRect(mTrackerRect, 2, 2, mTrackerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean update = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartTouchPoint = new Point((int) event.getX(), (int) event.getY());
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_MOVE:
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_UP:
                mStartTouchPoint = null;
                update = moveTrackersIfNeeded(event);
                break;
        }

        if (update) {
            if (mListener != null) {
                mListener.onValueUpdate(mType, mValue * SCALE_RANGE / SCALE_SIZE);
            }
            invalidate();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean moveTrackersIfNeeded(MotionEvent event) {
        if (mStartTouchPoint == null) return false;

        if (mRect.contains(mStartTouchPoint.x, mStartTouchPoint.y)) {
            mValue = pointToValue(event.getX());
            return true;
        }
        return false;
    }

    private float pointToValue(float x) {
        if (x < mRect.left) {
            x = 0f;
        } else if (x > mRect.right) {
            x = mRect.width();
        } else {
            x = x - mRect.left;
        }
        return (x * SCALE_SIZE / mRect.width());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float height = (TRACKER_OFFSET) * 2 + PANEL_HEIGHT + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(widthMeasureSpec, (int) height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mRect = new RectF();
        mRect.left = TRACKER_OFFSET + getPaddingLeft() + BORDER_WIDTH;
        mRect.top = h - TRACKER_OFFSET - getPaddingBottom() - PANEL_HEIGHT + BORDER_WIDTH;
        mRect.right = w - TRACKER_OFFSET - getPaddingRight() - BORDER_WIDTH;
        mRect.bottom = h - TRACKER_OFFSET - getPaddingBottom() - BORDER_WIDTH;

        mBorderRect = new RectF();
        mBorderRect.left = mRect.left - BORDER_WIDTH;
        mBorderRect.top = mRect.top - BORDER_WIDTH;
        mBorderRect.right = mRect.right + BORDER_WIDTH;
        mBorderRect.bottom = mRect.bottom + BORDER_WIDTH;

        mTrackerRect = new RectF();
    }

    public void updateScale(int[] values) {
        mValues = values;
        mShader = null;
        invalidate();
    }

    public void setValue(float value) {
        mValue = value * SCALE_SIZE / SCALE_RANGE;
        invalidate();
    }

    public void invertScale(boolean invert) {
        if (invert) {
            setRotation(180);
        } else {
            setRotation(0);
        }
    }

    public void setOnValueUpdateListener(OnValueUpdateListener listener) {
        mListener = listener;
    }
}