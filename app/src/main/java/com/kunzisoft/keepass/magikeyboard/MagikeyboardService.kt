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

package com.kunzisoft.keepass.magikeyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.EntrySelectionLauncherActivity
import com.kunzisoft.keepass.activities.helpers.EntrySelectionHelper
import com.kunzisoft.keepass.adapters.FieldsAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.DatabaseTaskProvider
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.otp.OtpEntryFields.OTP_TOKEN_FIELD
import com.kunzisoft.keepass.services.KeyboardEntryNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.KeyboardUtil.showKeyboardPicker
import com.kunzisoft.keepass.utils.KeyboardUtil.switchToPreviousKeyboard
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.LockReceiver
import com.kunzisoft.keepass.utils.REMOVE_ENTRY_MAGIKEYBOARD_ACTION
import com.kunzisoft.keepass.utils.registerLockReceiver
import com.kunzisoft.keepass.utils.unregisterLockReceiver
import java.util.UUID

class MagikeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var mDatabaseTaskProvider: DatabaseTaskProvider? = null
    private var mDatabase: ContextualDatabase? = null

    private var keyboardView: KeyboardView? = null
    private var entryContainer: View? = null
    private var entryText: TextView? = null
    private var databaseText: TextView? = null
    private var databaseColorView: ImageView? = null
    private var packageText: TextView? = null
    private var keyboard: Keyboard? = null
    private var keyboardEntry: Keyboard? = null
    private var popupCustomKeys: PopupWindow? = null
    private var fieldsAdapter: FieldsAdapter? = null
    private var playSoundDuringCLick: Boolean = false

    private var mFormPackageName: String? = null

    private var lockReceiver: LockReceiver? = null

    override fun onCreate() {
        super.onCreate()

        mDatabaseTaskProvider = DatabaseTaskProvider(this)
        mDatabaseTaskProvider?.registerProgressTask()
        mDatabaseTaskProvider?.onDatabaseRetrieved = { database ->
            this.mDatabase = database
            assignKeyboardView()
        }
        // Remove the entry and lock the keyboard when the lock signal is receive
        lockReceiver = LockReceiver {
                        removeEntryInfo()
                        assignKeyboardView()
        }
        lockReceiver?.backToPreviousKeyboardAction = {
            switchToPreviousKeyboard()
        }

        fieldsAdapter = FieldsAdapter(this)
        fieldsAdapter?.onItemClickListener = object : FieldsAdapter.OnItemClickListener {
            override fun onItemClick(item: Field) {
                currentInputConnection.commitText(getEntryInfo()?.getGeneratedFieldValue(item.name) , 1)
                actionTabAutomatically()
            }
        }

        registerLockReceiver(lockReceiver, true)
    }

    override fun onCreateInputView(): View {

        val rootKeyboardView = layoutInflater.inflate(R.layout.keyboard_container, null)
        entryContainer = rootKeyboardView.findViewById(R.id.magikeyboard_entry_container)
        entryText = rootKeyboardView.findViewById(R.id.magikeyboard_entry_text)
        databaseText = rootKeyboardView.findViewById(R.id.magikeyboard_database_text)
        databaseColorView = rootKeyboardView.findViewById(R.id.magikeyboard_database_color)
        packageText = rootKeyboardView.findViewById(R.id.magikeyboard_package_text)
        keyboardView = rootKeyboardView.findViewById(R.id.magikeyboard_view)

        if (keyboardView != null) {
            keyboard = Keyboard(this, R.xml.keyboard_password)
            keyboardEntry = Keyboard(this, R.xml.keyboard_password_entry)

            val context = baseContext
            val popupFieldsView = LayoutInflater.from(context)
                    .inflate(R.layout.keyboard_popup_fields, FrameLayout(context))

            popupCustomKeys = PopupWindow(context).apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
                contentView = popupFieldsView
            }

            val recyclerView = popupFieldsView.findViewById<RecyclerView>(R.id.keyboard_popup_fields_list)
            recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true)
            recyclerView.adapter = fieldsAdapter

            val closeView = popupFieldsView.findViewById<View>(R.id.keyboard_popup_close)
            closeView.setOnClickListener { popupCustomKeys?.dismiss() }

            assignKeyboardView()
            keyboardView?.onKeyboardActionListener = this

            return rootKeyboardView
        }

        return rootKeyboardView
    }

    private fun getEntryInfo(): EntryInfo? {
        var entryInfoRetrieved: EntryInfo? = null
        entryUUID?.let { entryId ->
            entryInfoRetrieved = mDatabase
                ?.getEntryById(NodeIdUUID(entryId))
                ?.getEntryInfo(mDatabase)
        }
        return entryInfoRetrieved
    }

    private fun assignKeyboardView() {
        dismissCustomKeys()
        if (keyboardView != null) {
            val entryInfo = getEntryInfo()
            populateEntryInfoInView(entryInfo)
            if (entryInfo != null) {
                if (keyboardEntry != null) {
                    keyboardView?.keyboard = keyboardEntry
                }
            } else {
                if (keyboard != null) {
                    keyboardView?.keyboard = keyboard
                }
            }

            // Define preferences
            keyboardView?.isHapticFeedbackEnabled = PreferencesUtil.isKeyboardVibrationEnable(this)
            playSoundDuringCLick = PreferencesUtil.isKeyboardSoundEnable(this)
        }
        setDatabaseViews()
    }

    private fun setDatabaseViews() {
        if (mDatabase == null || mDatabase?.loaded != true) {
            entryContainer?.visibility = View.GONE
        } else {
            entryContainer?.visibility = View.VISIBLE
        }
        databaseText?.text = mDatabase?.name ?: ""
        val databaseColor = mDatabase?.customColor
        if (databaseColor != null) {
            databaseColorView?.drawable?.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(databaseColor, BlendModeCompat.SRC_IN)
            databaseColorView?.visibility = View.VISIBLE
        } else {
            databaseColorView?.visibility = View.GONE
        }
    }

    private fun populateEntryInfoInView(entryInfo: EntryInfo?) {
        if (entryInfo == null) {
            entryText?.text = ""
            entryText?.visibility = View.GONE
        } else {
            entryText?.text = entryInfo.getVisualTitle()
            entryText?.visibility = View.VISIBLE
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        mFormPackageName = info.packageName
        if (!mFormPackageName.isNullOrEmpty()) {
            packageText?.text = mFormPackageName
            packageText?.visibility = View.VISIBLE
        } else {
            packageText?.visibility = View.GONE
        }
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

    override fun onKey(primaryCode: Int, keyCodes: IntArray) {
        val inputConnection = currentInputConnection

        // Vibrate
        playVibration(primaryCode)
        // Play a sound
        if (playSoundDuringCLick)
            playClick(primaryCode)

        when (primaryCode) {
            KEY_BACK_KEYBOARD -> {
                switchToPreviousKeyboard()
            }
            KEY_CHANGE_KEYBOARD -> {
                showKeyboardPicker()
            }
            KEY_ENTRY -> {
                var searchInfo: SearchInfo? = null
                if (mFormPackageName != null) {
                    searchInfo = SearchInfo().apply {
                        applicationId = mFormPackageName
                    }
                }
                actionKeyEntry(searchInfo)
            }
            KEY_ENTRY_ALT -> {
                actionKeyEntry()
            }
            KEY_LOCK -> {
                removeEntryInfo()
                sendBroadcast(Intent(LOCK_ACTION))
                dismissCustomKeys()
            }
            KEY_USERNAME -> {
                getEntryInfo()?.username?.let { username ->
                    currentInputConnection.commitText(username, 1)
                }
                actionTabAutomatically()
            }
            KEY_PASSWORD -> {
                val entryInfoKey = getEntryInfo()
                entryInfoKey?.password?.let { password ->
                    currentInputConnection.commitText(password, 1)
                }
                val otpFieldExists = entryInfoKey?.containsCustomField(OTP_TOKEN_FIELD) ?: false
                actionGoAutomatically(!otpFieldExists)
            }
            KEY_OTP -> {
                getEntryInfo()?.let { entryInfo ->
                    currentInputConnection.commitText(
                        entryInfo.getGeneratedFieldValue(OTP_TOKEN_FIELD), 1)
                }
                actionGoAutomatically()
            }
            KEY_OTP_ALT -> {
                getEntryInfo()?.let { entryInfo ->
                    val otpToken = entryInfo.getGeneratedFieldValue(OTP_TOKEN_FIELD)
                    if (otpToken.isNotEmpty()) {
                        // Cut to fill each digit separatelyKeyEvent.KEYCODE_TAB
                        val otpTokenChars = otpToken.chunked(1)
                        otpTokenChars.forEachIndexed { index, char ->
                            currentInputConnection.commitText(char, 1)
                            if (index < (otpTokenChars.size-1))
                                currentInputConnection.sendKeyEvent(
                                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB)
                                )
                        }
                    }
                }
                actionGoAutomatically()
            }
            KEY_URL -> {
                getEntryInfo()?.url?.let { url ->
                    currentInputConnection.commitText(url, 1)
                }
                actionGoAutomatically()
            }
            KEY_FIELDS -> {
                getEntryInfo()?.customFields?.let { customFields ->
                    fieldsAdapter?.apply {
                        setFields(customFields.filter { it.name != OTP_TOKEN_FIELD})
                        notifyDataSetChanged()
                    }
                }
                popupCustomKeys?.showAtLocation(keyboardView,
                    Gravity.END or Gravity.TOP, 0, 180)
            }
            Keyboard.KEYCODE_DELETE -> {
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            }
            Keyboard.KEYCODE_DONE -> inputConnection.performEditorAction(EditorInfo.IME_ACTION_GO)
        }
    }

    private fun actionKeyEntry(searchInfo: SearchInfo? = null) {
        SearchHelper.checkAutoSearchInfo(this,
            mDatabase,
            searchInfo,
            { _, items ->
                performSelection(
                    items,
                    {
                        // Automatically populate keyboard
                        addEntryAndLaunchNotificationIfAllowed(
                            this,
                            items[0],
                            true
                        )
                        assignKeyboardView()
                    },
                    {
                        launchEntrySelection(searchInfo)
                    }
                )
            },
            {
                // Select if not found
                launchEntrySelection(searchInfo)
            },
            {
                // Select if database not opened
                removeEntryInfo()
                launchEntrySelection(searchInfo)
            }
        )
    }

    private fun launchEntrySelection(searchInfo: SearchInfo?) {
        EntrySelectionLauncherActivity.launch(this, searchInfo)
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
        const val KEY_LOCK = 611
        const val KEY_ENTRY = 620
        const val KEY_ENTRY_ALT = 621
        const val KEY_USERNAME = 500
        const val KEY_PASSWORD = 510
        const val KEY_OTP = 515
        const val KEY_OTP_ALT = 516
        const val KEY_URL = 520
        const val KEY_FIELDS = 530

        private var entryUUID: UUID? = null

        private fun removeEntryInfo() {
            entryUUID = null
        }

        fun removeEntry(context: Context) {
            context.sendBroadcast(Intent(REMOVE_ENTRY_MAGIKEYBOARD_ACTION))
        }

        fun addEntryAndLaunchNotificationIfAllowed(context: Context, entry: EntryInfo, toast: Boolean = false) {
            // Add a new entry
            entryUUID = entry.id
            // Launch notification if allowed
            KeyboardEntryNotificationService.launchNotificationIfAllowed(context, entry, toast)
        }

        fun performSelection(items: List<EntryInfo>,
                             actionPopulateKeyboard: (entryInfo: EntryInfo) -> Unit,
                             actionEntrySelection: (autoSearch: Boolean) -> Unit) {
             if (items.size == 1) {
                 val itemFound = items[0]
                if (entryUUID != itemFound.id) {
                    actionPopulateKeyboard.invoke(itemFound)
                } else {
                    // Force selection if magikeyboard already populated
                    actionEntrySelection.invoke(false)
                }
            } else if (items.size > 1) {
                // Select the one we want in the selection
                actionEntrySelection.invoke(true)
            } else {
                // Select an arbitrary one
                actionEntrySelection.invoke(false)
            }
        }

        fun populateKeyboardAndMoveAppToBackground(activity: Activity,
                                                   entry: EntryInfo,
                                                   toast: Boolean = true) {
            // Populate Magikeyboard with entry
            addEntryAndLaunchNotificationIfAllowed(activity, entry, toast)
            // Consume the selection mode
            EntrySelectionHelper.removeModesFromIntent(activity.intent)
            activity.moveTaskToBack(true)
        }
    }
}