package com.kunzisoft.keepass.model

import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.security.ProtectedString

object EntryInfoPasskey {

    // field names from KeypassXC are used
    private const val FIELD_USERNAME = "KPEX_PASSKEY_USERNAME"
    private const val FIELD_PRIVATE_KEY = "KPEX_PASSKEY_PRIVATE_KEY_PEM"
    private const val FIELD_CREDENTIAL_ID = "KPEX_PASSKEY_CREDENTIAL_ID"
    private const val FIELD_USER_HANDLE = "KPEX_PASSKEY_USER_HANDLE"
    const val FIELD_RELYING_PARTY = "KPEX_PASSKEY_RELYING_PARTY"

    private const val PASSKEY_TAG = "Passkey"

    fun EntryInfo.getPasskey(): Passkey? {
        if (this.tags.toList().contains(PASSKEY_TAG).not()) {
            return null
        }
        var username = ""
        var privateKeyPem = ""
        var credId = ""
        var userHandle = ""
        var relyingParty = ""
        for (field in this.customFields) {
            when (field.name) {
                FIELD_USERNAME -> {
                    username = field.protectedValue.stringValue
                }
                FIELD_PRIVATE_KEY -> {
                    privateKeyPem = field.protectedValue.stringValue
                }
                FIELD_CREDENTIAL_ID -> {
                    credId = field.protectedValue.stringValue
                }
                FIELD_USER_HANDLE -> {
                    userHandle = field.protectedValue.stringValue
                }
                FIELD_RELYING_PARTY -> {
                    relyingParty = field.protectedValue.stringValue
                }
            }
        }
        return Passkey(
            username = username,
            displayName = this.getVisualTitle(),
            privateKeyPem = privateKeyPem,
            credentialId = credId,
            userHandle = userHandle,
            relyingParty = relyingParty
        )
    }

    fun EntryInfo.setPasskey(passkey: Passkey) {
        tags.put(PASSKEY_TAG)
        title = passkey.displayName
        username = passkey.username
        url = passkey.relyingParty
        addOrReplaceField(
            Field(
                FIELD_USERNAME,
                ProtectedString(enableProtection = false, passkey.username)
            )
        )
        addOrReplaceField(
            Field(
                FIELD_PRIVATE_KEY,
                ProtectedString(enableProtection = true, passkey.privateKeyPem)
            )
        )
        addOrReplaceField(
            Field(
                FIELD_CREDENTIAL_ID,
                ProtectedString(enableProtection = true, passkey.credentialId)
            )
        )
        addOrReplaceField(
            Field(
                FIELD_USER_HANDLE,
                ProtectedString(enableProtection = true, passkey.userHandle)
            )
        )
        addOrReplaceField(
            Field(
                FIELD_RELYING_PARTY,
                ProtectedString(enableProtection = false, passkey.relyingParty)
            )
        )
    }
}