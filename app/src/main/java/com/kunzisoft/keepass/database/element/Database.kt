/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element

import android.content.ContentResolver
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine
import com.kunzisoft.keepass.database.NodeHandler
import com.kunzisoft.keepass.database.cursor.EntryCursorV3
import com.kunzisoft.keepass.database.cursor.EntryCursorV4
import com.kunzisoft.keepass.database.exception.*
import com.kunzisoft.keepass.database.file.PwDbHeaderV3
import com.kunzisoft.keepass.database.file.PwDbHeaderV4
import com.kunzisoft.keepass.database.file.load.ImporterV3
import com.kunzisoft.keepass.database.file.load.ImporterV4
import com.kunzisoft.keepass.database.file.save.PwDbV3Output
import com.kunzisoft.keepass.database.file.save.PwDbV4Output
import com.kunzisoft.keepass.database.search.SearchDbHelper
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.stream.LEDataInputStream
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.SingletonHolder
import com.kunzisoft.keepass.utils.UriUtil
import org.apache.commons.io.FileUtils
import java.io.*
import java.util.*


class Database {

    // To keep a reference for specific methods provided by version
    private var pwDatabaseV3: PwDatabaseV3? = null
    private var pwDatabaseV4: PwDatabaseV4? = null

    var fileUri: Uri? = null
        private set

    private var mSearchHelper: SearchDbHelper? = null

    var isReadOnly = false

    val drawFactory = IconDrawableFactory()

    var loaded = false

    val iconFactory: PwIconFactory
        get() {
            return pwDatabaseV3?.iconFactory ?: pwDatabaseV4?.iconFactory ?: PwIconFactory()
        }

    val allowName: Boolean
        get() = pwDatabaseV4 != null

    var name: String
        get() {
            return pwDatabaseV4?.name ?: ""
        }
        set(name) {
            pwDatabaseV4?.name = name
            pwDatabaseV4?.nameChanged = PwDate()
        }

    val allowDescription: Boolean
        get() = pwDatabaseV4 != null

    var description: String
        get() {
            return pwDatabaseV4?.description ?: ""
        }
        set(description) {
            pwDatabaseV4?.description = description
            pwDatabaseV4?.descriptionChanged = PwDate()
        }

    val allowDefaultUsername: Boolean
        get() = pwDatabaseV4 != null
        // TODO get() = pwDatabaseV3 != null || pwDatabaseV4 != null

    var defaultUsername: String
        get() {
            return pwDatabaseV4?.defaultUserName ?: "" // TODO pwDatabaseV3 default username
        }
        set(username) {
            pwDatabaseV4?.defaultUserName = username
            pwDatabaseV4?.defaultUserNameChanged = PwDate()
        }

    val allowCustomColor: Boolean
        get() = pwDatabaseV4 != null
        // TODO get() = pwDatabaseV3 != null || pwDatabaseV4 != null

    // with format "#000000"
    var customColor: String
        get() {
            return pwDatabaseV4?.color ?: "" // TODO pwDatabaseV3 color
        }
        set(value) {
            // TODO Check color string
            pwDatabaseV4?.color = value
        }

    val version: String
        get() = pwDatabaseV3?.version ?: pwDatabaseV4?.version ?: "-"

    val allowDataCompression: Boolean
        get() = pwDatabaseV4 != null

    val availableCompressionAlgorithms: List<PwCompressionAlgorithm>
        get() = pwDatabaseV4?.availableCompressionAlgorithms ?: ArrayList()

    var compressionAlgorithm: PwCompressionAlgorithm?
        get() = pwDatabaseV4?.compressionAlgorithm
        set(value) {
            value?.let {
                pwDatabaseV4?.compressionAlgorithm = it
            }
        }

    val allowNoMasterKey: Boolean
        get() = pwDatabaseV4 != null

    val allowEncryptionAlgorithmModification: Boolean
        get() = availableEncryptionAlgorithms.size > 1

    fun getEncryptionAlgorithmName(resources: Resources): String {
        return pwDatabaseV3?.encryptionAlgorithm?.getName(resources)
                ?: pwDatabaseV4?.encryptionAlgorithm?.getName(resources)
                ?: ""
    }

    val availableEncryptionAlgorithms: List<PwEncryptionAlgorithm>
        get() = pwDatabaseV3?.availableEncryptionAlgorithms ?: pwDatabaseV4?.availableEncryptionAlgorithms ?: ArrayList()

    var encryptionAlgorithm: PwEncryptionAlgorithm?
        get() = pwDatabaseV3?.encryptionAlgorithm ?: pwDatabaseV4?.encryptionAlgorithm
        set(algorithm) {
            algorithm?.let {
                pwDatabaseV4?.encryptionAlgorithm = algorithm
                pwDatabaseV4?.setDataEngine(algorithm.cipherEngine)
                pwDatabaseV4?.dataCipher = algorithm.dataCipher
            }
        }

    val availableKdfEngines: List<KdfEngine>
        get() = pwDatabaseV3?.kdfAvailableList ?: pwDatabaseV4?.kdfAvailableList ?: ArrayList()

    val allowKdfModification: Boolean
        get() = availableKdfEngines.size > 1

    var kdfEngine: KdfEngine?
        get() = pwDatabaseV3?.kdfEngine ?: pwDatabaseV4?.kdfEngine
        set(kdfEngine) {
            kdfEngine?.let {
                if (pwDatabaseV4?.kdfParameters?.uuid != kdfEngine.defaultParameters.uuid)
                    pwDatabaseV4?.kdfParameters = kdfEngine.defaultParameters
                numberKeyEncryptionRounds = kdfEngine.defaultKeyRounds
                memoryUsage = kdfEngine.defaultMemoryUsage
                parallelism = kdfEngine.defaultParallelism
            }
        }

    fun getKeyDerivationName(resources: Resources): String {
        return kdfEngine?.getName(resources) ?: ""
    }

    var numberKeyEncryptionRounds: Long
        get() = pwDatabaseV3?.numberKeyEncryptionRounds ?: pwDatabaseV4?.numberKeyEncryptionRounds ?: 0
        @Throws(NumberFormatException::class)
        set(numberRounds) {
            pwDatabaseV3?.numberKeyEncryptionRounds = numberRounds
            pwDatabaseV4?.numberKeyEncryptionRounds = numberRounds
        }

    var memoryUsage: Long
        get() {
            return pwDatabaseV4?.memoryUsage ?: return KdfEngine.UNKNOWN_VALUE.toLong()
        }
        set(memory) {
            pwDatabaseV4?.memoryUsage = memory
        }

    var parallelism: Int
        get() = pwDatabaseV4?.parallelism ?: KdfEngine.UNKNOWN_VALUE
        set(parallelism) {
            pwDatabaseV4?.parallelism = parallelism
        }

    var masterKey: ByteArray
        get() = pwDatabaseV3?.masterKey ?: pwDatabaseV4?.masterKey ?: ByteArray(32)
        set(masterKey) {
            pwDatabaseV3?.masterKey = masterKey
            pwDatabaseV4?.masterKey = masterKey
        }

    val rootGroup: GroupVersioned?
        get() {
            pwDatabaseV3?.rootGroup?.let {
                return GroupVersioned(it)
            }
            pwDatabaseV4?.rootGroup?.let {
                return GroupVersioned(it)
            }
            return null
        }

    val manageHistory: Boolean
        get() = pwDatabaseV4 != null

    var historyMaxItems: Int
        get() {
            return pwDatabaseV4?.historyMaxItems ?: 0
        }
        set(value) {
            pwDatabaseV4?.historyMaxItems = value
        }

    var historyMaxSize: Long
        get() {
            return pwDatabaseV4?.historyMaxSize ?: 0
        }
        set(value) {
            pwDatabaseV4?.historyMaxSize = value
        }

    /**
     * Determine if RecycleBin is available or not for this version of database
     * @return true if RecycleBin available
     */
    val allowRecycleBin: Boolean
        get() = pwDatabaseV4 != null

    val isRecycleBinEnabled: Boolean
        get() = pwDatabaseV4?.isRecycleBinEnabled ?: false

    val recycleBin: GroupVersioned?
        get() {
            pwDatabaseV4?.recycleBin?.let {
                return GroupVersioned(it)
            }
            return null
        }

    private fun setDatabaseV3(pwDatabaseV3: PwDatabaseV3) {
        this.pwDatabaseV3 = pwDatabaseV3
        this.pwDatabaseV4 = null
    }

    private fun setDatabaseV4(pwDatabaseV4: PwDatabaseV4) {
        this.pwDatabaseV3 = null
        this.pwDatabaseV4 = pwDatabaseV4
    }

    private fun dbNameFromUri(databaseUri: Uri): String {
        val filename = URLUtil.guessFileName(databaseUri.path, null, null)
        if (filename == null || filename.isEmpty()) {
            return "KeePass Database"
        }
        val lastExtDot = filename.lastIndexOf(".")
        return if (lastExtDot == -1) {
            filename
        } else filename.substring(0, lastExtDot)
    }

    fun createData(databaseUri: Uri) {
        // Always create a new database with the last version
        setDatabaseV4(PwDatabaseV4(dbNameFromUri(databaseUri)))
        this.fileUri = databaseUri
    }

    @Throws(LoadDatabaseException::class)
    fun loadData(uri: Uri, password: String?, keyfile: Uri?,
                 readOnly: Boolean,
                 contentResolver: ContentResolver,
                 cacheDirectory: File,
                 omitBackup: Boolean,
                 fixDuplicateUUID: Boolean,
                 progressTaskUpdater: ProgressTaskUpdater?) {

        this.fileUri = uri
        isReadOnly = readOnly
        if (uri.scheme == "file") {
            val file = File(uri.path!!)
            isReadOnly = !file.canWrite()
        }

        // Pass Uris as InputStreams
        val inputStream: InputStream?
        try {
            inputStream = UriUtil.getUriInputStream(contentResolver, uri)
        } catch (e: Exception) {
            Log.e("KPD", "Database::loadData", e)
            throw LoadDatabaseFileNotFoundException()
        }

        // Pass KeyFile Uri as InputStreams
        var keyFileInputStream: InputStream? = null
        keyfile?.let {
            try {
                keyFileInputStream = UriUtil.getUriInputStream(contentResolver, keyfile)
            } catch (e: Exception) {
                Log.e("KPD", "Database::loadData", e)
                throw LoadDatabaseFileNotFoundException()
            }
        }

        // Load Data

        val bufferedInputStream = BufferedInputStream(inputStream)
        if (!bufferedInputStream.markSupported()) {
            throw IOException("Input stream does not support mark.")
        }

        // We'll end up reading 8 bytes to identify the header. Might as well use two extra.
        bufferedInputStream.mark(10)

        // Get the file directory to save the attachments
        val sig1 = LEDataInputStream.readInt(bufferedInputStream)
        val sig2 = LEDataInputStream.readInt(bufferedInputStream)

        // Return to the start
        bufferedInputStream.reset()

        when {
            // Header of database V3
            PwDbHeaderV3.matchesHeader(sig1, sig2) -> setDatabaseV3(ImporterV3()
                    .openDatabase(bufferedInputStream,
                            password,
                            keyFileInputStream,
                            progressTaskUpdater))

            // Header of database V4
            PwDbHeaderV4.matchesHeader(sig1, sig2) -> setDatabaseV4(ImporterV4(
                    cacheDirectory,
                    fixDuplicateUUID)
                    .openDatabase(bufferedInputStream,
                            password,
                            keyFileInputStream,
                            progressTaskUpdater))

            // Header not recognized
            else -> throw LoadDatabaseSignatureException()
        }

        this.mSearchHelper = SearchDbHelper(omitBackup)
        loaded = true
    }

    fun isGroupSearchable(group: GroupVersioned, isOmitBackup: Boolean): Boolean {
        return pwDatabaseV3?.isGroupSearchable(group.pwGroupV3, isOmitBackup) ?:
        pwDatabaseV4?.isGroupSearchable(group.pwGroupV4, isOmitBackup) ?:
        false
    }

    @JvmOverloads
    fun search(str: String, max: Int = Integer.MAX_VALUE): GroupVersioned? {
        return mSearchHelper?.search(this, str, max)
    }

    fun searchEntries(query: String): Cursor? {

        var cursorV3: EntryCursorV3? = null
        var cursorV4: EntryCursorV4? = null

        if (pwDatabaseV3 != null)
            cursorV3 = EntryCursorV3()
        if (pwDatabaseV4 != null)
            cursorV4 = EntryCursorV4()

        val searchResult = search(query, SearchDbHelper.MAX_SEARCH_ENTRY)
        if (searchResult != null) {
            for (entry in searchResult.getChildEntries(true)) {
                entry.pwEntryV3?.let {
                    cursorV3?.addEntry(it)
                }
                entry.pwEntryV4?.let {
                    cursorV4?.addEntry(it)
                }
            }
        }

        return cursorV3 ?: cursorV4
    }

    fun getEntryFrom(cursor: Cursor): EntryVersioned? {
        val iconFactory = pwDatabaseV3?.iconFactory ?: pwDatabaseV4?.iconFactory ?: PwIconFactory()
        val entry = createEntry()

        // TODO invert field reference manager
        entry?.let { entryVersioned ->
            startManageEntry(entryVersioned)
            pwDatabaseV3?.let {
                entryVersioned.pwEntryV3?.let { entryV3 ->
                    (cursor as EntryCursorV3).populateEntry(entryV3, iconFactory)
                }
            }
            pwDatabaseV4?.let {
                entryVersioned.pwEntryV4?.let { entryV4 ->
                    (cursor as EntryCursorV4).populateEntry(entryV4, iconFactory)
                }
            }
            stopManageEntry(entryVersioned)
        }

        return entry
    }

    @Throws(IOException::class, DatabaseOutputException::class)
    fun saveData(contentResolver: ContentResolver) {
        this.fileUri?.let {
            saveData(contentResolver, it)
        }
    }

    @Throws(IOException::class, DatabaseOutputException::class)
    private fun saveData(contentResolver: ContentResolver, uri: Uri) {
        val errorMessage = "Failed to store database."

        if (uri.scheme == "file") {
            uri.path?.let { filename ->
                val tempFile = File("$filename.tmp")

                var fileOutputStream: FileOutputStream? = null
                try {
                    fileOutputStream = FileOutputStream(tempFile)
                    val pmo = pwDatabaseV3?.let { PwDbV3Output(it, fileOutputStream) }
                            ?: pwDatabaseV4?.let { PwDbV4Output(it, fileOutputStream) }
                    pmo?.output()
                } catch (e: Exception) {
                    Log.e(TAG, errorMessage, e)
                    throw IOException(errorMessage, e)
                } finally {
                    fileOutputStream?.close()
                }

                // Force data to disk before continuing
                try {
                    fileOutputStream?.fd?.sync()
                } catch (e: SyncFailedException) {
                    // Ignore if fsync fails. We tried.
                }
    
                if (!tempFile.renameTo(File(filename))) {
                    throw IOException(errorMessage)
                }
            }
        } else {
            var outputStream: OutputStream? = null
            try {
                outputStream = contentResolver.openOutputStream(uri)
                val pmo =
                        pwDatabaseV3?.let { PwDbV3Output(it, outputStream) }
                        ?: pwDatabaseV4?.let { PwDbV4Output(it, outputStream) }
                pmo?.output()
            } catch (e: Exception) {
                Log.e(TAG, errorMessage, e)
                throw IOException(errorMessage, e)
            } finally {
                outputStream?.close()
            }
        }
        this.fileUri = uri
    }

    // TODO Clear database when lock broadcast is receive in backstage
    fun closeAndClear(filesDirectory: File? = null) {
        drawFactory.clearCache()
        // Delete the cache of the database if present
        pwDatabaseV3?.clearCache()
        pwDatabaseV4?.clearCache()
        // In all cases, delete all the files in the temp dir
        try {
            FileUtils.cleanDirectory(filesDirectory)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to clear the directory cache.", e)
        }

        this.pwDatabaseV3 = null
        this.pwDatabaseV4 = null
        this.fileUri = null
        this.loaded = false
    }

    fun validatePasswordEncoding(password: String?, containsKeyFile: Boolean): Boolean {
        return pwDatabaseV3?.validatePasswordEncoding(password, containsKeyFile)
                ?: pwDatabaseV4?.validatePasswordEncoding(password, containsKeyFile)
                ?: false
    }

    @Throws(IOException::class)
    fun retrieveMasterKey(key: String?, keyInputStream: InputStream?) {
        pwDatabaseV3?.retrieveMasterKey(key, keyInputStream)
        pwDatabaseV4?.retrieveMasterKey(key, keyInputStream)
    }

    fun rootCanContainsEntry(): Boolean {
        return pwDatabaseV3?.rootCanContainsEntry() ?: pwDatabaseV4?.rootCanContainsEntry() ?: false
    }

    fun createEntry(): EntryVersioned? {
        pwDatabaseV3?.let { database ->
            return EntryVersioned(database.createEntry()).apply {
                nodeId = database.newEntryId()
            }
        }
        pwDatabaseV4?.let { database ->
            return EntryVersioned(database.createEntry()).apply {
                nodeId = database.newEntryId()
            }
        }

        return null
    }

    fun createGroup(): GroupVersioned? {
        pwDatabaseV3?.let { database ->
            return GroupVersioned(database.createGroup()).apply {
                setNodeId(database.newGroupId())
            }
        }
        pwDatabaseV4?.let { database ->
            return GroupVersioned(database.createGroup()).apply {
                setNodeId(database.newGroupId())
            }
        }

        return null
    }

    fun getEntryById(id: PwNodeId<UUID>): EntryVersioned? {
        pwDatabaseV3?.getEntryById(id)?.let {
            return EntryVersioned(it)
        }
        pwDatabaseV4?.getEntryById(id)?.let {
            return EntryVersioned(it)
        }
        return null
    }

    fun getGroupById(id: PwNodeId<*>): GroupVersioned? {
        if (id is PwNodeIdInt)
            pwDatabaseV3?.getGroupById(id)?.let {
                return GroupVersioned(it)
            }
        else if (id is PwNodeIdUUID)
            pwDatabaseV4?.getGroupById(id)?.let {
                return GroupVersioned(it)
            }
        return null
    }

    fun addEntryTo(entry: EntryVersioned, parent: GroupVersioned) {
        entry.pwEntryV3?.let { entryV3 ->
            pwDatabaseV3?.addEntryTo(entryV3, parent.pwGroupV3)
        }
        entry.pwEntryV4?.let { entryV4 ->
            pwDatabaseV4?.addEntryTo(entryV4, parent.pwGroupV4)
        }
        entry.afterAssignNewParent()
    }

    fun updateEntry(entry: EntryVersioned) {
        entry.pwEntryV3?.let { entryV3 ->
            pwDatabaseV3?.updateEntry(entryV3)
        }
        entry.pwEntryV4?.let { entryV4 ->
            pwDatabaseV4?.updateEntry(entryV4)
        }
    }

    fun removeEntryFrom(entry: EntryVersioned, parent: GroupVersioned) {
        entry.pwEntryV3?.let { entryV3 ->
            pwDatabaseV3?.removeEntryFrom(entryV3, parent.pwGroupV3)
        }
        entry.pwEntryV4?.let { entryV4 ->
            pwDatabaseV4?.removeEntryFrom(entryV4, parent.pwGroupV4)
        }
        entry.afterAssignNewParent()
    }

    fun addGroupTo(group: GroupVersioned, parent: GroupVersioned) {
        group.pwGroupV3?.let { groupV3 ->
            pwDatabaseV3?.addGroupTo(groupV3, parent.pwGroupV3)
        }
        group.pwGroupV4?.let { groupV4 ->
            pwDatabaseV4?.addGroupTo(groupV4, parent.pwGroupV4)
        }
        group.afterAssignNewParent()
    }

    fun updateGroup(group: GroupVersioned) {
        group.pwGroupV3?.let { groupV3 ->
            pwDatabaseV3?.updateGroup(groupV3)
        }
        group.pwGroupV4?.let { groupV4 ->
            pwDatabaseV4?.updateGroup(groupV4)
        }
    }

    fun removeGroupFrom(group: GroupVersioned, parent: GroupVersioned) {
        group.pwGroupV3?.let { groupV3 ->
            pwDatabaseV3?.removeGroupFrom(groupV3, parent.pwGroupV3)
        }
        group.pwGroupV4?.let { groupV4 ->
            pwDatabaseV4?.removeGroupFrom(groupV4, parent.pwGroupV4)
        }
        group.afterAssignNewParent()
    }

    /**
     * @return A duplicate entry with the same values, a new random UUID and a new parent
     * @param entryToCopy
     * @param newParent
     */
    fun copyEntryTo(entryToCopy: EntryVersioned, newParent: GroupVersioned): EntryVersioned? {
        val entryCopied = EntryVersioned(entryToCopy, false)
        entryCopied.nodeId = pwDatabaseV3?.newEntryId() ?: pwDatabaseV4?.newEntryId() ?: PwNodeIdUUID()
        entryCopied.parent = newParent
        entryCopied.title += " (~)"
        addEntryTo(entryCopied, newParent)
        return entryCopied
    }

    fun moveEntryTo(entryToMove: EntryVersioned, newParent: GroupVersioned) {
        entryToMove.parent?.let {
            removeEntryFrom(entryToMove, it)
        }
        addEntryTo(entryToMove, newParent)
    }

    fun moveGroupTo(groupToMove: GroupVersioned, newParent: GroupVersioned) {
        groupToMove.parent?.let {
            removeGroupFrom(groupToMove, it)
        }
        addGroupTo(groupToMove, newParent)
    }

    fun deleteEntry(entry: EntryVersioned) {
        entry.parent?.let {
            removeEntryFrom(entry, it)
        }
    }

    fun deleteGroup(group: GroupVersioned) {
        group.doForEachChildAndForIt(
                object : NodeHandler<EntryVersioned>() {
                    override fun operate(node: EntryVersioned): Boolean {
                        deleteEntry(node)
                        return true
                    }
                },
                object : NodeHandler<GroupVersioned>() {
                    override fun operate(node: GroupVersioned): Boolean {
                        node.parent?.let {
                            removeGroupFrom(node, it)
                        }
                        return true
                    }
                })
    }

    fun undoDeleteEntry(entry: EntryVersioned, parent: GroupVersioned) {
        entry.pwEntryV3?.let { entryV3 ->
            pwDatabaseV3?.undoDeleteEntryFrom(entryV3, parent.pwGroupV3)
        }
        entry.pwEntryV4?.let { entryV4 ->
            pwDatabaseV4?.undoDeleteEntryFrom(entryV4, parent.pwGroupV4)
        }
    }

    fun undoDeleteGroup(group: GroupVersioned, parent: GroupVersioned) {
        group.pwGroupV3?.let { groupV3 ->
            pwDatabaseV3?.undoDeleteGroupFrom(groupV3, parent.pwGroupV3)
        }
        group.pwGroupV4?.let { groupV4 ->
            pwDatabaseV4?.undoDeleteGroupFrom(groupV4, parent.pwGroupV4)
        }
    }

    fun canRecycle(entry: EntryVersioned): Boolean {
        var canRecycle: Boolean? = null
        entry.pwEntryV4?.let { entryV4 ->
            canRecycle = pwDatabaseV4?.canRecycle(entryV4)
        }
        return canRecycle ?: false
    }

    fun canRecycle(group: GroupVersioned): Boolean {
        var canRecycle: Boolean? = null
        group.pwGroupV4?.let { groupV4 ->
            canRecycle = pwDatabaseV4?.canRecycle(groupV4)
        }
        return canRecycle ?: false
    }

    fun recycle(entry: EntryVersioned, resources: Resources) {
        entry.pwEntryV4?.let {
            pwDatabaseV4?.recycle(it, resources)
        }
    }

    fun recycle(group: GroupVersioned, resources: Resources) {
        group.pwGroupV4?.let {
            pwDatabaseV4?.recycle(it, resources)
        }
    }

    fun undoRecycle(entry: EntryVersioned, parent: GroupVersioned) {
        entry.pwEntryV4?.let { entryV4 ->
            parent.pwGroupV4?.let { parentV4 ->
                pwDatabaseV4?.undoRecycle(entryV4, parentV4)
            }
        }
    }

    fun undoRecycle(group: GroupVersioned, parent: GroupVersioned) {
        group.pwGroupV4?.let { groupV4 ->
            parent.pwGroupV4?.let { parentV4 ->
                pwDatabaseV4?.undoRecycle(groupV4, parentV4)
            }
        }
    }

    fun startManageEntry(entry: EntryVersioned) {
        pwDatabaseV4?.let {
            entry.startToManageFieldReferences(it)
        }
    }

    fun stopManageEntry(entry: EntryVersioned) {
        pwDatabaseV4?.let {
            entry.stopToManageFieldReferences()
        }
    }

    fun removeOldestHistory(entry: EntryVersioned) {

        // Remove oldest history if more than max items or max memory
        pwDatabaseV4?.let {
            val history = entry.getHistory()

            val maxItems = historyMaxItems
            if (maxItems >= 0) {
                while (history.size > maxItems) {
                    entry.removeOldestEntryFromHistory()
                }
            }

            val maxSize = historyMaxSize
            if (maxSize >= 0) {
                while (true) {
                    var historySize: Long = 0
                    for (entryHistory in history) {
                        historySize += entryHistory.getSize()
                    }

                    if (historySize > maxSize) {
                        entry.removeOldestEntryFromHistory()
                    } else {
                        break
                    }
                }
            }
        }
    }

    companion object : SingletonHolder<Database>(::Database) {

        private val TAG = Database::class.java.name
    }
}
