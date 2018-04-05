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
package com.keepassdroid.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StrikethroughSpan;
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

	private final static String stringToStrikeThrough = "0";

    /**
     * Replace font by monospace and strike through all zeros, must be called after seText()
     */
    public static void applyFontVisibilityTo(final TextView textView) {
        textView.setText(strikeThroughToZero(textView.getText()));
        textView.setTypeface(Typeface.MONOSPACE);
    }

    /**
     * Replace font by monospace and strike through all zeros, must be called after seText()
     */
	public static void applyFontVisibilityTo(final EditText editText) {
        // Assign spans to default text
        editText.setText(strikeThroughToZero(editText.getText()));
        // Add spans for each new 0 character
        class TextWatcherCustomFont implements TextWatcher {

            private boolean applySpannable;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                applySpannable = count < after;
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (applySpannable) {
                    String text = charSequence.toString();
                    if (text.contains(stringToStrikeThrough)) {
                        for (int index = text.indexOf(stringToStrikeThrough);
                                index >= 0; index = text.indexOf(stringToStrikeThrough,
                                index + 1)) {
                            editText.getText().setSpan(new StrikethroughSpan(),
                                    index,
                                    index + stringToStrikeThrough.length(),
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };
        TextWatcher textWatcher = new TextWatcherCustomFont();
        editText.addTextChangedListener(textWatcher);
        editText.setTypeface(Typeface.MONOSPACE);
	}

	private static CharSequence strikeThroughToZero(final CharSequence text) {
	    if (text.toString().contains(stringToStrikeThrough)) {
            SpannableString spannable = new SpannableString(stringToStrikeThrough);
            spannable.setSpan(new StrikethroughSpan(),
                    0,
                    stringToStrikeThrough.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return SpannableReplacer.replace(text, stringToStrikeThrough, spannable);
        }
        return text;
    }
}
