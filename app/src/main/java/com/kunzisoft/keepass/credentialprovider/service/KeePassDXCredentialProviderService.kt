package com.kunzisoft.keepass.credentialprovider.service

import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.credentialprovider.data.Passkey
import com.kunzisoft.keepass.credentialprovider.util.DatabaseHelper
import com.kunzisoft.keepass.credentialprovider.util.IntentHelper
import com.kunzisoft.keepass.credentialprovider.util.JsonHelper
import com.kunzisoft.keepass.database.DatabaseTaskProvider
import com.kunzisoft.keepass.database.element.Database
import java.time.Instant

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class KeePassDXCredentialProviderService : CredentialProviderService() {

    private var mDatabaseTaskProvider: DatabaseTaskProvider? = null
    private var mDatabase: Database? = null

    override fun onCreate() {
        super.onCreate()

        mDatabaseTaskProvider = DatabaseTaskProvider(this)
        mDatabaseTaskProvider?.registerProgressTask()
        mDatabaseTaskProvider?.onDatabaseRetrieved = { database ->
            this.mDatabase = database
        }
    }

    override fun onDestroy() {
        mDatabaseTaskProvider?.unregisterProgressTask()
        super.onDestroy()
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        Log.d(javaClass.simpleName, "onBeginCreateCredentialRequest called")
        val response: BeginCreateCredentialResponse? = processCreateCredentialRequest(request)
        if (response != null) {
            callback.onResult(response)
        } else {
            callback.onError(CreateCredentialUnknownException())
        }
    }

    private fun processCreateCredentialRequest(request: BeginCreateCredentialRequest): BeginCreateCredentialResponse? {
        when (request) {
            is BeginCreatePublicKeyCredentialRequest -> {
                // Request is passkey type
                return handleCreatePasskeyQuery(request)
            }
        }
        // request type not supported
        Log.w(javaClass.simpleName, "unknown type of BeginCreateCredentialRequest")
        return null
    }

    private fun handleCreatePasskeyQuery(request: BeginCreatePublicKeyCredentialRequest): BeginCreateCredentialResponse {
        if (mDatabase == null) {
            // database is locked, a dummy entry is shown.
            val messageToUnlockDatabase = getString(R.string.passkey_usage_unlock_database_message)
            val dummyEntryList = listOf(
                CreateEntry(
                    getString(R.string.passkey_unknown_username),
                    IntentHelper.generateUnlockPendingIntent(applicationContext),
                    messageToUnlockDatabase
                )
            )
            return BeginCreateCredentialResponse(dummyEntryList)
        }

        val createEntries: MutableList<CreateEntry> = mutableListOf()
        val accountName = mDatabase!!.name
        val descriptionNewEntry = getString(R.string.passkey_creation_description)
        val createPendingIntentNewEntry =
            IntentHelper.generateCreatePendingIntent(applicationContext)!!
        createEntries.add(
            CreateEntry(
                accountName,
                createPendingIntentNewEntry,
                descriptionNewEntry
            )
        )

        val relyingParty = JsonHelper.parseJsonToCreateOptions(request.requestJson).relyingParty
        val passkeyList = getCredentialsFromDb(relyingParty, mDatabase!!)
        for (passkey in passkeyList) {
            val createPendingIntent =
                IntentHelper.generateCreatePendingIntent(applicationContext, passkey.nodeId)!!
            val description = getString(R.string.passkey_update_description, passkey.displayName)
            createEntries.add(
                CreateEntry(
                    accountName,
                    createPendingIntent,
                    description
                )
            )
        }

        return BeginCreateCredentialResponse(createEntries)
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        Log.d(javaClass.simpleName, "onBeginGetCredentialRequest called")
        val response = processGetCredentialsRequest(request)
        if (response != null) {
            callback.onResult(response)
        } else {
            callback.onError(GetCredentialUnknownException())
        }
    }

    private fun processGetCredentialsRequest(request: BeginGetCredentialRequest): BeginGetCredentialResponse? {
        val credentialEntries: MutableList<CredentialEntry> = mutableListOf()

        for (option in request.beginGetCredentialOptions) {
            when (option) {
                is BeginGetPublicKeyCredentialOption -> {
                    credentialEntries.addAll(
                        populatePasskeyData(option)
                    )
                    return BeginGetCredentialResponse(credentialEntries)
                }
            }
        }
        Log.w(javaClass.simpleName, "unknown beginGetCredentialOption")
        return null
    }

    private fun populatePasskeyData(option: BeginGetPublicKeyCredentialOption): List<CredentialEntry> {

        val relyingParty = JsonHelper.parseJsonToRequestOptions(option.requestJson).relyingParty
        if (relyingParty.isBlank()) {
            throw CreateCredentialUnknownException("relying party id is null or blank")
        }

        if (mDatabase == null) {
            val unknownUsername = getString(R.string.passkey_unknown_username)
            val messageToUnlockDatabase = getString(R.string.passkey_usage_unlock_database_message)
            val unlockPendingIntent = IntentHelper.generateUnlockPendingIntent(applicationContext)
            val entry = PublicKeyCredentialEntry(
                context = applicationContext,
                username = unknownUsername,
                pendingIntent = unlockPendingIntent,
                beginGetPublicKeyCredentialOption = option,
                displayName = messageToUnlockDatabase,
                lastUsedTime = Instant.now(),
                isAutoSelectAllowed = true
            )
            return listOf(entry)
        }

        val passkeys = getCredentialsFromDb(relyingParty, mDatabase!!)

        val passkeyEntries: MutableList<CredentialEntry> = mutableListOf()
        for (passkey in passkeys) {
            val usagePendingIntent =
                IntentHelper.generateUsagePendingIntent(applicationContext, passkey.nodeId)!!
            passkeyEntries.add(
                PublicKeyCredentialEntry(
                    context = applicationContext,
                    username = passkey.username,
                    pendingIntent = usagePendingIntent,
                    beginGetPublicKeyCredentialOption = option,
                    displayName = passkey.displayName,
                    isAutoSelectAllowed = false
                )
            )
        }
        return passkeyEntries
    }

    private fun getCredentialsFromDb(relyingPartyId: String, database: Database): List<Passkey> {
        val passkeys = DatabaseHelper.getAllPasskeys(database)
        val passkeysMatching = passkeys.filter { p -> p.relyingParty == relyingPartyId }
        return passkeysMatching
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        // nothing to do
    }

}