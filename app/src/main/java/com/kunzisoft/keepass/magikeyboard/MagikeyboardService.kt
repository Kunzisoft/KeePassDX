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

package com.kunzisoft.keepass.magikeyboard

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.MagikeyboardLauncherActivity
import com.kunzisoft.keepass.adapters.FieldsAdapter
import com.kunzisoft.keepass.database.action.DatabaseTaskProvider
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpEntryFields.OTP_TOKEN_FIELD
import com.kunzisoft.keepass.services.KeyboardEntryNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.*

class MagikeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var mDatabaseTaskProvider: DatabaseTaskProvider? = null
    private var mDatabase: Database? = null

    private var keyboardView: KeyboardView? = null
    private var entryText: TextView? = null
    private var keyboard: Keyboard? = null
    private var keyboardEntry: Keyboard? = null
    private var popupCustomKeys: PopupWindow? = null
    private var fieldsAdapter: FieldsAdapter? = null
    private var playSoundDuringCLick: Boolean = false

    private var lockReceiver: LockReceiver? = null

    override fun onCreate() {
        super.onCreate()

        mDatabaseTaskProvider = DatabaseTaskProvider(this)
        mDatabaseTaskProvider?.registerProgressTask()
        mDatabaseTaskProvider?.onDatabaseRetrieved = { database ->
            this.mDatabase = database
        }
        // Remove the entry and lock the keyboard when the lock signal is receive
        lockReceiver = LockReceiver {
                        removeEntryInfo()
                        assignKeyboardView()
        }
        lockReceiver?.backToPreviousKeyboardAction = {
            switchToPreviousKeyboard()
        }

        registerLockReceiver(lockReceiver, true)
    }

    override fun onCreateInputView(): View {

        val rootKeyboardView = layoutInflater.inflate(R.layout.keyboard_container, null)
        entryText = rootKeyboardView.findViewById(R.id.magikeyboard_entry_text)
        keyboardView = rootKeyboardView.findViewById(R.id.magikeyboard_view)

        if (keyboardView != null) {
            keyboard = Keyboard(this, R.xml.keyboard_password)
            keyboardEntry = Keyboard(this, R.xml.keyboard_password_entry)

            val context = baseContext
            val popupFieldsView = LayoutInflater.from(context)
                    .inflate(R.layout.keyboard_popup_fields, FrameLayout(context))

            popupCustomKeys = PopupWindow(context).apply {
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
                contentView = popupFieldsView
            }

            val recyclerView = popupFieldsView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.keyboard_popup_fields_list)
            fieldsAdapter = FieldsAdapter(this)
            fieldsAdapter?.onItemClickListener = object : FieldsAdapter.OnItemClickListener {
                override fun onItemClick(item: Field) {
                    currentInputConnection.commitText(entryInfoKey?.getGeneratedFieldValue(item.name) , 1)
                    actionTabAutomatically()
                }
            }
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, true)
            recyclerView.adapter = fieldsAdapter

            val closeView = popupFieldsView.findViewById<View>(R.id.keyboard_popup_close)
            closeView.setOnClickListener { popupCustomKeys?.dismiss() }

            // Remove entry info if the database is not loaded
            // or if entry info timestamp is before database loaded timestamp
            val databaseTime = mDatabase?.loadTimestamp
            val entryTime = entryInfoTimestamp
            if (mDatabase == null
                || mDatabase?.loaded != true
                || databaseTime == null
                || entryTime == null
                || entryTime < databaseTime) {
                removeEntryInfo()
            }
            assignKeyboardView()
            keyboardView?.setOnKeyboardActionListener(this)
            keyboardView?.isPreviewEnabled = false

            return rootKeyboardView
        }

        return super.onCreateInputView()
    }

    private fun assignKeyboardView() {
        dismissCustomKeys()
        if (keyboardView != null) {
            if (entryInfoKey != null) {
                if (keyboardEntry != null) {
                    populateEntryInfoInView()
                    keyboardView?.keyboard = keyboardEntry
                }
            } else {
                if (keyboard != null) {
                    hideEntryInfo()
                    keyboardView?.keyboard = keyboard
                }
            }

            // Define preferences
            keyboardView?.isHapticFeedbackEnabled = PreferencesUtil.isKeyboardVibrationEnable(this)
            playSoundDuringCLick = PreferencesUtil.isKeyboardSoundEnable(this)
        }
    }

    private fun populateEntryInfoInView() {
        entryText?.visibility = View.VISIBLE
        if (entryInfoKey?.title?.isNotEmpty() == true) {
            entryText?.text = entryInfoKey?.title
        } else {
            hideEntryInfo()
        }
    }

    private fun hideEntryInfo() {
        entryText?.visibility = View.GONE
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        assignKeyboardView()
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return resources.getBoolean(R.bool.magikeyboard_allow_fullscreen_mode)
                && super.onEvaluateFullscreenMode()
    }

    private fun playVibration(keyCode: Int) {
        when (keyCode) {
            Keyboard.KEYCODE_DELETE -> {}
            else -> keyboardView?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    private fun playClick(keyCode: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        when (keyCode) {
            Keyboard.KEYCODE_DONE, 10 -> audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN)
            Keyboard.KEYCODE_DELETE -> audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE)
            else -> audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        }
    }

    private fun switchToPreviousKeyboard() {
        var imeManager: InputMethodManager? = null
        try {
            imeManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToPreviousInputMethod()
            } else {
                window.window?.let { window ->
                    imeManager?.switchToLastInputMethod(window.attributes.token)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to switch to the previous IME", e)
            imeManager?.showInputMethodPicker()
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray) {
        val inputConnection = currentInputConnection

        // Vibrate
        playVibration(primaryCode)
        // Play a sound
        if (playSoundDuringCLick)
            playClick(primaryCode)

        when (primaryCode) {
            KEY_BACK_KEYBOARD -> switchToPreviousKeyboard()

            KEY_CHANGE_KEYBOARD -> {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
                        ?.showInputMethodPicker()
            }
            KEY_UNLOCK -> {
            }
            KEY_ENTRY -> {
                // Stop current service and reinit entry
                stopService(Intent(this, KeyboardEntryNotificationService::class.java))
                removeEntryInfo()
                val intent = Intent(this, MagikeyboardLauncherActivity::class.java)
                // New task needed because don't launch from an Activity context
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            KEY_LOCK -> {
                removeEntryInfo()
                sendBroadcast(Intent(LOCK_ACTION))
                dismissCustomKeys()
            }
            KEY_USERNAME -> {
                if (entryInfoKey != null) {
                    currentInputConnection.commitText(entryInfoKey!!.username, 1)
                }
                actionTabAutomatically()
            }
            KEY_PASSWORD -> {
                if (entryInfoKey != null) {
                    currentInputConnection.commitText(entryInfoKey!!.password, 1)
                }
                val otpFieldExists = entryInfoKey?.containsCustomField(OTP_TOKEN_FIELD) ?: false
                actionGoAutomatically(!otpFieldExists)
            }
            KEY_OTP -> {
                if (entryInfoKey != null) {
                    currentInputConnection.commitText(
                            entryInfoKey!!.getGeneratedFieldValue(OTP_TOKEN_FIELD), 1)
                }
                actionGoAutomatically()
            }
            KEY_URL -> {
                if (entryInfoKey != null) {
                    currentInputConnection.commitText(entryInfoKey!!.url, 1)
                }
                actionGoAutomatically()
            }
            KEY_FIELDS -> {
                if (entryInfoKey != null) {
                    fieldsAdapter?.apply {
                        setFields(entryInfoKey!!.customFields.filter { it.name != OTP_TOKEN_FIELD})
                        notifyDataSetChanged()
                    }
                }
                popupCustomKeys?.showAtLocation(keyboardView, Gravity.END or Gravity.TOP, 0, 0)
            }
            Keyboard.KEYCODE_DELETE -> {
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            }
            Keyboard.KEYCODE_DONE -> inputConnection.performEditorAction(EditorInfo.IME_ACTION_GO)
        }
    }

    private fun actionTabAutomatically() {
        if (PreferencesUtil.isAutoGoActionEnable(this))
            currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
    }

    private fun actionGoAutomatically(switchToPreviousKeyboardIfAllowed: Boolean = true) {
        if (PreferencesUtil.isAutoGoActionEnable(this)) {
            currentInputConnection.performEditorAction(EditorInfo.IME_ACTION_GO)
            if (switchToPreviousKeyboardIfAllowed
                    && PreferencesUtil.isKeyboardPreviousFillInEnable(this)) {
                switchToPreviousKeyboard()
            }
        }
    }

    override fun onPress(primaryCode: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        if (audioManager != null)
            when (primaryCode) {
                Keyboard.KEYCODE_DELETE -> keyboardView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
    }

    override fun onRelease(primaryCode: Int) {
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> { }
        }
    }

    override fun onText(text: CharSequence) {}

    override fun swipeDown() {}

    override fun swipeLeft() {}

    override fun swipeRight() {}

    override fun swipeUp() {}

    private fun dismissCustomKeys() {
        popupCustomKeys?.dismiss()
        fieldsAdapter?.clear()
    }

    override fun onDestroy() {
        dismissCustomKeys()
        unregisterLockReceiver(lockReceiver)
        mDatabaseTaskProvider?.unregisterProgressTask()
        super.onDestroy()
    }

    companion object {
        private val TAG = MagikeyboardService::class.java.name

        const val KEY_BACK_KEYBOARD = 600
        const val KEY_CHANGE_KEYBOARD = 601
        private const val KEY_UNLOCK = 610
        private const val KEY_LOCK = 611
        private const val KEY_ENTRY = 620
        private const val KEY_USERNAME = 500
        private const val KEY_PASSWORD = 510
        private const val KEY_OTP = 515
        private const val KEY_URL = 520
        private const val KEY_FIELDS = 530

        // TODO Retrieve entry info from id and service when database is open
        private var entryInfoKey: EntryInfo? = null
        private var entryInfoTimestamp: Long? = null

        private fun removeEntryInfo() {
            entryInfoKey = null
            entryInfoTimestamp = null
        }

        fun removeEntry(context: Context) {
            context.sendBroadcast(Intent(REMOVE_ENTRY_MAGIKEYBOARD_ACTION))
        }

        fun addEntryAndLaunchNotificationIfAllowed(context: Context, entry: EntryInfo, toast: Boolean = false) {
            // Add a new entry
            entryInfoKey = entry
            entryInfoTimestamp = System.currentTimeMillis()
            // Launch notification if allowed
            KeyboardEntryNotificationService.launchNotificationIfAllowed(context, entry, toast)
        }
    }
}