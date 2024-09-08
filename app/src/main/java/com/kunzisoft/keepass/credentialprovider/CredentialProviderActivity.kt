package com.kunzisoft.keepass.credentialprovider

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.webauthn.MyAuthenticatorAssertionResponse
import androidx.credentials.webauthn.FidoPublicKeyCredential
import androidx.credentials.webauthn.PublicKeyCredentialRequestOptions
import com.kunzisoft.signature.Signature
import com.kunzisoft.keepass.activities.legacy.DatabaseActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import org.apache.commons.codec.binary.Base64
import androidx.biometric.BiometricPrompt;
import com.kunzisoft.keepass.R

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CredentialProviderActivity : DatabaseActivity() {

    @SuppressLint("RestrictedApi")
    private fun validatePasskey(requestJson: String, origin: String, packageName: String, uid: ByteArray, username: Any, credId: ByteArray, privateKey: String) {

        val request = PublicKeyCredentialRequestOptions(requestJson)

        // https://www.w3.org/TR/webauthn-3/#authdata-flags
        val userPresent = true
        val userVerified = true
        val backupEligibility = true
        val backupState = true
        val response = MyAuthenticatorAssertionResponse(
            requestOptions = request,
            credentialId = credId,
            origin = origin,
            up = userPresent,
            uv = userVerified,
            be = backupEligibility,
            bs = backupState,
            userHandle = uid
        )

        val messageToSign = response.dataToSign()

        val sig = Signature.sign(privateKey, messageToSign)

        response.signature = sig

        val credential = FidoPublicKeyCredential(
            rawId = credId, response = response, authenticatorAttachment = "platform"
        )
        val result = Intent()

        val cJson = credential.json()
        Log.w("", cJson)
        val passkeyCredential = PublicKeyCredential(cJson)
        PendingIntentHandler.setGetCredentialResponse(
            result, GetCredentialResponse(passkeyCredential)
        )
        setResult(RESULT_OK, result)
        finish()
    }

    private fun b64Decode(encodedString: String?): ByteArray {
        return Base64.decodeBase64(encodedString)
    }

    private fun cleanUp() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        Log.d(javaClass.simpleName,"onDatabaseRetrieved called: database = $database")
        super.onDatabaseRetrieved(database)

        val getRequest =
            PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

        if (getRequest?.credentialOptions?.size != 1) {
            throw Exception("not exact 1 credentialOption")
        }

        when (val credOption = getRequest.credentialOptions[0]) {
            is GetPublicKeyCredentialOption -> handlePublicKeyCredOption(credOption, getRequest.callingAppInfo)
            is GetPasswordOption -> handlePasswordOption(credOption)
            else -> throw Exception("unknown type of credentialOption")
        }

        Log.d(javaClass.simpleName, "onDatabaseRetrieved finished")

    }
    private fun handlePublicKeyCredOption(publicKeyRequest: GetPublicKeyCredentialOption, callingAppInfo: CallingAppInfo) {

        val requestInfo = intent.getBundleExtra(KeePassDXCredentialProviderService.INTENT_EXTRA_KEY)
        val nodeId = requestInfo?.getString(KeePassDXCredentialProviderService.NODE_ID_KEY)

        Log.d(javaClass.simpleName, "nodeId = $nodeId")

        if (mDatabase == null || nodeId == null) {
            cleanUp()
            return
        }
        val passkey = PasskeyUtil.searchPassKeyByNodeId(mDatabase!!, nodeId)


        if (passkey == null) {
            cleanUp()
            return
        }
        Log.d(javaClass.simpleName, "passkey found")

        val credId = b64Decode(passkey.credId)
        val privateKey = passkey.privateKeyPem
        val uid = b64Decode(passkey.userHandle)

        val origin = appInfoToOrigin(callingAppInfo)

        Log.d(javaClass.simpleName, "origin = $origin")
        val packageName = callingAppInfo.packageName

        val biometricPrompt = BiometricPrompt(
            this,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    cleanUp()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    cleanUp()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    validatePasskey(
                        publicKeyRequest.requestJson,
                        origin!!,
                        packageName,
                        uid,
                        passkey.username,
                        credId,
                        privateKey
                    )
                }
            }
        )

        val title = getString(R.string.passkey_biometric_prompt_title)
        val subtitle = getString(R.string.passkey_biometric_prompt_subtitle, origin)
        val negativeButtonText = getString(R.string.passkey_biometric_prompt_negative_button_text)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(negativeButtonText)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun handlePasswordOption(passwordOption: GetPasswordOption) {
        // TODO
    }

    private fun appInfoToOrigin(callingAppInfo: CallingAppInfo): String? {
        val privilegedAllowlist = assets.open("trustedPackages.json").bufferedReader().use {
            it.readText()
        }
        return callingAppInfo.getOrigin(privilegedAllowlist)?.removeSuffix("/")
    }


}