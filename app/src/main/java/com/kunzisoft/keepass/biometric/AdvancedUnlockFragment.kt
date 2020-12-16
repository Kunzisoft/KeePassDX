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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.getkeepsafe.taptargetview.TapTargetView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.app.database.CipherDatabaseAction
import com.kunzisoft.keepass.database.exception.IODatabaseException
import com.kunzisoft.keepass.education.PasswordActivityEducation
import com.kunzisoft.keepass.notifications.AdvancedUnlockNotificationService
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.AdvancedUnlockInfoView

class AdvancedUnlockFragment: StylishFragment(), AdvancedUnlockManager.AdvancedUnlockCallback {

    private var mBuilderListener: BuilderListener? = null

    private var mAdvancedUnlockEnabled = false
    private var mAutoOpenPromptEnabled = false

    private var advancedUnlockManager: AdvancedUnlockManager? = null
    private var biometricMode: Mode = Mode.BIOMETRIC_UNAVAILABLE
    private var mAdvancedUnlockInfoView: AdvancedUnlockInfoView? = null

    var databaseFileUri: Uri? = null
        private set

    /**
     * Manage setting to auto open biometric prompt
     */
    private var mAutoOpenPrompt: Boolean = false
        get() {
            return field && mAutoOpenPromptEnabled
        }

    // Variable to check if the prompt can be open (if the right activity is currently shown)
    // checkBiometricAvailability() allows open biometric prompt and onDestroy() removes the authorization
    private var allowOpenBiometricPrompt = false

    private lateinit var cipherDatabaseAction : CipherDatabaseAction

    private var cipherDatabaseListener: CipherDatabaseAction.DatabaseListener? = null

    // Only to fix multiple fingerprint menu #332
    private var mAllowAdvancedUnlockMenu = false
    private var mAddBiometricMenuInProgress = false

    // Only keep connection when we request a device credential activity
    private var keepConnection = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mAdvancedUnlockEnabled = PreferencesUtil.isAdvancedUnlockEnable(context)
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

        retainInstance = true
        setHasOptionsMenu(true)

        cipherDatabaseAction = CipherDatabaseAction.getInstance(requireContext().applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val rootView = inflater.cloneInContext(contextThemed)
                .inflate(R.layout.fragment_advanced_unlock, container, false)

        mAdvancedUnlockInfoView = rootView.findViewById(R.id.advanced_unlock_view)

        return rootView
    }

    private data class ActivityResult(var requestCode: Int, var resultCode: Int, var data: Intent?)
    private var activityResult: ActivityResult? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // To wait resume
        activityResult = ActivityResult(requestCode, resultCode, data)
        keepConnection = false

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        mAdvancedUnlockEnabled = PreferencesUtil.isAdvancedUnlockEnable(requireContext())
        mAutoOpenPromptEnabled = PreferencesUtil.isAdvancedUnlockPromptAutoOpenEnable(requireContext())
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

    fun loadDatabase(databaseUri: Uri?, autoOpenPrompt: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // To get device credential unlock result, only if same database uri
            if (databaseUri != null
                    && mAdvancedUnlockEnabled) {
                activityResult?.let {
                    if (databaseUri == databaseFileUri) {
                        advancedUnlockManager?.onActivityResult(it.requestCode, it.resultCode)
                    } else {
                        disconnect()
                    }
                } ?: run {
                    connect(databaseUri)
                    this.mAutoOpenPrompt = autoOpenPrompt
                }
            } else {
                disconnect()
            }
            activityResult = null
        }
    }

    /**
     * Check unlock availability and change the current mode depending of device's state
     */
    fun checkUnlockAvailability() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            allowOpenBiometricPrompt = true
            if (PreferencesUtil.isBiometricUnlockEnable(requireContext())) {
                mAdvancedUnlockInfoView?.setIconResource(R.drawable.fingerprint)

                // biometric not supported (by API level or hardware) so keep option hidden
                // or manually disable
                val biometricCanAuthenticate = AdvancedUnlockManager.canAuthenticate(requireContext())
                if (!PreferencesUtil.isAdvancedUnlockEnable(requireContext())
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
            } else if (PreferencesUtil.isDeviceCredentialUnlockEnable(requireContext())) {
                mAdvancedUnlockInfoView?.setIconResource(R.drawable.bolt)
                if (AdvancedUnlockManager.isDeviceSecure(requireContext())) {
                    selectMode()
                } else {
                    toggleMode(Mode.DEVICE_CREDENTIAL_OR_BIOMETRIC_NOT_CONFIGURED)
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
                } ?: throw IODatabaseException()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun toggleMode(newBiometricMode: Mode) {
        if (newBiometricMode != biometricMode) {
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
            requireContext().startActivity(Intent(Settings.ACTION_SETTINGS))
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

        mAdvancedUnlockInfoView?.setIconViewClickListener(false) {
            onAuthenticationError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                    requireContext().getString(R.string.credential_before_click_advanced_unlock_button))
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun openAdvancedUnlockPrompt(cryptoPrompt: AdvancedUnlockCryptoPrompt) {
        requireActivity().runOnUiThread {
            if (allowOpenBiometricPrompt) {
                if (cryptoPrompt.isDeviceCredentialOperation)
                    keepConnection = true
                try {
                    advancedUnlockManager?.openAdvancedUnlockPrompt(cryptoPrompt)
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
        } ?: throw Exception("AdvancedUnlockHelper not initialized")
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
        } ?: throw Exception("AdvancedUnlockHelper not initialized")
    }

    @Synchronized
    fun initAdvancedUnlockMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAllowAdvancedUnlockMenu = false
            when (biometricMode) {
                Mode.BIOMETRIC_UNAVAILABLE -> initNotAvailable()
                Mode.BIOMETRIC_SECURITY_UPDATE_REQUIRED -> initSecurityUpdateRequired()
                Mode.DEVICE_CREDENTIAL_OR_BIOMETRIC_NOT_CONFIGURED -> initNotConfigured()
                Mode.KEY_MANAGER_UNAVAILABLE -> initKeyManagerNotAvailable()
                Mode.WAIT_CREDENTIAL -> initWaitData()
                Mode.STORE_CREDENTIAL -> initEncryptData()
                Mode.EXTRACT_CREDENTIAL -> initDecryptData()
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
                    requireActivity().invalidateOptionsMenu()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun connect(databaseUri: Uri) {
        showViews(true)
        this.databaseFileUri = databaseUri
        cipherDatabaseListener = object: CipherDatabaseAction.DatabaseListener {
            override fun onDatabaseCleared() {
                deleteEncryptedDatabaseKey()
            }
        }
        cipherDatabaseAction.apply {
            reloadPreferences()
            cipherDatabaseListener?.let {
                registerDatabaseListener(it)
            }
        }
        checkUnlockAvailability()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun disconnect(hideViews: Boolean = true,
                   closePrompt: Boolean = true) {
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
    fun deleteEncryptedDatabaseKey() {
        allowOpenBiometricPrompt = false
        mAdvancedUnlockInfoView?.setIconViewClickListener(false, null)
        advancedUnlockManager?.closeBiometricPrompt()
        databaseFileUri?.let { databaseUri ->
            cipherDatabaseAction.deleteByDatabaseUri(databaseUri) {
                checkUnlockAvailability()
            }
        }
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        requireActivity().runOnUiThread {
            Log.e(TAG, "Biometric authentication error. Code : $errorCode Error : $errString")
            setAdvancedUnlockedMessageView(errString.toString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onAuthenticationFailed() {
        requireActivity().runOnUiThread {
            Log.e(TAG, "Biometric authentication failed, biometric not recognized")
            setAdvancedUnlockedMessageView(R.string.advanced_unlock_not_recognized)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onAuthenticationSucceeded() {
        requireActivity().runOnUiThread {
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
                    AdvancedUnlockNotificationService.startServiceForTimeout(requireContext())
                }
                Mode.EXTRACT_CREDENTIAL -> {
                    // retrieve the encrypted value from preferences
                    databaseFileUri?.let { databaseUri ->
                        cipherDatabaseAction.getCipherDatabase(databaseUri) { cipherDatabase ->
                            cipherDatabase?.encryptedValue?.let { value ->
                                advancedUnlockManager?.decryptData(value)
                            } ?: deleteEncryptedDatabaseKey()
                        }
                    } ?: throw IODatabaseException()
                }
            }
        }
    }

    override fun handleEncryptedResult(encryptedValue: String, ivSpec: String) {
        databaseFileUri?.let { databaseUri ->
            mBuilderListener?.onCredentialEncrypted(databaseUri, encryptedValue, ivSpec)
        }
    }

    override fun handleDecryptedResult(decryptedValue: String) {
        // Load database directly with password retrieve
        databaseFileUri?.let {
            mBuilderListener?.onCredentialDecrypted(it, decryptedValue)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onInvalidKeyException(e: Exception) {
        setAdvancedUnlockedMessageView(R.string.advanced_unlock_invalid_key)
    }

    override fun onGenericException(e: Exception) {
        val errorMessage = e.cause?.localizedMessage ?: e.localizedMessage ?: ""
        setAdvancedUnlockedMessageView(errorMessage)
    }

    private fun showViews(show: Boolean) {
        requireActivity().runOnUiThread {
            mAdvancedUnlockInfoView?.visibility = if (show)
                View.VISIBLE
            else {
                View.GONE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setAdvancedUnlockedTitleView(textId: Int) {
        requireActivity().runOnUiThread {
            mAdvancedUnlockInfoView?.setTitle(textId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setAdvancedUnlockedMessageView(textId: Int) {
        requireActivity().runOnUiThread {
            mAdvancedUnlockInfoView?.setMessage(textId)
        }
    }

    private fun setAdvancedUnlockedMessageView(text: CharSequence) {
        requireActivity().runOnUiThread {
            mAdvancedUnlockInfoView?.message = text
        }
    }

    fun performEducation(passwordActivityEducation: PasswordActivityEducation,
                         readOnlyEducationPerformed: Boolean,
                         onEducationViewClick: ((TapTargetView?) -> Unit)? = null,
                         onOuterViewClick: ((TapTargetView?) -> Unit)? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !readOnlyEducationPerformed) {
            val biometricCanAuthenticate = AdvancedUnlockManager.canAuthenticate(requireContext())
            PreferencesUtil.isAdvancedUnlockEnable(requireContext())
                    && (biometricCanAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
                    || biometricCanAuthenticate == BiometricManager.BIOMETRIC_SUCCESS)
                    && mAdvancedUnlockInfoView != null && mAdvancedUnlockInfoView?.visibility == View.VISIBLE
                    && mAdvancedUnlockInfoView?.unlockIconImageView != null
                    && passwordActivityEducation.checkAndPerformedBiometricEducation(mAdvancedUnlockInfoView!!.unlockIconImageView!!,
                    onEducationViewClick,
                    onOuterViewClick)
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
        fun retrieveCredentialForEncryption(): String
        fun conditionToStoreCredential(): Boolean
        fun onCredentialEncrypted(databaseUri: Uri, encryptedCredential: String, ivSpec: String)
        fun onCredentialDecrypted(databaseUri: Uri, decryptedCredential: String)
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