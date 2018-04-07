/*
 * Copyright 2018 Brian Pellin, Jeremy Jamet / Kunzisoft, Justin Gross.
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
package com.keepassdroid.utils;

import android.os.Parcel;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpannableReplacer {
    private final CharSequence mSource;
    private final CharSequence mReplacement;
    private final Matcher mMatcher;
    private int mAppendPosition;
    private final boolean mIsSpannable;

    public static CharSequence replace(CharSequence source, String regex,
                                       CharSequence replacement) {

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        return new SpannableReplacer(source, matcher, replacement).doReplace();
    }

    private SpannableReplacer(CharSequence source, Matcher matcher,
                     CharSequence replacement) {
        mSource = source;
        mReplacement = replacement;
        mMatcher = matcher;
        mAppendPosition = 0;
        mIsSpannable = replacement instanceof Spannable;
    }

    private CharSequence doReplace() {
        SpannableStringBuilder buffer = new SpannableStringBuilder();
        while (mMatcher.find()) {
            appendReplacement(buffer);
        }
        return appendTail(buffer);
    }

    private void appendReplacement(SpannableStringBuilder buffer) {
        buffer.append(mSource.subSequence(mAppendPosition, mMatcher.start()));
        CharSequence replacement = mIsSpannable
                ? copyCharSequenceWithSpans(mReplacement)
                : mReplacement;
        buffer.append(replacement);

        mAppendPosition = mMatcher.end();
    }

    public SpannableStringBuilder appendTail(SpannableStringBuilder buffer) {
        buffer.append(mSource.subSequence(mAppendPosition, mSource.length()));
        return buffer;
    }

    // This is a weird way of copying spans, but I don't know any better way.
    private CharSequence copyCharSequenceWithSpans(CharSequence string) {
        Parcel parcel = Parcel.obtain();
        try {
            TextUtils.writeToParcel(string, parcel, 0);
            parcel.setDataPosition(0);
            return TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }
}