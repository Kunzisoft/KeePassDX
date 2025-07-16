package com.kunzisoft.keepass.credentialprovider.passkey.data

import java.security.KeyPair

data class PublicKeyCredentialCreationParameters(
        val relyingParty: String,
        val credentialId: ByteArray, // TODO Equals Hashcode
        val signatureKey: Pair<KeyPair, Long>,
        val isPrivilegedApp: Boolean,
        val challenge: ByteArray, // TODO Equals Hashcode
)