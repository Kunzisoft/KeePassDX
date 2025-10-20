package com.kunzisoft.keepass.credentialprovider.viewmodel

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveAndRemoveEntries
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveNodeId
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.retrieveSearchInfo
import com.kunzisoft.keepass.credentialprovider.SpecialMode
import com.kunzisoft.keepass.credentialprovider.TypeMode
import com.kunzisoft.keepass.credentialprovider.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.exception.RegisterInReadOnlyDatabaseException
import com.kunzisoft.keepass.database.helper.SearchHelper
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.utils.KeyboardUtil.isKeyboardActivatedInSettings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class EntrySelectionViewModel(application: Application): CredentialLauncherViewModel(application) {

    private var searchShareForMagikeyboard: Boolean = false
    private var mLockDatabaseAfterSelection: Boolean = false
    private val mUiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = mUiState

    fun initialize() {
        searchShareForMagikeyboard = getApplication<Application>().isKeyboardActivatedInSettings()
        mLockDatabaseAfterSelection = false // TODO Close database after selection
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
        val searchInfo: SearchInfo? = intent.retrieveSearchInfo()
        if (searchInfo != null) {
            launch(database, searchInfo)
        } else {
            // To manage share
            var sharedWebDomain: String? = null
            var otpString: String? = null

            when (intent.action) {
                Intent.ACTION_SEND -> {
                    if ("text/plain" == intent.type) {
                        // Retrieve web domain or OTP
                        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { extra ->
                            if (OtpEntryFields.isOTPUri(extra))
                                otpString = extra
                            else
                                sharedWebDomain = extra.toUri().host
                        }
                    }
                    launchSelection(database, sharedWebDomain, otpString)
                }
                Intent.ACTION_VIEW -> {
                    // Retrieve OTP
                    intent.dataString?.let { extra ->
                        if (OtpEntryFields.isOTPUri(extra))
                            otpString = extra
                    }
                    launchSelection(database, null, otpString)
                }
                else -> {
                    if (database != null && database.loaded) {
                        mUiState.value = UIState.LaunchGroupActivityForSearch(
                            database = database,
                            searchInfo = SearchInfo()
                        )
                    } else {
                        mUiState.value = UIState.LaunchFileDatabaseSelectForSearch(
                            searchInfo = SearchInfo()
                        )
                    }
                }
            }
        }
    }

    // -------------
    //  Selection
    // -------------

    private fun launchSelection(
        database: ContextualDatabase?,
        sharedWebDomain: String?,
        otpString: String?
    ) {
        // Build domain search param
        val searchInfo = SearchInfo().apply {
            this.webDomain = sharedWebDomain
            this.otpString = otpString
        }
        launch(database, searchInfo)
    }

    private fun launch(
        database: ContextualDatabase?,
        searchInfo: SearchInfo
    ) {
        // If database is open
        val readOnly = database?.isReadOnly != false
        SearchHelper.checkAutoSearchInfo(
            context = getApplication(),
            database = database,
            searchInfo = searchInfo,
            onItemsFound = { openedDatabase, items ->
                // Items found
                if (searchInfo.otpString != null) {
                    if (!readOnly) {
                        mCredentialUiState.value =
                            CredentialState.LaunchGroupActivityForRegistration(
                                database = openedDatabase,
                                registerInfo = searchInfo.toRegisterInfo(),
                                typeMode = TypeMode.DEFAULT
                            )
                    } else {
                        mCredentialUiState.value = CredentialState.ShowError(
                            RegisterInReadOnlyDatabaseException()
                        )
                    }
                } else if (searchShareForMagikeyboard) {
                    MagikeyboardService.performSelection(
                        items,
                        { entryInfo ->
                            populateKeyboard(entryInfo)
                        },
                        { autoSearch ->
                            mCredentialUiState.value = CredentialState.LaunchGroupActivityForSelection(
                                database = openedDatabase,
                                searchInfo = searchInfo,
                                typeMode = TypeMode.MAGIKEYBOARD
                            )
                        }
                    )
                } else {
                    mUiState.value = UIState.LaunchGroupActivityForSearch(
                        database = openedDatabase,
                        searchInfo = searchInfo
                    )
                }
            },
            onItemNotFound = { openedDatabase ->
                // Show the database UI to select the entry
                if (searchInfo.otpString != null) {
                    if (!readOnly) {
                        mCredentialUiState.value =
                            CredentialState.LaunchGroupActivityForRegistration(
                                database = openedDatabase,
                                registerInfo = searchInfo.toRegisterInfo(),
                                typeMode = TypeMode.DEFAULT
                            )
                    } else {
                        mCredentialUiState.value = CredentialState.ShowError(
                            RegisterInReadOnlyDatabaseException()
                        )
                    }
                } else if (searchShareForMagikeyboard) {
                    mCredentialUiState.value = CredentialState.LaunchGroupActivityForSelection(
                        database = openedDatabase,
                        searchInfo = searchInfo,
                        typeMode = TypeMode.MAGIKEYBOARD
                    )
                } else {
                    mUiState.value = UIState.LaunchGroupActivityForSearch(
                        database = openedDatabase,
                        searchInfo = searchInfo
                    )
                }
            },
            onDatabaseClosed = {
                // If database not open
                if (searchInfo.otpString != null) {
                    mCredentialUiState.value = CredentialState.LaunchFileDatabaseSelectActivityForRegistration(
                        registerInfo = searchInfo.toRegisterInfo(),
                        typeMode = TypeMode.DEFAULT
                    )
                } else if (searchShareForMagikeyboard) {
                    mCredentialUiState.value = CredentialState.LaunchFileDatabaseSelectActivityForSelection(
                        searchInfo = searchInfo,
                        typeMode = TypeMode.MAGIKEYBOARD
                    )
                } else {
                    mUiState.value = UIState.LaunchFileDatabaseSelectForSearch(
                        searchInfo = searchInfo
                    )
                }
            }
        )
    }

    private fun populateKeyboard(entryInfo: EntryInfo) {
        // Automatically populate keyboard
        mUiState.value = UIState.PopulateKeyboard(entryInfo)
        setResult(Intent(), lockDatabase = mLockDatabaseAfterSelection)
    }

    override fun manageSelectionResult(
        database: ContextualDatabase,
        activityResult: ActivityResult
    ) {
        super.manageSelectionResult(database, activityResult)
        val intent = activityResult.data
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Unable to create selection response for Magikeyboard", e)
            showError(e)
        }) {
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    withContext(Dispatchers.IO) {
                        Log.d(TAG, "Magikeyboard selection result")
                        if (intent == null)
                            throw IOException("Intent is null")
                        val entries = intent.retrieveAndRemoveEntries(database)
                        withContext(Dispatchers.Main) {
                            // Populate Magikeyboard with entry
                            entries.firstOrNull()?.let { entryInfo ->
                                populateKeyboard(entryInfo)
                            } // TODO Manage multiple entries in Magikeyboard
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

    override fun manageRegistrationResult(activityResult: ActivityResult) {
        super.manageRegistrationResult(activityResult)
        viewModelScope.launch(CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Unable to create selection response for Magikeyboard", e)
            showError(e)
        }) {
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    // Empty data result
                    // TODO Show Toast indicating value is saved
                    withContext(Dispatchers.Main) {
                        setResult(Intent(), lockDatabase = false)
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
        data class PopulateKeyboard(
            val entryInfo: EntryInfo
        ): UIState()
        data class LaunchFileDatabaseSelectForSearch(
            val searchInfo: SearchInfo
        ): UIState()
        data class LaunchGroupActivityForSearch(
            val database: ContextualDatabase,
            val searchInfo: SearchInfo
        ): UIState()
    }

    companion object {
        private val TAG = EntrySelectionViewModel::class.java.name
    }
}