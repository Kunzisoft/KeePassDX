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

package com.kunzisoft.keepass.credentialprovider.magikeyboard

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.KeyboardEntriesAdapter
import com.kunzisoft.keepass.adapters.KeyboardFieldsAdapter
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.buildIcon
import com.kunzisoft.keepass.credentialprovider.TypeMode
import com.kunzisoft.keepass.credentialprovider.activity.EntrySelectionLauncherActivity
import com.kunzisoft.keepass.credentialprovider.autofill.isKeeAutofillActivated
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.DatabaseTaskProvider
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.services.ClipboardEntryNotificationService
import com.kunzisoft.keepass.services.KeyboardEntryNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.AppUtil
import com.kunzisoft.keepass.utils.AppUtil.isElementAllowed
import com.kunzisoft.keepass.utils.AppUtil.withoutBrowserOrAppBlocked
import com.kunzisoft.keepass.utils.EXTRA_PROGRESS
import com.kunzisoft.keepass.utils.KeyboardUtil.showKeyboardPicker
import com.kunzisoft.keepass.utils.KeyboardUtil.switchToPreviousKeyboard
import com.kunzisoft.keepass.utils.LOCK_ACTION
import com.kunzisoft.keepass.utils.LockReceiver
import com.kunzisoft.keepass.utils.REMOVE_ENTRY_MAGIKEYBOARD_ACTION
import com.kunzisoft.keepass.utils.UPDATE_TIMEOUT_PROGRESS_ACTION
import com.kunzisoft.keepass.utils.clear
import com.kunzisoft.keepass.utils.registerLockReceiver
import com.kunzisoft.keepass.utils.unregisterLockReceiver
import java.util.UUID

class MagikeyboardService : InputMethodService(),
    KeyboardView.OnKeyboardActionListener,
    LifecycleOwner {

    private var mDatabaseTaskProvider: DatabaseTaskProvider? = null
    private var mDatabase: ContextualDatabase? = null

    private var keyboardView: KeyboardView? = null
    private var entryContainer: View? = null
    private var databaseText: TextView? = null
    private var databaseColorView: ImageView? = null
    private var containerPackageText: View? = null
    private var containerShareText: View? = null
    private var shareBrowserText: TextView? = null
    private var packageText: TextView? = null
    private var appIdIcon: ImageView? = null
    private var webDomainIcon: ImageView? = null
    private var keyboard: Keyboard? = null
    private var keyboardEntry: Keyboard? = null
    private var entryListView: RecyclerView? = null
    private var popupCustomKeys: PopupWindow? = null
    private var screenshotModeView: View? = null
    private var timeoutProgressBar: ProgressBar? = null
    private var entriesAdapter: KeyboardEntriesAdapter? = null
    private var fieldsAdapter: KeyboardFieldsAdapter? = null
    private var playSoundDuringCLick: Boolean = false

    private var lockReceiver: LockReceiver? = null

    private val onScreenshotModePrefListener = OnSharedPreferenceChangeListener { _, key ->
        if (key != getString(R.string.enable_screenshot_mode_key))
            return@OnSharedPreferenceChangeListener
        setScreenshotMode()
    }

    private val lifecycleDispatcher = ServiceLifecycleDispatcher(this)

    override val lifecycle: Lifecycle
        get() = lifecycleDispatcher.lifecycle

    override fun onCreate() {
        lifecycleDispatcher.onServicePreSuperOnCreate()
        super.onCreate()

        mDatabaseTaskProvider = DatabaseTaskProvider(this)
        mDatabaseTaskProvider?.registerProgressTask()
        mDatabaseTaskProvider?.onDatabaseRetrieved = { database ->
            this.mDatabase = database
            assignKeyboardView()
        }
        // Remove the entry and lock the keyboard when the lock signal is receive
        lockReceiver = object : LockReceiver({
            removeSearchInfo()
            removeEntryInfo()
            assignKeyboardView()
        }) {
            override fun onReceive(context: Context, intent: Intent) {
                super.onReceive(context, intent)
                if (intent.action == UPDATE_TIMEOUT_PROGRESS_ACTION) {
                    val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                    timeoutProgressBar?.progress = progress
                    timeoutProgressBar?.visibility = if (progress > 0) VISIBLE else GONE
                }
            }
        }
        lockReceiver?.backToPreviousKeyboardAction = {
            switchToPreviousKeyboard()
        }

        entriesAdapter = KeyboardEntriesAdapter(this)
        entriesAdapter?.entrySelectionListener = object  : KeyboardEntriesAdapter.EntrySelectionListener {
            override fun onEntrySelected(item: KeyboardEntriesAdapter.KeyboardEntry) {
                KeyboardEntryNotificationService.launchNotificationIfAllowed(
                    this@MagikeyboardService,
                    item.title
                )
                assignKeyboardView()
            }
        }

        fieldsAdapter = KeyboardFieldsAdapter(this)
        fieldsAdapter?.onItemClickListener = object : KeyboardFieldsAdapter.OnItemClickListener {
            override fun onItemClick(item: Field) {
                getEntryInfo()?.getGeneratedFieldValue(item.name)?.let { otpToken ->
                    currentInputConnection.commitText(String(otpToken), 1)
                    actionTabAutomatically()
                }
            }
        }

        entryUUIDList.observe(this) { entryIdList ->
            entryIdList?.let {
                if (entryIdList.isNotEmpty()) {
                    entriesAdapter?.setEntries(
                        entryIdList.mapNotNull {
                            getEntryInfo(it)?.let { entry ->
                                KeyboardEntriesAdapter.KeyboardEntry(
                                    id = entry.id,
                                    icon = mDatabase?.let { database ->
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            entry.buildIcon(
                                                this@MagikeyboardService,
                                                database
                                            )
                                        } else null
                                    },
                                    title = entry.getVisualTitle(),
                                    subtitle = entry.username
                                )
                            }
                        }
                    )
                } else {
                    entriesAdapter?.clear()
                }
            } ?: entriesAdapter?.clear()
            assignKeyboardView()
        }

        searchInfo.observe(this) { _ ->
            assignKeyboardView()
        }

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(onScreenshotModePrefListener)

        registerLockReceiver(
            lockReceiver = lockReceiver,
            registerKeyboardAction = true,
            registerTimeoutProgress = true
        )
    }

    override fun onBindInput() {
        lifecycleDispatcher.onServicePreSuperOnBind()
        super.onBindInput()
    }

    override fun onCreateInputView(): View {

        val rootKeyboardView = layoutInflater.inflate(R.layout.keyboard_container, null)
        entryContainer = rootKeyboardView.findViewById(R.id.magikeyboard_entry_container)
        entryListView = rootKeyboardView.findViewById(R.id.magikeyboard_entry_list)
        databaseText = rootKeyboardView.findViewById(R.id.magikeyboard_database_text)
        databaseColorView = rootKeyboardView.findViewById(R.id.magikeyboard_database_color)
        containerPackageText = rootKeyboardView.findViewById(R.id.magikeyboard_container_package)
        containerShareText = rootKeyboardView.findViewById(R.id.magikeyboard_share_browser)
        shareBrowserText = rootKeyboardView.findViewById(R.id.magikeyboard_share_browser_text)
        packageText = rootKeyboardView.findViewById(R.id.magikeyboard_package_text)
        appIdIcon = rootKeyboardView.findViewById(R.id.magikeyboard_app_id_icon)
        webDomainIcon = rootKeyboardView.findViewById(R.id.magikeyboard_web_domain_icon)
        keyboardView = rootKeyboardView.findViewById(R.id.magikeyboard_view)
        screenshotModeView = rootKeyboardView.findViewById(R.id.screenshot_mode_banner)
        timeoutProgressBar = rootKeyboardView.findViewById(R.id.magikeyboard_timeout_progress)

        if (keyboardView != null) {
            keyboard = Keyboard(this, R.xml.keyboard_password)
            keyboardEntry = Keyboard(this, R.xml.keyboard_password_entry)

            val context = baseContext

            entryListView?.apply {
                layoutManager = LinearLayoutManager(
                    this@MagikeyboardService,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = entriesAdapter
            }

            val popupFieldsView = LayoutInflater.from(context)
                    .inflate(R.layout.keyboard_popup_fields, FrameLayout(context))

            popupCustomKeys = PopupWindow(context).apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
                contentView = popupFieldsView
            }

            popupFieldsView.findViewById<RecyclerView>(R.id.keyboard_popup_fields_list)?.apply {
                layoutManager = LinearLayoutManager(
                    this@MagikeyboardService,
                    LinearLayoutManager.HORIZONTAL,
                    true
                )
                adapter = fieldsAdapter
            }

            val closeView = popupFieldsView.findViewById<View>(R.id.keyboard_popup_close)
            closeView.setOnClickListener { popupCustomKeys?.dismiss() }

            assignKeyboardView()
            keyboardView?.onKeyboardActionListener = this

            return rootKeyboardView
        }

        return rootKeyboardView
    }

    private fun getEntryInfo(entryId: UUID? = entriesAdapter?.selectedEntry?.id): EntryInfo? {
        var entryInfoRetrieved: EntryInfo? = null
        entryId?.let {
            entryInfoRetrieved = mDatabase
                ?.getEntryById(NodeIdUUID(entryId))
                ?.getEntryInfo(mDatabase)
        }
        return entryInfoRetrieved
    }

    private fun assignKeyboardView() {
        val entryListEmpty = entryUUIDList.value.isNullOrEmpty()
        val searchInfo: SearchInfo? = searchInfo.value
        val searchString = searchInfo?.toString()
        if (searchInfo != null
            && searchString.isNullOrEmpty().not()
            ) {
            if (searchInfo.isDomainSearch) {
                appIdIcon?.visibility = GONE
                webDomainIcon?.visibility = VISIBLE
            } else if (searchInfo.isAppIdSearch) {
                appIdIcon?.visibility = VISIBLE
                webDomainIcon?.visibility = GONE
            } else {
                appIdIcon?.visibility = GONE
                webDomainIcon?.visibility = GONE
            }
            packageText?.text = searchString
            containerPackageText?.visibility = VISIBLE
            containerShareText?.visibility = GONE
        } else {
            containerPackageText?.visibility = GONE
            shareBrowserText?.text = shareBrowser.value?.let {
                getString(R.string.keyboard_share_browser, it)
            } ?: ""
            containerShareText?.visibility = if (entryListEmpty && shareBrowser.value != null)
                VISIBLE else GONE
        }
        dismissCustomKeys()
        if (keyboardView != null) {
            if (entryListEmpty) {
                entryListView?.visibility = GONE
                if (keyboard != null) {
                    keyboardView?.keyboard = keyboard
                }
            } else {
                entryListView?.visibility = VISIBLE
                if (keyboardEntry != null) {
                    keyboardView?.keyboard = keyboardEntry
                }
            }
            // Define preferences
            keyboardView?.isHapticFeedbackEnabled = PreferencesUtil.isKeyboardVibrationEnable(this)
            playSoundDuringCLick = PreferencesUtil.isKeyboardSoundEnable(this)
        }
        setDatabaseViews()
        entriesAdapter?.notifyDataSetChanged()
    }

    private fun setDatabaseViews() {
        if (mDatabase == null || mDatabase?.loaded != true) {
            entryContainer?.visibility = GONE
        } else {
            entryContainer?.visibility = VISIBLE
        }
        databaseText?.text = mDatabase?.name ?: ""
        val databaseColor = mDatabase?.customColor
        if (databaseColor != null) {
            databaseColorView?.drawable?.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(databaseColor, BlendModeCompat.SRC_IN)
            databaseColorView?.visibility = VISIBLE
        } else {
            databaseColorView?.visibility = GONE
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        addSearchInfo(application, SearchInfo().apply {
            applicationId = info.packageName
        }, TypeMode.MAGIKEYBOARD)
        assignKeyboardView()
        setScreenshotMode()
    }

    override fun onUnbindInput() {
        super.onUnbindInput()
        // Do not clear the search context when the bound client
        // is no longer associated with the input method #2394
        if (!application.isKeeAutofillActivated()
            || !PreferencesUtil.isAutofillSharedToMagikeyboardEnable(application))
            removeSearchInfo()
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
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager?
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
                // Filter browser and app blocked to prevent unwanted auto save
                actionKeyEntry(searchInfo.value?.withoutBrowserOrAppBlocked(this) ?: SearchInfo())
            }
            KEY_ENTRY_ALT -> {
                actionKeyEntry(SearchInfo())
            }
            KEY_LOCK -> {
                removeSearchInfo()
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
                    currentInputConnection.commitText(String(password), 1)
                }
                val otpFieldExists = entryInfoKey?.containsOtpToken() ?: false
                actionGoAutomatically(!otpFieldExists)
            }
            KEY_OTP -> {
                getEntryInfo()?.let { entryInfo ->
                    entryInfo.getOtpToken()?.let {
                        currentInputConnection.commitText(String(it), 1)
                    }
                }
                actionGoAutomatically()
            }
            KEY_OTP_ALT -> {
                getEntryInfo()?.let { entryInfo ->
                    val otpToken = entryInfo.getOtpToken()?.copyOf()
                    if (otpToken != null && otpToken.isNotEmpty()) {
                        // Cut to fill each digit separatelyKeyEvent.KEYCODE_TAB
                        otpToken.forEachIndexed { index, char ->
                            currentInputConnection.commitText(char.toString(), 1)
                            if (index < (otpToken.size-1))
                                currentInputConnection.sendKeyEvent(
                                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB)
                                )
                        }
                    }
                    otpToken?.clear()
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
                getEntryInfo()?.getCustomFieldsForFilling()?.let { customFields ->
                    fieldsAdapter?.apply {
                        setFields(customFields)
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

    private fun actionKeyEntry(searchInfo: SearchInfo) {
        // Prevent auto entries filling to correctly get manual entry selection
        // when entries already populated
        preventAutoFill()
        SearchHelper.checkAutoSearchInfo(
            context = this,
            database = mDatabase,
            searchInfo = searchInfo,
            onItemsFound = { _, items ->
                // Force manual selection if items already retrieved
                if (entriesAlreadyRetrieved(items.map { it.id })) {
                    launchEntrySelection(
                        SearchInfo(searchInfo).apply { manualSelection = true }
                    )
                } else {
                    // Automatically populate keyboard
                    addEntries(
                        context = this,
                        entryList = items,
                        autoSwitchKeyboard = false,
                        from = TypeMode.MAGIKEYBOARD
                    )
                    assignKeyboardView()
                }
            },
            onItemNotFound = {
                // Select if not found
                launchEntrySelection(searchInfo)
            },
            onDatabaseClosed = {
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
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager?
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

    private fun setScreenshotMode() {
        // Several gingerbread devices have problems with FLAG_SECURE
        val isEnabled = PreferencesUtil.isScreenshotModeEnabled(this)
        AppUtil.setScreenshotMode(window?.window, isEnabled)
        screenshotModeView?.visibility = if (isEnabled) VISIBLE else GONE
    }

    override fun onDestroy() {
        lifecycleDispatcher.onServicePreSuperOnDestroy()
        dismissCustomKeys()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(onScreenshotModePrefListener)
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

        private val searchInfo = MutableLiveData<SearchInfo?>()
        private val shareBrowser = MutableLiveData<String?>()
        private val entryUUIDList = MutableLiveData<List<UUID>?>()
        private var onlyAllowedFromMagikeyboard: Boolean = false

        private const val SWITCH_KEYBOARD_ACTION = "com.android.keyboard.SWITCH_KEYBOARD"
        private const val KEYBOARD_ID = "KEYBOARD_ID"

        private fun removeEntryInfo() {
            this.entryUUIDList.value = null
        }

        fun removeEntry(context: Context) {
            removeEntryInfo()
            context.sendBroadcast(Intent(REMOVE_ENTRY_MAGIKEYBOARD_ACTION))
        }

        /**
         *  Add the search info to the magikeyboard service if not browser ot app blocked
         */
        fun addSearchInfo(context: Context, value: SearchInfo, from: TypeMode) {
            val newSearchInfo = value.withoutBrowserOrAppBlocked(context)
            // With Autofill sharing, keep the autofill search context
            if (context.isKeeAutofillActivated()
                && PreferencesUtil.isAutofillSharedToMagikeyboardEnable(context)) {
                when (from) {
                    TypeMode.AUTOFILL -> {
                        newSearchInfo?.let {
                            // Condition to manually select another entry
                            if (this.searchInfo.value != newSearchInfo) {
                                this.onlyAllowedFromMagikeyboard = false
                                this.searchInfo.value = newSearchInfo
                            }
                        }
                    }
                    TypeMode.MAGIKEYBOARD -> {
                        newSearchInfo?.let {
                            this.onlyAllowedFromMagikeyboard = false
                            this.searchInfo.value = newSearchInfo
                        }
                    }
                    else -> {}
                }
            } else {
                // Without context sharing, Magikeyboard manages itself and filter browsers
                this.onlyAllowedFromMagikeyboard = false
                this.searchInfo.value = newSearchInfo
                this.shareBrowser.value = if (isElementAllowed(
                        value.applicationId,
                        PreferencesUtil.applicationIdBlocklist(context)))
                    value.applicationId else null
            }
        }

        fun removeSearchInfo() {
            this.onlyAllowedFromMagikeyboard = false
            this.searchInfo.value = null
        }

        private fun preventAutoFill() {
            if (this.searchInfo.value != null && !entryUUIDList.value.isNullOrEmpty())
                this.onlyAllowedFromMagikeyboard = true
        }

        private fun entriesAlreadyRetrieved(entries: List<UUID>): Boolean {
            if (entryUUIDList.value == null || entries.isEmpty())
                return false
            return entryUUIDList.value?.equals(entries) == true
        }

        fun addEntry(
            context: Context,
            entry: EntryInfo,
            autoSwitchKeyboard: Boolean
        ) {
            addEntries(
                context = context,
                entryList = listOf(entry),
                autoSwitchKeyboard = autoSwitchKeyboard,
                from = TypeMode.MAGIKEYBOARD
            )
        }

        fun addEntries(
            context: Context,
            entryList: List<EntryInfo>,
            autoSwitchKeyboard: Boolean,
            from: TypeMode
        ) {
            if (!onlyAllowedFromMagikeyboard || from == TypeMode.MAGIKEYBOARD) {
                // Open OTP notification
                ClipboardEntryNotificationService.launchOtpNotificationIfAllowed(
                    context = context,
                    entries = entryList
                )
                // Add a new entry if keyboard activated
                if (context.isMagikeyboardActivated()) {
                    val newList = entryList.map { it.id }
                    if (entriesAlreadyRetrieved(newList).not()) {
                        this.entryUUIDList.value = newList
                        // Auto switch to the Magikeyboard
                        if (autoSwitchKeyboard
                            && isAutoSwitchMagikeyboardAllowed(context)
                            && currentDefaultKeyboard(context) != getMagikeyboardId(context)
                        ) {
                            context.startActivity(getSwitchMagikeyboardIntent(context))
                        }
                    }
                } else {
                    removeEntryInfo()
                }
            }
        }

        fun getMagikeyboardId(context: Context): String {
            return "${context.packageName}/${MagikeyboardService::class.java.canonicalName}"
        }

        fun getSwitchMagikeyboardIntent(context: Context): Intent {
            return Intent(SWITCH_KEYBOARD_ACTION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(KEYBOARD_ID, getMagikeyboardId(context))
            }
        }

        fun isAutoSwitchMagikeyboardAllowed(context: Context): Boolean {
            return getSwitchMagikeyboardIntent(context)
                .resolveActivity(context.packageManager) != null
        }

        fun currentDefaultKeyboard(context: Context): String {
            return Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
        }

        fun Context.isMagikeyboardActivated(): Boolean {
            return ContextCompat.getSystemService(
                this,
                InputMethodManager::class.java
            )?.enabledInputMethodList?.any { inputMethod ->
                inputMethod.packageName == this.packageName
            } ?: false
        }

        fun Context.showKeyboardDeviceSettings() {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}