package com.keepassdroid.stylish;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public abstract class StylishActivity extends AppCompatActivity {

    private @StyleRes int themeId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.themeId = Stylish.getThemeId(this);
        setTheme(themeId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Stylish.getThemeId(this) != this.themeId) {
            Log.d(this.getClass().getName(), "Theme change detected, restarting activity");
            this.recreate();
        }
    }
}
