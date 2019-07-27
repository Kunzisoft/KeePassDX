package com.kunzisoft.keepass.magikeyboard.view

import android.content.Context
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet

import com.kunzisoft.keepass.magikeyboard.MagikIME.Companion.KEY_BACK_KEYBOARD
import com.kunzisoft.keepass.magikeyboard.MagikIME.Companion.KEY_CHANGE_KEYBOARD

class MagikeyboardView : KeyboardView {

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onLongPress(key: Keyboard.Key): Boolean {
        // TODO Action on long press
        if (key.codes[0] == KEY_BACK_KEYBOARD) {
            onKeyboardActionListener.onKey(KEY_CHANGE_KEYBOARD, null)
            return true
        } else {
            //Log.d("LatinKeyboardView", "KEY: " + key.codes[0]);
            return super.onLongPress(key)
        }
    }
}
