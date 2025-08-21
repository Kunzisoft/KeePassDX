package com.kunzisoft.keepass.model

import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.security.ProtectedString

object PasskeyEntryFields {

    // field names from KeypassXC are used
    const val FIELD_USERNAME = "KPEX_PASSKEY_USERNAME"

    const val FIELD_PRIVATE_KEY = "KPEX_PASSKEY_PRIVATE_KEY_PEM"
    const val FIELD_CREDENTIAL_ID = "KPEX_PASSKEY_CREDENTIAL_ID"
    const val FIELD_USER_HANDLE = "KPEX_PASSKEY_USER_HANDLE"
    const val FIELD_RELYING_PARTY = "KPEX_PASSKEY_RELYING_PARTY"

    const val PASSKEY_FIELD = "Passkey"
    const val PASSKEY_TAG = "Passkey"

    /**
     * Parse fields of an entry to retrieve a Passkey
     */
    fun parseFields(getField: (id: String) -> String?): Passkey? {
        val usernameField = getField(FIELD_USERNAME)
        val privateKeyField = getField(FIELD_PRIVATE_KEY)
        val credentialIdField = getField(FIELD_CREDENTIAL_ID)
        val userHandleField = getField(FIELD_USER_HANDLE)
        val relyingPartyField = getField(FIELD_RELYING_PARTY)
        if (usernameField == null
            || privateKeyField == null
            || credentialIdField == null
            || userHandleField == null
            || relyingPartyField == null)
            return null
        return Passkey(
            username = usernameField,
            privateKeyPem = privateKeyField,
            credentialId = credentialIdField,
            userHandle = userHandleField,
            relyingParty = relyingPartyField
        )
    }

    fun EntryInfo.setPasskey(passkey: Passkey) {
        tags.put(PASSKEY_TAG)
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

    /**
     * Build Passkey field from a Passkey
     */
    fun buildPasskeyField(passkey: Passkey): Field {
        return Field(
            name = PASSKEY_FIELD,
            value = ProtectedString(
                enableProtection = false,
                string = passkey.relyingParty
            )
        )
    }

    /**
     * Build new generated fields in a new list from [fieldsToParse] in parameter,
     * Remove parameters fields use to generate auto fields
     */
    fun generateAutoFields(fieldsToParse: List<Field>): MutableList<Field> {
        val newCustomFields: MutableList<Field> = ArrayList(fieldsToParse)
        // Remove parameter fields
        val usernameField = Field(FIELD_USERNAME)
        val privateKeyField = Field(FIELD_PRIVATE_KEY)
        val credentialIdField = Field(FIELD_CREDENTIAL_ID)
        val userHandleField = Field(FIELD_USER_HANDLE)
        val relyingPartyField = Field(FIELD_RELYING_PARTY)
        newCustomFields.remove(usernameField)
        newCustomFields.remove(privateKeyField)
        newCustomFields.remove(credentialIdField)
        newCustomFields.remove(userHandleField)
        newCustomFields.remove(relyingPartyField)
        // Empty auto generated OTP Token field
        if (fieldsToParse.contains(usernameField)
            || fieldsToParse.contains(privateKeyField)
            || fieldsToParse.contains(credentialIdField)
            || fieldsToParse.contains(userHandleField)
            || fieldsToParse.contains(relyingPartyField)
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
     * Field ignored for a search or a form filling
     */
    fun Field.isPasskeyExclusion(): Boolean {
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
}