package com.kunzisoft.keepass.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Passkey(
    val username: String,
    val privateKeyPem: String,
    val credentialId: String,
    val userHandle: String,
    val relyingParty: String
): Parcelable
