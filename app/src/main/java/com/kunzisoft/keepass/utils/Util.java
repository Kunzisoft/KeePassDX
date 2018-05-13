/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Util {

	public static void gotoUrl(Context context, String url) throws ActivityNotFoundException {
		if ( url != null && url.length() > 0 ) {
			Uri uri = Uri.parse(url);
			context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
		}
	}
	
	public static void gotoUrl(Context context, int resId) throws ActivityNotFoundException {
		gotoUrl(context, context.getString(resId));
	}
	
	public static void copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buf = new byte[1024];
		int read;
		try {
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        } catch (OutOfMemoryError error) {
		    throw new IOException(error);
        }
	}

    /**
     * Replace font by monospace, must be called after seText()
     */
    public static void applyFontVisibilityTo(final Context context, final TextView textView) {
        Typeface typeFace=Typeface.createFromAsset(context.getAssets(),"fonts/DroidSansMonoSlashed.ttf");
        textView.setTypeface(typeFace);
    }

    /**
     * Replace font by monospace, must be called after seText()
     */
	public static void applyFontVisibilityTo(final Context context, final EditText editText) {
        applyFontVisibilityTo(context, (TextView) editText);
	}

    public static void lockScreenOrientation(Activity activity) {
		if (activity != null) {
			int currentOrientation = activity.getResources().getConfiguration().orientation;
			if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			} else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			}
		}
	}

    public static void unlockScreenOrientation(Activity activity) {
		if (activity != null)
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	}
}
