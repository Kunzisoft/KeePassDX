package com.kunzisoft.keepass.credentialprovider.passkey.data

data class PublicKeyCredentialUsageParameters(
        val relyingParty: String,
        val packageName: String? = null,
        val clientDataHash: ByteArray?, // TODO Equals Hashcode
        val isPrivilegedApp: Boolean,
        val challenge: ByteArray, // TODO Equals Hashcode
)