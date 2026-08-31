/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kunzisoft.keepass.credentialprovider.passkey.data

import android.util.Log
import com.kunzisoft.encrypt.Base64Helper
import org.json.JSONException
import org.json.JSONObject

data class PublicKeyCredentialRpEntity(
    val name: String,
    val id: String
) {
    companion object {
        private const val NAME = "name"
        private const val ID = "id"

        fun JSONObject.getPublicKeyCredentialRpEntity(
            parameterName: String
        ): PublicKeyCredentialRpEntity {
            val rpJson = this.getJSONObject(parameterName)
            return PublicKeyCredentialRpEntity(
                rpJson.getString(NAME),
                rpJson.getString(ID)
            )
        }
    }
}

data class PublicKeyCredentialUserEntity(
    val name: String,
    val id: ByteArray,
    val displayName: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PublicKeyCredentialUserEntity

        if (name != other.name) return false
        if (!id.contentEquals(other.id)) return false
        if (displayName != other.displayName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + id.contentHashCode()
        result = 31 * result + displayName.hashCode()
        return result
    }

    companion object {
        private const val NAME = "name"
        private const val ID = "id"
        private const val DISPLAY_NAME = "displayName"

        fun JSONObject.getPublicKeyCredentialUserEntity(
            parameterName: String
        ): PublicKeyCredentialUserEntity {
            val rpUser = this.getJSONObject(parameterName)
            return PublicKeyCredentialUserEntity(
                rpUser.getString(NAME),
                Base64Helper.b64Decode(rpUser.getString(ID)),
                rpUser.getString(DISPLAY_NAME)
            )
        }
    }
}

data class PublicKeyCredentialParameters(
    val type: String,
    val alg: Long
) {
    companion object {
        private const val TYPE = "type"
        private const val ALG = "alg"

        fun JSONObject.getPublicKeyCredentialParametersList(
            parameterName: String
        ): List<PublicKeyCredentialParameters> {
            val pubKeyCredParamsJson = this.getJSONArray(parameterName)
            val pubKeyCredParamsTmp: MutableList<PublicKeyCredentialParameters> = mutableListOf()
            for (i in 0 until pubKeyCredParamsJson.length()) {
                val e = pubKeyCredParamsJson.getJSONObject(i)
                pubKeyCredParamsTmp.add(
                    PublicKeyCredentialParameters(e.getString(TYPE), e.getLong(ALG))
                )
            }
            return pubKeyCredParamsTmp.toList()
        }
    }
}

data class PublicKeyCredentialDescriptor(
    val type: String,
    val id: ByteArray,
    val transports: List<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PublicKeyCredentialDescriptor

        if (type != other.type) return false
        if (!id.contentEquals(other.id)) return false
        if (transports != other.transports) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + id.contentHashCode()
        result = 31 * result + transports.hashCode()
        return result
    }

    companion object {
        private const val TYPE = "type"
        private const val ID = "id"
        private const val TRANSPORTS = "transports"

        fun JSONObject.getPublicKeyCredentialDescriptorList(
            parameterName: String
        ): List<PublicKeyCredentialDescriptor> {
            val credentialsTmp: MutableList<PublicKeyCredentialDescriptor> = mutableListOf()
            try {
                val credentialsJson = this.optJSONArray(parameterName) ?: return emptyList()
                for (i in 0 until credentialsJson.length()) {
                    val credentialJson = credentialsJson.getJSONObject(i)

                    val transports: MutableList<String> = mutableListOf()
                    credentialJson.optJSONArray(TRANSPORTS)?.let { transportsJson ->
                        for (j in 0 until transportsJson.length()) {
                            transports.add(transportsJson.getString(j))
                        }
                    }
                    credentialsTmp.add(
                        PublicKeyCredentialDescriptor(
                            type = credentialJson.getString(TYPE),
                            id = Base64Helper.b64Decode(credentialJson.getString(ID)),
                            transports = transports
                        )
                    )
                }
            } catch (e: JSONException) {
                Log.w(
                    PublicKeyCredentialDescriptor::class.java.simpleName,
                    "Unable to parse PublicKeyCredentialDescriptor",
                    e
                )
            }
            return credentialsTmp.toList()
        }
    }
}

// https://www.w3.org/TR/webauthn-3/#dictdef-authenticatorselectioncriteria
data class AuthenticatorSelectionCriteria(
    val authenticatorAttachment: String? = null,
    val residentKey: ResidentKeyRequirement? = null,
    val requireResidentKey: Boolean?,
    val userVerification: UserVerificationRequirement = UserVerificationRequirement.PREFERRED
) {
    companion object {
        private const val AUTHENTICATION_ATTACHMENTS = "authenticatorAttachment"
        private const val RESIDENT_KEY = "residentKey"
        private const val REQUIRE_RESIDENT_KEY = "requireResidentKey"
        private const val USER_VERIFICATION = "userVerification"

        fun JSONObject.getAuthenticatorSelectionCriteria(
            parameterName: String
        ): AuthenticatorSelectionCriteria {
            val authenticatorSelection = this.optJSONObject(parameterName)
                ?: return AuthenticatorSelectionCriteria(requireResidentKey = null)
            val authenticatorAttachment = if (!authenticatorSelection.isNull(AUTHENTICATION_ATTACHMENTS))
                authenticatorSelection.getString(AUTHENTICATION_ATTACHMENTS) else null
            var residentKey = if (!authenticatorSelection.isNull(RESIDENT_KEY))
                    ResidentKeyRequirement.fromString(authenticatorSelection.getString(RESIDENT_KEY))
                else null
            val requireResidentKey = authenticatorSelection.optBoolean(REQUIRE_RESIDENT_KEY, false)
            val userVerification = UserVerificationRequirement
                .fromString(authenticatorSelection.optString(
                    USER_VERIFICATION,
                    UserVerificationRequirement.PREFERRED.value
                ))
                ?: UserVerificationRequirement.PREFERRED
            // https://www.w3.org/TR/webauthn-3/#enumdef-residentkeyrequirement
            if (residentKey == null) {
                residentKey = if (requireResidentKey) {
                    ResidentKeyRequirement.REQUIRED
                } else {
                    ResidentKeyRequirement.DISCOURAGED
                }
            }
            return AuthenticatorSelectionCriteria(
                authenticatorAttachment = authenticatorAttachment,
                residentKey = residentKey,
                requireResidentKey = requireResidentKey,
                userVerification = userVerification
            )
        }
    }
}

// https://www.w3.org/TR/webauthn-3/#prf-extension
data class AuthenticationExtensionsPRFValues(
    val first: ByteArray,
    val second: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuthenticationExtensionsPRFValues

        if (!first.contentEquals(other.first)) return false
        if (second != null) {
            if (other.second == null) return false
            if (!second.contentEquals(other.second)) return false
        } else if (other.second != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = first.contentHashCode()
        result = 31 * result + (second?.contentHashCode() ?: 0)
        return result
    }

    fun toJSON(): JSONObject {
        val json = JSONObject()
        json.put(FIRST, Base64Helper.b64Encode(first))
        second?.let { json.put(SECOND, Base64Helper.b64Encode(it)) }
        return json
    }

    fun toAuthDataCbor(): Map<Int, Any> {
        val map = mutableMapOf<Int, Any>()
        map[1] = first
        second?.let { map[2] = it }
        return map
    }

    companion object {
        private const val FIRST = "first"
        private const val SECOND = "second"

        fun JSONObject.getAuthenticationExtensionsPRFValues(): AuthenticationExtensionsPRFValues {
            return AuthenticationExtensionsPRFValues(
                Base64Helper.b64Decode(getString(FIRST)),
                if (has(SECOND)) Base64Helper.b64Decode(getString(SECOND)) else null
            )
        }
    }
}

data class AuthenticationExtensionsPRFInputs(
    val eval: AuthenticationExtensionsPRFValues? = null,
    val evalByCredential: Map<String, AuthenticationExtensionsPRFValues>? = null
) {
    companion object {
        private const val EVAL = "eval"
        private const val EVAL_BY_CREDENTIAL = "evalByCredential"

        fun JSONObject.getAuthenticationExtensionsPRFInputs(): AuthenticationExtensionsPRFInputs {
            val eval = if (has(EVAL)) getJSONObject(EVAL).let {
                AuthenticationExtensionsPRFValues.run { it.getAuthenticationExtensionsPRFValues() }
            } else null
            val evalByCredentialJson = optJSONObject(EVAL_BY_CREDENTIAL)
            val evalByCredential = evalByCredentialJson?.let {
                val map = mutableMapOf<String, AuthenticationExtensionsPRFValues>()
                it.keys().forEach { key ->
                    map[key] = AuthenticationExtensionsPRFValues.run {
                        it.getJSONObject(key).getAuthenticationExtensionsPRFValues()
                    }
                }
                map.toMap()
            }
            return AuthenticationExtensionsPRFInputs(eval, evalByCredential)
        }
    }
}

data class AuthenticationExtensionsClientInputs(
    val prf: AuthenticationExtensionsPRFInputs? = null
) {
    companion object {
        private const val PRF = "prf"

        fun JSONObject.getAuthenticationExtensionsClientInputs(
            parameterName: String
        ): AuthenticationExtensionsClientInputs {
            val extensionsJson = optJSONObject(parameterName) ?: return AuthenticationExtensionsClientInputs()
            val prfJson = extensionsJson.optJSONObject(PRF)
            val prf = if (prfJson != null) {
                AuthenticationExtensionsPRFInputs.run { prfJson.getAuthenticationExtensionsPRFInputs() }
            } else if (extensionsJson.has(PRF)) {
                AuthenticationExtensionsPRFInputs()
            } else null
            return AuthenticationExtensionsClientInputs(prf)
        }
    }
}

data class AuthenticationExtensionsPRFOutputs(
    val enabled: Boolean? = null,
    val results: AuthenticationExtensionsPRFValues? = null
) {
    fun toJSON(isRegistration: Boolean): JSONObject {
        val json = JSONObject()
        results?.let { prfValues ->
            json.put(RESULTS, prfValues.toJSON())
        }
        if (isRegistration) {
            json.put(ENABLED, true)
        } else if (results == null) {
            enabled?.let { json.put(ENABLED, it) }
        }
        return json
    }

    fun toAuthDataCbor(isRegistration: Boolean): Map<Int, Any> {
        val map = mutableMapOf<Int, Any>()
        results?.let {
            map[2] = it.toAuthDataCbor() // results
        }
        if (isRegistration) {
            map[1] = true // enabled
        } else if (results == null) {
            enabled?.let { map[1] = it }
        }
        return map
    }

    companion object {
        private const val ENABLED = "enabled"
        private const val RESULTS = "results"
    }
}

data class AuthenticationExtensionsClientOutputs(
    val prf: AuthenticationExtensionsPRFOutputs? = null
) {
    fun toJSON(isRegistration: Boolean): JSONObject {
        val json = JSONObject()
        prf?.let { json.put(PRF, it.toJSON(isRegistration)) }
        return json
    }

    fun toAuthDataCbor(isRegistration: Boolean): Map<String, Any>? {
        val prfAuthData = prf?.toAuthDataCbor(isRegistration) ?: return null
        val map = mutableMapOf<String, Any>()
        map[PRF] = prfAuthData
        return map
    }

    companion object {
        private const val PRF = "prf"
    }
}

// https://www.w3.org/TR/webauthn-3/#enumdef-residentkeyrequirement
enum class ResidentKeyRequirement(val value: String) {
    DISCOURAGED("discouraged"),
    PREFERRED("preferred"),
    REQUIRED("required");
    override fun toString(): String {
        return value
    }
    companion object {
        fun fromString(value: String): ResidentKeyRequirement? {
            return ResidentKeyRequirement.entries.firstOrNull {
                it.value.equals(other = value, ignoreCase = true)
            }
        }
    }
}

// https://www.w3.org/TR/webauthn-3/#enumdef-userverificationrequirement
enum class UserVerificationRequirement(val value: String) {
    REQUIRED("required"),
    PREFERRED("preferred"),
    DISCOURAGED("discouraged");
    override fun toString(): String {
        return value
    }
    companion object {
        fun fromString(value: String): UserVerificationRequirement? {
            return UserVerificationRequirement.entries.firstOrNull {
                it.value.equals(other = value, ignoreCase = true)
            }
        }
    }
}