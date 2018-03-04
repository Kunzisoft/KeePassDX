package com.keepassdroid.database.edit;

import android.os.Handler;

import com.keepassdroid.database.PwNode;

public abstract class AfterAddNodeOnFinish extends OnFinish {
    public AfterAddNodeOnFinish(Handler handler) {
        super(handler);
    }

    public abstract void run(PwNode pwNode);
}
