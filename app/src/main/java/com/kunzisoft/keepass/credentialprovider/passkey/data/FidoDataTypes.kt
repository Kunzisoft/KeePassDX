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
import kotlin.jvm.java

data class PublicKeyCredentialRpEntity(
    val name: String,
    val id: String
) {
    companion object {
        fun JSONObject.getPublicKeyCredentialRpEntity(
            parameterName: String
        ): PublicKeyCredentialRpEntity {
            val rpJson = this.getJSONObject(parameterName)
            return PublicKeyCredentialRpEntity(
                rpJson.getString("name"),
                rpJson.getString("id")
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
        fun JSONObject.getPublicKeyCredentialUserEntity(
            parameterName: String
        ): PublicKeyCredentialUserEntity {
            val rpUser = this.getJSONObject(parameterName)
            return PublicKeyCredentialUserEntity(
                rpUser.getString("name"),
                Base64Helper.b64Decode(rpUser.getString("id")),
                rpUser.getString("displayName")
            )
        }
    }
}

data class PublicKeyCredentialParameters(
    val type: String,
    val alg: Long
) {
    companion object {
        fun JSONObject.getPublicKeyCredentialParametersList(
            parameterName: String
        ): List<PublicKeyCredentialParameters> {
            val pubKeyCredParamsJson = this.getJSONArray(parameterName)
            val pubKeyCredParamsTmp: MutableList<PublicKeyCredentialParameters> = mutableListOf()
            for (i in 0 until pubKeyCredParamsJson.length()) {
                val e = pubKeyCredParamsJson.getJSONObject(i)
                pubKeyCredParamsTmp.add(
                    PublicKeyCredentialParameters(e.getString("type"), e.getLong("alg"))
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
        fun JSONObject.getPublicKeyCredentialDescriptorList(
            parameterName: String
        ): List<PublicKeyCredentialDescriptor> {
            val credentialsTmp: MutableList<PublicKeyCredentialDescriptor> = mutableListOf()
            try {
                val credentialsJson = this.getJSONArray(parameterName)
                for (i in 0 until credentialsJson.length()) {
                    val credentialJson = credentialsJson.getJSONObject(i)

                    val transports: MutableList<String> = mutableListOf()
                    val transportsJson = credentialJson.getJSONArray("transports")
                    for (j in 0 until transportsJson.length()) {
                        transports.add(transportsJson.getString(j))
                    }
                    credentialsTmp.add(
                        PublicKeyCredentialDescriptor(
                            type = credentialJson.getString("type"),
                            id = Base64Helper.b64Decode(credentialJson.getString("id")),
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
        fun JSONObject.getAuthenticatorSelectionCriteria(
            parameterName: String
        ): AuthenticatorSelectionCriteria {
            val authenticatorSelection = this.optJSONObject(parameterName)
                ?: return AuthenticatorSelectionCriteria(requireResidentKey = null)
            val authenticatorAttachment = if (!authenticatorSelection.isNull("authenticatorAttachment"))
                authenticatorSelection.getString("authenticatorAttachment") else null
            var residentKey = if (!authenticatorSelection.isNull("residentKey"))
                    ResidentKeyRequirement.fromString(authenticatorSelection.getString("residentKey"))
                else null
            val requireResidentKey = authenticatorSelection.optBoolean("requireResidentKey", false)
            val userVerification = UserVerificationRequirement
                .fromString(authenticatorSelection.optString("userVerification", "preferred"))
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