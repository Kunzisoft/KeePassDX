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

data class PublicKeyCredentialRpEntity(val name: String, val id: String)

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
}

data class PublicKeyCredentialParameters(val type: String, val alg: Long)

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
}

data class AuthenticatorSelectionCriteria(
    val authenticatorAttachment: String,
    val residentKey: String,
    val requireResidentKey: Boolean = false,
    val userVerification: String = "preferred"
)
