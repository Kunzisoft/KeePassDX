package com.kunzisoft.keepass.credentialprovider.viewmodel

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.TypeMode
import com.kunzisoft.keepass.credentialprovider.passkey.data.AndroidPrivilegedApp
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialCreationParameters
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialUsageParameters
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.buildCreatePublicKeyCredentialResponse
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.buildPasskeyPublicKeyCredential
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.checkSecurity
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.getVerifiedGETClientDataResponse
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.removeAppOrigin
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.removePasskey
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.retrieveAppOrigin
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.retrieveNodeId
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.retrievePasskey
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.retrievePasskeyCreationRequestParameters
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.retrievePasskeyUsageRequestParameters
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.retrieveSearchInfo
import com.kunzisoft.keepass.credentialprovider.passkey.util.PrivilegedAllowLists
import com.kunzisoft.keepass.credentialprovider.passkey.util.PrivilegedAllowLists.saveCustomPrivilegedApps
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.model.AppOrigin
import com.kunzisoft.keepass.model.Passkey
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.model.SignatureNotFoundException
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InvalidObjectException
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PasskeyLauncherViewModel(application: Application): AndroidViewModel(application) {

    private var mUsageParameters: PublicKeyCredentialUsageParameters? = null
    private var mCreationParameters: PublicKeyCredentialCreationParameters? = null
    private var mPasskey: Passkey? = null

    private var mBackupEligibility: Boolean = true
    private var mBackupState: Boolean = false
    private var mLockDatabase: Boolean = true

    private var isResultLauncherRegistered: Boolean = false
    private var currentDatabase: ContextualDatabase? = null

    private val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = _uiState

    fun initialize() {
        mBackupEligibility = PreferencesUtil.isPasskeyBackupEligibilityEnable(getApplication())
        mBackupState = PreferencesUtil.isPasskeyBackupStateEnable(getApplication())
    }

    fun showAppPrivilegedDialog(
        database: ContextualDatabase,
        temptingApp: AndroidPrivilegedApp
    ) {
        _uiState.value = UIState.ShowAppPrivilegedDialog(database, temptingApp)
    }

    fun showAppSignatureDialog(
        database: ContextualDatabase,
        temptingApp: AppOrigin
    ) {
        _uiState.value = UIState.ShowAppSignatureDialog(database, temptingApp)
    }

    fun showError(error: Throwable) {
        _uiState.value = UIState.ShowError(error)
    }

    fun saveCustomPrivilegedApp(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase,
        temptingApp: AndroidPrivilegedApp
    ) {
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            cancelResult()
        }) {
            saveCustomPrivilegedApps(
                context = getApplication(),
                privilegedApps = listOf(temptingApp)
            )
            launchPasskeyAction(
                intent = intent,
                specialMode = specialMode,
                database = database
            )
        }
    }

    fun saveAppSignature(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase,
        temptingApp: AppOrigin
    ) {
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            cancelResult()
        }) {
            // TODO Save app signature
        }
    }

    fun setResult(intent: Intent) {
        currentDatabase = null
        _uiState.value = UIState.SetActivityResult(
            lockDatabase = mLockDatabase,
            resultCode = RESULT_OK,
            data = intent
        )
    }

    fun cancelResult() {
        currentDatabase = null
        _uiState.value = UIState.SetActivityResult(
            lockDatabase = mLockDatabase,
            resultCode = RESULT_CANCELED
        )
    }

    fun onDatabaseRetrieved(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    ) {
        currentDatabase = database
        if (isResultLauncherRegistered.not()) {
            viewModelScope.launch(CoroutineExceptionHandler { _, e ->
                if (e is PrivilegedAllowLists.PrivilegedException && database != null) {
                    showAppPrivilegedDialog(database, e.temptingApp)
                } else {
                    showError(e)
                    cancelResult()
                }
            }) {
                launchPasskeyAction(intent, specialMode, database)
            }
        }
    }

    /**
     * Launch the main action to manage Passkey
     */
    private suspend fun launchPasskeyAction(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    ) {
        isResultLauncherRegistered = true
        val searchInfo = intent.retrieveSearchInfo() ?: SearchInfo()
        val appOrigin = intent.retrieveAppOrigin() ?: AppOrigin(verified = false)
        val nodeId = intent.retrieveNodeId()
        checkSecurity(intent, nodeId)
        when (specialMode) {
            SpecialMode.SELECTION -> {
                launchSelection(
                    intent = intent,
                    database = database,
                    nodeId = nodeId,
                    searchInfo = searchInfo,
                    appOrigin = appOrigin
                )
            }
            SpecialMode.REGISTRATION -> {
                // TODO Registration in predefined group
                // launchRegistration(database, nodeId, mSearchInfo)
                launchRegistration(
                    intent = intent,
                    database = database,
                    nodeId = null,
                    searchInfo = searchInfo
                )
            }
            else -> {
                throw InvalidObjectException("Passkey launch mode not supported")
            }
        }
    }

    // -------------
    //  Selection
    // -------------

    private suspend fun launchSelection(
        intent: Intent,
        database: ContextualDatabase?,
        nodeId: UUID?,
        searchInfo: SearchInfo,
        appOrigin: AppOrigin
    ) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Launch passkey selection")
            retrievePasskeyUsageRequestParameters(
                intent = intent,
                context = getApplication()
            ) { usageParameters ->
                // Save the requested parameters
                mUsageParameters = usageParameters
                // Manage the passkey to use
                nodeId?.let { nodeId ->
                    autoSelectPasskeyAndSetResult(database, nodeId, appOrigin)
                } ?: run {
                    SearchHelper.checkAutoSearchInfo(
                        context = getApplication(),
                        database = database,
                        searchInfo = searchInfo,
                        onItemsFound = { _, _ ->
                            Log.w(
                                TAG, "Passkey found for auto selection, should not append," +
                                        "use PasskeyProviderService instead"
                            )
                            cancelResult()
                        },
                        onItemNotFound = { openedDatabase ->
                            Log.d(
                                TAG, "No Passkey found for selection," +
                                        "launch manual selection in opened database"
                            )
                            _uiState.value = UIState.LaunchGroupActivityForSelection(
                                database = openedDatabase
                            )
                        },
                        onDatabaseClosed = {
                            Log.d(TAG, "Manual passkey selection in closed database")
                            _uiState.value =
                                UIState.LaunchFileDatabaseSelectActivityForSelection(
                                    searchInfo = searchInfo
                                )
                        }
                    )
                }
            }
        }
    }

    private fun autoSelectPasskeyAndSetResult(
        database: ContextualDatabase?,
        nodeId: UUID,
        appOrigin: AppOrigin
    ) {
        mUsageParameters?.let { usageParameters ->
            // To get the passkey from the database
            val passkey = database
                ?.getEntryById(NodeIdUUID(nodeId))
                ?.getEntryInfo(database)
                ?.passkey
                ?: throw GetCredentialUnknownException(
                    "No passkey with nodeId $nodeId found"
                )
            // Build the response
            val result = Intent()
            try {
                PendingIntentHandler.setGetCredentialResponse(
                    result,
                    GetCredentialResponse(
                        buildPasskeyPublicKeyCredential(
                            requestOptions = usageParameters.publicKeyCredentialRequestOptions,
                            clientDataResponse = getVerifiedGETClientDataResponse(
                                usageParameters = usageParameters,
                                appOrigin = appOrigin
                            ),
                            passkey = passkey,
                            backupEligibility = mBackupEligibility,
                            backupState = mBackupState
                        )
                    )
                )
                setResult(result)
            } catch (e: SignatureNotFoundException) {
                // Request the dialog if signature exception
                showAppSignatureDialog(database, e.temptingApp)
            }
        } ?: throw IOException("Usage parameters is null")
    }

    fun manageSelectionResult(
        activityResult: ActivityResult
    ) {
        val intent = activityResult.data
        // Build a new formatted response from the selection response
        val responseIntent = Intent()
        when (activityResult.resultCode) {
            RESULT_OK -> {
                try {
                    Log.d(TAG, "Passkey selection result")
                    if (intent == null)
                        throw IOException("Intent is null")
                    val passkey = intent.retrievePasskey()
                        ?: throw IOException("Passkey is null")
                    val appOrigin = intent.retrieveAppOrigin()
                        ?: throw IOException("App origin is null")
                    intent.removePasskey()
                    intent.removeAppOrigin()
                    mUsageParameters?.let { usageParameters ->
                        // Check verified origin
                        PendingIntentHandler.setGetCredentialResponse(
                            responseIntent,
                            GetCredentialResponse(
                                buildPasskeyPublicKeyCredential(
                                    requestOptions = usageParameters.publicKeyCredentialRequestOptions,
                                    clientDataResponse = getVerifiedGETClientDataResponse(
                                        usageParameters = usageParameters,
                                        appOrigin = appOrigin
                                    ),
                                    passkey = passkey,
                                    backupEligibility = mBackupEligibility,
                                    backupState = mBackupState
                                )
                            )
                        )
                    } ?: run {
                        throw IOException("Usage parameters is null")
                    }
                    setResult(responseIntent)
                } catch (e: SignatureNotFoundException) {
                    currentDatabase?.let {
                        showAppSignatureDialog(it, e.temptingApp)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to create selection response for passkey", e)
                    showError(e)
                    cancelResult()
                }
            }
            RESULT_CANCELED -> {
                cancelResult()
            }
        }
        // Remove the launcher register
        isResultLauncherRegistered = false
    }

    // -------------
    //  Registation
    // -------------

    private suspend fun launchRegistration(
        intent: Intent,
        database: ContextualDatabase?,
        nodeId: UUID?,
        searchInfo: SearchInfo
    ) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Launch passkey registration")
            retrievePasskeyCreationRequestParameters(
                intent = intent,
                context = getApplication(),
                passkeyCreated = { passkey, appInfoToStore, publicKeyCredentialParameters ->
                    // Save the requested parameters
                    mPasskey = passkey
                    mCreationParameters = publicKeyCredentialParameters
                    // Manage the passkey and create a register info
                    val registerInfo = RegisterInfo(
                        searchInfo = searchInfo,
                        passkey = passkey,
                        appOrigin = appInfoToStore
                    )
                    // If nodeId already provided
                    nodeId?.let { nodeId ->
                        autoRegisterPasskeyAndSetResult(database, nodeId, passkey)
                    } ?: run {
                        SearchHelper.checkAutoSearchInfo(
                            context = getApplication(),
                            database = database,
                            searchInfo = searchInfo,
                            onItemsFound = { openedDatabase, _ ->
                                Log.w(
                                    TAG, "Passkey found for registration, " +
                                            "but launch manual registration for a new entry"
                                )
                                _uiState.value = UIState.LaunchGroupActivityForRegistration(
                                    database = openedDatabase,
                                    registerInfo = registerInfo,
                                    typeMode = TypeMode.PASSKEY
                                )
                            },
                            onItemNotFound = { openedDatabase ->
                                Log.d(TAG, "Launch new manual registration in opened database")
                                _uiState.value = UIState.LaunchGroupActivityForRegistration(
                                    database = openedDatabase,
                                    registerInfo = registerInfo,
                                    typeMode = TypeMode.PASSKEY
                                )
                            },
                            onDatabaseClosed = {
                                Log.d(TAG, "Manual passkey registration in closed database")
                                _uiState.value =
                                    UIState.LaunchFileDatabaseSelectActivityForRegistration(
                                        registerInfo = registerInfo,
                                        typeMode = TypeMode.PASSKEY
                                    )
                            }
                        )
                    }
                }
            )
        }
    }

    private fun autoRegisterPasskeyAndSetResult(
        database: ContextualDatabase?,
        nodeId: UUID,
        passkey: Passkey
    ) {
        // TODO Overwrite and Register in a predefined group
        mCreationParameters?.let { creationParameters ->
            // To set the passkey to the database
            setResult(Intent())
        } ?: run {
            Log.e(TAG, "Unable to auto select passkey, usage parameters are empty")
            cancelResult()
        }
    }

    fun manageRegistrationResult(activityResult: ActivityResult) {
        val intent = activityResult.data
        // Build a new formatted response from the creation response
        val responseIntent = Intent()
        try {
            Log.d(TAG, "Passkey registration result")
            val passkey = intent?.retrievePasskey()
            intent?.removePasskey()
            intent?.removeAppOrigin()
            // If registered passkey is the same as the one we want to validate,
            if (mPasskey == passkey) {
                mCreationParameters?.let {
                    PendingIntentHandler.setCreateCredentialResponse(
                        intent = responseIntent,
                        response = buildCreatePublicKeyCredentialResponse(
                            publicKeyCredentialCreationParameters = it,
                            backupEligibility = mBackupEligibility,
                            backupState = mBackupState
                        )
                    )
                }
            } else {
                throw SecurityException("Passkey was modified before registration")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to create registration response for passkey", e)
            _uiState.value = UIState.ShowError(e)
        }
        _uiState.value = UIState.SetActivityResult(
            lockDatabase = mLockDatabase,
            resultCode = activityResult.resultCode,
            data = responseIntent
        )
    }

    sealed class UIState {
        object Loading : UIState()
        data class ShowAppPrivilegedDialog(
            val database: ContextualDatabase,
            val temptingApp: AndroidPrivilegedApp
        ): UIState()
        data class ShowAppSignatureDialog(
            val database: ContextualDatabase,
            val temptingApp: AppOrigin
        ): UIState()
        data class LaunchGroupActivityForSelection(
            val database: ContextualDatabase
        ): UIState()
        data class LaunchGroupActivityForRegistration(
            val database: ContextualDatabase,
            val registerInfo: RegisterInfo,
            val typeMode: TypeMode
        ): UIState()
        data class LaunchFileDatabaseSelectActivityForSelection(
            val searchInfo: SearchInfo
        ): UIState()
        data class LaunchFileDatabaseSelectActivityForRegistration(
            val registerInfo: RegisterInfo,
            val typeMode: TypeMode
        ): UIState()
        data class SetActivityResult(
            val lockDatabase: Boolean,
            val resultCode: Int,
            val data: Intent? = null
        ): UIState()
        data class ShowError(
            val error: Throwable
        ): UIState()
    }

    companion object {
        private val TAG = PasskeyLauncherViewModel::class.java.name
    }
}