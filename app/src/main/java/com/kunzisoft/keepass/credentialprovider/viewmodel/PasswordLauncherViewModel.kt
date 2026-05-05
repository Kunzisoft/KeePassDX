package com.kunzisoft.keepass.credentialprovider.viewmodel

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.removeInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.removeNodeId
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveNodeId
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveSearchInfo
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.TypeMode
import com.kunzisoft.keepass.credentialprovider.passkey.util.PassHelper.checkSecurity
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasswordHelper.removePasswordInfo
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasswordHelper.retrievePasswordCreationRequestParameters
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasswordHelper.retrievePasswordInfo
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasswordHelper.retrievePasswordUsageRequestParameters
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.model.AppOrigin
import com.kunzisoft.keepass.model.PasswordInfo
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.settings.PreferencesUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InvalidObjectException
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PasswordLauncherViewModel(application: Application): CredentialLauncherViewModel(application) {

    // To check registration
    private var mPasswordInfo: PasswordInfo? = null

    // To check usage
    private var mAppOrigin: AppOrigin? = null

    private var mLockDatabaseAfterSelection: Boolean = false

    private val mUiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = mUiState.asStateFlow()

    fun initialize() {
        mLockDatabaseAfterSelection = PreferencesUtil.isPasskeyCloseDatabaseEnable(getApplication())
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
        val nodeId = intent.retrieveNodeId()
        intent.removeInfo()
        intent.removeNodeId()
        checkSecurity(intent, nodeId)
        when (specialMode) {
            SpecialMode.SELECTION -> {
                launchSelection(
                    intent = intent,
                    database = database,
                    nodeId = nodeId,
                    searchInfo = searchInfo
                )
            }
            SpecialMode.REGISTRATION -> {
                launchRegistration(
                    intent = intent,
                    database = database,
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
        searchInfo: SearchInfo
    ) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Launch password selection")
            retrievePasswordUsageRequestParameters(
                intent = intent,
                context = getApplication()
            ) { appOrigin ->
                mAppOrigin = appOrigin
                nodeId?.let { nodeId ->
                    selectPasswordAndSetResult(database, nodeId)
                } ?: run {
                    SearchHelper.checkAutoSearchInfo(
                        context = getApplication(),
                        database = database,
                        searchInfo = searchInfo,
                        onItemsFound = { _, _ ->
                            Log.w(
                                TAG, "Password found for auto selection, should not append," +
                                        "use PasswordProviderService instead"
                            )
                            cancelResult()
                        },
                        onItemNotFound = { openedDatabase ->
                            Log.d(
                                TAG, "No password found for selection," +
                                        "launch manual selection in opened database"
                            )
                            mCredentialUiState.value =
                                CredentialState.LaunchGroupActivityForSelection(
                                    database = openedDatabase,
                                    searchInfo = searchInfo,
                                    typeMode = TypeMode.PASSWORD
                                )
                        },
                        onDatabaseClosed = {
                            Log.d(TAG, "Manual password selection in closed database")
                            mCredentialUiState.value =
                                CredentialState.LaunchFileDatabaseSelectActivityForSelection(
                                    searchInfo = searchInfo,
                                    typeMode = TypeMode.PASSWORD
                                )
                        }
                    )
                }
            }
        }
    }

    private suspend fun selectPasswordAndSetResult(
        passwordInfo: PasswordInfo
    ) {
        withContext(Dispatchers.IO) {
            // Check AppOrigin
            val origin = passwordInfo.appOrigin
            if (origin == null || origin.isTheSameOriginThan(mAppOrigin).not())
                throw SecurityException("Password origin do not match")
            else {
                val passwordCredential = PasswordCredential(
                    id = passwordInfo.username,
                    password = String(passwordInfo.password)
                )
                // Build the response
                val result = Intent()
                PendingIntentHandler.setGetCredentialResponse(
                    result,
                    GetCredentialResponse(passwordCredential)
                )
                withContext(Dispatchers.Main) {
                    setResult(result, lockDatabase = mLockDatabaseAfterSelection)
                }
            }
        }
    }

    private suspend fun selectPasswordAndSetResult(
        database: ContextualDatabase?,
        nodeId: UUID
    ) {
        // To get the password from the database
        val entry = database
            ?.getEntryById(NodeIdUUID(nodeId))
            ?.getEntryInfo(database)
            ?: throw IOException(
                "No entry with nodeId $nodeId found"
            )
        selectPasswordAndSetResult(
            passwordInfo = PasswordInfo(
                username = entry.username,
                password = entry.password,
                appOrigin = entry.appOrigin
            )
        )
    }

    override fun manageSelectionResult(
        database: ContextualDatabase,
        activityResult: ActivityResult
    ) {
        super.manageSelectionResult(database, activityResult)
        val intent = activityResult.data
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Unable to create selection response for password", e)
            showError(e)
        }) {
            // Build a new formatted response from the selection response
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    withContext(Dispatchers.IO) {
                        Log.d(TAG, "Password selection result")
                        if (intent == null)
                            throw IOException("Intent is null")
                        val passwordInfo = intent.retrievePasswordInfo()
                            ?: throw IOException("Password info is null")
                        intent.removePasswordInfo()
                        selectPasswordAndSetResult(passwordInfo)
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
        searchInfo: SearchInfo
    ) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Launch password registration")
            retrievePasswordCreationRequestParameters(
                intent = intent,
                context = getApplication()
            ) { passwordInfo ->
                    // Save the requested parameters
                    mPasswordInfo = passwordInfo
                    // Manage the password info and create a register info
                    val registerInfo = RegisterInfo(
                        searchInfo = searchInfo,
                        username = passwordInfo.username,
                        password = passwordInfo.password,
                        appOrigin = passwordInfo.appOrigin
                    )
                SearchHelper.checkAutoSearchInfo(
                    context = getApplication(),
                    database = database,
                    searchInfo = searchInfo,
                    onItemsFound = { openedDatabase, _ ->
                        Log.w(
                            TAG, "Password found for registration, " +
                                    "but launch manual registration for a new entry"
                        )
                        mCredentialUiState.value =
                            CredentialState.LaunchGroupActivityForRegistration(
                                database = openedDatabase,
                                registerInfo = registerInfo,
                                typeMode = TypeMode.PASSWORD
                            )
                    },
                    onItemNotFound = { openedDatabase ->
                        Log.d(TAG, "Launch new manual registration in opened database")
                        mCredentialUiState.value =
                            CredentialState.LaunchGroupActivityForRegistration(
                                database = openedDatabase,
                                registerInfo = registerInfo,
                                typeMode = TypeMode.PASSWORD
                            )
                    },
                    onDatabaseClosed = {
                        Log.d(TAG, "Manual password registration in closed database")
                        mCredentialUiState.value =
                            CredentialState.LaunchFileDatabaseSelectActivityForRegistration(
                                registerInfo = registerInfo,
                                typeMode = TypeMode.PASSWORD
                            )
                    }
                )
            }
        }
    }

    override fun manageRegistrationResult(activityResult: ActivityResult) {
        val intent = activityResult.data
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Unable to create registration response for password", e)
            showError(e)
        }) {
            // Build a new formatted response from the creation response
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    withContext(Dispatchers.IO) {
                        Log.d(TAG, "Password registration result")
                        val passwordInfo = intent?.retrievePasswordInfo()
                        intent?.removePasswordInfo()
                        val responseIntent = Intent()
                        // If registered password info is the same as the one we want to validate,
                        if (mPasswordInfo == passwordInfo) {
                            PendingIntentHandler.setCreateCredentialResponse(
                                intent = responseIntent,
                                response = CreatePasswordResponse()
                            )
                        } else {
                            throw SecurityException("Password was modified before registration")
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
        data class UpdateEntry(
            val oldEntry: Entry,
            val newEntry: Entry
        ): UIState()
    }

    companion object {
        private val TAG = PasswordLauncherViewModel::class.java.name
    }
}