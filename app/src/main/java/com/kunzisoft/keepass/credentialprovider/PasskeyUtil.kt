package com.kunzisoft.keepass.credentialprovider

import android.os.Build
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.utils.UuidUtil
import java.time.Instant

class PasskeyUtil {

    data class Passkey(val nodeId: String, val username: String, val displayName: String, val privateKeyPem: String, val credId: String, val userHandle: String, val relyingParty: String, val lastUsedTime: Instant?)

    companion object {

        const val PASSKEY_TAG = "Passkey"
        fun convertEntryToPasskey(entry: Entry): Passkey? {
            if (!entry.tags.toList().contains(PASSKEY_TAG)) {
                return null
            }

            val nodeId = UuidUtil.toHexString(entry.nodeId.id)!!

            val displayName = entry.getVisualTitle()
            val lastUsedTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                entry.lastAccessTime.date.toInstant()
            } else {
                null
            }
            var username = ""
            var privateKeyPem = ""
            var credId = ""
            var userHandle = ""
            var relyingParty = ""

            for (field in entry.getExtraFields()) {
                val fieldName = field.name

                // field names from KeypassXC are used
                if (fieldName == "KPEX_PASSKEY_USERNAME") {
                    username = field.protectedValue.stringValue
                } else if (field.name == "KPEX_PASSKEY_PRIVATE_KEY_PEM") {
                    privateKeyPem = field.protectedValue.stringValue
                } else if (field.name == "KPEX_PASSKEY_CREDENTIAL_ID") {
                    credId = field.protectedValue.stringValue
                } else if (field.name == "KPEX_PASSKEY_USER_HANDLE") {
                    userHandle = field.protectedValue.stringValue
                } else if (field.name == "KPEX_PASSKEY_RELYING_PARTY") {
                    relyingParty = field.protectedValue.stringValue
                }
                // KPEX_PASSKEY_RELYING_PARTY
            }
            return Passkey(nodeId, username, displayName, privateKeyPem, credId, userHandle, relyingParty, lastUsedTime)
        }

        fun convertEntriesListToPasskeys(entries: List<Entry>): List<Passkey> {
            return entries.mapNotNull { e -> convertEntryToPasskey(e) }
        }

        fun searchPasskeys(database: Database): List<Passkey> {
            val searchHelper =  SearchHelper()
            val searchParameters = SearchParameters().apply {
                searchQuery = PASSKEY_TAG
                searchInTitles = false
                searchInUsernames = false
                searchInPasswords = false
                searchInUrls = false
                searchInNotes = false
                searchInOTP = false
                searchInOther = false
                searchInUUIDs = false
                searchInTags = true
                searchInCurrentGroup = false
                searchInSearchableGroup = false
                searchInRecycleBin = false
                searchInTemplates = false
            }
            val searchResult = searchHelper.createVirtualGroupWithSearchResult(database, searchParameters, null, Int.MAX_VALUE)
                ?: return emptyList()

            return convertEntriesListToPasskeys(searchResult.getChildEntries())
        }

        fun searchPassKeyByNodeId(database: Database, nodeId: String): Passkey? {
            val uuidToSearch = UuidUtil.fromHexString(nodeId)!!
            val nodeIdUUIDToSearch =  NodeIdUUID(uuidToSearch)
            val entry = database.getEntryById(nodeIdUUIDToSearch)!!
            return convertEntryToPasskey(entry)
        }
    }
}