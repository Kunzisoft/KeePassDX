package com.kunzisoft.keepass.credentialprovider.passkey.util

import android.annotation.SuppressLint
import androidx.credentials.webauthn.Cbor
import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialCreationOptions
import com.kunzisoft.keepass.credentialprovider.passkey.data.PublicKeyCredentialRequestOptions
import com.kunzisoft.keepass.credentialprovider.passkey.util.Base64Helper.Companion.b64Decode
import com.kunzisoft.keepass.credentialprovider.passkey.util.Base64Helper.Companion.b64Encode
import org.json.JSONArray
import org.json.JSONObject

class JsonHelper {

    companion object {

        fun generateClientDataJsonNonPrivileged(
            challenge: ByteArray,
            origin: String,
            packageName: String?,
            isGet: Boolean,
            isCrossOriginAdded: Boolean
        ): String {
            val clientJson = JSONObject()
            val type = if (isGet) {
                "webauthn.get"
            } else {
                "webauthn.create"
            }
            clientJson.put("type", type)
            clientJson.put("challenge", b64Encode(challenge))
            clientJson.put("origin", origin)

            if (isCrossOriginAdded) {
                clientJson.put("crossOrigin", false)
            }

            if (packageName != null) {
                clientJson.put("androidPackageName", packageName)
            }

            val clientDataFinal = clientJson.toString().replace("\\/", "/")
            return clientDataFinal
        }

        fun generateClientDataJsonPrivileged(): String {
            // will be replaced by the clientData from the privileged app like a browser
            return "<placeholder>"
        }

        fun generateAuthDataForUsage(
            rpId: ByteArray,
            userPresent: Boolean,
            userVerified: Boolean,
            backupEligibility: Boolean,
            backupState: Boolean,
            attestedCredentialData: Boolean = false
        ): ByteArray {
            val rpHash = HashManager.hashSha256(rpId)

            // see https://www.w3.org/TR/webauthn-3/#table-authData
            var flags = 0
            val one = 1
            if (userPresent) {
                flags = flags or one.shl(0)
            }
            // bit at index 1 is reserved

            if (userVerified) {
                flags = flags or one.shl(2)
            }
            if (backupEligibility) {
                flags = flags or one.shl(3)
            }
            if (backupState) {
                flags = flags or one.shl(4)
            }

            // bit at index 5 is reserved

            if (attestedCredentialData) {
                flags = flags or one.shl(6)
            }

            // bit at index 7: Extension data included == false

            val signCount = byteArrayOf(0, 0, 0, 0)

            return rpHash + byteArrayOf(flags.toByte()) + signCount
        }

        fun generateAuthDataForCreate(
            rpId: ByteArray,
            userPresent: Boolean,
            userVerified: Boolean,
            backupEligibility: Boolean,
            backupState: Boolean,
            credentialId: ByteArray,
            credentialPublicKey: ByteArray
        ): ByteArray {
            val authDataPartOne = generateAuthDataForUsage(
                rpId,
                userPresent,
                userVerified,
                backupEligibility,
                backupState,
                attestedCredentialData = true
            )

            // Authenticator Attestation Globally Unique Identifier
            val aaguid = ByteArray(16) { 0 }

            val credIdLen =
                byteArrayOf((credentialId.size.shr(8)).toByte(), credentialId.size.toByte())

            return authDataPartOne + aaguid + credIdLen + credentialId + credentialPublicKey
        }

        fun generateDataTosSignNonPrivileged(
            clientDataJson: String,
            authenticatorData: ByteArray
        ): ByteArray {
            val hash = HashManager.hashSha256(clientDataJson.toByteArray())
            return authenticatorData + hash
        }

        fun generateDataToSignPrivileged(
            clientDataHash: ByteArray,
            authenticatorData: ByteArray
        ): ByteArray {
            return authenticatorData + clientDataHash
        }

        fun generateAttestationObject(authData: ByteArray): ByteArray {
            val ao = mutableMapOf<String, Any>()
            ao["fmt"] = "none"
            ao["attStmt"] = emptyMap<Any, Any>()
            ao["authData"] = authData
            return generateCborFromMap(ao)
        }

        @SuppressLint("RestrictedApi")
        fun <T> generateCborFromMap(map: Map<T, Any>): ByteArray {
            return Cbor().encode(map)
        }

        fun createAuthenticatorAttestationResponseJSON(
            credentialId: ByteArray,
            clientDataJson: String,
            attestationObject: ByteArray,
            publicKeyCbor: ByteArray,
            authData: ByteArray,
            publicKeyTypeId: Long
        ): String {
            // See AuthenticatorAttestationResponseJSON at
            // https://www.w3.org/TR/webauthn-3/#ref-for-dom-publickeycredential-tojson

            val rk = JSONObject()

            // see at https://www.w3.org/TR/webauthn-3/#sctn-authenticator-credential-properties-extension
            val discoverableCredential = true
            rk.put("rk", discoverableCredential)
            val credProps = JSONObject()
            credProps.put("credProps", rk)


            val response = JSONObject()
            response.put("attestationObject", b64Encode(attestationObject))
            response.put("clientDataJSON", clientDataJson)
            response.put("transports", JSONArray(listOf("internal", "hybrid")))
            response.put("publicKeyAlgorithm", publicKeyTypeId)
            response.put("publicKey", b64Encode(publicKeyCbor))
            response.put("authenticatorData", b64Encode(authData))

            val all = JSONObject()
            all.put("id", b64Encode(credentialId))
            all.put("rawId", b64Encode(credentialId))
            all.put("response", response)
            all.put("type", "public-key")
            all.put("clientExtensionResults", credProps)
            all.put("authenticatorAttachment", "platform")
            return all.toString()
        }

        fun generateGetCredentialResponse(
            clientDataJson: ByteArray,
            authenticatorData: ByteArray,
            signature: ByteArray,
            userHandle: String,
            id: String
        ): String {

            val response = JSONObject()
            response.put("clientDataJSON", b64Encode(clientDataJson))
            response.put("authenticatorData", b64Encode(authenticatorData))
            response.put("signature", b64Encode(signature))
            response.put("userHandle", userHandle)

            val ret = JSONObject()
            ret.put("id", id)
            ret.put("rawId", id)
            ret.put("type", "public-key")
            ret.put("authenticatorAttachment", "platform")
            ret.put("response", response)
            ret.put("clientExtensionResults", JSONObject())

            return ret.toString()
        }

        fun parseJsonToRequestOptions(requestJson: String): PublicKeyCredentialRequestOptions {
            val jsonObject = JSONObject(requestJson)

            val challengeString = jsonObject.getString("challenge")
            val relyingParty = jsonObject.optString("rpId", "")

            return PublicKeyCredentialRequestOptions(relyingParty, challengeString)
        }

        fun parseJsonToCreateOptions(requestJson: String): PublicKeyCredentialCreationOptions {
            val jsonObject = JSONObject(requestJson)
            val rpJson = jsonObject.getJSONObject("rp")
            val relyingParty = rpJson.getString("id")

            val challenge = b64Decode(jsonObject.getString("challenge"))

            val rpUser = jsonObject.getJSONObject("user")
            val username = rpUser.getString("name")
            val userId = b64Decode(rpUser.getString("id"))


            val pubKeyCredParamsJson = jsonObject.getJSONArray("pubKeyCredParams")
            val keyTypeIdList: MutableList<Long> = mutableListOf()
            for (i in 0 until pubKeyCredParamsJson.length()) {
                val e = pubKeyCredParamsJson.getJSONObject(i)
                if (e.getString("type") == "public-key") {
                    keyTypeIdList.add(e.getLong("alg"))
                }
            }

            return PublicKeyCredentialCreationOptions(
                relyingParty,
                challenge,
                username,
                userId,
                keyTypeIdList.distinct()
            )
        }
    }
}