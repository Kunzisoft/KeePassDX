/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat


abstract class ProtectedTextFieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle),
    GenericTextFieldView, ProtectedFieldView {

    var isProtected: Boolean = false
        private set
    var needUserVerificationToReveal: Boolean = true
        private set
    private var mRevealed: Boolean = false

    // Only to fix rebuild view from template
    var onSaveInstanceState: (() -> Unit)? = null

    override fun isRevealed(): Boolean {
        return mRevealed
    }

    override fun mask() {
        mRevealed = false
        changeProtectedValueParameters()
    }

    override fun reveal() {
        mRevealed = true
        changeProtectedValueParameters()
    }

    override fun setProtection(
        isProtected: Boolean,
        isRevealedByDefault: Boolean,
        needUserVerificationToReveal: Boolean
    ) {
        this.isProtected = isProtected
        this.mRevealed = isRevealedByDefault
        this.needUserVerificationToReveal = needUserVerificationToReveal
        if (isProtected) {
            changeProtectedValueParameters()
        }
    }

    protected abstract fun changeProtectedValueParameters()

    override fun onSaveInstanceState(): Parcelable? {
        onSaveInstanceState?.invoke()
        return ProtectionState(super.onSaveInstanceState()).apply {
            this.isRevealed = isRevealed()
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            is ProtectionState -> {
                super.onRestoreInstanceState(state.superState)
                mRevealed = state.isRevealed
            }
            else -> super.onRestoreInstanceState(state)
        }
    }

    internal class ProtectionState : BaseSavedState {

        var isRevealed: Boolean = false

        constructor(superState: Parcelable?) : super(superState)

        private constructor(parcel: Parcel) : super(parcel) {
            isRevealed = parcel.readBooleanCompat()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeBooleanCompat(isRevealed)
        }

        companion object CREATOR : Creator<ProtectionState> {
            override fun createFromParcel(parcel: Parcel): ProtectionState {
                return ProtectionState(parcel)
            }

            override fun newArray(size: Int): Array<ProtectionState?> {
                return arrayOfNulls(size)
            }
        }
    }
}
