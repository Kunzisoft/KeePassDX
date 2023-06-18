/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.autofill

import android.annotation.TargetApi
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.service.autofill.FillRequest
import android.view.inputmethod.InlineSuggestionsRequest
import androidx.annotation.RequiresApi
import com.kunzisoft.keepass.utils.readParcelableCompat

/**
 * Utility class only to prevent java.lang.NoClassDefFoundError for old Android version and new lib compilation
 */
@RequiresApi(Build.VERSION_CODES.O)
class CompatInlineSuggestionsRequest : Parcelable {

    @TargetApi(Build.VERSION_CODES.R)
    var inlineSuggestionsRequest: InlineSuggestionsRequest? = null
        private set

    constructor(fillRequest: FillRequest) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.inlineSuggestionsRequest = fillRequest.inlineSuggestionsRequest
        } else {
            this.inlineSuggestionsRequest = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    constructor(inlineSuggestionsRequest: InlineSuggestionsRequest?) {
        this.inlineSuggestionsRequest = inlineSuggestionsRequest
    }

    constructor(parcel: Parcel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.inlineSuggestionsRequest = parcel.readParcelableCompat()
        }
        else {
            this.inlineSuggestionsRequest = null
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            parcel.writeParcelable(inlineSuggestionsRequest, flags)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CompatInlineSuggestionsRequest> {
        override fun createFromParcel(parcel: Parcel): CompatInlineSuggestionsRequest {
            return CompatInlineSuggestionsRequest(parcel)
        }

        override fun newArray(size: Int): Array<CompatInlineSuggestionsRequest?> {
            return arrayOfNulls(size)
        }
    }

}