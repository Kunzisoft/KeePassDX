/*
 * Copyright 2017 Brian Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.fingerprint;

import android.content.Context;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.widget.ImageView;

import tech.jgross.keepass.R;

@RequiresApi(api = Build.VERSION_CODES.M)
public class FingerPrintAnimatedVector {

    private AnimatedVectorDrawable scanFingerprint;

    public FingerPrintAnimatedVector(Context context, ImageView imageView) {
        scanFingerprint = (AnimatedVectorDrawable) context.getDrawable(R.drawable.scan_fingerprint);
        imageView.setImageDrawable(scanFingerprint);
    }

    public void startScan() {
        scanFingerprint.registerAnimationCallback(new Animatable2.AnimationCallback() {
            public void onAnimationEnd(Drawable drawable) {
                scanFingerprint.start();
            }
        });
        scanFingerprint.start();
    }

    public void stopScan() {
        scanFingerprint.stop();
    }
}
