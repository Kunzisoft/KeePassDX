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
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.removeInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.removeNodeId
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveNodeId
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveSearchInfo
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
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.retrievePasskey
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.retrievePasskeyCreationRequestParameters
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper.retrievePasskeyUsageRequestParameters
import com.kunzisoft.keepass.credentialprovider.passkey.util.PrivilegedAllowLists
import com.kunzisoft.keepass.credentialprovider.passkey.util.PrivilegedAllowLists.saveCustomPrivilegedApps
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.exception.RegisterInReadOnlyDatabaseException
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.model.AppOrigin
import com.kunzisoft.keepass.model.Passkey
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.model.SignatureNotFoundException
import com.kunzisoft.keepass.services.DatabaseTaskNotificationService.Companion.getNewEntry
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.tasks.ActionRunnable
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
class PasskeyLauncherViewModel(application: Application): CredentialLauncherViewModel(application) {

    private var mUsageParameters: PublicKeyCredentialUsageParameters? = null
    private var mCreationParameters: PublicKeyCredentialCreationParameters? = null
    private var mPasskey: Passkey? = null

    private var mLockDatabaseAfterSelection: Boolean = false
    private var mBackupEligibility: Boolean = true
    private var mBackupState: Boolean = false

    private val mUiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = mUiState

    fun initialize() {
        mLockDatabaseAfterSelection = PreferencesUtil.isPasskeyCloseDatabaseEnable(getApplication())
        mBackupEligibility = PreferencesUtil.isPasskeyBackupEligibilityEnable(getApplication())
        mBackupState = PreferencesUtil.isPasskeyBackupStateEnable(getApplication())
    }

    fun showAppPrivilegedDialog(
        temptingApp: AndroidPrivilegedApp
    ) {
        mUiState.value = UIState.ShowAppPrivilegedDialog(temptingApp)
    }

    fun showAppSignatureDialog(
        temptingApp: AppOrigin,
        nodeId: UUID
    ) {
        mUiState.value = UIState.ShowAppSignatureDialog(temptingApp, nodeId)
    }

    fun saveCustomPrivilegedApp(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?,
        temptingApp: AndroidPrivilegedApp
    ) {
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            showError(e)
        }) {
            saveCustomPrivilegedApps(
                context = getApplication(),
                privilegedApps = listOf(temptingApp)
            )
            launchAction(
                intent = intent,
                specialMode = specialMode,
                database = database
            )
        }
    }

    fun saveAppSignature(
        database: ContextualDatabase?,
        temptingApp: AppOrigin,
        nodeId: UUID
    ) {
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            showError(e)
        }) {
            // Update the entry with app signature
            val entry = database
                ?.getEntryById(NodeIdUUID(nodeId))
                ?: throw GetCredentialUnknownException(
                    "No passkey with nodeId $nodeId found"
                )
            if (database.isReadOnly)
                throw RegisterInReadOnlyDatabaseException()
            val newEntry = Entry(entry)
            val entryInfo = newEntry.getEntryInfo(
                database,
                raw = true,
                removeTemplateConfiguration = false
            )
            entryInfo.saveAppOrigin(database, temptingApp)
            newEntry.setEntryInfo(database, entryInfo)
            mUiState.value = UIState.UpdateEntry(
                oldEntry = entry,
                newEntry = newEntry
            )
        }
    }

    override fun onExceptionOccurred(e: Throwable) {
        if (e is PrivilegedAllowLists.PrivilegedException) {
            showAppPrivilegedDialog(e.temptingApp)
        } else {
            super.onExceptionOccurred(e)
        }
    }

    override fun launchActionIfNeeded(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    ) {
        // Launch with database when a nodeId is present
        if ((database != null && database.loaded) || intent.retrieveNodeId() == null) {
            super.launchActionIfNeeded(intent, specialMode, database)
        }
    }

    override suspend fun launchAction(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    ) {
        val searchInfo = intent.retrieveSearchInfo() ?: SearchInfo()
        val appOrigin = intent.retrieveAppOrigin() ?: AppOrigin(verified = false)
        val nodeId = intent.retrieveNodeId()
        intent.removeInfo()
        intent.removeAppOrigin()
        intent.removeNodeId()
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
                            mCredentialUiState.value =
                                CredentialState.LaunchGroupActivityForSelection(
                                    database = openedDatabase,
                                    searchInfo = searchInfo,
                                    typeMode = TypeMode.PASSKEY
                                )
                        },
                        onDatabaseClosed = {
                            Log.d(TAG, "Manual passkey selection in closed database")
                            mCredentialUiState.value =
                                CredentialState.LaunchFileDatabaseSelectActivityForSelection(
                                    searchInfo = searchInfo,
                                    typeMode = TypeMode.PASSKEY
                                )
                        }
                    )
                }
            }
        }
    }

    fun autoSelectPasskey(
        result: ActionRunnable.Result,
        database: ContextualDatabase
    ) {
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            showError(e)
        }) {
            if (result.isSuccess) {
                val entry = result.data?.getNewEntry(database)
                    ?: throw IOException("No passkey entry found")
                autoSelectPasskeyAndSetResult(
                    database = database,
                    nodeId = entry.nodeId.id,
                    appOrigin = entry.getAppOrigin()
                        ?: throw IOException("No App origin found")
                )
            } else throw result.exception
                ?: IOException("Unable to auto select passkey")
        }
    }

    private suspend fun autoSelectPasskeyAndSetResult(
        database: ContextualDatabase?,
        nodeId: UUID,
        appOrigin: AppOrigin
    ) {
        withContext(Dispatchers.IO) {
            mUsageParameters?.let { usageParameters ->
                // To get the passkey from the database
                val passkey = database
                    ?.getEntryById(NodeIdUUID(nodeId))
                    ?.getEntryInfo(database)
                    ?.passkey
                    ?: throw IOException(
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
                                defaultBackupEligibility = mBackupEligibility,
                                defaultBackupState = mBackupState
                            )
                        )
                    )
                    setResult(result, lockDatabase = mLockDatabaseAfterSelection)
                } catch (e: SignatureNotFoundException) {
                    // Request the dialog if signature exception
                    showAppSignatureDialog(e.temptingApp, nodeId)
                }
            } ?: throw IOException("Usage parameters is null")
        }
    }

    override fun manageSelectionResult(
        database: ContextualDatabase,
        activityResult: ActivityResult
    ) {
        super.manageSelectionResult(database, activityResult)
        val intent = activityResult.data
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Unable to create selection response for passkey", e)
            if (e is SignatureNotFoundException) {
                intent?.retrieveNodeId()?.let { nodeId ->
                    showAppSignatureDialog(e.temptingApp, nodeId)
                } ?: cancelResult()
            } else {
                showError(e)
            }
        }) {
            // Build a new formatted response from the selection response
            val responseIntent = Intent()
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    withContext(Dispatchers.IO) {
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
                                        defaultBackupEligibility = mBackupEligibility,
                                        defaultBackupState = mBackupState
                                    )
                                )
                            )
                        } ?: run {
                            throw IOException("Usage parameters is null")
                        }
                        withContext(Dispatchers.Main) {
                            setResult(responseIntent, lockDatabase = mLockDatabaseAfterSelection)
                        }
                    }
                }
                RESULT_CANCELED -> {
                    withContext(Dispatchers.Main) {
                        cancelResult()
                    }
                }
            }
        }
    }

    // -------------
    //  Registration
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
                defaultBackupEligibility = mBackupEligibility,
                defaultBackupState = mBackupState,
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
                                mCredentialUiState.value =
                                    CredentialState.LaunchGroupActivityForRegistration(
                                        database = openedDatabase,
                                        registerInfo = registerInfo,
                                        typeMode = TypeMode.PASSKEY
                                    )
                            },
                            onItemNotFound = { openedDatabase ->
                                Log.d(TAG, "Launch new manual registration in opened database")
                                mCredentialUiState.value =
                                    CredentialState.LaunchGroupActivityForRegistration(
                                        database = openedDatabase,
                                        registerInfo = registerInfo,
                                        typeMode = TypeMode.PASSKEY
                                    )
                            },
                            onDatabaseClosed = {
                                Log.d(TAG, "Manual passkey registration in closed database")
                                mCredentialUiState.value =
                                    CredentialState.LaunchFileDatabaseSelectActivityForRegistration(
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

    private suspend fun autoRegisterPasskeyAndSetResult(
        database: ContextualDatabase?,
        nodeId: UUID,
        passkey: Passkey
    ) {
        withContext(Dispatchers.IO) {
            mCreationParameters?.let { creationParameters ->
                // To set the passkey to the database
                // TODO Overwrite and Register in a predefined group
                withContext(Dispatchers.Main) {
                    setResult(Intent())
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Unable to auto select passkey, usage parameters are empty")
                    cancelResult()
                }
            }
        }
    }

    override fun manageRegistrationResult(activityResult: ActivityResult) {
        val intent = activityResult.data
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Unable to create registration response for passkey", e)
            if (e is SignatureNotFoundException) {
                intent?.retrieveNodeId()?.let { nodeId ->
                    showAppSignatureDialog(e.temptingApp, nodeId)
                } ?: cancelResult()
            } else {
                showError(e)
            }
        }) {
            // Build a new formatted response from the creation response
            val responseIntent = Intent()
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    withContext(Dispatchers.IO) {
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
                                        backupEligibility = passkey?.backupEligibility
                                            ?: mBackupEligibility,
                                        backupState = passkey?.backupState
                                            ?: mBackupState
                                    )
                                )
                            }
                        } else {
                            throw SecurityException("Passkey was modified before registration")
                        }
                        withContext(Dispatchers.Main) {
                            setResult(responseIntent)
                        }
                    }
                }
                RESULT_CANCELED -> {
                    withContext(Dispatchers.Main) {
                        cancelResult()
                    }
                }
            }
        }
    }

    sealed class UIState {
        object Loading : UIState()
        data class ShowAppPrivilegedDialog(
            val temptingApp: AndroidPrivilegedApp
        ): UIState()
        data class ShowAppSignatureDialog(
            val temptingApp: AppOrigin,
            val nodeId: UUID
        ): UIState()
        data class UpdateEntry(
            val oldEntry: Entry,
            val newEntry: Entry
        ): UIState()
    }

    companion object {
        private val TAG = PasskeyLauncherViewModel::class.java.name
    }
}