package com.kunzisoft.keepass.credentialprovider.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.PendingIntentHandler
import com.kunzisoft.asymmetric.Signature
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.legacy.DatabaseActivity
import com.kunzisoft.keepass.credentialprovider.data.Passkey
import com.kunzisoft.keepass.credentialprovider.util.AppRelyingPartyRelation
import com.kunzisoft.keepass.credentialprovider.util.Base64Helper
import com.kunzisoft.keepass.credentialprovider.util.DatabaseHelper
import com.kunzisoft.keepass.credentialprovider.util.IntentHelper
import com.kunzisoft.keepass.credentialprovider.util.JsonHelper
import com.kunzisoft.keepass.credentialprovider.util.OriginHelper
import com.kunzisoft.keepass.credentialprovider.util.OriginHelper.Companion.DEFAULT_PROTOCOL
import com.kunzisoft.keepass.database.ContextualDatabase


@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class UsePasskeyActivity : DatabaseActivity() {

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
            usePasskeyAfterPrompt(database, intent)
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

    private fun usePasskeyAfterPrompt(
        database: ContextualDatabase,
        intent: Intent
    ) {
        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
            ?: throw CreateCredentialUnknownException("could not retrieve request from intent")

        if (request.credentialOptions.size != 1) {
            throw GetCredentialUnknownException("not exact one credentialOption")
        }

        if (request.credentialOptions[0] !is GetPublicKeyCredentialOption) {
            throw CreateCredentialUnknownException("credentialOptions is of wrong type: ${request.credentialOptions[0]}")
        }

        val credentialOption = request.credentialOptions[0] as GetPublicKeyCredentialOption
        val clientDataHash = credentialOption.clientDataHash

        val requestOptions = JsonHelper.parseJsonToRequestOptions(credentialOption.requestJson)

        val relyingParty = requestOptions.relyingParty
        val challenge = Base64Helper.b64Decode(requestOptions.challengeString)
        val packageName = request.callingAppInfo.packageName
        val webOrigin = OriginHelper.getWebOrigin(request.callingAppInfo, assets)

        val isPrivilegedApp =
            (webOrigin != null && webOrigin == DEFAULT_PROTOCOL + relyingParty && clientDataHash != null)

        Log.d(javaClass.simpleName, "isPrivilegedApp = $isPrivilegedApp")

        if (!isPrivilegedApp) {
            val apkSigners = request.callingAppInfo.signingInfo.apkContentsSigners
            val apkSigningCertificate = apkSigners.getOrNull(0)?.toByteArray()
            val isValid =
                AppRelyingPartyRelation.isRelationValid(relyingParty, apkSigningCertificate)
            if (!isValid) {
                throw CreateCredentialUnknownException(
                    "could not verify relation between app " +
                            "and relyingParty $relyingParty"
                )
            }
        }

        val nodeId = IntentHelper.getVerifiedNodeId(intent)
            ?: throw GetCredentialUnknownException("could not get verified nodeId from intent")

        val passkey = DatabaseHelper.searchPassKeyByNodeId(database, nodeId)
            ?: throw GetCredentialUnknownException("no passkey with nodeId $nodeId found")

        usePasskeyAfterPromptWithParameters(
            relyingParty,
            packageName,
            clientDataHash,
            isPrivilegedApp,
            challenge,
            passkey
        )
    }

    private fun usePasskeyAfterPromptWithParameters(
        relyingParty: String,
        packageName: String,
        clientDataHash: ByteArray?,
        isPrivilegedApp: Boolean,
        challenge: ByteArray,
        passkey: Passkey
    ) {
        val biometricPrompt = BiometricPrompt(
            this,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    throw GetCredentialUnknownException("authentication error: errorCode = $errorCode, errString = $errString")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    throw GetCredentialUnknownException("authentication failed")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    createResponse(
                        relyingParty,
                        packageName,
                        clientDataHash,
                        isPrivilegedApp,
                        challenge,
                        passkey
                    )
                }
            }
        )

        val title = getString(R.string.passkey_usage_biometric_prompt_title)
        val subtitle = getString(R.string.passkey_usage_biometric_prompt_subtitle, relyingParty)
        val negativeButtonText =
            getString(R.string.passkey_usage_biometric_prompt_negative_button_text)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(negativeButtonText)
            .build()
        biometricPrompt.authenticate(promptInfo)


    }

    private fun createResponse(
        relyingParty: String,
        packageName: String,
        clientDataHash: ByteArray?,
        isPrivilegedApp: Boolean,
        challenge: ByteArray,
        passkey: Passkey
    ) {

        // https://www.w3.org/TR/webauthn-3/#authdata-flags
        val userPresent = true
        val userVerified = true
        val backupEligibility = true
        val backupState = true

        val authenticatorData = JsonHelper.generateAuthDataForUsage(
            relyingParty.toByteArray(),
            userPresent,
            userVerified,
            backupEligibility,
            backupState
        )

        val clientDataJson: String
        val dataToSign: ByteArray
        if (isPrivilegedApp) {
            clientDataJson = JsonHelper.generateClientDataJsonPrivileged()
            dataToSign =
                JsonHelper.generateDataToSignPrivileged(clientDataHash!!, authenticatorData)
        } else {
            val origin = DEFAULT_PROTOCOL + relyingParty
            clientDataJson = JsonHelper.generateClientDataJsonNonPrivileged(
                challenge,
                origin,
                packageName,
                isGet = true,
                isCrossOriginAdded = false
            )
            dataToSign =
                JsonHelper.generateDataTosSignNonPrivileged(clientDataJson, authenticatorData)
        }

        val signature = Signature.sign(passkey.privateKeyPem, dataToSign)
            ?: throw GetCredentialUnknownException("signing failed")

        val getCredentialResponse =
            JsonHelper.generateGetCredentialResponse(
                clientDataJson.toByteArray(),
                authenticatorData,
                signature,
                passkey.userHandle,
                passkey.credId
            )
        
        val result = Intent()
        val passkeyCredential = PublicKeyCredential(getCredentialResponse)
        PendingIntentHandler.setGetCredentialResponse(
            result, GetCredentialResponse(passkeyCredential)
        )
        setResult(RESULT_OK, result)
        finish()
    }


}