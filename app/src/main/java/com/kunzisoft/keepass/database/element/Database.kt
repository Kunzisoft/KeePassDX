/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element

import android.content.ContentResolver
import android.content.res.Resources
import android.net.Uri
import android.util.Log
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine
import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.database.DatabaseKDB
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.icon.IconImageFactory
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdInt
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.security.EncryptionAlgorithm
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.database.exception.FileNotFoundDatabaseException
import com.kunzisoft.keepass.database.exception.LoadDatabaseException
import com.kunzisoft.keepass.database.exception.SignatureDatabaseException
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDB
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX
import com.kunzisoft.keepass.database.file.input.DatabaseInputKDB
import com.kunzisoft.keepass.database.file.input.DatabaseInputKDBX
import com.kunzisoft.keepass.database.file.output.DatabaseOutputKDB
import com.kunzisoft.keepass.database.file.output.DatabaseOutputKDBX
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.SearchInfo
import com.kunzisoft.keepass.stream.readBytes4ToInt
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.SingletonHolder
import com.kunzisoft.keepass.utils.UriUtil
import org.apache.commons.io.FileUtils
import java.io.*
import java.util.*


class Database {

    // To keep a reference for specific methods provided by version
    private var mDatabaseKDB: DatabaseKDB? = null
    private var mDatabaseKDBX: DatabaseKDBX? = null

    var fileUri: Uri? = null
        private set

    private var mSearchHelper: SearchHelper? = null

    var isReadOnly = false

    val drawFactory = IconDrawableFactory()

    var loaded = false

    val iconFactory: IconImageFactory
        get() {
            return mDatabaseKDB?.iconFactory ?: mDatabaseKDBX?.iconFactory ?: IconImageFactory()
        }

    val allowName: Boolean
        get() = mDatabaseKDBX != null

    var name: String
        get() {
            return mDatabaseKDBX?.name ?: ""
        }
        set(name) {
            mDatabaseKDBX?.name = name
            mDatabaseKDBX?.nameChanged = DateInstant()
        }

    val allowDescription: Boolean
        get() = mDatabaseKDBX != null

    var description: String
        get() {
            return mDatabaseKDBX?.description ?: ""
        }
        set(description) {
            mDatabaseKDBX?.description = description
            mDatabaseKDBX?.descriptionChanged = DateInstant()
        }

    val allowDefaultUsername: Boolean
        get() = mDatabaseKDBX != null
        // TODO get() = mDatabaseKDB != null || mDatabaseKDBX != null

    var defaultUsername: String
        get() {
            return mDatabaseKDBX?.defaultUserName ?: "" // TODO mDatabaseKDB default username
        }
        set(username) {
            mDatabaseKDBX?.defaultUserName = username
            mDatabaseKDBX?.defaultUserNameChanged = DateInstant()
        }

    val allowCustomColor: Boolean
        get() = mDatabaseKDBX != null
        // TODO get() = mDatabaseKDB != null || mDatabaseKDBX != null

    // with format "#000000"
    var customColor: String
        get() {
            return mDatabaseKDBX?.color ?: "" // TODO mDatabaseKDB color
        }
        set(value) {
            // TODO Check color string
            mDatabaseKDBX?.color = value
        }

    val allowOTP: Boolean
        get() = mDatabaseKDBX != null

    val version: String
        get() = mDatabaseKDB?.version ?: mDatabaseKDBX?.version ?: "-"

    val type: Class<*>?
        get() = mDatabaseKDB?.javaClass ?: mDatabaseKDBX?.javaClass

    val allowDataCompression: Boolean
        get() = mDatabaseKDBX != null

    val availableCompressionAlgorithms: List<CompressionAlgorithm>
        get() = mDatabaseKDBX?.availableCompressionAlgorithms ?: ArrayList()

    var compressionAlgorithm: CompressionAlgorithm?
        get() = mDatabaseKDBX?.compressionAlgorithm
        set(value) {
            value?.let {
                mDatabaseKDBX?.compressionAlgorithm = it
            }
        }

    fun updateDataBinaryCompression(oldCompression: CompressionAlgorithm,
                                    newCompression: CompressionAlgorithm) {
        mDatabaseKDBX?.changeBinaryCompression(oldCompression, newCompression)
    }

    val allowNoMasterKey: Boolean
        get() = mDatabaseKDBX != null

    val allowEncryptionAlgorithmModification: Boolean
        get() = availableEncryptionAlgorithms.size > 1

    fun getEncryptionAlgorithmName(resources: Resources): String {
        return mDatabaseKDB?.encryptionAlgorithm?.getName(resources)
                ?: mDatabaseKDBX?.encryptionAlgorithm?.getName(resources)
                ?: ""
    }

    val availableEncryptionAlgorithms: List<EncryptionAlgorithm>
        get() = mDatabaseKDB?.availableEncryptionAlgorithms ?: mDatabaseKDBX?.availableEncryptionAlgorithms ?: ArrayList()

    var encryptionAlgorithm: EncryptionAlgorithm?
        get() = mDatabaseKDB?.encryptionAlgorithm ?: mDatabaseKDBX?.encryptionAlgorithm
        set(algorithm) {
            algorithm?.let {
                mDatabaseKDBX?.encryptionAlgorithm = algorithm
                mDatabaseKDBX?.setDataEngine(algorithm.cipherEngine)
                mDatabaseKDBX?.dataCipher = algorithm.dataCipher
            }
        }

    val availableKdfEngines: List<KdfEngine>
        get() = mDatabaseKDB?.kdfAvailableList ?: mDatabaseKDBX?.kdfAvailableList ?: ArrayList()

    val allowKdfModification: Boolean
        get() = availableKdfEngines.size > 1

    var kdfEngine: KdfEngine?
        get() = mDatabaseKDB?.kdfEngine ?: mDatabaseKDBX?.kdfEngine
        set(kdfEngine) {
            kdfEngine?.let {
                if (mDatabaseKDBX?.kdfParameters?.uuid != kdfEngine.defaultParameters.uuid)
                    mDatabaseKDBX?.kdfParameters = kdfEngine.defaultParameters
                numberKeyEncryptionRounds = kdfEngine.defaultKeyRounds
                memoryUsage = kdfEngine.defaultMemoryUsage
                parallelism = kdfEngine.defaultParallelism
            }
        }

    fun getKeyDerivationName(resources: Resources): String {
        return kdfEngine?.getName(resources) ?: ""
    }

    var numberKeyEncryptionRounds: Long
        get() = mDatabaseKDB?.numberKeyEncryptionRounds ?: mDatabaseKDBX?.numberKeyEncryptionRounds ?: 0
        @Throws(NumberFormatException::class)
        set(numberRounds) {
            mDatabaseKDB?.numberKeyEncryptionRounds = numberRounds
            mDatabaseKDBX?.numberKeyEncryptionRounds = numberRounds
        }

    var memoryUsage: Long
        get() {
            return mDatabaseKDBX?.memoryUsage ?: return KdfEngine.UNKNOWN_VALUE.toLong()
        }
        set(memory) {
            mDatabaseKDBX?.memoryUsage = memory
        }

    var parallelism: Int
        get() = mDatabaseKDBX?.parallelism ?: KdfEngine.UNKNOWN_VALUE
        set(parallelism) {
            mDatabaseKDBX?.parallelism = parallelism
        }

    var masterKey: ByteArray
        get() = mDatabaseKDB?.masterKey ?: mDatabaseKDBX?.masterKey ?: ByteArray(32)
        set(masterKey) {
            mDatabaseKDB?.masterKey = masterKey
            mDatabaseKDBX?.masterKey = masterKey
        }

    val rootGroup: Group?
        get() {
            mDatabaseKDB?.rootGroup?.let {
                return Group(it)
            }
            mDatabaseKDBX?.rootGroup?.let {
                return Group(it)
            }
            return null
        }

    val manageHistory: Boolean
        get() = mDatabaseKDBX != null

    var historyMaxItems: Int
        get() {
            return mDatabaseKDBX?.historyMaxItems ?: 0
        }
        set(value) {
            mDatabaseKDBX?.historyMaxItems = value
        }

    var historyMaxSize: Long
        get() {
            return mDatabaseKDBX?.historyMaxSize ?: 0
        }
        set(value) {
            mDatabaseKDBX?.historyMaxSize = value
        }

    /**
     * Determine if RecycleBin is available or not for this version of database
     * @return true if RecycleBin available
     */
    val allowRecycleBin: Boolean
        get() = mDatabaseKDBX != null

    var isRecycleBinEnabled: Boolean
        // TODO #394 isRecycleBinEnabled mDatabaseKDB
        get() = mDatabaseKDB != null || mDatabaseKDBX?.isRecycleBinEnabled ?: false
        set(value) {
            mDatabaseKDBX?.isRecycleBinEnabled = value
        }

    val recycleBin: Group?
        get() {
            mDatabaseKDB?.backupGroup?.let {
                return Group(it)
            }
            mDatabaseKDBX?.recycleBin?.let {
                return Group(it)
            }
            return null
        }

    fun ensureRecycleBinExists(resources: Resources) {
        mDatabaseKDB?.ensureRecycleBinExists()
        mDatabaseKDBX?.ensureRecycleBinExists(resources)
    }

    fun removeRecycleBin() {
        // TODO #394 delete backup mDatabaseKDB?.removeRecycleBin()
        mDatabaseKDBX?.removeRecycleBin()
    }

    private fun setDatabaseKDB(databaseKDB: DatabaseKDB) {
        this.mDatabaseKDB = databaseKDB
        this.mDatabaseKDBX = null
    }

    private fun setDatabaseKDBX(databaseKDBX: DatabaseKDBX) {
        this.mDatabaseKDB = null
        this.mDatabaseKDBX = databaseKDBX
    }

    fun createData(databaseUri: Uri, databaseName: String, rootName: String) {
        setDatabaseKDBX(DatabaseKDBX(databaseName, rootName))
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

        // Pass KeyFile Uri as InputStreams
        var databaseInputStream: InputStream? = null
        var keyFileInputStream: InputStream? = null
        try {
            // Get keyFile inputStream
            keyfile?.let {
                keyFileInputStream = UriUtil.getUriInputStream(contentResolver, keyfile)
            }

            // Load Data, pass Uris as InputStreams
            databaseInputStream = BufferedInputStream(UriUtil.getUriInputStream(contentResolver, uri))
            if (!databaseInputStream.markSupported()) {
                throw IOException("Input stream does not support mark.")
            }

            // We'll end up reading 8 bytes to identify the header. Might as well use two extra.
            databaseInputStream.mark(10)

            // Get the file directory to save the attachments
            val sig1 = databaseInputStream.readBytes4ToInt()
            val sig2 = databaseInputStream.readBytes4ToInt()

            // Return to the start
            databaseInputStream.reset()

            when {
                // Header of database KDB
                DatabaseHeaderKDB.matchesHeader(sig1, sig2) -> setDatabaseKDB(DatabaseInputKDB(
                        cacheDirectory,
                        fixDuplicateUUID)
                        .openDatabase(databaseInputStream,
                                password,
                                keyFileInputStream,
                                progressTaskUpdater))

                // Header of database KDBX
                DatabaseHeaderKDBX.matchesHeader(sig1, sig2) -> setDatabaseKDBX(DatabaseInputKDBX(
                        cacheDirectory,
                        fixDuplicateUUID)
                        .openDatabase(databaseInputStream,
                                password,
                                keyFileInputStream,
                                progressTaskUpdater))

                // Header not recognized
                else -> throw SignatureDatabaseException()
            }

            this.mSearchHelper = SearchHelper(omitBackup)
            loaded = true

        } catch (e: LoadDatabaseException) {
            Log.e("KPD", "Database::loadData", e)
            throw e
        } catch (e: Exception) {
            Log.e("KPD", "Database::loadData", e)
            throw FileNotFoundDatabaseException()
        } finally {
            keyFileInputStream?.close()
            databaseInputStream?.close()
        }
    }

    fun isGroupSearchable(group: Group, isOmitBackup: Boolean): Boolean {
        return mDatabaseKDB?.isGroupSearchable(group.groupKDB, isOmitBackup) ?:
        mDatabaseKDBX?.isGroupSearchable(group.groupKDBX, isOmitBackup) ?:
        false
    }

    fun createVirtualGroupFromSearch(searchQuery: String,
                                     max: Int = Integer.MAX_VALUE): Group? {
        return mSearchHelper?.createVirtualGroupWithSearchResult(this, searchQuery, SearchParameters(), max)
    }

    fun createVirtualGroupFromSearch(searchInfo: SearchInfo,
                                     max: Int = Integer.MAX_VALUE): Group? {
        val query = (if (searchInfo.webDomain != null)
            searchInfo.webDomain
        else
            searchInfo.applicationId)
                ?: return null
        return mSearchHelper?.createVirtualGroupWithSearchResult(this, query, SearchParameters().apply {
            searchInTitles = false
            searchInUserNames = false
            searchInPasswords = false
            searchInUrls = true
            searchInNotes = true
            searchInOther = true
            searchInUUIDs = false
            searchInTags = false
            ignoreCase = true
        }, max)
    }

    @Throws(DatabaseOutputException::class)
    fun saveData(contentResolver: ContentResolver) {
        try {
            this.fileUri?.let {
                saveData(contentResolver, it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to save database", e)
            throw DatabaseOutputException(e)
        }
    }

    @Throws(IOException::class, DatabaseOutputException::class)
    private fun saveData(contentResolver: ContentResolver, uri: Uri) {

        if (uri.scheme == "file") {
            uri.path?.let { filename ->
                val tempFile = File("$filename.tmp")

                var fileOutputStream: FileOutputStream? = null
                try {
                    fileOutputStream = FileOutputStream(tempFile)
                    val pmo = mDatabaseKDB?.let { DatabaseOutputKDB(it, fileOutputStream) }
                            ?: mDatabaseKDBX?.let { DatabaseOutputKDBX(it, fileOutputStream) }
                    pmo?.output()
                } catch (e: Exception) {
                    throw IOException(e)
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
                    throw IOException()
                }
            }
        } else {
            var outputStream: OutputStream? = null
            try {
                outputStream = contentResolver.openOutputStream(uri)
                outputStream?.let { definedOutputStream ->
                    val databaseOutput = mDatabaseKDB?.let { DatabaseOutputKDB(it, definedOutputStream) }
                                    ?: mDatabaseKDBX?.let { DatabaseOutputKDBX(it, definedOutputStream) }
                    databaseOutput?.output()
                }
            } catch (e: Exception) {
                throw IOException(e)
            } finally {
                outputStream?.close()
            }
        }
        this.fileUri = uri
    }

    fun closeAndClear(filesDirectory: File? = null) {
        drawFactory.clearCache()
        // Delete the cache of the database if present
        mDatabaseKDB?.clearCache()
        mDatabaseKDBX?.clearCache()
        // In all cases, delete all the files in the temp dir
        try {
            FileUtils.cleanDirectory(filesDirectory)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to clear the directory cache.", e)
        }

        this.mDatabaseKDB = null
        this.mDatabaseKDBX = null
        this.fileUri = null
        this.loaded = false
    }

    fun validatePasswordEncoding(password: String?, containsKeyFile: Boolean): Boolean {
        return mDatabaseKDB?.validatePasswordEncoding(password, containsKeyFile)
                ?: mDatabaseKDBX?.validatePasswordEncoding(password, containsKeyFile)
                ?: false
    }

    @Throws(IOException::class)
    fun retrieveMasterKey(key: String?, keyInputStream: InputStream?) {
        mDatabaseKDB?.retrieveMasterKey(key, keyInputStream)
        mDatabaseKDBX?.retrieveMasterKey(key, keyInputStream)
    }

    fun rootCanContainsEntry(): Boolean {
        return mDatabaseKDB?.rootCanContainsEntry() ?: mDatabaseKDBX?.rootCanContainsEntry() ?: false
    }

    fun createEntry(): Entry? {
        mDatabaseKDB?.let { database ->
            return Entry(database.createEntry()).apply {
                nodeId = database.newEntryId()
            }
        }
        mDatabaseKDBX?.let { database ->
            return Entry(database.createEntry()).apply {
                nodeId = database.newEntryId()
            }
        }

        return null
    }

    fun createGroup(): Group? {
        mDatabaseKDB?.let { database ->
            return Group(database.createGroup()).apply {
                setNodeId(database.newGroupId())
            }
        }
        mDatabaseKDBX?.let { database ->
            return Group(database.createGroup()).apply {
                setNodeId(database.newGroupId())
            }
        }

        return null
    }

    fun getEntryById(id: NodeId<UUID>): Entry? {
        mDatabaseKDB?.getEntryById(id)?.let {
            return Entry(it)
        }
        mDatabaseKDBX?.getEntryById(id)?.let {
            return Entry(it)
        }
        return null
    }

    fun getGroupById(id: NodeId<*>): Group? {
        if (id is NodeIdInt)
            mDatabaseKDB?.getGroupById(id)?.let {
                return Group(it)
            }
        else if (id is NodeIdUUID)
            mDatabaseKDBX?.getGroupById(id)?.let {
                return Group(it)
            }
        return null
    }

    fun addEntryTo(entry: Entry, parent: Group) {
        entry.entryKDB?.let { entryKDB ->
            mDatabaseKDB?.addEntryTo(entryKDB, parent.groupKDB)
        }
        entry.entryKDBX?.let { entryKDBX ->
            mDatabaseKDBX?.addEntryTo(entryKDBX, parent.groupKDBX)
        }
        entry.afterAssignNewParent()
    }

    fun updateEntry(entry: Entry) {
        entry.entryKDB?.let { entryKDB ->
            mDatabaseKDB?.updateEntry(entryKDB)
        }
        entry.entryKDBX?.let { entryKDBX ->
            mDatabaseKDBX?.updateEntry(entryKDBX)
        }
    }

    fun removeEntryFrom(entry: Entry, parent: Group) {
        entry.entryKDB?.let { entryKDB ->
            mDatabaseKDB?.removeEntryFrom(entryKDB, parent.groupKDB)
        }
        entry.entryKDBX?.let { entryKDBX ->
            mDatabaseKDBX?.removeEntryFrom(entryKDBX, parent.groupKDBX)
        }
        entry.afterAssignNewParent()
    }

    fun addGroupTo(group: Group, parent: Group) {
        group.groupKDB?.let { entryKDB ->
            mDatabaseKDB?.addGroupTo(entryKDB, parent.groupKDB)
        }
        group.groupKDBX?.let { entryKDBX ->
            mDatabaseKDBX?.addGroupTo(entryKDBX, parent.groupKDBX)
        }
        group.afterAssignNewParent()
    }

    fun updateGroup(group: Group) {
        group.groupKDB?.let { entryKDB ->
            mDatabaseKDB?.updateGroup(entryKDB)
        }
        group.groupKDBX?.let { entryKDBX ->
            mDatabaseKDBX?.updateGroup(entryKDBX)
        }
    }

    fun removeGroupFrom(group: Group, parent: Group) {
        group.groupKDB?.let { entryKDB ->
            mDatabaseKDB?.removeGroupFrom(entryKDB, parent.groupKDB)
        }
        group.groupKDBX?.let { entryKDBX ->
            mDatabaseKDBX?.removeGroupFrom(entryKDBX, parent.groupKDBX)
        }
        group.afterAssignNewParent()
    }

    /**
     * @return A duplicate entry with the same values, a new random UUID and a new parent
     * @param entryToCopy
     * @param newParent
     */
    fun copyEntryTo(entryToCopy: Entry, newParent: Group): Entry? {
        val entryCopied = Entry(entryToCopy, false)
        entryCopied.nodeId = mDatabaseKDB?.newEntryId() ?: mDatabaseKDBX?.newEntryId() ?: NodeIdUUID()
        entryCopied.parent = newParent
        entryCopied.title += " (~)"
        addEntryTo(entryCopied, newParent)
        return entryCopied
    }

    fun moveEntryTo(entryToMove: Entry, newParent: Group) {
        entryToMove.parent?.let {
            removeEntryFrom(entryToMove, it)
        }
        addEntryTo(entryToMove, newParent)
    }

    fun moveGroupTo(groupToMove: Group, newParent: Group) {
        groupToMove.parent?.let {
            removeGroupFrom(groupToMove, it)
        }
        addGroupTo(groupToMove, newParent)
    }

    fun deleteEntry(entry: Entry) {
        entry.parent?.let {
            removeEntryFrom(entry, it)
        }
    }

    fun deleteGroup(group: Group) {
        group.doForEachChildAndForIt(
                object : NodeHandler<Entry>() {
                    override fun operate(node: Entry): Boolean {
                        deleteEntry(node)
                        return true
                    }
                },
                object : NodeHandler<Group>() {
                    override fun operate(node: Group): Boolean {
                        node.parent?.let {
                            removeGroupFrom(node, it)
                        }
                        return true
                    }
                })
    }

    fun undoDeleteEntry(entry: Entry, parent: Group) {
        entry.entryKDB?.let {
            mDatabaseKDB?.undoDeleteEntryFrom(it, parent.groupKDB)
        }
        entry.entryKDBX?.let {
            mDatabaseKDBX?.undoDeleteEntryFrom(it, parent.groupKDBX)
        }
    }

    fun undoDeleteGroup(group: Group, parent: Group) {
        group.groupKDB?.let {
            mDatabaseKDB?.undoDeleteGroupFrom(it, parent.groupKDB)
        }
        group.groupKDBX?.let {
            mDatabaseKDBX?.undoDeleteGroupFrom(it, parent.groupKDBX)
        }
    }

    fun canRecycle(entry: Entry): Boolean {
        var canRecycle: Boolean? = null
        entry.entryKDB?.let {
            canRecycle = mDatabaseKDB?.canRecycle()
        }
        entry.entryKDBX?.let {
            canRecycle = mDatabaseKDBX?.canRecycle(it)
        }
        return canRecycle ?: false
    }

    fun canRecycle(group: Group): Boolean {
        var canRecycle: Boolean? = null
        group.groupKDB?.let {
            canRecycle = mDatabaseKDB?.canRecycle()
        }
        group.groupKDBX?.let {
            canRecycle = mDatabaseKDBX?.canRecycle(it)
        }
        return canRecycle ?: false
    }

    fun recycle(entry: Entry, resources: Resources) {
        entry.entryKDB?.let {
            mDatabaseKDB?.recycle(it)
        }
        entry.entryKDBX?.let {
            mDatabaseKDBX?.recycle(it, resources)
        }
    }

    fun recycle(group: Group, resources: Resources) {
        group.groupKDB?.let {
            mDatabaseKDB?.recycle(it)
        }
        group.groupKDBX?.let {
            mDatabaseKDBX?.recycle(it, resources)
        }
    }

    fun undoRecycle(entry: Entry, parent: Group) {
        entry.entryKDB?.let { entryKDB ->
            parent.groupKDB?.let { parentKDB ->
                mDatabaseKDB?.undoRecycle(entryKDB, parentKDB)
            }
        }
        entry.entryKDBX?.let { entryKDBX ->
            parent.groupKDBX?.let { parentKDBX ->
                mDatabaseKDBX?.undoRecycle(entryKDBX, parentKDBX)
            }
        }
    }

    fun undoRecycle(group: Group, parent: Group) {
        group.groupKDB?.let { groupKDB ->
            parent.groupKDB?.let { parentKDB ->
                mDatabaseKDB?.undoRecycle(groupKDB, parentKDB)
            }
        }
        group.groupKDBX?.let { entryKDBX ->
            parent.groupKDBX?.let { parentKDBX ->
                mDatabaseKDBX?.undoRecycle(entryKDBX, parentKDBX)
            }
        }
    }

    fun startManageEntry(entry: Entry) {
        mDatabaseKDBX?.let {
            entry.startToManageFieldReferences(it)
        }
    }

    fun stopManageEntry(entry: Entry) {
        mDatabaseKDBX?.let {
            entry.stopToManageFieldReferences()
        }
    }

    /**
     * Remove oldest history for each entry if more than max items or max memory
     */
    fun removeOldestHistoryForEachEntry() {
        rootGroup?.doForEachChildAndForIt(
                object : NodeHandler<Entry>() {
                    override fun operate(node: Entry): Boolean {
                        removeOldestEntryHistory(node)
                        return true
                    }
                },
                object : NodeHandler<Group>() {
                    override fun operate(node: Group): Boolean {
                        return true
                    }
                })
    }

    fun removeEachEntryHistory() {
        rootGroup?.doForEachChildAndForIt(
                object : NodeHandler<Entry>() {
                    override fun operate(node: Entry): Boolean {
                        node.removeAllHistory()
                        return true
                    }
                },
                object : NodeHandler<Group>() {
                    override fun operate(node: Group): Boolean {
                        return true
                    }
                })
    }

    /**
     * Remove oldest history if more than max items or max memory
     */
    fun removeOldestEntryHistory(entry: Entry) {
        mDatabaseKDBX?.let {

            val maxItems = historyMaxItems
            if (maxItems >= 0) {
                while (entry.getHistory().size > maxItems) {
                    entry.removeOldestEntryFromHistory()
                }
            }

            val maxSize = historyMaxSize
            if (maxSize >= 0) {
                while (true) {
                    var historySize: Long = 0
                    for (entryHistory in entry.getHistory()) {
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
