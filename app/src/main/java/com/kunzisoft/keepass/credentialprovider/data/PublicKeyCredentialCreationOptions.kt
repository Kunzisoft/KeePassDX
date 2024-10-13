package com.kunzisoft.keepass.credentialprovider.data

data class PublicKeyCredentialCreationOptions(
    val relyingParty: String,
    val challenge: ByteArray,
    val username: String,
    val userId: ByteArray,
    val keyTypeIdList: List<Long>
)