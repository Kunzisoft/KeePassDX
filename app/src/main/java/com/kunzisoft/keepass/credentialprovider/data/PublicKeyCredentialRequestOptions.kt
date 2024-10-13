package com.kunzisoft.keepass.credentialprovider.data

data class PublicKeyCredentialRequestOptions(
    val relyingParty: String,
    val challengeString: String
) {
}