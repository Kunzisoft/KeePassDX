package com.kunzisoft.keepass.credentialprovider.util

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.kunzisoft.keepass.credentialprovider.data.Passkey
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.DatabaseTaskProvider
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.utils.UuidUtil

@RequiresApi(Build.VERSION_CODES.O)
class DatabaseHelper {

    companion object {

        fun getAllPasskeys(database: Database): List<Passkey> {
            val searchHelper = SearchHelper()
            val searchParameters = SearchParameters().apply {
                searchQuery = PasskeyConverter.PASSKEY_TAG
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
            val fromGroup = null
            val max = Int.MAX_VALUE
            val searchResult = searchHelper.createVirtualGroupWithSearchResult(
                database,
                searchParameters,
                fromGroup,
                max
            )
                ?: return emptyList()

            return PasskeyConverter.convertEntriesListToPasskeys(searchResult.getChildEntries())
        }

        fun searchPassKeyByNodeId(database: Database, nodeId: String): Passkey? {
            val uuidToSearch = UuidUtil.fromHexString(nodeId) ?: return null
            val nodeIdUUIDToSearch = NodeIdUUID(uuidToSearch)
            val entry = database.getEntryById(nodeIdUUIDToSearch) ?: return null
            return PasskeyConverter.convertEntryToPasskey(entry)
        }

        fun updateEntry(
            database: Database,
            databaseTaskProvider: DatabaseTaskProvider,
            updatedPasskey: Passkey
        ) {
            val oldEntry = Entry(updatedPasskey.databaseEntry!!)
            val entryToUpdate = Entry(updatedPasskey.databaseEntry)

            PasskeyConverter.setPasskeyInEntry(updatedPasskey, entryToUpdate)

            entryToUpdate.setEntryInfo(
                database,
                entryToUpdate.getEntryInfo(
                    database,
                    raw = true,
                    removeTemplateConfiguration = false
                )
            )

            val save = true
            databaseTaskProvider.startDatabaseUpdateEntry(oldEntry, entryToUpdate, save)
            Log.d(this::class.java.simpleName, "passkey in entry ${oldEntry.title} updated")
        }

        fun saveNewEntry(
            database: ContextualDatabase,
            databaseTaskProvider: DatabaseTaskProvider,
            newPasskey: Passkey
        ) {
            val newEntry = database.createEntry() ?: throw Exception("can not create new entry")
            PasskeyConverter.setPasskeyInEntry(newPasskey, newEntry)
            val save = true

            val group = database.rootGroup
                ?: throw Exception("can not save new entry in database, because rootGroup is null")
            databaseTaskProvider.startDatabaseCreateEntry(newEntry, group, save)
            Log.d(this::class.java.simpleName, "new entry saved")
        }
    }
}