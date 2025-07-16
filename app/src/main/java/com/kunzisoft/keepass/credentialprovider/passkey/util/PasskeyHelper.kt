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
import com.kunzisoft.asymmetric.Signature
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialCreationOptions
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialCreationParameters
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialUsageParameters
import com.kunzisoft.keepass.credentialprovider.passkey.util.OriginHelper.Companion.DEFAULT_PROTOCOL
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.EntryInfoPasskey.getPasskey
import com.kunzisoft.keepass.model.Passkey
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

    private const val KEY_NODE_ID = "nodeId"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_AUTHENTICATION_CODE = "authenticationCode"

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
            entryInfo.getPasskey()?.let {
                val mReplyIntent = Intent()
                Log.d(javaClass.name, "Success Passkey manual selection")
                mReplyIntent.putExtra(EXTRA_PASSKEY_ELEMENT, entryInfo.getPasskey())
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
        passkeyEntryNodeId?.let {
            putExtras(Bundle().apply {
                val timestamp = Instant.now().epochSecond
                putParcelable(KEY_NODE_ID, ParcelUuid(passkeyEntryNodeId))
                putString(KEY_TIMESTAMP, timestamp.toString())
                putString(
                    KEY_AUTHENTICATION_CODE, generatedAuthenticationCode(
                        passkeyEntryNodeId, timestamp
                    ).toHexString()
                )
            })
        }
    }

    fun Intent.retrievePasskey(): Passkey? {
        return this.getParcelableExtraCompat(EXTRA_PASSKEY_ELEMENT)
    }

    fun Intent.removePasskey() {
        return this.removeExtra(EXTRA_PASSKEY_ELEMENT)
    }

    fun Intent.retrieveNodeId(): UUID? {
        return getParcelableExtraCompat<ParcelUuid>(KEY_NODE_ID)?.uuid
    }

    fun checkSecurity(intent: Intent, nodeId: UUID?) {
        val timestampString = intent.getStringExtra(KEY_TIMESTAMP)
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
            intent.getStringExtra(KEY_AUTHENTICATION_CODE),
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

    fun Intent.retrievePasskeyCreationComponent(): PublicKeyCredentialCreationOptions {
        val request = PendingIntentHandler.retrieveProviderCreateCredentialRequest(this)
            ?: throw CreateCredentialUnknownException("could not retrieve request from intent")
        if (request.callingRequest !is CreatePublicKeyCredentialRequest) {
            throw CreateCredentialUnknownException("callingRequest is of wrong type: ${request.callingRequest.type}")
        }
        return JsonHelper.parseJsonToCreateOptions(
            (request.callingRequest as CreatePublicKeyCredentialRequest).requestJson
        )
    }

    fun Intent.retrievePasskeyUsageComponent(): GetPublicKeyCredentialOption {
        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(this)
            ?: throw CreateCredentialUnknownException("could not retrieve request from intent")
        if (request.credentialOptions.size != 1) {
            throw GetCredentialUnknownException("not exact one credentialOption")
        }
        if (request.credentialOptions[0] !is GetPublicKeyCredentialOption) {
            throw CreateCredentialUnknownException("credentialOptions is of wrong type: ${request.credentialOptions[0]}")
        }
        return request.credentialOptions[0] as GetPublicKeyCredentialOption
    }

    fun retrievePasskeyCreationRequestParameters(
        creationOptions: PublicKeyCredentialCreationOptions,
        webOrigin: String?,
        apkSigningCertificate: ByteArray?,
        passkeyCreated: (Passkey, PublicKeyCredentialCreationParameters) -> Unit
    ) {
        val relyingParty = creationOptions.relyingParty
        val username = creationOptions.username
        val userHandle = creationOptions.userId
        val keyTypeIdList = creationOptions.keyTypeIdList
        val challenge = creationOptions.challenge

        val isPrivilegedApp =
            (webOrigin != null && webOrigin == DEFAULT_PROTOCOL + relyingParty)
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

        // create new entry in database
        passkeyCreated.invoke(
            Passkey(
                username = username,
                displayName = "$relyingParty (Passkey)",
                privateKeyPem = privateKeyPem,
                credentialId = Base64Helper.b64Encode(credentialId),
                userHandle = Base64Helper.b64Encode(userHandle),
                relyingParty = DEFAULT_PROTOCOL + relyingParty
            ),
            PublicKeyCredentialCreationParameters(
                relyingParty = relyingParty,
                challenge = challenge,
                credentialId = credentialId,
                signatureKey = Pair(keyPair, keyTypeId),
                isPrivilegedApp = isPrivilegedApp
            )
        )
    }

    fun buildCreatePublicKeyCredentialResponse(
        packageName: String?,
        publicKeyCredentialCreationParameters: PublicKeyCredentialCreationParameters
    ): CreatePublicKeyCredentialResponse {

        val keyPair = publicKeyCredentialCreationParameters.signatureKey.first
        val keyTypeId = publicKeyCredentialCreationParameters.signatureKey.second

        val publicKeyEncoded = Signature.convertPublicKey(keyPair.public, keyTypeId)
        val publicKeyMap = Signature.convertPublicKeyToMap(keyPair.public, keyTypeId)

        val authData = JsonHelper.generateAuthDataForCreate(
            userPresent = true,
            userVerified = true,
            backupEligibility = true,
            backupState = true,
            rpId = publicKeyCredentialCreationParameters.relyingParty.toByteArray(),
            credentialId = publicKeyCredentialCreationParameters.credentialId,
            credentialPublicKey = JsonHelper.generateCborFromMap(publicKeyMap!!)
        )

        val attestationObject = JsonHelper.generateAttestationObject(authData)

        val clientJson: String
        if (publicKeyCredentialCreationParameters.isPrivilegedApp) {
            clientJson = JsonHelper.generateClientDataJsonPrivileged()
        } else {
            val origin = DEFAULT_PROTOCOL + publicKeyCredentialCreationParameters.relyingParty
            clientJson = JsonHelper.generateClientDataJsonNonPrivileged(
                publicKeyCredentialCreationParameters.challenge,
                origin,
                packageName,
                isCrossOriginAdded = true,
                isGet = false
            )
        }

        val responseJson = JsonHelper.createAuthenticatorAttestationResponseJSON(
            publicKeyCredentialCreationParameters.credentialId,
            clientJson,
            attestationObject,
            publicKeyEncoded!!,
            authData,
            keyTypeId
        )

        // log only the length to prevent logging sensitive information
        Log.d(javaClass.simpleName, "responseJson with length ${responseJson.length} created")
        return CreatePublicKeyCredentialResponse(responseJson)
    }

    fun retrievePasskeyUsageRequestParameters(
        context: Context,
        intent: Intent,
        result: (PublicKeyCredentialUsageParameters) -> Unit
    ) {
        val callingAppInfo = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)?.callingAppInfo
        val credentialOption = intent.retrievePasskeyUsageComponent()
        val clientDataHash = credentialOption.clientDataHash

        val requestOptions = JsonHelper.parseJsonToRequestOptions(credentialOption.requestJson)

        val relyingParty = requestOptions.relyingParty
        val challenge = Base64Helper.b64Decode(requestOptions.challengeString)
        val packageName = callingAppInfo?.packageName
        val webOrigin = OriginHelper.getWebOrigin(callingAppInfo, context.assets)

        val isPrivilegedApp =
            (webOrigin != null && webOrigin == DEFAULT_PROTOCOL + relyingParty && clientDataHash != null)

        Log.d(javaClass.simpleName, "isPrivilegedApp = $isPrivilegedApp")

        if (!isPrivilegedApp) {
            if (!AppRelyingPartyRelation.isRelationValid(
                    relyingParty,
                    apkSigningCertificate = callingAppInfo?.signingInfo?.apkContentsSigners
                        ?.getOrNull(0)?.toByteArray()
                )) {
                throw CreateCredentialUnknownException(
                    "could not verify relation between app " +
                            "and relyingParty $relyingParty"
                )
            }
        }

        result.invoke(
            PublicKeyCredentialUsageParameters(
                relyingParty = relyingParty,
                packageName = packageName,
                clientDataHash = clientDataHash,
                isPrivilegedApp = isPrivilegedApp,
                challenge = challenge
            )
        )
    }

    fun buildPasskeyPublicKeyCredential(
        usageParameters: PublicKeyCredentialUsageParameters,
        passkey: Passkey
    ): PublicKeyCredential {

        // https://www.w3.org/TR/webauthn-3/#authdata-flags
        val authenticatorData = JsonHelper.generateAuthDataForUsage(
            usageParameters.relyingParty.toByteArray(),
            userPresent = true,
            userVerified = true,
            backupEligibility = true,
            backupState = true
        )

        val clientDataJson: String
        val dataToSign: ByteArray
        if (usageParameters.isPrivilegedApp) {
            clientDataJson = JsonHelper.generateClientDataJsonPrivileged()
            dataToSign =
                JsonHelper.generateDataToSignPrivileged(usageParameters.clientDataHash!!, authenticatorData)
        } else {
            val origin = DEFAULT_PROTOCOL + usageParameters.relyingParty
            clientDataJson = JsonHelper.generateClientDataJsonNonPrivileged(
                usageParameters.challenge,
                origin,
                usageParameters.packageName,
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
                passkey.credentialId
            )
        return PublicKeyCredential(getCredentialResponse)
    }

}