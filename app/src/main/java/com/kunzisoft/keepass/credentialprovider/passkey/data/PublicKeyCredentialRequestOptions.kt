package com.kunzisoft.keepass.credentialprovider.passkey.data

data class PublicKeyCredentialRequestOptions(
    val relyingParty: String,
    val challengeString: String
)