package com.kunzisoft.keepass.credentialprovider;

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import android.provider.ContactsContract.Directory.PACKAGE_NAME

import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse;
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import org.json.JSONObject

import android.util.Log
import com.kunzisoft.keepass.database.DatabaseTaskProvider
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters

@RequiresApi(value = 34)
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBeginCreateCredentialRequest(request: BeginCreateCredentialRequest, cancellationSignal: CancellationSignal, callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>) {
        TODO("Not yet implemented")

    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        /*
        val unlockEntryTitle = "Authenticate to continue"
        if (isAppLocked()) {
            callback.onResult(BeginGetCredentialResponse(
                    authenticationActions = mutableListOf(AuthenticationAction(
                            unlockEntryTitle, createUnlockPendingIntent())
                    )
            )
            )
            return
        }
         */
        try {
            val response = processGetCredentialsRequest(request)
            callback.onResult(response)
        } catch (e: GetCredentialException) {
            callback.onError(GetCredentialUnknownException())
        }
    }

    companion object {
        private const val GET_PASSKEY_INTENT_ACTION = "com.kunzisoft.keepass.credentialprovider.GET_PASSKEY"
        private const val GET_PASSWORD_INTENT_ACTION = "com.kunzisoft.keepass.credentialprovider.GET_PASSWORD"

        const val NODE_ID_KEY = "nodeId"
        const val INTENT_EXTRA_KEY = "CREDENTIAL_DATA"
    }

    private fun processGetCredentialsRequest(
        request: BeginGetCredentialRequest
    ): BeginGetCredentialResponse {

        val callingAppInfo = request.callingAppInfo ?: throw Exception("callingAppInfo is null")
        val credentialEntries: MutableList<CredentialEntry> = mutableListOf()

        for (option in request.beginGetCredentialOptions) {
            when (option) {
                is BeginGetPasswordOption -> {
                    // TODO
                }
                is BeginGetPublicKeyCredentialOption -> {
                    credentialEntries.addAll(
                        populatePasskeyData(callingAppInfo, option)
                    )
                } else -> {
                Log.d(javaClass.simpleName,"Request not supported")
            }
            }
        }
        return BeginGetCredentialResponse(credentialEntries)
    }

    private fun populatePasskeyData(callingAppInfo: CallingAppInfo, option: BeginGetPublicKeyCredentialOption): List<CredentialEntry> {

        val json = JSONObject(option.requestJson)

        val relyingPartyId = json.optString("rpId", "")

        val passkeys = getCredentialsFromDb(relyingPartyId)

        val passkeyEntries: MutableList<CredentialEntry> = mutableListOf()
        for (passkey in passkeys) {
            val data = Bundle()
            data.putString(NODE_ID_KEY, passkey.nodeId)
            passkeyEntries.add(
                PublicKeyCredentialEntry(
                    context = applicationContext,
                    username = passkey.username,
                    pendingIntent = createNewPendingIntent(
                        GET_PASSKEY_INTENT_ACTION,
                        data
                    ),
                    beginGetPublicKeyCredentialOption = option,
                    displayName = passkey.displayName,
                    lastUsedTime = passkey.lastUsedTime,
                    isAutoSelectAllowed = false
                )
            )
        }
        return passkeyEntries
    }

    private fun getCredentialsFromDb(relyingPartyId: String) : List<PasskeyUtil.Passkey> {
        if (mDatabase == null) {
            // TODO make sure that the database is open
            val dummyPassKey =  PasskeyUtil.Passkey("", "unknown", "unlock db", "", "", "", "", null)
            return listOf(dummyPassKey)
        }
        val passkeys = PasskeyUtil.searchPasskeys(mDatabase!!)
        val passkeysMatching = passkeys.filter { p -> p.relyingParty == relyingPartyId }
        return passkeysMatching
    }


    private fun createNewPendingIntent(action: String, extra: Bundle? = null): PendingIntent {
        val intent = Intent(action).setPackage(PACKAGE_NAME).setClass(applicationContext, CredentialProviderActivity::class.java)
        if (extra != null) {
            intent.putExtra(INTENT_EXTRA_KEY, extra)
        }
        val requestCode = 42 // not used
        return PendingIntent.getActivity(
            applicationContext, requestCode, intent,
            (PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        )
    }

    override fun onClearCredentialStateRequest(request: ProviderClearCredentialStateRequest, cancellationSignal: CancellationSignal, callback: OutcomeReceiver<Void?, ClearCredentialException>) {
        // nothing to do
    }

}
