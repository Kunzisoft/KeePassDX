/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.magikeyboard;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import com.kunzisoft.keepass_model.Entry;

public class MagikIME extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {
    private static final String TAG = MagikIME.class.getName();

    private static final int KEY_CHANGE_KEYBOARD = 600;
    private static final int KEY_UNLOCK = 610;
    private static final int KEY_LOCK = 611;
    private static final int KEY_ENTRY = 620;
    private static final int KEY_USERNAME = 500;
    private static final int KEY_PASSWORD = 510;
    private static final int KEY_URL = 520;
    private static final int KEY_FIELDS = 530;

    private static Entry entryKey = null;

    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private Keyboard keyboard_entry;

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);
        keyboard = new Keyboard(this, R.xml.password_keys);
        keyboard_entry = new Keyboard(this, R.xml.password_entry_keys);

        assignKeyboardView();
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(false);

        return keyboardView;
    }

    private void assignKeyboardView() {
        if (entryKey != null) {
            keyboardView.setKeyboard(keyboard_entry);
        } else {
            keyboardView.setKeyboard(keyboard);
        }
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        assignKeyboardView();
    }

    public static void setEntryKey(Entry entry) {
        entryKey = entry;
    }

    public static void deleteEntryKey() {
        entryKey = null;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        switch(primaryCode){
            case KEY_CHANGE_KEYBOARD:
                InputMethodManager imeManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imeManager != null)
                    imeManager.showInputMethodPicker();
                break;
            case KEY_ENTRY:
            case KEY_UNLOCK:
                Intent intent = new Intent(this, EntryRetrieverActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case KEY_LOCK:
                Intent lockIntent = new Intent("com.kunzisoft.keepass.LOCK");
                sendBroadcast(lockIntent);
                deleteEntryKey();
                assignKeyboardView();
                break;
            case KEY_USERNAME:
                if (entryKey != null) {
                    InputConnection inputConnection = getCurrentInputConnection();
                    inputConnection.commitText(entryKey.getUsername(), 1);
                }
                break;
            case KEY_PASSWORD:
                if (entryKey != null) {
                    InputConnection inputConnection = getCurrentInputConnection();
                    inputConnection.commitText(entryKey.getPassword(), 1);
                }
                break;
            case KEY_URL:
                if (entryKey != null) {
                    InputConnection inputConnection = getCurrentInputConnection();
                    inputConnection.commitText(entryKey.getUrl(), 1);
                }
                break;
            case KEY_FIELDS:
                break;
            case Keyboard.KEYCODE_DELETE :
                ic.deleteSurroundingText(1, 0);
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            default:
        }
    }


    @Override
    public void onPress(int primaryCode) {
    }

    @Override
    public void onRelease(int primaryCode) {
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeUp() {
    }
}