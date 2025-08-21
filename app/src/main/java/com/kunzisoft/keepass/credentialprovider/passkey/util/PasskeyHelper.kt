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
import android.content.res.AssetManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.kunzisoft.asymmetric.Signature
import com.kunzisoft.keepass.credentialprovider.passkey.data.AuthenticatorAssertionResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.AuthenticatorAttestationResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.Cbor
import com.kunzisoft.keepass.credentialprovider.passkey.data.ClientDataDefinedResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.ClientDataNotDefinedResponse
import com.kunzisoft.keepass.credentialprovider.passkey.data.FidoPublicKeyCredential
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialCreationOptions
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialCreationParameters
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialRequestOptions
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialUsageParameters
import com.kunzisoft.keepass.credentialprovider.passkey.util.Base64Helper.Companion.b64Encode
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.Passkey
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.utils.StringUtil.toHexString
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import com.kunzisoft.random.KeePassDXRandom
import java.security.KeyStore
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
object PasskeyHelper {

    private const val EXTRA_PASSKEY_ELEMENT = "com.kunzisoft.keepass.passkey.extra.EXTRA_PASSKEY_ELEMENT"

    private const val HMAC_TYPE = "HmacSHA256"


    private const val EXTRA_SEARCH_INFO = "com.kunzisoft.keepass.extra.SEARCH_INFO"
    private const val EXTRA_NODE_ID = "com.kunzisoft.keepass.extra.nodeId"
    private const val EXTRA_TIMESTAMP = "com.kunzisoft.keepass.extra.timestamp"
    private const val EXTRA_AUTHENTICATION_CODE = "com.kunzisoft.keepass.extra.authenticationCode"

    private const val SEPARATOR = "_"

    private const val NAME_OF_HMAC_KEY = "KeePassDXCredentialProviderHMACKey"

    private const val KEYSTORE_TYPE = "AndroidKeyStore"

    private val PLACEHOLDER_FOR_NEW_NODE_ID = "0".repeat(32)

    private val REGEX_TIMESTAMP = "[0-9]{10}".toRegex()
    private val REGEX_AUTHENTICATION_CODE = "[A-F0-9]{64}".toRegex() // 256 bits = 64 hex chars

    private const val MAX_DIFF_IN_SECONDS = 60

    /**
     * Build the Passkey response for one entry
     */
    fun Activity.buildPasskeyResponseAndSetResult(
        entryInfo: EntryInfo,
        extras: Bundle? = null
    ) {
        try {
            entryInfo.passkey?.let {
                val mReplyIntent = Intent()
                Log.d(javaClass.name, "Success Passkey manual selection")
                mReplyIntent.putExtra(EXTRA_PASSKEY_ELEMENT, entryInfo.passkey)
                extras?.let {
                    mReplyIntent.putExtras(it)
                }
                setResult(Activity.RESULT_OK, mReplyIntent)
            } ?: run {
                Log.w(javaClass.name, "Failed Passkey manual selection")
                setResult(Activity.RESULT_CANCELED)
            }
        } catch (e: Exception) {
            Log.e(javaClass.name, "Cant add passkey entry as result", e)
            setResult(Activity.RESULT_CANCELED)
        }
    }

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

    fun Intent.retrievePasskey(): Passkey? {
        return this.getParcelableExtraCompat(EXTRA_PASSKEY_ELEMENT)
    }

    fun Intent.removePasskey() {
        return this.removeExtra(EXTRA_PASSKEY_ELEMENT)
    }

    fun Intent.addSearchInfo(searchInfo: SearchInfo?) {
        searchInfo?.let {
            putExtra(EXTRA_SEARCH_INFO, searchInfo)
        }
    }

    fun Intent.retrieveSearchInfo(): SearchInfo? {
        return this.getParcelableExtraCompat(EXTRA_SEARCH_INFO)
    }

    fun Intent.addNodeId(nodeId: UUID?) {
        nodeId?.let {
            putExtra(EXTRA_NODE_ID, ParcelUuid(nodeId))
        }
    }

    fun Intent.retrieveNodeId(): UUID? {
        return getParcelableExtraCompat<ParcelUuid>(EXTRA_NODE_ID)?.uuid
    }

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

    private fun generatedAuthenticationCode(nodeId: UUID?, timestamp: Long): ByteArray {
        return generateAuthenticationCode(
            (nodeId?.toString() ?: PLACEHOLDER_FOR_NEW_NODE_ID) + SEPARATOR + timestamp.toString()
        )
    }

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

    private fun generateAuthenticationCode(message: String): ByteArray {
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
        keyStore.load(null)
        val hmacKey = try {
            keyStore.getKey(NAME_OF_HMAC_KEY, null) as SecretKey
        } catch (e: Exception) {
            // key not found
            generateKey()
        }

        val mac = Mac.getInstance(HMAC_TYPE)
        mac.init(hmacKey)
        val authenticationCode = mac.doFinal(message.toByteArray())
        return authenticationCode
    }

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

    private fun String.decodeHexToByteArray(): ByteArray {
        if (length % 2 != 0) {
            throw IllegalArgumentException("Must have an even length")
        }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

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

    fun retrievePasskeyCreationRequestParameters(
        intent: Intent,
        assetManager: AssetManager,
        packageName: String?,
        passkeyCreated: (Passkey, PublicKeyCredentialCreationParameters) -> Unit
    ) {
        val getCredentialRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        val callingAppInfo = getCredentialRequest?.callingAppInfo
        val createCredentialRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        if (createCredentialRequest == null)
            throw CreateCredentialUnknownException("could not retrieve request from intent")
        val creationOptions = createCredentialRequest.retrievePasskeyCreationComponent()

        val relyingParty = creationOptions.relyingPartyEntity.id
        val username = creationOptions.userEntity.name
        val userHandle = creationOptions.userEntity.id
        val pubKeyCredParams = creationOptions.pubKeyCredParams
        val clientDataHash = creationOptions.clientDataHash

        val originManager = OriginManager(callingAppInfo, assetManager, relyingParty)
        originManager.checkPrivilegedApp(clientDataHash)

        val credentialId = KeePassDXRandom.generateCredentialId()

        val (keyPair, keyTypeId) = Signature.generateKeyPair(
            pubKeyCredParams.map { params -> params.alg }
        ) ?: throw CreateCredentialUnknownException("no known public key type found")
        val privateKeyPem = Signature.convertPrivateKeyToPem(keyPair.private)

        // create new entry in database
        passkeyCreated.invoke(
            Passkey(
                username = username,
                privateKeyPem = privateKeyPem,
                credentialId = b64Encode(credentialId),
                userHandle = b64Encode(userHandle),
                relyingParty = relyingParty
            ),
            PublicKeyCredentialCreationParameters(
                publicKeyCredentialCreationOptions = creationOptions,
                credentialId = credentialId,
                signatureKey = Pair(keyPair, keyTypeId),
                clientDataResponse = clientDataHash?.let {
                    ClientDataDefinedResponse(clientDataHash)
                } ?: run {
                    ClientDataNotDefinedResponse(
                        type = ClientDataNotDefinedResponse.Type.CREATE,
                        challenge = creationOptions.challenge,
                        origin = originManager.origin,
                        packageName = packageName
                    )
                }
            )
        )
    }

    fun buildCreatePublicKeyCredentialResponse(
        publicKeyCredentialCreationParameters: PublicKeyCredentialCreationParameters
    ): CreatePublicKeyCredentialResponse {

        val keyPair = publicKeyCredentialCreationParameters.signatureKey.first
        val keyTypeId = publicKeyCredentialCreationParameters.signatureKey.second
        val responseJson = FidoPublicKeyCredential(
            id = b64Encode(publicKeyCredentialCreationParameters.credentialId),
            response = AuthenticatorAttestationResponse(
                requestOptions = publicKeyCredentialCreationParameters.publicKeyCredentialCreationOptions,
                credentialId = publicKeyCredentialCreationParameters.credentialId,
                credentialPublicKey = Cbor().encode(Signature.convertPublicKeyToMap(
                    publicKeyIn = keyPair.public,
                    keyTypeId = keyTypeId
                ) ?: mapOf<Int, Any>()),
                userPresent = true,
                userVerified = true,
                backupEligibility = true,
                backupState = true,
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

    fun retrievePasskeyUsageRequestParameters(
        context: Context,
        intent: Intent,
        result: (PublicKeyCredentialUsageParameters) -> Unit
    ) {
        val getCredentialRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        if (getCredentialRequest == null)
            throw CreateCredentialUnknownException("could not retrieve request from intent")
        val callingAppInfo = getCredentialRequest.callingAppInfo
        val credentialOption = getCredentialRequest.retrievePasskeyUsageComponent()
        val clientDataHash = credentialOption.clientDataHash

        val requestOptions = PublicKeyCredentialRequestOptions(credentialOption.requestJson)
        val relyingParty = requestOptions.rpId

        val originManager = OriginManager(callingAppInfo, context.assets, relyingParty)
        originManager.checkPrivilegedApp(clientDataHash)

        result.invoke(
            PublicKeyCredentialUsageParameters(
                publicKeyCredentialRequestOptions = requestOptions,
                clientDataResponse = clientDataHash?.let {
                    ClientDataDefinedResponse(clientDataHash)
                } ?: run {
                    ClientDataNotDefinedResponse(
                        type = ClientDataNotDefinedResponse.Type.GET,
                        challenge = requestOptions.challenge,
                        origin = originManager.origin,
                        packageName = callingAppInfo.packageName
                    )
                }
            )
        )
    }

    fun buildPasskeyPublicKeyCredential(
        usageParameters: PublicKeyCredentialUsageParameters,
        passkey: Passkey
    ): PublicKeyCredential {
        val getCredentialResponse = FidoPublicKeyCredential(
            id = passkey.credentialId,
            response = AuthenticatorAssertionResponse(
                requestOptions = usageParameters.publicKeyCredentialRequestOptions,
                userPresent = true,
                userVerified = true,
                backupEligibility = true,
                backupState = true,
                userHandle = passkey.userHandle,
                privateKey = passkey.privateKeyPem,
                clientDataResponse = usageParameters.clientDataResponse
            ),
            authenticatorAttachment = "platform"
        ).json()
        Log.d(javaClass.simpleName, "Json response for key usage")
        return PublicKeyCredential(getCredentialResponse)
    }

}