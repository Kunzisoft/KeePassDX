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
        processCreateCredentialRequest(request)?.let { response ->
            callback.onResult(response)
        } ?: let {
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

        val accountName = mDatabase?.name ?: getString(R.string.passkey_locked_database_account_name)
        val createEntries: MutableList<CreateEntry> = mutableListOf()

        mDatabase?.let { database ->
            // To create a new entry
            IntentHelper.generateCreatePendingIntent(applicationContext)
                ?.let { pendingIntentNewEntry ->
                    createEntries.add(
                        CreateEntry(
                            accountName = accountName,
                            pendingIntent = pendingIntentNewEntry,
                            description = getString(R.string.passkey_creation_description)
                        )
                    )
                }

            // To select an existing entry
            for (passkey in getCredentialsFromDb(
                relyingPartyId = JsonHelper.parseJsonToCreateOptions(request.requestJson).relyingParty,
                database = database
            )) {
                IntentHelper.generateCreatePendingIntent(applicationContext, passkey.nodeId)
                    ?.let { createPendingIntent ->
                        createEntries.add(
                            CreateEntry(
                                accountName = accountName,
                                pendingIntent = createPendingIntent,
                                description = getString(
                                    R.string.passkey_update_description,
                                    passkey.displayName
                                )
                            )
                        )
                    }
            }
        } ?: run {
            // Database is locked, an entry is shown to unlock it
            createEntries.add(
                CreateEntry(
                    accountName = accountName,
                    pendingIntent = IntentHelper.generateUnlockPendingIntent(applicationContext),
                    description = getString(R.string.passkey_locked_database_description)
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
        processGetCredentialsRequest(request)?.let { response ->
            callback.onResult(response)
        } ?: run {
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

        val passkeyEntries: MutableList<CredentialEntry> = mutableListOf()

        mDatabase?.let { database ->
            // Retrieve passkeys entries from database
            val relyingParty = JsonHelper.parseJsonToRequestOptions(option.requestJson).relyingParty
            if (relyingParty.isBlank()) {
                throw CreateCredentialUnknownException("relying party id is null or blank")
            }
            for (passkey in getCredentialsFromDb(
                relyingPartyId = relyingParty,
                database = database
            )) {
                IntentHelper.generateUsagePendingIntent(applicationContext, passkey.nodeId)
                    ?.let { usagePendingIntent ->
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
            }
        } ?: run {
            // Database is locked, a public key credential entry is shown to unlock it
            passkeyEntries.add(
                PublicKeyCredentialEntry(
                    context = applicationContext,
                    username = getString(R.string.passkey_locked_database_account_name),
                    pendingIntent = IntentHelper.generateUnlockPendingIntent(applicationContext),
                    beginGetPublicKeyCredentialOption = option,
                    displayName = getString(R.string.passkey_locked_database_description),
                    lastUsedTime = Instant.now(),
                    isAutoSelectAllowed = true
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