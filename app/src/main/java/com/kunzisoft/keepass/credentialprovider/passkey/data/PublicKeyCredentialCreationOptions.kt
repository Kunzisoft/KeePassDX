package com.kunzisoft.keepass.credentialprovider.passkey.data

data class PublicKeyCredentialCreationOptions(
    val relyingParty: String,
    val challenge: ByteArray, // TODO Equals Hashcode
    val username: String,
    val userId: ByteArray, // TODO Equals Hashcode
    val keyTypeIdList: List<Long>
)