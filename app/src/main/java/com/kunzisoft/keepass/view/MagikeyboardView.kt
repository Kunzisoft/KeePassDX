/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
@file:Suppress("DEPRECATION")

package com.kunzisoft.keepass.view

import android.content.Context
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import androidx.annotation.RequiresApi
import android.util.AttributeSet

import com.kunzisoft.keepass.magikeyboard.MagikIME.Companion.KEY_BACK_KEYBOARD
import com.kunzisoft.keepass.magikeyboard.MagikIME.Companion.KEY_CHANGE_KEYBOARD

class MagikeyboardView : KeyboardView {

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onLongPress(key: Keyboard.Key): Boolean {
        return if (key.codes[0] == KEY_BACK_KEYBOARD) {
            onKeyboardActionListener.onKey(KEY_CHANGE_KEYBOARD, IntArray(0))
            true
        } else {
            //Log.d("LatinKeyboardView", "KEY: " + key.codes[0]);
            super.onLongPress(key)
        }
    }
}
