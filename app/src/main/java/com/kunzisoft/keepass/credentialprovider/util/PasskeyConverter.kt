package com.kunzisoft.keepass.credentialprovider.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.kunzisoft.keepass.credentialprovider.data.Passkey
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.utils.UuidUtil

@RequiresApi(Build.VERSION_CODES.O)
class PasskeyConverter {

    companion object {

        // field names from KeypassXC are used
        private const val FIELD_USERNAME = "KPEX_PASSKEY_USERNAME"
        private const val FIELD_PRIVATE_KEY = "KPEX_PASSKEY_PRIVATE_KEY_PEM"
        private const val FIELD_CREDENTIAL_ID = "KPEX_PASSKEY_CREDENTIAL_ID"
        private const val FIELD_USER_HANDLE = "KPEX_PASSKEY_USER_HANDLE"
        private const val FIELD_RELYING_PARTY = "KPEX_PASSKEY_RELYING_PARTY"

        const val PASSKEY_TAG = "Passkey"

        fun convertEntryToPasskey(entry: Entry): Passkey? {
            if (entry.tags.toList().contains(PASSKEY_TAG).not()) {
                return null
            }

            val nodeId = UuidUtil.toHexString(entry.nodeId.id) ?: return null

            val displayName = entry.getVisualTitle()

            var username = ""
            var privateKeyPem = ""
            var credId = ""
            var userHandle = ""
            var relyingParty = ""

            for (field in entry.getExtraFields()) {
                val fieldName = field.name

                if (fieldName == FIELD_USERNAME) {
                    username = field.protectedValue.stringValue
                } else if (field.name == FIELD_PRIVATE_KEY) {
                    privateKeyPem = field.protectedValue.stringValue
                } else if (field.name == FIELD_CREDENTIAL_ID) {
                    credId = field.protectedValue.stringValue
                } else if (field.name == FIELD_USER_HANDLE) {
                    userHandle = field.protectedValue.stringValue
                } else if (field.name == FIELD_RELYING_PARTY) {
                    relyingParty = field.protectedValue.stringValue
                }
            }
            return Passkey(
                nodeId,
                username,
                displayName,
                privateKeyPem,
                credId,
                userHandle,
                relyingParty,
                entry
            )
        }

        fun convertEntriesListToPasskeys(entries: List<Entry>): List<Passkey> {
            return entries.mapNotNull { e -> convertEntryToPasskey(e) }
        }


        fun setPasskeyInEntry(passkey: Passkey, entry: Entry) {
            entry.tags.put(PASSKEY_TAG)

            entry.title = passkey.displayName
            entry.lastModificationTime = DateInstant()

            entry.username = passkey.username

            entry.url = OriginHelper.DEFAULT_PROTOCOL + passkey.relyingParty

            val protected = true
            val unProtected = false

            entry.putExtraField(
                Field(
                    FIELD_USERNAME,
                    ProtectedString(unProtected, passkey.username)
                )
            )
            entry.putExtraField(
                Field(
                    FIELD_PRIVATE_KEY,
                    ProtectedString(protected, passkey.privateKeyPem)
                )
            )
            entry.putExtraField(
                Field(
                    FIELD_CREDENTIAL_ID,
                    ProtectedString(protected, passkey.credId)
                )
            )
            entry.putExtraField(
                Field(
                    FIELD_USER_HANDLE,
                    ProtectedString(protected, passkey.userHandle)
                )
            )
            entry.putExtraField(
                Field(
                    FIELD_RELYING_PARTY,
                    ProtectedString(unProtected, passkey.relyingParty)
                )
            )
        }

    }

}