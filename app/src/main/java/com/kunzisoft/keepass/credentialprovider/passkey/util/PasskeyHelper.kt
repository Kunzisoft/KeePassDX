/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.credentialprovider.passkey.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.kunzisoft.encrypt.Base64Helper.Companion.b64Encode
import com.kunzisoft.encrypt.Signature
import com.kunzisoft.encrypt.Signature.getApplicationFingerprints
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.credentialprovider.EntrySelectionHelper.addNodeId
import com.kunzisoft.keepass.credentialprovider.passkey.data.AuthenticatorAssertionResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.AuthenticatorAttestationResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.Cbor
import com.kunzisoft.keepass.credentialprovider.passkey.data.ClientDataBuildResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.ClientDataDefinedResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.ClientDataResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.FidoPublicKeyCredential
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialCreationOptions
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialCreationParameters
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialRequestOptions
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialUsageParameters
import com.kunzisoft.keepass.credentialprovider.passkey.util.PrivilegedAllowLists.getOriginFromPrivilegedAllowLists
import com.kunzisoft.keepass.model.AndroidOrigin
import com.kunzisoft.keepass.model.AppOrigin
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.Passkey
import com.kunzisoft.keepass.utils.AppUtil
import com.kunzisoft.keepass.utils.StringUtil.toHexString
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Utility class to manage the passkey elements,
 * allows to add and retrieve intent values with preconfigured keys,
 * and makes it easy to create creation and usage requests
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
object PasskeyHelper {

    private const val EXTRA_PASSKEY = "com.kunzisoft.keepass.passkey.extra.passkey"

    private const val HMAC_TYPE = "HmacSHA256"

    private const val EXTRA_APP_ORIGIN = "com.kunzisoft.keepass.extra.appOrigin"
    private const val EXTRA_TIMESTAMP = "com.kunzisoft.keepass.extra.timestamp"
    private const val EXTRA_AUTHENTICATION_CODE = "com.kunzisoft.keepass.extra.authenticationCode"

    private const val SEPARATOR = "_"

    private const val NAME_OF_HMAC_KEY = "KeePassDXCredentialProviderHMACKey"

    private const val KEYSTORE_TYPE = "AndroidKeyStore"

    private val PLACEHOLDER_FOR_NEW_NODE_ID = "0".repeat(32)

    private val REGEX_TIMESTAMP = "[0-9]{10}".toRegex()
    private val REGEX_AUTHENTICATION_CODE = "[A-F0-9]{64}".toRegex() // 256 bits = 64 hex chars

    private const val MAX_DIFF_IN_SECONDS = 60

    private val internalSecureRandom: SecureRandom = SecureRandom()

    /**
     * Add an authentication code generated by an entry to the intent
     */
    fun Intent.addAuthCode(passkeyEntryNodeId: UUID? = null) {
        putExtras(Bundle().apply {
            val timestamp = Instant.now().epochSecond
            putString(EXTRA_TIMESTAMP, timestamp.toString())
            putString(
                EXTRA_AUTHENTICATION_CODE,
                generatedAuthenticationCode(
                    passkeyEntryNodeId, timestamp
                ).toHexString()
            )
        })
    }

    /**
     * Add the passkey to the intent
     */
    fun Intent.addPasskey(passkey: Passkey?) {
        passkey?.let {
            putExtra(EXTRA_PASSKEY, passkey)
        }
    }

    /**
     * Retrieve the passkey from the intent
     */
    fun Intent.retrievePasskey(): Passkey? {
        return this.getParcelableExtraCompat(EXTRA_PASSKEY)
    }

    /**
     * Remove the passkey from the intent
     */
    fun Intent.removePasskey() {
        return this.removeExtra(EXTRA_PASSKEY)
    }

    /**
     * Add the app origin to the intent
     */
    fun Intent.addAppOrigin(appOrigin: AppOrigin?) {
        appOrigin?.let {
            putExtra(EXTRA_APP_ORIGIN, appOrigin)
        }
    }

    /**
     * Retrieve the app origin from the intent
     */
    fun Intent.retrieveAppOrigin(): AppOrigin? {
        return this.getParcelableExtraCompat(EXTRA_APP_ORIGIN)
    }

    /**
     * Remove the app origin from the intent
     */
    fun Intent.removeAppOrigin() {
        return this.removeExtra(EXTRA_APP_ORIGIN)
    }

    /**
     * Build the Passkey response for one entry
     */
    fun Activity.buildPasskeyResponseAndSetResult(
        entryInfo: EntryInfo,
        extras: Bundle? = null
    ) {
        try {
            entryInfo.passkey?.let { passkey ->
                val mReplyIntent = Intent()
                Log.d(javaClass.name, "Success Passkey manual selection")
                mReplyIntent.addPasskey(passkey)
                mReplyIntent.addAppOrigin(entryInfo.appOrigin)
                mReplyIntent.addNodeId(entryInfo.id)
                extras?.let {
                    mReplyIntent.putExtras(it)
                }
                setResult(Activity.RESULT_OK, mReplyIntent)
            } ?: run {
                throw IOException("No passkey found")
            }
        } catch (e: Exception) {
            Log.e(javaClass.name, "Unable to add the passkey as result", e)
            Toast.makeText(
                this,
                getString(R.string.error_passkey_result),
                Toast.LENGTH_SHORT
            ).show()
            setResult(Activity.RESULT_CANCELED)
        }
    }

    /**
     * Check the timestamp and authentication code transmitted via PendingIntent
     */
    fun checkSecurity(intent: Intent, nodeId: UUID?) {
        val timestampString = intent.getStringExtra(EXTRA_TIMESTAMP)
        if (timestampString.isNullOrEmpty())
            throw CreateCredentialUnknownException("Timestamp null")
        if (timestampString.matches(REGEX_TIMESTAMP).not()) {
            throw CreateCredentialUnknownException("Timestamp not valid")
        }
        val timestamp = timestampString.toLong()
        val diff = Instant.now().epochSecond - timestamp
        if (diff < 0 || diff > MAX_DIFF_IN_SECONDS) {
            throw CreateCredentialUnknownException("Out of time")
        }
        verifyAuthenticationCode(
            intent.getStringExtra(EXTRA_AUTHENTICATION_CODE),
            generatedAuthenticationCode(nodeId, timestamp)
        )
    }

    /**
     * Verify the authentication code from the encrypted message received from the intent
     */
    private fun verifyAuthenticationCode(
        valueToCheck: String?,
        authenticationCode: ByteArray
    ) {
        if (valueToCheck.isNullOrEmpty())
            throw CreateCredentialUnknownException("Authentication code empty")
        if (valueToCheck.matches(REGEX_AUTHENTICATION_CODE).not())
            throw CreateCredentialUnknownException("Authentication not valid")
        if (MessageDigest.isEqual(authenticationCode, generateAuthenticationCode(valueToCheck)))
            throw CreateCredentialUnknownException("Authentication code incorrect")
    }

    /**
     * Generate the authentication code base on the entry [nodeId] and [timestamp]
     */
    private fun generatedAuthenticationCode(nodeId: UUID?, timestamp: Long): ByteArray {
        return generateAuthenticationCode(
            (nodeId?.toString() ?: PLACEHOLDER_FOR_NEW_NODE_ID) + SEPARATOR + timestamp.toString()
        )
    }

    /**
     * Generate the authentication code base on the entry [message]
     */
    private fun generateAuthenticationCode(message: String): ByteArray {
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
        keyStore.load(null)
        val hmacKey = try {
            keyStore.getKey(NAME_OF_HMAC_KEY, null) as SecretKey
        } catch (_: Exception) {
            // key not found
            generateKey()
        }

        val mac = Mac.getInstance(HMAC_TYPE)
        mac.init(hmacKey)
        val authenticationCode = mac.doFinal(message.toByteArray())
        return authenticationCode
    }

    /**
     * Generate the HMAC key if cannot be found in the KeyStore
     */
    private fun generateKey(): SecretKey? {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEYSTORE_TYPE
        )
        val keySizeInBits = 128
        keyGenerator.init(
            KeyGenParameterSpec.Builder(NAME_OF_HMAC_KEY, KeyProperties.PURPOSE_SIGN)
                .setKeySize(keySizeInBits)
                .build()
        )
        val key = keyGenerator.generateKey()
        return key
    }

    /**
     * Retrieve the [PublicKeyCredentialCreationOptions] from the intent
     */
    fun ProviderCreateCredentialRequest.retrievePasskeyCreationComponent(): PublicKeyCredentialCreationOptions {
        val request = this
        if (request.callingRequest !is CreatePublicKeyCredentialRequest) {
            throw CreateCredentialUnknownException("callingRequest is of wrong type: ${request.callingRequest.type}")
        }
        val createPublicKeyCredentialRequest = request.callingRequest as CreatePublicKeyCredentialRequest
        return PublicKeyCredentialCreationOptions(
            requestJson = createPublicKeyCredentialRequest.requestJson,
            clientDataHash = createPublicKeyCredentialRequest.clientDataHash
        )
    }

    /**
     * Retrieve the [GetPublicKeyCredentialOption] from the intent
     */
    fun ProviderGetCredentialRequest.retrievePasskeyUsageComponent(): GetPublicKeyCredentialOption {
        val request = this
        if (request.credentialOptions.size != 1) {
            throw GetCredentialUnknownException("not exact one credentialOption")
        }
        if (request.credentialOptions[0] !is GetPublicKeyCredentialOption) {
            throw CreateCredentialUnknownException("credentialOptions is of wrong type: ${request.credentialOptions[0]}")
        }
        return request.credentialOptions[0] as GetPublicKeyCredentialOption
    }

    /**
     * Utility method to retrieve the origin asynchronously,
     * checks for the presence of the application in the privilege lists
     *
     * @param providedClientDataHash Client data hash precalculated by the system
     * @param callingAppInfo CallingAppInfo to verify and retrieve the specific Origin
     * @param context Context for file operations.
     * call [onOriginRetrieved] if the origin is already calculated by the system and in the privileged list, return the clientDataHash
     * call [onOriginNotRetrieved] if the origin is not retrieved from the system, return a new Android Origin
     */
    suspend fun getOrigin(
        providedClientDataHash: ByteArray?,
        callingAppInfo: CallingAppInfo?,
        context: Context,
        onOriginRetrieved: suspend (appOrigin: AppOrigin, clientDataHash: ByteArray) -> Unit,
        onOriginNotRetrieved: suspend (appOrigin: AppOrigin, androidOriginString: String) -> Unit
    ) {
        if (callingAppInfo == null) {
            throw SecurityException("Calling app info cannot be retrieved")
        }
        withContext(Dispatchers.IO) {

            // For trusted browsers like Chrome and Firefox
            val callOrigin = try {
                getOriginFromPrivilegedAllowLists(callingAppInfo, context)
            } catch (e: Exception) {
                // Throw the Privileged Exception only if it's a browser
                if (e is PrivilegedAllowLists.PrivilegedException
                    && AppUtil.getInstalledBrowsersWithSignatures(context).any {
                        it.packageName == e.temptingApp.packageName
                    }
                ) throw e
                null
            }

            // Build the default Android origin
            val androidOrigin = AndroidOrigin(
                packageName = callingAppInfo.packageName,
                fingerprint = callingAppInfo.signingInfo.getApplicationFingerprints()
            )

            // Check if the webDomain is validated by the system
            withContext(Dispatchers.Main) {
                if (callOrigin != null && providedClientDataHash != null) {
                    // Origin already defined by the system
                    Log.d(javaClass.simpleName, "Origin $callOrigin retrieved from callingAppInfo")
                    onOriginRetrieved(
                        AppOrigin.fromOrigin(callOrigin, androidOrigin, verified = true),
                        providedClientDataHash
                    )
                } else {
                    // Add Android origin by default
                    onOriginNotRetrieved(
                        AppOrigin(verified = false).apply {
                            addAndroidOrigin(androidOrigin)
                        },
                        androidOrigin.toOriginValue()
                    )
                }
            }
        }
    }

    /**
     * Generate a credential id randomly
     */
    private fun generateCredentialId(): ByteArray {
        // see https://w3c.github.io/webauthn/#credential-id
        val size = 16
        val credentialId = ByteArray(size)
        internalSecureRandom.nextBytes(credentialId)
        return credentialId
    }

    /**
     * Utility method to create a passkey and the associated creation request parameters
     * [intent] allows to retrieve the request
     * [context] context to manage package verification files
     * [defaultBackupEligibility] the default backup eligibility to add the the passkey entry
     * [defaultBackupState] the default backup state to add the the passkey entry
     * [passkeyCreated] is called asynchronously when the passkey has been created
     */
    suspend fun retrievePasskeyCreationRequestParameters(
        intent: Intent,
        context: Context,
        defaultBackupEligibility: Boolean?,
        defaultBackupState: Boolean?,
        passkeyCreated: suspend (Passkey, AppOrigin?, PublicKeyCredentialCreationParameters) -> Unit
    ) {
        val createCredentialRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        if (createCredentialRequest == null)
            throw CreateCredentialUnknownException("could not retrieve request from intent")
        val callingAppInfo = createCredentialRequest.callingAppInfo
        val creationOptions = createCredentialRequest.retrievePasskeyCreationComponent()

        val relyingParty = creationOptions.relyingPartyEntity.id
        val username = creationOptions.userEntity.name
        val userHandle = creationOptions.userEntity.id
        val pubKeyCredParams = creationOptions.pubKeyCredParams
        val clientDataHash = creationOptions.clientDataHash

        val credentialId = generateCredentialId()

        val (keyPair, keyTypeId) = Signature.generateKeyPair(
            pubKeyCredParams.map { params -> params.alg }
        ) ?: throw CreateCredentialUnknownException("no known public key type found")
        val privateKeyPem = Signature.convertPrivateKeyToPem(keyPair.private)

        // Create the passkey element
        val passkey = Passkey(
            username = username,
            privateKeyPem = privateKeyPem,
            credentialId = b64Encode(credentialId),
            userHandle = b64Encode(userHandle),
            relyingParty = relyingParty,
            backupEligibility = defaultBackupEligibility,
            backupState = defaultBackupState
        )

        // create new entry in database
        getOrigin(
            providedClientDataHash = clientDataHash,
            callingAppInfo = callingAppInfo,
            context = context,
            onOriginRetrieved = { appInfoToStore, clientDataHash ->
                passkeyCreated.invoke(
                    passkey,
                    appInfoToStore,
                    PublicKeyCredentialCreationParameters(
                        publicKeyCredentialCreationOptions = creationOptions,
                        credentialId = credentialId,
                        signatureKey = Pair(keyPair, keyTypeId),
                        clientDataResponse = ClientDataDefinedResponse(clientDataHash)
                    )
                )
            },
            onOriginNotRetrieved = { appInfoToStore, origin ->
                passkeyCreated.invoke(
                    passkey,
                    appInfoToStore,
                    PublicKeyCredentialCreationParameters(
                        publicKeyCredentialCreationOptions = creationOptions,
                        credentialId = credentialId,
                        signatureKey = Pair(keyPair, keyTypeId),
                        clientDataResponse = ClientDataBuildResponse(
                            type = ClientDataBuildResponse.Type.CREATE,
                            challenge = creationOptions.challenge,
                            origin = origin
                        )
                    )
                )
            }
        )
    }

    /**
     * Build the passkey public key credential response,
     * by calling this method the user is always recognized as present and verified
     */
    fun buildCreatePublicKeyCredentialResponse(
        publicKeyCredentialCreationParameters: PublicKeyCredentialCreationParameters,
        backupEligibility: Boolean,
        backupState: Boolean
    ): CreatePublicKeyCredentialResponse {

        val keyPair = publicKeyCredentialCreationParameters.signatureKey.first
        val keyTypeId = publicKeyCredentialCreationParameters.signatureKey.second
        val responseJson = FidoPublicKeyCredential(
            id = b64Encode(publicKeyCredentialCreationParameters.credentialId),
            response = AuthenticatorAttestationResponse(
                requestOptions = publicKeyCredentialCreationParameters.publicKeyCredentialCreationOptions,
                credentialId = publicKeyCredentialCreationParameters.credentialId,
                credentialPublicKey = Cbor().encode(
                    Signature.convertPublicKeyToMap(
                    publicKeyIn = keyPair.public,
                    keyTypeId = keyTypeId
                ) ?: mapOf<Int, Any>()),
                userPresent = true,
                userVerified = true,
                backupEligibility = backupEligibility,
                backupState = backupState,
                publicKeyTypeId = keyTypeId,
                publicKeyCbor = Signature.convertPublicKey(keyPair.public, keyTypeId)!!,
                clientDataResponse = publicKeyCredentialCreationParameters.clientDataResponse
            ),
            authenticatorAttachment = "platform"
        ).json()
        // log only the length to prevent logging sensitive information
        Log.d(javaClass.simpleName, "Json response for key creation")
        return CreatePublicKeyCredentialResponse(responseJson)
    }

    /**
     * Utility method to use a passkey and create the associated usage request parameters
     * [intent] allows to retrieve the request
     * [context] context to manage package verification files
     * [result] is called asynchronously after the creation of PublicKeyCredentialUsageParameters, the origin associated with it may or may not be verified
     */
    suspend fun retrievePasskeyUsageRequestParameters(
        intent: Intent,
        context: Context,
        result: suspend (PublicKeyCredentialUsageParameters) -> Unit
    ) {
        val getCredentialRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        if (getCredentialRequest == null)
            throw CreateCredentialUnknownException("could not retrieve request from intent")
        val callingAppInfo = getCredentialRequest.callingAppInfo
        val credentialOption = getCredentialRequest.retrievePasskeyUsageComponent()
        val clientDataHash = credentialOption.clientDataHash

        val requestOptions = PublicKeyCredentialRequestOptions(credentialOption.requestJson)

        getOrigin(
            providedClientDataHash = clientDataHash,
            callingAppInfo = callingAppInfo,
            context = context,
            onOriginRetrieved = { appOrigin, clientDataHash ->
                result.invoke(
                    PublicKeyCredentialUsageParameters(
                        publicKeyCredentialRequestOptions = requestOptions,
                        clientDataResponse = ClientDataDefinedResponse(clientDataHash),
                        appOrigin = appOrigin
                    )
                )
            },
            onOriginNotRetrieved = { appOrigin, androidOriginString ->
                // By default we crate an usage parameter with Android origin
                result.invoke(
                    PublicKeyCredentialUsageParameters(
                        publicKeyCredentialRequestOptions = requestOptions,
                        clientDataResponse = ClientDataBuildResponse(
                            type = ClientDataBuildResponse.Type.GET,
                            challenge = requestOptions.challenge,
                            origin = androidOriginString
                        ),
                        appOrigin = appOrigin
                    )
                )
            }
        )
    }

    /**
     * Build the passkey public key credential response,
     * by calling this method the user is always recognized as present and verified
     */
    fun buildPasskeyPublicKeyCredential(
        requestOptions: PublicKeyCredentialRequestOptions,
        clientDataResponse: ClientDataResponse,
        passkey: Passkey,
        defaultBackupEligibility: Boolean,
        defaultBackupState: Boolean
    ): PublicKeyCredential {
        val getCredentialResponse = FidoPublicKeyCredential(
            id = passkey.credentialId,
            response = AuthenticatorAssertionResponse(
                requestOptions = requestOptions,
                userPresent = true,
                userVerified = true,
                backupEligibility = passkey.backupEligibility ?: defaultBackupEligibility,
                backupState = passkey.backupState ?: defaultBackupState,
                userHandle = passkey.userHandle,
                privateKey = passkey.privateKeyPem,
                clientDataResponse = clientDataResponse
            ),
            authenticatorAttachment = "platform"
        ).json()
        Log.d(javaClass.simpleName, "Json response for key usage")
        return PublicKeyCredential(getCredentialResponse)
    }


    /**
     * Verify that the application signature is contained in the [appOrigin]
     */
    fun getVerifiedGETClientDataResponse(
        usageParameters: PublicKeyCredentialUsageParameters,
        appOrigin: AppOrigin
    ): ClientDataResponse {
        val appToCheck = usageParameters.appOrigin
        return if (appToCheck.verified) {
            usageParameters.clientDataResponse
        } else {
            // Origin checked by Android app signature
            ClientDataBuildResponse(
                type = ClientDataBuildResponse.Type.GET,
                challenge = usageParameters.publicKeyCredentialRequestOptions.challenge,
                origin = appToCheck.checkAppOrigin(appOrigin)
            )
        }
    }
}