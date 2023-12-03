package com.kunzisoft.keepass.colorpicker;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.annotation.Nullable;

public class ColorPicker extends LinearLayout implements Scale.OnValueUpdateListener {
    private final String TAG = "ColorPicker";

    private final int HUE_SIZE = 360;
    private final int SAT_SIZE = 255;
    private final int VAL_SIZE = 255;

    private final float SCALE_SPACING = 10f;

    private Scale mHueScale;
    private Scale mSatScale;
    private Scale mValScale;

    private float mHue = 90f;
    private float mSat = 1f;
    private float mVal = 1f;

    private OnColorChangedListener mListener;

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    public ColorPicker(Context context) {
        this(context, null);
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ColorPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mHueScale = new Scale(context, getHueValues(), Scale.ScaleType.HUE, 360f);
        mSatScale = new Scale(context, getSatValues(), Scale.ScaleType.SAT, 1f);
        mValScale = new Scale(context, getValValues(), Scale.ScaleType.VAL, 1f);

        mHueScale.setOnValueUpdateListener(this);
        mSatScale.setOnValueUpdateListener(this);
        mValScale.setOnValueUpdateListener(this);

        setOrientation(LinearLayout.VERTICAL);
        removeAllViews();

        addView(mHueScale);
        addView(getSpacer());
        addView(mSatScale);
        addView(getSpacer());
        addView(mValScale);

        mValScale.invertScale(true);
    }

    private int[] getHueValues() {
        int[] hue = new int[HUE_SIZE + 1];
        for (int i = 0; i < HUE_SIZE + 1; i++) {
            hue[i] = Color.HSVToColor(new float[]{i, 1f, 1f});
        }
        return hue;
    }

    private int[] getSatValues() {
        int[] sat = new int[SAT_SIZE + 1];
        int color = Color.HSVToColor(new float[]{mHue, 1f, 1f});

        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        for (int i = 0; i < SAT_SIZE + 1; i++) {
            sat[i] = Color.rgb(
                    255 - (255 - red) * i / 255,
                    255 - (255 - green) * i / 255,
                    255 - (255 - blue) * i / 255);
        }
        return sat;
    }

    private int[] getValValues() {
        int[] val = new int[VAL_SIZE + 1];

        for (int i = 0; i < VAL_SIZE + 1; i++) {
            val[i] = Color.rgb(i, i, i);
        }
        return val;
    }

    private int getColor() {
        return Color.HSVToColor(new float[]{mHue, mSat, mVal});
    }

    @Override
    public void onValueUpdate(Scale.ScaleType type, float val) {
        switch (type) {
            case HUE:
                mHue = val;
                mSatScale.updateScale(getSatValues());
                break;
            case SAT:
                mSat = val;
                break;
            case VAL:
                mVal = val;
                break;
        }

        if (mListener != null) {
            mListener.onColorChanged(getColor());
        }
    }

    public void setColor(String hex) {
        setColor(Color.parseColor(hex));
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        mHue = hsv[0];
        mSat = hsv[1];
        mVal = hsv[2];

        mHueScale.setValue(mHue);
        mSatScale.setValue(mSat);
        mValScale.setValue(mVal);

        mSatScale.updateScale(getSatValues());
        if (mListener != null) {
            mListener.onColorChanged(getColor());
        }
    }

    private Space getSpacer() {
        float height = (int) (SCALE_SPACING * getContext().getResources().getDisplayMetrics().density);
        Space space = new Space(getContext());
        space.setLayoutParams(new LayoutParams(0, (int) height));
        return space;
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }
}