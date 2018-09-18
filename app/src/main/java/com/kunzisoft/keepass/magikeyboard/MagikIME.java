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
package com.kunzisoft.keepass.magikeyboard;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.magikeyboard.receiver.LockBroadcastReceiver;
import com.kunzisoft.keepass.model.Entry;

import static com.kunzisoft.keepass.magikeyboard.receiver.LockBroadcastReceiver.LOCK_ACTION;

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

    private LockBroadcastReceiver lockBroadcastReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        // Remove the entry and lock the keyboard when the lock signal is receive
        lockBroadcastReceiver = new LockBroadcastReceiver() {
            @Override
            public void onReceiveLock(Context context, Intent intent) {
                entryKey = null;
                assignKeyboardView();
            }
        };
        registerReceiver(lockBroadcastReceiver, new IntentFilter(LOCK_ACTION));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(lockBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);
        keyboard = new Keyboard(this, R.xml.keyboard_password);
        keyboard_entry = new Keyboard(this, R.xml.keyboard_password_entry);

        assignKeyboardView();
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(false);
        keyboardView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        return keyboardView;
    }

    private void assignKeyboardView() {
        if (keyboardView != null) {
            if (entryKey != null) {
                if (keyboard_entry != null)
                    keyboardView.setKeyboard(keyboard_entry);
            } else {
                if (keyboard != null)
                    keyboardView.setKeyboard(keyboard);
            }
        }
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        assignKeyboardView();
    }

    public static Entry getEntryKey() {
        return entryKey;
    }

    public static void setEntryKey(Entry entry) {
        entryKey = entry;
    }

    public static void deleteEntryKey(Context context) {
        Intent lockIntent = new Intent(LOCK_ACTION);
        context.sendBroadcast(lockIntent);
        entryKey = null;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        switch(primaryCode){
            case KEY_CHANGE_KEYBOARD:
                try {
                    InputMethodManager imeManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    assert imeManager != null;
                    assert getWindow().getWindow() != null;
                    imeManager.switchToLastInputMethod(getWindow().getWindow().getAttributes().token);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to switch to the previous IME", e);
                    InputMethodManager imeManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imeManager != null)
                        imeManager.showInputMethodPicker();
                }
                // TODO Add a long press to choose the keyboard
                break;
            case KEY_UNLOCK:
                // TODO Unlock key
                break;
            case KEY_ENTRY:
                Intent intent = new Intent(this, EntryRetrieverActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case KEY_LOCK:
                deleteEntryKey(this);
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
                // TODO Fields key
            case Keyboard.KEYCODE_DELETE :
                ic.deleteSurroundingText(1, 0);
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            default:
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getBoolean(getString(R.string.keyboard_key_vibrate_key), getResources().getBoolean(R.bool.keyboard_key_vibrate_default)))
            vibrate();

        if (sharedPreferences.getBoolean(getString(R.string.keyboard_key_sound_key), getResources().getBoolean(R.bool.keyboard_key_sound_default)))
            playClick(primaryCode);

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

    private void vibrate() {
        // TODO better vibration
        /*
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
        */
    }

    private void playClick(int keyCode){
        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        if (am != null)
            switch(keyCode){
                case Keyboard.KEYCODE_DONE:
                case 10:
                    am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                    break;
                case Keyboard.KEYCODE_DELETE:
                    am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                    break;
                default: am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
            }
    }
}