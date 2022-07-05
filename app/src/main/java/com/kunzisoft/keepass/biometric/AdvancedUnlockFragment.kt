/*
 * Copyright 2020 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.biometric

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.getkeepsafe.taptargetview.TapTargetView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.database.exception.IODatabaseException
import com.kunzisoft.keepass.education.PasswordActivityEducation
import com.kunzisoft.keepass.model.CipherDecryptDatabase
import com.kunzisoft.keepass.model.CipherEncryptDatabase
import com.kunzisoft.keepass.model.CredentialStorage
import com.kunzisoft.keepass.services.NfcService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.settings.SettingsAdvancedUnlockActivity
import com.kunzisoft.keepass.view.AdvancedUnlockInfoView
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.viewmodels.AdvancedUnlockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdvancedUnlockFragment: StylishFragment(), AdvancedUnlockManager.AdvancedUnlockCallback {

    private var mBuilderListener: BuilderListener? = null

    private var mAdvancedUnlockEnabled = false
    private var mAutoOpenPromptEnabled = false

    private var advancedUnlockManager: AdvancedUnlockManager? = null
    private var biometricMode: Mode = Mode.BIOMETRIC_UNAVAILABLE
    private var mAdvancedUnlockInfoView: AdvancedUnlockInfoView? = null

    var databaseFileUri: Uri? = null
        private set

    // TODO Retrieve credential storage from app database
    var credentialDatabaseStorage: CredentialStorage = CredentialStorage.DEFAULT

    /**
     * Manage setting to auto open biometric prompt
     */
    private var mAutoOpenPrompt: Boolean
        get() {
            return mAdvancedUnlockViewModel.allowAutoOpenBiometricPrompt && mAutoOpenPromptEnabled
        }
        set(value) {
            mAdvancedUnlockViewModel.allowAutoOpenBiometricPrompt = value
        }

    // Variable to check if the prompt can be open (if the right activity is currently shown)
    // checkBiometricAvailability() allows open biometric prompt and onDestroy() removes the authorization
    private var allowOpenBiometricPrompt = false

    private lateinit var cipherDatabaseAction : CipherDatabaseAction

    private var cipherDatabaseListener: CipherDatabaseAction.CipherDatabaseListener? = null

    private val mAdvancedUnlockViewModel: AdvancedUnlockViewModel by activityViewModels()

    // Only to fix multiple fingerprint menu #332
    private var mAllowAdvancedUnlockMenu = false
    private var mAddBiometricMenuInProgress = false

    // Only keep connection when we request a device credential activity
    private var keepConnection = false

    private var mDeviceCredentialResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        mAdvancedUnlockViewModel.allowAutoOpenBiometricPrompt = false
        // To wait resume
        if (keepConnection) {
            mAdvancedUnlockViewModel.deviceCredentialAuthSucceeded = result.resultCode == Activity.RESULT_OK
        }
        keepConnection = false
    }

    class Nfc(private val that: AdvancedUnlockFragment) {
        private var nfcService: NfcService? = null

        fun startIfEnabled() {
            try {
                if (!PreferencesUtil.isUnlockNfcEnable(that.requireContext())) return
                if (nfcService == null) nfcService = NfcService()
                nfcService?.enableDispatch(that.activity, that.activity?.javaClass)
                if (true != nfcService?.isEnabled) Toast.makeText(that.context, R.string.nfc_not_enabled, Toast.LENGTH_SHORT).show()
                else processPendingTag() // after onResume (onTap NFC tag...)
            } catch (e: Throwable) {
                Log.d(TAG, "NFC error: Start", e)
            }
        }

        fun stop() {
            try {
                nfcService?.disableDispatch(that.activity)
            } catch (e: Throwable) {
                Log.d(TAG, "NFC error: Stop", e)
            }
        }

        fun checkAndProcessTag(intent: Intent? = null): Boolean {
            // testSimulateTapNfcTagOnNewIntent = true; return true

            return nfcService?.readTag(intent) { nfcTag, nfcTagData, errMsg ->
                if (null != errMsg)
                    Toast.makeText(that.context, errMsg, Toast.LENGTH_SHORT).show()
                else if (null != that.databaseFileUri) {
                    // onTap NFC tag explained: onTap NFC tag the Activity/Fragment receive onPause and 'disconnect' is called
                    // Here, after 'disconnect', databaseFileUri is null. Therefore:
                    // 1. processTagAfterResume; 2. optional: NFC stop moved to Activity onStop (NFC start dispatch must be enabled AFTER onResume)
                    // Another solution could be - move onPause code and call it after delay:
                    //fun onPause() {
                    //    that.lifecycleScope.launch {
                    //        kotlinx.coroutines.delay(250)
                    //        that.lifecycleScope.launch(Dispatchers.Main) { ...move onPause code here... }
                    //    }
                    //}

                    onTapNfcTag((nfcTag?.tagId ?: "") + (nfcTagData ?: ""))
                    nfcService?.nfcTag?.close()
                    nfcService?.nfcTag = null
                }
            } ?: false
        }

        private fun processPendingTag() { // after onResume (onTap NFC tag...)
            // if (testSimulateTapNfcTagOnNewIntent) { testSimulateTapNfcTagOnNewIntent = false; onTapNfcTag("test", true); return }

            nfcService?.nfcTag?.let { onTapNfcTag((it.tagId ?: "") + (it.data ?: "")) }
            nfcService?.nfcTag?.close()
            nfcService?.nfcTag = null
        }

        private fun onTapNfcTag(unlockData: String) {
            fun askOverwrite(onOK: () -> Unit) {
                androidx.appcompat.app.AlertDialog.Builder(that.requireContext())
                    .setMessage(R.string.nfc_record_overwrite)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(that.getString(android.R.string.ok)) { _, _ ->
                        onOK()
                    }.setNegativeButton(that.getString(android.R.string.cancel)) { dialog, _ ->
                        dialog.cancel()
                    }.create().show()
            }

            fun extractCredential() { // Like onAuthenticationSucceeded() for Mode.EXTRACT_CREDENTIAL
                // todo-op? not like onAuthenticationSucceeded - onAuthenticationSucceeded is in the main thread
                that.getDatabaseKey(DatabaseKeyId.Nfc.value)?.let { databaseKey ->
                    that.cipherDatabaseAction.getCipherDatabase(databaseKey) { cipherDatabase ->
                        cipherDatabase?.encryptedValue?.let { value ->
                            that.advancedUnlockManager?.initDecryptData(cipherDatabase.specParameters) {} // Like initDecryptData()
                            that.advancedUnlockManager?.decryptData(value, unlockData, DatabaseKeyId.Nfc.value)
                        } ?: that.deleteEncryptedDatabaseKey(DatabaseKeyId.Nfc.value)
                    }
                } ?: that.onAuthenticationError(-1, that.getString(R.string.error_database_uri_null))
            }

            fun storeCredential() { // Like onAuthenticationSucceeded() for Mode.STORE_CREDENTIAL
                // todo-op? not like onAuthenticationSucceeded - onAuthenticationSucceeded is in the main thread
                that.mBuilderListener?.retrieveCredentialForEncryption()?.let { credential ->
                    that.advancedUnlockManager?.initEncryptData {} // Like initEncryptData()
                    that.advancedUnlockManager?.encryptData(credential, unlockData, DatabaseKeyId.Nfc.value)
                }
            }

            if (unlockData.isBlank()) return
            that.getDatabaseKey(DatabaseKeyId.Nfc.value)?.let { databaseKey ->
                if ((that.biometricMode == Mode.EXTRACT_CREDENTIAL || that.biometricMode == Mode.WAIT_CREDENTIAL)
                    && true != that.mBuilderListener?.conditionToStoreCredential())
                    that.cipherDatabaseAction.containsCipherDatabase(databaseKey) { containsCipher ->
                        if (containsCipher) extractCredential()
                    }
                else if ((that.biometricMode == Mode.STORE_CREDENTIAL || that.biometricMode == Mode.WAIT_CREDENTIAL)
                    && true == that.mBuilderListener?.conditionToStoreCredential())
                    that.cipherDatabaseAction.containsCipherDatabase(databaseKey) { containsCipher ->
                        if (!containsCipher) storeCredential()
                        else askOverwrite { storeCredential() }
                    }
            }
        }

        // private var testSimulateTapNfcTagOnNewIntent = false

        fun checkAndSetupDebug(tapNfcTagEnabled: Boolean = true) { // debug in emulator: simulate tap NFC tag
            if (!PreferencesUtil.Nfc.isDebug || !PreferencesUtil.isUnlockNfcEnable(that.requireContext())) return
            if (tapNfcTagEnabled) that.mAdvancedUnlockInfoView?.setOnClickListener {
                PreferencesUtil.Nfc.debugAskNfcTagData(that.requireContext(), null) { nfcTagData ->
                    //if (testSimulateTapNfcTagOnNewIntent) that.activity?.let { activity ->
                    //    fun stringHexToBytes(src: String?): ByteArray? = if (src.isNullOrEmpty()) null else
                    //        ByteArray(src.length / 2) { (src.substring(it * 2 + 1, it * 2 + 2)).toInt(16).toByte() }
                    //    activity.startActivity(Intent(activity, activity::class.java)
                    //        .putExtra(android.nfc.NfcAdapter.EXTRA_ID, stringHexToBytes("044466DA3F7080"))
                    //        //.also { it.extras?.putParcelable(android.nfc.NfcAdapter.EXTRA_TAG, Tag(...)) } //? TAG: Tech [android.nfc.tech.IsoDep, android.nfc.tech.NfcA, android.nfc.tech.NdefFormatable]
                    //        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
                    //    return@debugAskNfcTagData
                    //}

                    onTapNfcTag(nfcTagData)
                }
            } else that.mAdvancedUnlockInfoView?.setOnClickListener(null)
        }
    }
    val nfc = Nfc(this)

    enum class DatabaseKeyId(val value: Int?) { None(null), Nfc(1) }

    // NFC unlock data - stored with different key, not overriding Biometric/DeviceCredential data
    private fun getDatabaseKey(databaseKeyId: Int?): Uri? =
        if (databaseKeyId == DatabaseKeyId.Nfc.value)
            databaseFileUri?.let { Uri.parse("$it#nfc") }
        else databaseFileUri

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mAdvancedUnlockEnabled = PreferencesUtil.isUnlockNfcEnable(requireContext())
                || PreferencesUtil.isAdvancedUnlockEnable(context)
        mAutoOpenPromptEnabled = PreferencesUtil.isAdvancedUnlockPromptAutoOpenEnable(context)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mBuilderListener = context as BuilderListener
            }
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString()
                    + " must implement " + BuilderListener::class.java.name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        cipherDatabaseAction = CipherDatabaseAction.getInstance(requireContext().applicationContext)

        mAdvancedUnlockViewModel.onInitAdvancedUnlockModeRequested.observe(this) {
            initAdvancedUnlockMode()
        }

        mAdvancedUnlockViewModel.onUnlockAvailabilityCheckRequested.observe(this) {
            checkUnlockAvailability()
        }

        mAdvancedUnlockViewModel.onDatabaseFileLoaded.observe(this) {
            onDatabaseLoaded(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val rootView = inflater.cloneInContext(contextThemed)
                .inflate(R.layout.fragment_advanced_unlock, container, false)

        mAdvancedUnlockInfoView = rootView.findViewById(R.id.advanced_unlock_view)

        return rootView
    }

    override fun onResume() {
        super.onResume()
        context?.let {
            mAdvancedUnlockEnabled = PreferencesUtil.isUnlockNfcEnable(requireContext())
                    || PreferencesUtil.isAdvancedUnlockEnable(it)
            mAutoOpenPromptEnabled = PreferencesUtil.isAdvancedUnlockPromptAutoOpenEnable(it)
        }
        keepConnection = false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // biometric menu
            if (mAllowAdvancedUnlockMenu)
                inflater.inflate(R.menu.advanced_unlock, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_keystore_remove_key -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                deleteEncryptedDatabaseKey()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun onDatabaseLoaded(databaseUri: Uri?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // To get device credential unlock result, only if same database uri
            if (databaseUri != null
                    && mAdvancedUnlockEnabled) {
                val deviceCredentialAuthSucceeded = mAdvancedUnlockViewModel.deviceCredentialAuthSucceeded
                deviceCredentialAuthSucceeded?.let {
                    if (databaseUri == databaseFileUri) {
                        if (deviceCredentialAuthSucceeded == true) {
                            advancedUnlockManager?.advancedUnlockCallback?.onAuthenticationSucceeded()
                        } else {
                            advancedUnlockManager?.advancedUnlockCallback?.onAuthenticationFailed()
                        }
                    } else {
                        disconnect()
                    }
                } ?: run {
                    if (databaseUri != databaseFileUri) {
                        connect(databaseUri)
                    }
                }
            } else {
                disconnect()
            }
            mAdvancedUnlockViewModel.deviceCredentialAuthSucceeded = null
        }
    }

    /**
     * Check unlock availability and change the current mode depending of device's state
     */
    private fun checkUnlockAvailability() {
        context?.let { context ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                allowOpenBiometricPrompt = true
                if (PreferencesUtil.isBiometricUnlockEnable(context)) {
                    mAdvancedUnlockInfoView?.setIconBackgroundTint()
                    mAdvancedUnlockInfoView?.setIconResource(R.drawable.fingerprint)

                    // biometric not supported (by API level or hardware) so keep option hidden
                    // or manually disable
                    val biometricCanAuthenticate = AdvancedUnlockManager.canAuthenticate(context)
                    if (!PreferencesUtil.isAdvancedUnlockEnable(context)
                            || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
                            || biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                        toggleMode(Mode.BIOMETRIC_UNAVAILABLE)
                    } else if (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED) {
                        toggleMode(Mode.BIOMETRIC_SECURITY_UPDATE_REQUIRED)
                    } else {
                        // biometric is available but not configured, show icon but in disabled state with some information
                        if (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                            toggleMode(Mode.DEVICE_CREDENTIAL_OR_BIOMETRIC_NOT_CONFIGURED)
                        } else {
                            selectMode()
                        }
                    }
                } else if (PreferencesUtil.isDeviceCredentialUnlockEnable(context)) {
                    mAdvancedUnlockInfoView?.setIconBackgroundTint()
                    mAdvancedUnlockInfoView?.setIconResource(R.drawable.bolt)
                    if (AdvancedUnlockManager.isDeviceSecure(context)) {
                        selectMode()
                    } else {
                        toggleMode(Mode.DEVICE_CREDENTIAL_OR_BIOMETRIC_NOT_CONFIGURED)
                    }
                } else { // Only NFC unlock
                    mAdvancedUnlockInfoView?.setIconBackgroundTint(resources.getColor(R.color.green_light, null))
                    mAdvancedUnlockInfoView?.setIconResource(R.drawable.ic_app_white_24dp)
                    if (advancedUnlockManager?.isKeyManagerInitialized != true) { // Like selectMode()
                        advancedUnlockManager = AdvancedUnlockManager { requireActivity() }
                        advancedUnlockManager?.advancedUnlockCallback = this // callback for fingerprint findings
                    }
                    toggleMode(Mode.WAIT_CREDENTIAL, true)
                    // Force setAdvancedUnlockedTitleView() - password/keyfile switch will not change the Mode
                    // Force invalidateBiometricMenu() - delete NFC data will not change the Mode
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun selectMode() {
        // Check if fingerprint well init (be called the first time the fingerprint is configured
        // and the activity still active)
        if (advancedUnlockManager?.isKeyManagerInitialized != true) {
            advancedUnlockManager = AdvancedUnlockManager { requireActivity() }
            // callback for fingerprint findings
            advancedUnlockManager?.advancedUnlockCallback = this
        }
        // Recheck to change the mode
        if (advancedUnlockManager?.isKeyManagerInitialized != true) {
            toggleMode(Mode.KEY_MANAGER_UNAVAILABLE)
        } else {
            if (mBuilderListener?.conditionToStoreCredential() == true) {
                // listen for encryption
                toggleMode(Mode.STORE_CREDENTIAL)
            } else {
                databaseFileUri?.let { databaseUri ->
                    cipherDatabaseAction.containsCipherDatabase(databaseUri) { containsCipher ->
                        // biometric available but no stored password found yet for this DB so show info don't listen
                        toggleMode(if (containsCipher) {
                            // listen for decryption
                            Mode.EXTRACT_CREDENTIAL
                        } else {
                            // wait for typing
                            Mode.WAIT_CREDENTIAL
                        })
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun toggleMode(newBiometricMode: Mode, force: Boolean = false) {
        if (force || newBiometricMode != biometricMode) {
            biometricMode = newBiometricMode
            initAdvancedUnlockMode()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initNotAvailable() {
        showViews(false)

        mAdvancedUnlockInfoView?.setIconViewClickListener(false, null)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun openBiometricSetting() {
        mAdvancedUnlockInfoView?.setIconViewClickListener(false) {
            // ACTION_SECURITY_SETTINGS does not contain fingerprint enrollment on some devices...
            context?.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initSecurityUpdateRequired() {
        showViews(true)
        setAdvancedUnlockedTitleView(R.string.biometric_security_update_required)

        openBiometricSetting()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initNotConfigured() {
        showViews(true)
        setAdvancedUnlockedTitleView(R.string.configure_biometric)
        setAdvancedUnlockedMessageView("")

        openBiometricSetting()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initKeyManagerNotAvailable() {
        showViews(true)
        setAdvancedUnlockedTitleView(R.string.keystore_not_accessible)

        openBiometricSetting()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initWaitData() {
        showViews(true)
        setAdvancedUnlockedTitleView(R.string.no_credentials_stored)
        setAdvancedUnlockedMessageView("")

        context?.let { context ->
            mAdvancedUnlockInfoView?.setIconViewClickListener(false) {
                if (!PreferencesUtil.isBiometricUnlockEnable(context) &&
                    !PreferencesUtil.isDeviceCredentialUnlockEnable(context)) { // Only NFC unlock
                    context.startActivity(Intent(activity, SettingsAdvancedUnlockActivity::class.java))
                    return@setIconViewClickListener
                }

                onAuthenticationError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                        context.getString(R.string.credential_before_click_advanced_unlock_button))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun openAdvancedUnlockPrompt(cryptoPrompt: AdvancedUnlockCryptoPrompt) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (allowOpenBiometricPrompt) {
                if (cryptoPrompt.isDeviceCredentialOperation)
                    keepConnection = true
                try {
                    advancedUnlockManager?.openAdvancedUnlockPrompt(cryptoPrompt,
                        mDeviceCredentialResultLauncher)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to open advanced unlock prompt", e)
                    setAdvancedUnlockedTitleView(R.string.advanced_unlock_prompt_not_initialized)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initEncryptData() {
        showViews(true)
        setAdvancedUnlockedTitleView(R.string.open_advanced_unlock_prompt_store_credential)
        setAdvancedUnlockedMessageView("")

        advancedUnlockManager?.initEncryptData { cryptoPrompt ->
            // Set listener to open the biometric dialog and save credential
            mAdvancedUnlockInfoView?.setIconViewClickListener { _ ->
                openAdvancedUnlockPrompt(cryptoPrompt)
            }
        } ?: throw Exception("AdvancedUnlockManager not initialized")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initDecryptData() {
        showViews(true)
        setAdvancedUnlockedTitleView(R.string.open_advanced_unlock_prompt_unlock_database)
        setAdvancedUnlockedMessageView("")

        advancedUnlockManager?.let { unlockHelper ->
            databaseFileUri?.let { databaseUri ->
                cipherDatabaseAction.getCipherDatabase(databaseUri) { cipherDatabase ->
                    cipherDatabase?.let {
                        unlockHelper.initDecryptData(it.specParameters) { cryptoPrompt ->

                            // Set listener to open the biometric dialog and check credential
                            mAdvancedUnlockInfoView?.setIconViewClickListener { _ ->
                                openAdvancedUnlockPrompt(cryptoPrompt)
                            }

                            // Auto open the biometric prompt
                            if (mAutoOpenPrompt) {
                                mAutoOpenPrompt = false
                                openAdvancedUnlockPrompt(cryptoPrompt)
                            }
                        }
                    } ?: deleteEncryptedDatabaseKey()
                }
            } ?: throw IODatabaseException()
        } ?: throw Exception("AdvancedUnlockManager not initialized")
    }

    private fun initAdvancedUnlockMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAllowAdvancedUnlockMenu = false
            try {
                when (biometricMode) {
                    Mode.BIOMETRIC_UNAVAILABLE -> initNotAvailable()
                    Mode.BIOMETRIC_SECURITY_UPDATE_REQUIRED -> initSecurityUpdateRequired()
                    Mode.DEVICE_CREDENTIAL_OR_BIOMETRIC_NOT_CONFIGURED -> initNotConfigured()
                    Mode.KEY_MANAGER_UNAVAILABLE -> initKeyManagerNotAvailable()
                    Mode.WAIT_CREDENTIAL -> initWaitData()
                    Mode.STORE_CREDENTIAL -> initEncryptData()
                    Mode.EXTRACT_CREDENTIAL -> initDecryptData()
                }
            } catch (e: Exception) {
                onGenericException(e)
            }
            invalidateBiometricMenu()
        }
    }

    private fun invalidateBiometricMenu() {
        // Show fingerprint key deletion
        if (!mAddBiometricMenuInProgress) {
            mAddBiometricMenuInProgress = true
            databaseFileUri?.let { databaseUri ->
                cipherDatabaseAction.containsCipherDatabase(databaseUri) { containsCipher ->
                    mAllowAdvancedUnlockMenu = containsCipher
                            && (biometricMode != Mode.BIOMETRIC_UNAVAILABLE
                            && biometricMode != Mode.KEY_MANAGER_UNAVAILABLE)
                    mAddBiometricMenuInProgress = false
                    activity?.invalidateOptionsMenu()

                    //todo-op? 1) After mAddBiometricMenuInProgress = false; 2) Invalidate twice; Needs single call of containsCipherDatabase for all database keys
                    if (!mAllowAdvancedUnlockMenu)
                        DatabaseKeyId.values().filter { null != it.value }.forEach {
                            getDatabaseKey(it.value)?.let { databaseKey ->
                                cipherDatabaseAction.containsCipherDatabase(databaseKey) { containsCipher ->
                                    if (!mAllowAdvancedUnlockMenu && containsCipher) {
                                        mAllowAdvancedUnlockMenu = true
                                        activity?.invalidateOptionsMenu() // invalidate again if needed
                                    }
                                }
                            }
                        }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun connect(databaseUri: Uri) {
        showViews(true)
        this.databaseFileUri = databaseUri
        cipherDatabaseListener = object: CipherDatabaseAction.CipherDatabaseListener {
            override fun onCipherDatabaseCleared() {
                advancedUnlockManager?.closeBiometricPrompt()
                checkUnlockAvailability()
            }
        }
        cipherDatabaseAction.apply {
            reloadPreferences()
            cipherDatabaseListener?.let {
                registerDatabaseListener(it)
            }
        }
        checkUnlockAvailability()

        nfc.startIfEnabled() // NFC start dispatch must be enabled AFTER onResume
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun disconnect(hideViews: Boolean = true,
                   closePrompt: Boolean = true) {
        //nfc.stop() // moved to Activity.onStop (onTap NFC tag...)

        this.databaseFileUri = null
        // Close the biometric prompt
        allowOpenBiometricPrompt = false
        if (closePrompt)
            advancedUnlockManager?.closeBiometricPrompt()
        cipherDatabaseListener?.let {
            cipherDatabaseAction.unregisterDatabaseListener(it)
        }
        biometricMode = Mode.BIOMETRIC_UNAVAILABLE
        if (hideViews) {
            showViews(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun deleteEncryptedDatabaseKey(databaseKeyId: Int? = null) {
        mAllowAdvancedUnlockMenu = false
        advancedUnlockManager?.closeBiometricPrompt()

        //todo-op? Multiple checkUnlockAvailability; Needs single call of deleteByDatabaseUri for all database keys
        var checkNow = true
        DatabaseKeyId.values().filter { null == databaseKeyId || databaseKeyId == it.value }.forEach {
            getDatabaseKey(it.value)?.let { databaseKey ->
                checkNow = false
                cipherDatabaseAction.deleteByDatabaseUri(databaseKey) {
                    checkUnlockAvailability()
                }
            }
        }
        if (checkNow) checkUnlockAvailability()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        lifecycleScope.launch(Dispatchers.Main) {
            Log.e(TAG, "Biometric authentication error. Code : $errorCode Error : $errString")
            setAdvancedUnlockedMessageView(errString.toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onAuthenticationFailed() {
        lifecycleScope.launch(Dispatchers.Main) {
            Log.e(TAG, "Biometric authentication failed, biometric not recognized")
            setAdvancedUnlockedMessageView(R.string.advanced_unlock_not_recognized)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onAuthenticationSucceeded() {
        lifecycleScope.launch(Dispatchers.Main) {
            when (biometricMode) {
                Mode.BIOMETRIC_UNAVAILABLE -> {
                }
                Mode.BIOMETRIC_SECURITY_UPDATE_REQUIRED -> {
                }
                Mode.DEVICE_CREDENTIAL_OR_BIOMETRIC_NOT_CONFIGURED -> {
                }
                Mode.KEY_MANAGER_UNAVAILABLE -> {
                }
                Mode.WAIT_CREDENTIAL -> {
                }
                Mode.STORE_CREDENTIAL -> {
                    // newly store the entered password in encrypted way
                    mBuilderListener?.retrieveCredentialForEncryption()?.let { credential ->
                        advancedUnlockManager?.encryptData(credential)
                    }
                }
                Mode.EXTRACT_CREDENTIAL -> {
                    // retrieve the encrypted value from preferences
                    databaseFileUri?.let { databaseUri ->
                        cipherDatabaseAction.getCipherDatabase(databaseUri) { cipherDatabase ->
                            cipherDatabase?.encryptedValue?.let { value ->
                                advancedUnlockManager?.decryptData(value)
                            } ?: deleteEncryptedDatabaseKey()
                        }
                    } ?: run {
                        onAuthenticationError(-1, getString(R.string.error_database_uri_null))
                    }
                }
            }
        }
    }

    override fun handleEncryptedResult(encryptedValue: ByteArray, ivSpec: ByteArray, databaseKeyId: Int?) {
        getDatabaseKey(databaseKeyId)?.let { databaseKey ->
            mBuilderListener?.onCredentialEncrypted(
                CipherEncryptDatabase().apply {
                    this.databaseUri = databaseKey
                    this.credentialStorage = credentialDatabaseStorage
                    this.encryptedValue = encryptedValue
                    this.specParameters = ivSpec
                }
            )
        }
    }

    override fun handleDecryptedResult(decryptedValue: ByteArray, databaseKeyId: Int?) {
        // Load database directly with password retrieve
        getDatabaseKey(databaseKeyId)?.let { databaseKey ->
            mBuilderListener?.onCredentialDecrypted(
                CipherDecryptDatabase().apply {
                    this.databaseUri = databaseKey
                    this.credentialStorage = credentialDatabaseStorage
                    this.decryptedValue = decryptedValue
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onUnrecoverableKeyException(e: Exception) {
        setAdvancedUnlockedMessageView(R.string.advanced_unlock_invalid_key)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onInvalidKeyException(e: Exception) {
        setAdvancedUnlockedMessageView(R.string.advanced_unlock_invalid_key)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onGenericException(e: Exception) {
        val errorMessage = e.cause?.localizedMessage ?: e.localizedMessage ?: ""
        setAdvancedUnlockedMessageView(errorMessage)
    }

    private fun showViews(show: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (show) {
                if (mAdvancedUnlockInfoView?.visibility != View.VISIBLE)
                    mAdvancedUnlockInfoView?.showByFading()
            }
            else {
                if (mAdvancedUnlockInfoView?.visibility == View.VISIBLE)
                    mAdvancedUnlockInfoView?.hideByFading()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setAdvancedUnlockedTitleView(textId: Int) {
        val msgTapNfcTag = getString(R.string.nfc_unlock_hint) //todo-op! Better message if Mode.WAIT_CREDENTIAL
        if (PreferencesUtil.isUnlockNfcEnable(requireContext())
            && (biometricMode == Mode.EXTRACT_CREDENTIAL || biometricMode == Mode.WAIT_CREDENTIAL)
            && true != mBuilderListener?.conditionToStoreCredential())
            getDatabaseKey(DatabaseKeyId.Nfc.value)?.let { databaseKey ->
                mAdvancedUnlockInfoView?.setTitle(textId) // default
                cipherDatabaseAction.containsCipherDatabase(databaseKey) { containsCipher ->
                    if (containsCipher) mAdvancedUnlockInfoView?.title = mAdvancedUnlockInfoView?.title.toString() + msgTapNfcTag
                    nfc.checkAndSetupDebug(containsCipher)
                }
                return
            }

        lifecycleScope.launch(Dispatchers.Main) {
            mAdvancedUnlockInfoView?.setTitle(textId)

            if (PreferencesUtil.isUnlockNfcEnable(requireContext())
                && (biometricMode == Mode.STORE_CREDENTIAL || biometricMode == Mode.WAIT_CREDENTIAL)
                && true == mBuilderListener?.conditionToStoreCredential()) {
                mAdvancedUnlockInfoView?.title = mAdvancedUnlockInfoView?.title.toString() + msgTapNfcTag
                nfc.checkAndSetupDebug()
            } else nfc.checkAndSetupDebug(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setAdvancedUnlockedMessageView(textId: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            mAdvancedUnlockInfoView?.setMessage(textId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setAdvancedUnlockedMessageView(text: CharSequence) {
        lifecycleScope.launch(Dispatchers.Main) {
            mAdvancedUnlockInfoView?.message = text
        }
    }

    enum class Mode {
        BIOMETRIC_UNAVAILABLE,
        BIOMETRIC_SECURITY_UPDATE_REQUIRED,
        DEVICE_CREDENTIAL_OR_BIOMETRIC_NOT_CONFIGURED,
        KEY_MANAGER_UNAVAILABLE,
        WAIT_CREDENTIAL,
        STORE_CREDENTIAL,
        EXTRACT_CREDENTIAL
    }

    interface BuilderListener {
        fun retrieveCredentialForEncryption(): ByteArray
        fun conditionToStoreCredential(): Boolean
        fun onCredentialEncrypted(cipherEncryptDatabase: CipherEncryptDatabase)
        fun onCredentialDecrypted(cipherDecryptDatabase: CipherDecryptDatabase)
    }

    override fun onPause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!keepConnection) {
                // If close prompt, bug "user not authenticated in Android R"
                disconnect(false)
                advancedUnlockManager = null
            }
        }

        super.onPause()
    }

    override fun onDestroyView() {
        mAdvancedUnlockInfoView = null

        super.onDestroyView()
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            disconnect()
            advancedUnlockManager = null
            mBuilderListener = null
        }

        super.onDestroy()
    }

    override fun onDetach() {
        mBuilderListener = null

        super.onDetach()
    }

    companion object {

        private val TAG = AdvancedUnlockFragment::class.java.name
    }
}