package com.kunzisoft.keepass.credentialprovider.data

import com.kunzisoft.keepass.database.element.Entry

data class Passkey(
    val nodeId: String,
    val username: String,
    val displayName: String,
    val privateKeyPem: String,
    val credId: String,
    val userHandle: String,
    val relyingParty: String,
    val databaseEntry: Entry?
)
