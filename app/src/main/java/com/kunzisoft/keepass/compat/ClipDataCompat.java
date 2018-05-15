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
package com.kunzisoft.keepass.compat;

import android.content.Intent;
import android.net.Uri;

import java.lang.reflect.Method;

public class ClipDataCompat {
    private static Method getClipDataFromIntent;
    private static Method getDescription;
    private static Method getItemCount;
    private static Method getLabel;
    private static Method getItemAt;
    private static Method getUri;

    private static boolean initSucceded;

    static {
        try {
            Class clipData = Class.forName("android.content.ClipData");
            getDescription = clipData.getMethod("getDescription", (Class[])null);
            getItemCount = clipData.getMethod("getItemCount", (Class[])null);
            getItemAt = clipData.getMethod("getItemAt", new Class[]{int.class});
            Class clipDescription = Class.forName("android.content.ClipDescription");
            getLabel = clipDescription.getMethod("getLabel", (Class[])null);

            Class clipDataItem = Class.forName("android.content.ClipData$Item");
            getUri = clipDataItem.getMethod("getUri", (Class[])null);

            getClipDataFromIntent = Intent.class.getMethod("getClipData", (Class[])null);

            initSucceded = true;
        } catch (Exception e) {
            initSucceded = false;
        }
    }

    public static Uri getUriFromIntent(Intent i, String key) {
        if (initSucceded) {
            try {
                Object clip = getClipDataFromIntent.invoke(i);

                if (clip != null) {
                    Object clipDescription = getDescription.invoke(clip);
                    CharSequence label = (CharSequence)getLabel.invoke(clipDescription);
                    if (label.equals(key)) {
                        int itemCount = (int) getItemCount.invoke(clip);
                        if (itemCount == 1) {
                            Object clipItem = getItemAt.invoke(clip,0);
                            if (clipItem != null) {
                                return (Uri)getUri.invoke(clipItem);
                            }
                        }
                    }
                }
                return null;

            } catch (Exception e) {
                // Fall through below to backup method if reflection fails
            }
        }

        return i.getParcelableExtra(key);
    }
}
