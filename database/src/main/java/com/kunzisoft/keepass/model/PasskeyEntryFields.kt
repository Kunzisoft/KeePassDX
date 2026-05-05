package com.kunzisoft.keepass.model

import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.containsWhen
import com.kunzisoft.keepass.database.element.removeFirstWhen
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.security.ProtectedString.Companion.toBooleanCompat
import com.kunzisoft.keepass.database.element.security.ProtectedString.Companion.toFieldValue

object PasskeyEntryFields {

    // field names from KeypassXC are used
    const val FIELD_USERNAME = "KPEX_PASSKEY_USERNAME"

    const val FIELD_PRIVATE_KEY = "KPEX_PASSKEY_PRIVATE_KEY_PEM"
    const val FIELD_CREDENTIAL_ID = "KPEX_PASSKEY_CREDENTIAL_ID"
    const val FIELD_USER_HANDLE = "KPEX_PASSKEY_USER_HANDLE"
    const val FIELD_RELYING_PARTY = "KPEX_PASSKEY_RELYING_PARTY"
    const val FIELD_FLAG_BE = "KPEX_PASSKEY_FLAG_BE"
    const val FIELD_FLAG_BS = "KPEX_PASSKEY_FLAG_BS"

    const val PASSKEY_FIELD = "Passkey"
    const val PASSKEY_TAG = "Passkey"

    /**
     * Parse fields of an entry to retrieve a Passkey
     */
    fun parseFields(getField: (id: String) -> CharArray?): Passkey? {
        val usernameField = getField(FIELD_USERNAME)
        val privateKeyField = getField(FIELD_PRIVATE_KEY)
        val credentialIdField = getField(FIELD_CREDENTIAL_ID)
        val userHandleField = getField(FIELD_USER_HANDLE)
        val relyingPartyField = getField(FIELD_RELYING_PARTY)
        // Optional fields
        val backupEligibilityField = getField(FIELD_FLAG_BE)?.let { String(it).toBooleanCompat() }
        val backupStateField = getField(FIELD_FLAG_BS)?.let { String(it).toBooleanCompat() }
        if (usernameField == null
            || privateKeyField == null
            || credentialIdField == null
            || userHandleField == null
            || relyingPartyField == null)
            return null
        return Passkey(
            username = String(usernameField),
            privateKeyPem = privateKeyField,
            credentialId = String(credentialIdField),
            userHandle = String(userHandleField),
            relyingParty = String(relyingPartyField),
            backupEligibility = backupEligibilityField,
            backupState = backupStateField
        )
    }

    fun EntryInfo.containsPasskey(): Boolean {
        return this.tags.contains(PASSKEY_TAG)
                || this.containsCustomField(FIELD_USERNAME)
                || this.containsCustomField(FIELD_PRIVATE_KEY)
                || this.containsCustomField(FIELD_CREDENTIAL_ID)
                || this.containsCustomField(FIELD_USER_HANDLE)
                || this.containsCustomField(FIELD_RELYING_PARTY)
    }

    /**
     * Set a passkey in an entry,
     * return true if data has been overwritten
     */
    fun EntryInfo.setPasskey(passkey: Passkey?): Boolean {
        var overwrite = false
        if (passkey != null) {
            if (containsPasskey())
                overwrite = true
            tags.put(PASSKEY_TAG)
            if (this.username.isEmpty())
                this.username = passkey.username
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
            passkey.backupEligibility?.let { backupEligibility ->
                addOrReplaceField(
                    Field(
                        FIELD_FLAG_BE,
                        ProtectedString(enableProtection = false,
                            backupEligibility.toFieldValue())
                    )
                )
            }
            passkey.backupState?.let { backupState ->
                addOrReplaceField(
                    Field(
                        FIELD_FLAG_BS,
                        ProtectedString(enableProtection = false,
                            backupState.toFieldValue())
                    )
                )
            }
        }
        return overwrite
    }

    /**
     * Build new generated fields in a new list from [fieldsToParse] in parameter,
     * Remove parameters fields use to generate auto fields
     */
    fun generateAutoFields(fieldsToParse: List<Field>): MutableList<Field> {
        val newCustomFields: MutableList<Field> = fieldsToParse.toMutableList()
        // Remove parameter fields
        newCustomFields.removeFirstWhen(FIELD_USERNAME)
        newCustomFields.removeFirstWhen(FIELD_PRIVATE_KEY)
        newCustomFields.removeFirstWhen(FIELD_CREDENTIAL_ID)
        newCustomFields.removeFirstWhen(FIELD_USER_HANDLE)
        newCustomFields.removeFirstWhen(FIELD_RELYING_PARTY)
        newCustomFields.removeFirstWhen(FIELD_FLAG_BE)
        newCustomFields.removeFirstWhen(FIELD_FLAG_BS)
        // Empty auto generated Passkey field
        if (fieldsToParse.containsWhen(FIELD_USERNAME)
            || fieldsToParse.containsWhen(FIELD_PRIVATE_KEY)
            || fieldsToParse.containsWhen(FIELD_CREDENTIAL_ID)
            || fieldsToParse.containsWhen(FIELD_USER_HANDLE)
            || fieldsToParse.containsWhen(FIELD_RELYING_PARTY)
            || fieldsToParse.containsWhen(FIELD_FLAG_BE)
            || fieldsToParse.containsWhen(FIELD_FLAG_BS)
        )
            newCustomFields.add(
                Field(
                    name = PASSKEY_FIELD,
                    value = ProtectedString(enableProtection = false)
                )
            )
        return newCustomFields
    }

    /**
     * Detect if the current field is a Passkey
     */
    fun Field.isPasskey(): Boolean {
        return when(name) {
            PASSKEY_FIELD -> true
            FIELD_USERNAME -> true
            FIELD_PRIVATE_KEY -> true
            FIELD_CREDENTIAL_ID -> true
            FIELD_USER_HANDLE -> true
            FIELD_RELYING_PARTY -> true
            else -> false
        }
    }

    /**
     * Detect if the current field is a Passkey credential id
     */
    fun Field.isCredentialId(): Boolean {
        return name == FIELD_CREDENTIAL_ID
    }

    /**
     * Detect if the current field is a Passkey relying party
     */
    fun Field.isRelyingParty(): Boolean {
        return name == FIELD_RELYING_PARTY
    }
}