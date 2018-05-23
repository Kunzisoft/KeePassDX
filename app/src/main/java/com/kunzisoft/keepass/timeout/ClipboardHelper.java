/*
 *
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.timeout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;
import android.widget.Toast;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.exception.SamsungClipboardException;
import com.kunzisoft.keepass.tasks.UIToastTask;

import java.util.Timer;
import java.util.TimerTask;

public class ClipboardHelper {

    private Context context;
    private ClipboardManager clipboardManager;

    private Timer mTimer = new Timer();

    public ClipboardHelper(Context context) {
        this.context = context;
        this.clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    public void timeoutCopyToClipboard(String text) {
        timeoutCopyToClipboard(text, "");
    }

    public void timeoutCopyToClipboard(String text, String toastString) {
        if (!toastString.isEmpty())
            Toast.makeText(context, toastString, Toast.LENGTH_LONG).show();
        try {
            copyToClipboard(text);
        } catch (SamsungClipboardException e) {
            showSamsungDialog();
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String sClipClear = prefs.getString(context.getString(R.string.clipboard_timeout_key),
                context.getString(R.string.clipboard_timeout_default));

        long clipClearTime = Long.parseLong(sClipClear);

        if ( clipClearTime > 0 ) {
            mTimer.schedule(new ClearClipboardTask(context, text), clipClearTime);
        }
    }

    public CharSequence getClipboard(Context context) {
        if (clipboardManager.hasPrimaryClip()) {
            ClipData data = clipboardManager.getPrimaryClip();
            if (data.getItemCount() > 0) {
                CharSequence text = data.getItemAt(0).coerceToText(context);
                if (text != null) {
                    return text;
                }
            }
        }
        return "";
    }

    public void copyToClipboard(String value) throws SamsungClipboardException {
        copyToClipboard("", value);
    }

    public void copyToClipboard(String label, String value) throws SamsungClipboardException {
        try {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value));
        } catch (NullPointerException e) {
            throw new SamsungClipboardException(e);
        }
    }

    public void cleanClipboard() throws SamsungClipboardException {
        cleanClipboard("");
    }

    public void cleanClipboard(String label) throws SamsungClipboardException {
        copyToClipboard(label,"");
    }


    // Setup to allow the toast to happen in the foreground
    private final Handler uiThreadCallback = new Handler();

    // Task which clears the clipboard, and sends a toast to the foreground.
    private class ClearClipboardTask extends TimerTask {

        private final String mClearText;
        private final Context mCtx;

        ClearClipboardTask(Context ctx, String clearText) {
            mClearText = clearText;
            mCtx = ctx;
        }

        @Override
        public void run() {
            String currentClip = getClipboard(mCtx).toString();
            if ( currentClip.equals(mClearText) ) {
                try {
                    cleanClipboard();
                    uiThreadCallback.post(new UIToastTask(mCtx, R.string.clipboard_cleared));
                } catch (SamsungClipboardException e) {
                    uiThreadCallback.post(new UIToastTask(mCtx, R.string.clipboard_error_clear));
                }
            }
        }
    }

    private void showSamsungDialog() {
        String text = context.getString(R.string.clipboard_error).concat(System.getProperty("line.separator"))
                .concat(context.getString(R.string.clipboard_error_url));
        SpannableString s = new SpannableString(text);
        TextView tv = new TextView(context);
        tv.setText(s);
        tv.setAutoLinkMask(Activity.RESULT_OK);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        Linkify.addLinks(s, Linkify.WEB_URLS);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.clipboard_error_title)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .setView(tv)
                .show();
    }
}
