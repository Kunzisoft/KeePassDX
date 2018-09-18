package com.kunzisoft.keepass.magikeyboard.view;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;

import static com.kunzisoft.keepass.magikeyboard.MagikIME.KEY_BACK_KEYBOARD;
import static com.kunzisoft.keepass.magikeyboard.MagikIME.KEY_CHANGE_KEYBOARD;

public class MagikeyboardView extends KeyboardView {

	public MagikeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MagikeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public MagikeyboardView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	protected boolean onLongPress(Keyboard.Key key) {
		// TODO Action on long press
		if (key.codes[0] == KEY_BACK_KEYBOARD) {
			getOnKeyboardActionListener().onKey(KEY_CHANGE_KEYBOARD, null);
			return true;
		} else {
			//Log.d("LatinKeyboardView", "KEY: " + key.codes[0]);
			return super.onLongPress(key);
		}
	}
}
