package com.kunzisoft.keepass.credentialprovider.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.PendingIntentHandler
import com.kunzisoft.asymmetric.Signature
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.legacy.DatabaseActivity
import com.kunzisoft.keepass.credentialprovider.data.Passkey
import com.kunzisoft.keepass.credentialprovider.data.PublicKeyCredentialCreationOptions
import com.kunzisoft.keepass.credentialprovider.util.AppRelyingPartyRelation
import com.kunzisoft.keepass.credentialprovider.util.Base64Helper
import com.kunzisoft.keepass.credentialprovider.util.DatabaseHelper
import com.kunzisoft.keepass.credentialprovider.util.IntentHelper
import com.kunzisoft.keepass.credentialprovider.util.JsonHelper
import com.kunzisoft.keepass.credentialprovider.util.OriginHelper
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.DatabaseTaskProvider
import com.kunzisoft.random.KeePassDXRandom

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CreatePasskeyActivity : DatabaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(javaClass.simpleName, "onCreate called")
        super.onCreate(savedInstanceState)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        Log.d(javaClass.simpleName, "onDatabaseRetrieved called")
        super.onDatabaseRetrieved(database)

        try {
            if (database == null) {
                throw CreateCredentialUnknownException("retrievedDatabase is null, maybe database is locked")
            }

            if (intent == null) {
                throw CreateCredentialUnknownException("intent is null")
            }

            if (mDatabaseTaskProvider == null) {
                throw CreateCredentialUnknownException("mDatabaseTaskProvider is null")
            }

            createPasskeyAfterPrompt(database, mDatabaseTaskProvider!!, intent)
        } catch (e: CreateCredentialUnknownException) {
            Log.e(this::class.java.simpleName, "CreateCredentialUnknownException was thrown", e)
            setResult(RESULT_CANCELED)
            finish()
        } catch (e: Exception) {
            Log.e(this::class.java.simpleName, "other exception was thrown", e)
            setResult(RESULT_CANCELED)
            finish()
        }

    }

    private fun createPasskeyAfterPrompt(
        database: ContextualDatabase,
        databaseTaskProvider: DatabaseTaskProvider,
        intent: Intent
    ) {

        val request = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
            ?: throw CreateCredentialUnknownException("could not retrieve request from intent")

        if (request.callingRequest !is CreatePublicKeyCredentialRequest) {
            throw CreateCredentialUnknownException("callingRequest is of wrong type: ${request.callingRequest.type}")
        }
        val publicKeyRequest = request.callingRequest as CreatePublicKeyCredentialRequest

        val creationOptions = JsonHelper.parseJsonToCreateOptions(publicKeyRequest.requestJson)

        val relyingParty = creationOptions.relyingParty

        val biometricPrompt = BiometricPrompt(
            this,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int, errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    throw CreateCredentialUnknownException("authentication error: errorCode = $errorCode, errString = $errString")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    throw CreateCredentialUnknownException("authentication failed")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    createPasskey(database, databaseTaskProvider, intent, creationOptions)
                }
            }
        )

        val title = getString(R.string.passkey_creation_biometric_prompt_title)
        val subtitle =
            getString(
                R.string.passkey_creation_biometric_prompt_subtitle,
                relyingParty
            )
        val negativeButtonText =
            getString(R.string.passkey_creation_biometric_prompt_negative_button_text)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(negativeButtonText)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun createPasskey(
        database: ContextualDatabase,
        databaseTaskProvider: DatabaseTaskProvider,
        intent: Intent,
        creationOptions: PublicKeyCredentialCreationOptions
    ) {
        val request = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

        val nodeId = IntentHelper.getVerifiedNodeId(intent)
            ?: throw CreateCredentialUnknownException("could not get verified nodeId from intent")

        val callingAppInfo = request!!.callingAppInfo
        val relyingParty = creationOptions.relyingParty
        val challenge = creationOptions.challenge
        val keyTypeIdList = creationOptions.keyTypeIdList
        val webOrigin = OriginHelper.getWebOrigin(callingAppInfo, assets)
        val apkSigningCertificate =
            callingAppInfo.signingInfo.apkContentsSigners.getOrNull(0)?.toByteArray()

        createPasskeyWithParameters(
            relyingParty,
            creationOptions.username,
            creationOptions.userId,
            database,
            databaseTaskProvider,
            keyTypeIdList,
            challenge,
            webOrigin,
            apkSigningCertificate,
            nodeId
        )
    }

    private fun createPasskeyWithParameters(
        relyingParty: String,
        username: String,
        userHandle: ByteArray,
        database: ContextualDatabase,
        databaseTaskProvider: DatabaseTaskProvider,
        keyTypeIdList: List<Long>,
        challenge: ByteArray,
        webOrigin: String?,
        apkSigningCertificate: ByteArray?,
        nodeId: String
    ) {

        val isPrivilegedApp =
            (webOrigin != null && webOrigin == OriginHelper.DEFAULT_PROTOCOL + relyingParty)
        Log.d(this::class.java.simpleName, "isPrivilegedApp = $isPrivilegedApp")

        if (!isPrivilegedApp) {
            val isValid =
                AppRelyingPartyRelation.isRelationValid(relyingParty, apkSigningCertificate)
            if (!isValid) {
                throw CreateCredentialUnknownException(
                    "could not verify relation between app " +
                            "and relyingParty $relyingParty"
                )
            }
        }

        val credentialId = KeePassDXRandom.generateCredentialId()

        val (keyPair, keyTypeId) = Signature.generateKeyPair(keyTypeIdList)
            ?: throw CreateCredentialUnknownException("no known public key type found")
        val privateKeyPem = Signature.convertPrivateKeyToPem(keyPair.private)

        if (IntentHelper.isPlaceholderNodeId(nodeId)) {
            // create new entry in database

            val displayName = "$relyingParty (Passkey)"
            val newPasskey = Passkey(
                nodeId = "", // created by the database
                username = username,
                displayName = displayName,
                privateKeyPem = privateKeyPem,
                credId = Base64Helper.b64Encode(credentialId),
                userHandle = Base64Helper.b64Encode(userHandle),
                relyingParty = relyingParty,
                databaseEntry = null
            )

            DatabaseHelper.saveNewEntry(database, databaseTaskProvider, newPasskey)
        } else {
            // update an existing entry in database
            val oldPasskey = DatabaseHelper.searchPassKeyByNodeId(database, nodeId)
                ?: throw GetCredentialUnknownException("no passkey with nodeId $nodeId found")

            val updatedPasskey = Passkey(
                nodeId = "", // unchanged
                username = username,
                displayName = oldPasskey.displayName,
                privateKeyPem = privateKeyPem,
                credId = Base64Helper.b64Encode(credentialId),
                userHandle = Base64Helper.b64Encode(userHandle),
                relyingParty = relyingParty,
                databaseEntry = oldPasskey.databaseEntry
            )

            DatabaseHelper.updateEntry(database, databaseTaskProvider, updatedPasskey)
        }

        val publicKeyEncoded = Signature.convertPublicKey(keyPair.public, keyTypeId)

        val publicKeyMap = Signature.convertPublicKeyToMap(keyPair.public, keyTypeId)
        val publicKeyCbor = JsonHelper.generateCborFromMap(publicKeyMap!!)

        val authData = JsonHelper.generateAuthDataForCreate(
            userPresent = true,
            userVerified = true,
            backupEligibility = true,
            backupState = true,
            rpId = relyingParty.toByteArray(),
            credentialId = credentialId,
            credentialPublicKey = publicKeyCbor
        )

        val attestationObject = JsonHelper.generateAttestationObject(authData)

        val clientJson: String
        if (isPrivilegedApp) {
            clientJson = JsonHelper.generateClientDataJsonPrivileged()
        } else {
            val origin = OriginHelper.DEFAULT_PROTOCOL + relyingParty
            clientJson = JsonHelper.generateClientDataJsonNonPrivileged(
                challenge,
                origin,
                packageName,
                isCrossOriginAdded = true,
                isGet = false
            )
        }

        val responseJson = JsonHelper.createAuthenticatorAttestationResponseJSON(
            credentialId,
            clientJson,
            attestationObject,
            publicKeyEncoded!!,
            authData,
            keyTypeId
        )

        // log only the length to prevent logging sensitive information
        Log.d(javaClass.simpleName, "responseJson with length ${responseJson.length} created")
        val createPublicKeyCredResponse = CreatePublicKeyCredentialResponse(responseJson)

        val resultOfActivity = Intent()

        PendingIntentHandler.setCreateCredentialResponse(
            resultOfActivity, createPublicKeyCredResponse
        )
        setResult(Activity.RESULT_OK, resultOfActivity)
        finish()
    }

}