package com.kunzisoft.keepass.credentialprovider.viewmodel

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.removeNodesIds
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveNodesIds
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveRegisterInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveSearchInfo
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveSpecialMode
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.TypeMode
import com.kunzisoft.keepass.credentialprovider.autofill.AutofillComponent
import com.kunzisoft.keepass.credentialprovider.autofill.AutofillHelper
import com.kunzisoft.keepass.credentialprovider.autofill.AutofillHelper.retrieveAutofillComponent
import com.kunzisoft.keepass.credentialprovider.autofill.KeeAutofillService
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.model.RegisterInfo
import com.kunzisoft.keepass.model.SearchInfo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@RequiresApi(api = Build.VERSION_CODES.O)
class AutofillLauncherViewModel(application: Application): CredentialLauncherViewModel(application) {

    private var mAutofillComponent: AutofillComponent? = null
    private var mSelectionResult: ActivityResult? = null

    private val mUiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = mUiState

    override fun onResult() {
        super.onResult()
        mAutofillComponent = null
        mSelectionResult = null
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)
        if (database != null) {
            mSelectionResult?.let { selectionResult ->
                manageSelectionResult(database, selectionResult)
            }
        }
    }

    override suspend fun launchAction(
        intent: Intent,
        specialMode: SpecialMode,
        database: ContextualDatabase?
    ) {
        // Retrieve selection mode
        when (intent.retrieveSpecialMode()) {
            SpecialMode.SELECTION -> {
                val searchInfo = intent.retrieveSearchInfo()
                if (searchInfo == null)
                    throw IOException("Search info is null")
                mAutofillComponent = intent.retrieveAutofillComponent()
                // Build search param
                launchSelection(database, mAutofillComponent, searchInfo)
            }
            SpecialMode.REGISTRATION -> {
                // To register info
                val registerInfo = intent.retrieveRegisterInfo()
                if (registerInfo == null)
                    throw IOException("Register info is null")
                launchRegistration(database, registerInfo)
            }
            else -> {
                // Not an autofill call
                cancelResult()
            }
        }
    }

    private suspend fun launchSelection(
        database: ContextualDatabase?,
        autofillComponent: AutofillComponent?,
        searchInfo: SearchInfo
    ) {
        withContext(Dispatchers.IO) {
            if (autofillComponent == null) {
                throw IOException("Autofill component is null")
            }
            if (KeeAutofillService.autofillAllowedFor(
                    applicationId = searchInfo.applicationId,
                    webDomain = searchInfo.webDomain,
                    context = getApplication()
                )
            ) {
                // If database is open
                SearchHelper.checkAutoSearchInfo(
                    context = getApplication(),
                    database = database,
                    searchInfo = searchInfo,
                    onItemsFound = { openedDatabase, items ->
                        // Items found
                        if (autofillComponent.compatInlineSuggestionsRequest != null) {
                            mUiState.value = UIState.ShowAutofillSuggestionMessage
                        }
                        AutofillHelper.buildResponse(
                            context = getApplication(),
                            autofillComponent = autofillComponent,
                            database = openedDatabase,
                            entriesInfo = items
                        ) { intent ->
                            setResult(intent)
                        }
                    },
                    onItemNotFound = { openedDatabase ->
                        // Show the database UI to select the entry
                        mCredentialUiState.value =
                            CredentialLauncherViewModel.UIState.LaunchGroupActivityForSelection(
                                database = openedDatabase,
                                searchInfo = searchInfo,
                                typeMode = TypeMode.AUTOFILL
                            )
                    },
                    onDatabaseClosed = {
                        // If database not open
                        mCredentialUiState.value =
                            CredentialLauncherViewModel.UIState.LaunchFileDatabaseSelectActivityForSelection(
                                searchInfo = searchInfo,
                                typeMode = TypeMode.AUTOFILL
                            )
                    }
                )
            } else {
                mUiState.value = UIState.ShowBlockRestartMessage
            }
        }
    }

    override fun manageSelectionResult(activityResult: ActivityResult) {
        // Waiting for the database if needed
        when (activityResult.resultCode) {
            RESULT_OK -> {
                mSelectionResult = activityResult
                mDatabase?.let { database ->
                    manageSelectionResult(database, activityResult)
                }
            }
            RESULT_CANCELED -> {
                cancelResult()
            }
        }
    }

    private fun manageSelectionResult(
        database: ContextualDatabase,
        activityResult: ActivityResult
    ) {
        mSelectionResult = null
        val intent = activityResult.data
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Unable to create selection response for autofill", e)
            showError(e)
        }) {
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    withContext(Dispatchers.IO) {
                        Log.d(TAG, "Autofill selection result")
                        if (intent == null)
                            throw IOException("Intent is null")
                        val nodesIds = intent.retrieveNodesIds()
                            ?: throw IOException("NodesIds is null")
                        intent.removeNodesIds()
                        val autofillComponent = mAutofillComponent
                        if (autofillComponent == null)
                            throw IOException("Autofill component is null")
                        val entries = nodesIds.mapNotNull { nodeId ->
                            database
                                .getEntryById(NodeIdUUID(nodeId))
                                ?.getEntryInfo(database)
                        }
                        withContext(Dispatchers.Main) {
                            AutofillHelper.buildResponse(
                                context = getApplication(),
                                autofillComponent = autofillComponent,
                                database = database,
                                entriesInfo = entries
                            ) { intent ->
                                setResult(intent)
                            }
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

    private fun launchRegistration(
        database: ContextualDatabase?,
        registerInfo: RegisterInfo
    ) {
        val searchInfo = registerInfo.searchInfo
        if (KeeAutofillService.autofillAllowedFor(
                applicationId = searchInfo.applicationId,
                webDomain = searchInfo.webDomain,
                context = getApplication()
            )) {
            val readOnly = database?.isReadOnly != false
            SearchHelper.checkAutoSearchInfo(
                context = getApplication(),
                database = database,
                searchInfo = searchInfo,
                onItemsFound = { openedDatabase, _ ->
                    if (!readOnly) {
                        // Show the database UI to select the entry
                        mCredentialUiState.value =
                            CredentialLauncherViewModel.UIState.LaunchGroupActivityForRegistration(
                                database = openedDatabase,
                                registerInfo = registerInfo,
                                typeMode = TypeMode.AUTOFILL
                            )
                    } else {
                        mUiState.value = UIState.ShowReadOnlyMessage
                    }
                },
                onItemNotFound = { openedDatabase ->
                    if (!readOnly) {
                        // Show the database UI to select the entry
                        mCredentialUiState.value =
                            CredentialLauncherViewModel.UIState.LaunchGroupActivityForRegistration(
                                database = openedDatabase,
                                registerInfo = registerInfo,
                                typeMode = TypeMode.AUTOFILL
                            )
                    } else {
                        mUiState.value = UIState.ShowReadOnlyMessage
                    }
                },
                onDatabaseClosed = {
                    // If database not open
                    mCredentialUiState.value =
                        CredentialLauncherViewModel.UIState.LaunchFileDatabaseSelectActivityForRegistration(
                            registerInfo = registerInfo,
                            typeMode = TypeMode.AUTOFILL
                        )
                }
            )
        } else {
            mUiState.value = UIState.ShowBlockRestartMessage
        }
    }

    override fun manageRegistrationResult(activityResult: ActivityResult) {
        isResultLauncherRegistered = false
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Unable to create registration response for autofill", e)
            showError(e)
        }) {
            val responseIntent = Intent()
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    Log.d(TAG, "Autofill registration result")
                    withContext(Dispatchers.Main) {
                        setResult(responseIntent)
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
        object Loading: UIState()
        object ShowBlockRestartMessage: UIState()
        object ShowReadOnlyMessage: UIState()
        object ShowAutofillSuggestionMessage: UIState()
    }

    companion object {
        private val TAG = AutofillLauncherViewModel::class.java.name
    }
}