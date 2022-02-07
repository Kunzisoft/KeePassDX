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
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.kunzisoft.androidclearchroma.ChromaUtil
import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.database.element.binary.AttachmentPool
import com.kunzisoft.keepass.database.element.binary.BinaryCache
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.database.DatabaseKDB
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.icon.IconsManager
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.NodeIdInt
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.database.element.template.TemplateEngine
import com.kunzisoft.keepass.database.exception.*
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDB
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_40
import com.kunzisoft.keepass.database.file.input.DatabaseInputKDB
import com.kunzisoft.keepass.database.file.input.DatabaseInputKDBX
import com.kunzisoft.keepass.database.file.output.DatabaseOutputKDB
import com.kunzisoft.keepass.database.file.output.DatabaseOutputKDBX
import com.kunzisoft.keepass.database.merge.DatabaseKDBXMerger
import com.kunzisoft.keepass.database.search.SearchHelper
import com.kunzisoft.keepass.database.search.SearchParameters
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.MainCredential
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.SingletonHolder
import com.kunzisoft.keepass.utils.UriUtil
import com.kunzisoft.keepass.utils.readBytes4ToUInt
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

    val iconDrawableFactory = IconDrawableFactory(
            { binaryCache },
            { iconId -> iconsManager.getBinaryForCustomIcon(iconId) }
    )

    var loaded = false
        set(value) {
            field = value
            loadTimestamp = if (field) System.currentTimeMillis() else null
        }

    /**
     * To reload the main activity
     */
    var wasReloaded = false

    var dataModifiedSinceLastLoading = false

    var loadTimestamp: Long? = null
        private set

    /**
     * Cipher key regenerated when the database is loaded and closed
     * Can be used to temporarily store database elements
     */
    var binaryCache: BinaryCache
        private set(value) {
            mDatabaseKDB?.binaryCache = value
            mDatabaseKDBX?.binaryCache = value
        }
        get() {
            return mDatabaseKDB?.binaryCache ?: mDatabaseKDBX?.binaryCache ?: BinaryCache()
        }

    private val iconsManager: IconsManager
        get() {
            return mDatabaseKDB?.iconsManager ?: mDatabaseKDBX?.iconsManager ?: IconsManager()
        }

    fun doForEachStandardIcons(action: (IconImageStandard) -> Unit) {
        return iconsManager.doForEachStandardIcon(action)
    }

    fun getStandardIcon(iconId: Int): IconImageStandard {
        return iconsManager.getIcon(iconId)
    }

    val allowCustomIcons: Boolean
        get() = mDatabaseKDBX != null

    fun doForEachCustomIcons(action: (IconImageCustom, BinaryData) -> Unit) {
        return iconsManager.doForEachCustomIcon(action)
    }

    fun getCustomIcon(iconId: UUID): IconImageCustom? {
        return iconsManager.getIcon(iconId)
    }

    fun buildNewCustomIcon(result: (IconImageCustom?, BinaryData?) -> Unit) {
        mDatabaseKDBX?.buildNewCustomIcon(null, result)
    }

    fun isCustomIconBinaryDuplicate(binaryData: BinaryData): Boolean {
        return mDatabaseKDBX?.isCustomIconBinaryDuplicate(binaryData) ?: false
    }

    fun removeCustomIcon(customIcon: IconImageCustom) {
        iconDrawableFactory.clearFromCache(customIcon)
        iconsManager.removeCustomIcon(customIcon.uuid, binaryCache)
        mDatabaseKDBX?.addDeletedObject(customIcon.uuid)
    }

    fun updateCustomIcon(customIcon: IconImageCustom) {
        iconsManager.getIcon(customIcon.uuid)?.updateWith(customIcon)
    }

    fun getTemplates(templateCreation: Boolean): List<Template> {
        return mDatabaseKDBX?.getTemplates(templateCreation) ?: listOf()
    }

    fun getTemplate(entry: Entry): Template? {
        if (entryIsTemplate(entry))
            return TemplateEngine.CREATION
        entry.entryKDBX?.let { entryKDBX ->
            return mDatabaseKDBX?.getTemplate(entryKDBX)
        }
        return null
    }

    fun entryIsTemplate(entry: Entry?): Boolean {
        // Define is current entry is a template (in direct template group)
        if (entry == null || templatesGroup == null)
            return false
        return templatesGroup == entry.parent
    }

    // Not the same as decode, here remove in all cases the template link in the entry data
    fun removeTemplateConfiguration(entry: Entry): Entry {
        entry.entryKDBX?.let {
            mDatabaseKDBX?.decodeEntryWithTemplateConfiguration(it, false)?.let { decode ->
                return Entry(decode)
            }
        }
        return entry
    }

    // Remove the template link in the entry data if it's a basic entry
    // or compress the template fields (as pseudo language) if it's a template entry
    fun decodeEntryWithTemplateConfiguration(entry: Entry, lastEntryVersion: Entry? = null): Entry {
        entry.entryKDBX?.let {
            val lastEntry = lastEntryVersion ?: entry
            mDatabaseKDBX?.decodeEntryWithTemplateConfiguration(it, entryIsTemplate(lastEntry))?.let { decode ->
                return Entry(decode)
            }
        }
        return entry
    }

    fun encodeEntryWithTemplateConfiguration(entry: Entry, template: Template): Entry {
        entry.entryKDBX?.let {
            mDatabaseKDBX?.encodeEntryWithTemplateConfiguration(it, entryIsTemplate(entry), template)?.let { encode ->
                return Entry(encode)
            }
        }
        return entry
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
            dataModifiedSinceLastLoading = true
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
            dataModifiedSinceLastLoading = true
        }

    var defaultUsername: String
        get() {
            return mDatabaseKDB?.defaultUserName ?: mDatabaseKDBX?.defaultUserName ?: ""
        }
        set(username) {
            mDatabaseKDB?.defaultUserName = username
            mDatabaseKDBX?.defaultUserName = username
            mDatabaseKDBX?.defaultUserNameChanged = DateInstant()
            dataModifiedSinceLastLoading = true
        }

    var customColor: Int?
        get() {
            var colorInt: Int? = null
            mDatabaseKDBX?.color?.let {
                try {
                    colorInt = Color.parseColor(it)
                } catch (e: Exception) {}
            }
            return mDatabaseKDB?.color ?: colorInt
        }
        set(value) {
            mDatabaseKDB?.color = value
            mDatabaseKDBX?.color = if (value == null) {
                ""
            } else {
                ChromaUtil.getFormattedColorString(value, false)
            }
            mDatabaseKDBX?.settingsChanged = DateInstant()
            dataModifiedSinceLastLoading = true
        }

    val allowOTP: Boolean
        get() = mDatabaseKDBX != null

    val version: String
        get() = mDatabaseKDB?.version ?: mDatabaseKDBX?.version ?: "-"

    fun checkVersion() {
        mDatabaseKDBX?.getMinKdbxVersion()?.let {
            mDatabaseKDBX?.kdbxVersion = it
        }
    }

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
            mDatabaseKDBX?.settingsChanged = DateInstant()
            dataModifiedSinceLastLoading = true
        }

    fun compressionForNewEntry(): Boolean {
        if (mDatabaseKDB != null)
            return false
        // Default compression not necessary if stored in header
        mDatabaseKDBX?.let {
            return it.compressionAlgorithm == CompressionAlgorithm.GZip
                    && it.kdbxVersion.isBefore(FILE_VERSION_40)
        }
        return false
    }

    fun updateDataBinaryCompression(oldCompression: CompressionAlgorithm,
                                    newCompression: CompressionAlgorithm) {
        mDatabaseKDBX?.changeBinaryCompression(oldCompression, newCompression)
        dataModifiedSinceLastLoading = true
    }

    val allowNoMasterKey: Boolean
        get() = mDatabaseKDBX != null

    fun getEncryptionAlgorithmName(): String {
        return mDatabaseKDB?.encryptionAlgorithm?.toString()
                ?: mDatabaseKDBX?.encryptionAlgorithm?.toString()
                ?: ""
    }

    val availableEncryptionAlgorithms: List<EncryptionAlgorithm>
        get() = mDatabaseKDB?.availableEncryptionAlgorithms ?: mDatabaseKDBX?.availableEncryptionAlgorithms ?: ArrayList()

    var encryptionAlgorithm: EncryptionAlgorithm?
        get() = mDatabaseKDB?.encryptionAlgorithm ?: mDatabaseKDBX?.encryptionAlgorithm
        set(algorithm) {
            algorithm?.let {
                mDatabaseKDBX?.encryptionAlgorithm = algorithm
            }
        }

    val availableKdfEngines: List<KdfEngine>
        get() = mDatabaseKDB?.kdfAvailableList ?: mDatabaseKDBX?.kdfAvailableList ?: ArrayList()

    val allowKdfModification: Boolean
        get() = availableKdfEngines.size > 1

    var kdfEngine: KdfEngine?
        get() = mDatabaseKDB?.kdfEngine ?: mDatabaseKDBX?.kdfEngine
        set(kdfEngine) {
            mDatabaseKDB?.kdfEngine = kdfEngine
            mDatabaseKDBX?.kdfEngine = kdfEngine
            mDatabaseKDBX?.settingsChanged = DateInstant()
            dataModifiedSinceLastLoading = true
        }

    fun getKeyDerivationName(): String {
        return kdfEngine?.toString() ?: ""
    }

    var numberKeyEncryptionRounds: Long
        get() = mDatabaseKDB?.numberKeyEncryptionRounds ?: mDatabaseKDBX?.numberKeyEncryptionRounds ?: 0
        set(numberRounds) {
            mDatabaseKDB?.numberKeyEncryptionRounds = numberRounds
            mDatabaseKDBX?.numberKeyEncryptionRounds = numberRounds
            mDatabaseKDBX?.settingsChanged = DateInstant()
            dataModifiedSinceLastLoading = true
        }

    var memoryUsage: Long
        get() {
            return mDatabaseKDBX?.memoryUsage ?: return KdfEngine.UNKNOWN_VALUE
        }
        set(memory) {
            mDatabaseKDBX?.memoryUsage = memory
            mDatabaseKDBX?.settingsChanged = DateInstant()
            dataModifiedSinceLastLoading = true
        }

    var parallelism: Long
        get() = mDatabaseKDBX?.parallelism ?: KdfEngine.UNKNOWN_VALUE
        set(parallelism) {
            mDatabaseKDBX?.parallelism = parallelism
            mDatabaseKDBX?.settingsChanged = DateInstant()
            dataModifiedSinceLastLoading = true
        }

    var masterKey: ByteArray
        get() = mDatabaseKDB?.masterKey ?: mDatabaseKDBX?.masterKey ?: ByteArray(32)
        set(masterKey) {
            mDatabaseKDB?.masterKey = masterKey
            mDatabaseKDBX?.masterKey = masterKey
            mDatabaseKDBX?.settingsChanged = DateInstant()
            dataModifiedSinceLastLoading = true
        }

    var rootGroup: Group?
        get() {
            mDatabaseKDB?.rootGroup?.let {
                return Group(it)
            }
            mDatabaseKDBX?.rootGroup?.let {
                return Group(it)
            }
            return null
        }
        set(value) {
            value?.groupKDB?.let { rootKDB ->
                mDatabaseKDB?.rootGroup = rootKDB
            }
            value?.groupKDBX?.let { rootKDBX ->
                mDatabaseKDBX?.rootGroup = rootKDBX
            }
        }

    val rootGroupIsVirtual: Boolean
        get() {
            mDatabaseKDB?.let {
                return true
            }
            mDatabaseKDBX?.let {
                return false
            }
            return true
        }

    /**
     * Do not modify groups here, used for read only
     */
    fun getAllGroupsWithoutRoot(): List<Group> {
        return mDatabaseKDB?.getAllGroupsWithoutRoot()?.map { Group(it) }
            ?: mDatabaseKDBX?.getAllGroupsWithoutRoot()?.map { Group(it) }
            ?: listOf()
    }

    val manageHistory: Boolean
        get() = mDatabaseKDBX != null

    var historyMaxItems: Int
        get() {
            return mDatabaseKDBX?.historyMaxItems ?: 0
        }
        set(value) {
            mDatabaseKDBX?.historyMaxItems = value
            mDatabaseKDBX?.settingsChanged = DateInstant()
            dataModifiedSinceLastLoading = true
        }

    var historyMaxSize: Long
        get() {
            return mDatabaseKDBX?.historyMaxSize ?: 0
        }
        set(value) {
            mDatabaseKDBX?.historyMaxSize = value
            mDatabaseKDBX?.settingsChanged = DateInstant()
            dataModifiedSinceLastLoading = true
        }

    /**
     * Determine if a configurable RecycleBin is available or not for this version of database
     * @return true if a configurable RecycleBin available
     */
    val allowConfigurableRecycleBin: Boolean
        get() = mDatabaseKDBX != null

    val isRecycleBinEnabled: Boolean
        // Backup is always enabled in KDB database
        get() = mDatabaseKDB != null || mDatabaseKDBX?.isRecycleBinEnabled ?: false

    fun enableRecycleBin(enable: Boolean, resources: Resources) {
        mDatabaseKDBX?.isRecycleBinEnabled = enable
        if (enable) {
            ensureRecycleBinExists(resources)
        } else {
            mDatabaseKDBX?.removeRecycleBin()
        }
        mDatabaseKDBX?.recycleBinChanged = DateInstant()
        dataModifiedSinceLastLoading = true
    }

    val recycleBin: Group?
        get() {
            mDatabaseKDB?.backupGroup?.let {
                return getGroupById(it.nodeId) ?: Group(it)
            }
            mDatabaseKDBX?.recycleBin?.let {
                return getGroupById(it.nodeId) ?: Group(it)
            }
            return null
        }

    fun setRecycleBin(group: Group?) {
        // Only the kdbx recycle bin can be changed
        if (group != null) {
            mDatabaseKDBX?.recycleBinUUID = group.nodeIdKDBX.id
        } else {
            mDatabaseKDBX?.removeRecycleBin()
        }
        mDatabaseKDBX?.recycleBinChanged = DateInstant()
        dataModifiedSinceLastLoading = true
    }

    /**
     * Determine if a configurable templates group is available or not for this version of database
     * @return true if a configurable templates group available
     */
    val allowConfigurableTemplatesGroup: Boolean
        get() = mDatabaseKDBX != null

    // Maybe another templates method with KDBX5
    val isTemplatesEnabled: Boolean
        get() = mDatabaseKDBX?.isTemplatesGroupEnabled() ?: false

    fun enableTemplates(enable: Boolean, templatesGroupName: String) {
        mDatabaseKDBX?.enableTemplatesGroup(enable, templatesGroupName)
        mDatabaseKDBX?.entryTemplatesGroupChanged = DateInstant()
        dataModifiedSinceLastLoading = true
    }

    val templatesGroup: Group?
        get() {
            mDatabaseKDBX?.getTemplatesGroup()?.let {
                return Group(it)
            }
            return null
        }

    fun setTemplatesGroup(group: Group?) {
        // Only the kdbx templates group can be changed
        if (group != null) {
            mDatabaseKDBX?.entryTemplatesGroup = group.nodeIdKDBX.id
        } else {
            mDatabaseKDBX?.removeTemplatesGroup()
        }
        mDatabaseKDBX?.entryTemplatesGroupChanged = DateInstant()
        dataModifiedSinceLastLoading = true
    }

    val groupNamesNotAllowed: List<String>
        get() {
            return mDatabaseKDB?.groupNamesNotAllowed ?: ArrayList()
        }

    private fun setDatabaseKDB(databaseKDB: DatabaseKDB) {
        this.mDatabaseKDB = databaseKDB
        this.mDatabaseKDBX = null
    }

    private fun setDatabaseKDBX(databaseKDBX: DatabaseKDBX) {
        this.mDatabaseKDB = null
        this.mDatabaseKDBX = databaseKDBX
    }

    fun createData(databaseUri: Uri,
                   databaseName: String,
                   rootName: String,
                   templateGroupName: String?) {
        val newDatabase = DatabaseKDBX(databaseName, rootName, templateGroupName)
        setDatabaseKDBX(newDatabase)
        this.fileUri = databaseUri
        // Set Database state
        this.loaded = true
        this.dataModifiedSinceLastLoading = false
    }

    @Throws(LoadDatabaseException::class)
    private fun readDatabaseStream(contentResolver: ContentResolver, uri: Uri,
                                   openDatabaseKDB: (InputStream) -> DatabaseKDB,
                                   openDatabaseKDBX: (InputStream) -> DatabaseKDBX) {
        var databaseInputStream: InputStream? = null
        try {
            // Load Data, pass Uris as InputStreams
            val databaseStream = UriUtil.getUriInputStream(contentResolver, uri)
                    ?: throw IOException("Database input stream cannot be retrieve")

            databaseInputStream = BufferedInputStream(databaseStream)
            if (!databaseInputStream.markSupported()) {
                throw IOException("Input stream does not support mark.")
            }

            // We'll end up reading 8 bytes to identify the header. Might as well use two extra.
            databaseInputStream.mark(10)

            // Get the file directory to save the attachments
            val sig1 = databaseInputStream.readBytes4ToUInt()
            val sig2 = databaseInputStream.readBytes4ToUInt()

            // Return to the start
            databaseInputStream.reset()

            when {
                // Header of database KDB
                DatabaseHeaderKDB.matchesHeader(sig1, sig2) -> setDatabaseKDB(openDatabaseKDB(databaseInputStream))

                // Header of database KDBX
                DatabaseHeaderKDBX.matchesHeader(sig1, sig2) -> setDatabaseKDBX(openDatabaseKDBX(databaseInputStream))

                // Header not recognized
                else -> throw SignatureDatabaseException()
            }

            this.mSearchHelper = SearchHelper()
            loaded = true
        } catch (e: LoadDatabaseException) {
            throw e
        } catch (e: Exception) {
            throw LoadDatabaseException(e)
        } finally {
            databaseInputStream?.close()
        }
    }

    @Throws(LoadDatabaseException::class)
    fun loadData(uri: Uri,
                 mainCredential: MainCredential,
                 readOnly: Boolean,
                 contentResolver: ContentResolver,
                 cacheDirectory: File,
                 isRAMSufficient: (memoryWanted: Long) -> Boolean,
                 fixDuplicateUUID: Boolean,
                 progressTaskUpdater: ProgressTaskUpdater?) {

        // Save database URI
        this.fileUri = uri

        // Check if the file is writable
        this.isReadOnly = readOnly

        // Pass KeyFile Uri as InputStreams
        var keyFileInputStream: InputStream? = null
        try {
            // Get keyFile inputStream
            mainCredential.keyFileUri?.let { keyFile ->
                keyFileInputStream = UriUtil.getUriInputStream(contentResolver, keyFile)
            }

            // Read database stream for the first time
            readDatabaseStream(contentResolver, uri,
                    { databaseInputStream ->
                        val databaseKDB = DatabaseKDB().apply {
                            binaryCache.cacheDirectory = cacheDirectory
                            changeDuplicateId = fixDuplicateUUID
                        }
                        DatabaseInputKDB(databaseKDB)
                            .openDatabase(databaseInputStream,
                                mainCredential.masterPassword,
                                keyFileInputStream,
                                progressTaskUpdater)
                        databaseKDB
                    },
                    { databaseInputStream ->
                        val databaseKDBX = DatabaseKDBX().apply {
                            binaryCache.cacheDirectory = cacheDirectory
                            changeDuplicateId = fixDuplicateUUID
                        }
                        DatabaseInputKDBX(databaseKDBX).apply {
                            setMethodToCheckIfRAMIsSufficient(isRAMSufficient)
                            openDatabase(databaseInputStream,
                                mainCredential.masterPassword,
                                keyFileInputStream,
                                progressTaskUpdater)
                        }
                        databaseKDBX
                    }
            )
        } catch (e: FileNotFoundException) {
            throw FileNotFoundDatabaseException("Unable to load the keyfile")
        } catch (e: LoadDatabaseException) {
            throw e
        } catch (e: Exception) {
            throw LoadDatabaseException(e)
        } finally {
            keyFileInputStream?.close()
            dataModifiedSinceLastLoading = false
        }
    }

    fun isMergeDataAllowed(): Boolean {
        return mDatabaseKDBX != null
    }

    @Throws(LoadDatabaseException::class)
    fun mergeData(contentResolver: ContentResolver,
                  isRAMSufficient: (memoryWanted: Long) -> Boolean,
                  progressTaskUpdater: ProgressTaskUpdater?) {

        mDatabaseKDB?.let {
            throw IODatabaseException("Unable to merge from a database V1")
        }

        // New database instance to get new changes
        val databaseToMerge = Database()
        databaseToMerge.fileUri = this.fileUri

        try {
            databaseToMerge.fileUri?.let { databaseUri ->

                val databaseKDB = DatabaseKDB()
                val databaseKDBX = DatabaseKDBX()

                databaseToMerge.readDatabaseStream(contentResolver, databaseUri,
                    { databaseInputStream ->
                        DatabaseInputKDB(databaseKDB)
                            .openDatabase(databaseInputStream,
                                masterKey,
                                progressTaskUpdater)
                        databaseKDB
                    },
                    { databaseInputStream ->
                        DatabaseInputKDBX(databaseKDBX).apply {
                            setMethodToCheckIfRAMIsSufficient(isRAMSufficient)
                            openDatabase(databaseInputStream,
                                masterKey,
                                progressTaskUpdater)
                        }
                        databaseKDBX
                    }
                )

                mDatabaseKDBX?.let { currentDatabaseKDBX ->
                    val databaseMerger = DatabaseKDBXMerger(currentDatabaseKDBX).apply {
                        this.isRAMSufficient = isRAMSufficient
                    }
                    databaseToMerge.mDatabaseKDB?.let { databaseKDBToMerge ->
                        databaseMerger.merge(databaseKDBToMerge)
                    }
                    databaseToMerge.mDatabaseKDBX?.let { databaseKDBXToMerge ->
                        databaseMerger.merge(databaseKDBXToMerge)
                    }
                }
            } ?: run {
                throw IODatabaseException("Database URI is null, database cannot be reloaded")
            }
        } catch (e: FileNotFoundException) {
            throw FileNotFoundDatabaseException("Unable to load the keyfile")
        } catch (e: LoadDatabaseException) {
            throw e
        } catch (e: Exception) {
            throw LoadDatabaseException(e)
        } finally {
            databaseToMerge.clearAndClose()
        }
    }

    @Throws(LoadDatabaseException::class)
    fun reloadData(contentResolver: ContentResolver,
                   isRAMSufficient: (memoryWanted: Long) -> Boolean,
                   progressTaskUpdater: ProgressTaskUpdater?) {

        // Retrieve the stream from the old database URI
        try {
            fileUri?.let { oldDatabaseUri ->
                readDatabaseStream(contentResolver, oldDatabaseUri,
                    { databaseInputStream ->
                        val databaseKDB = DatabaseKDB()
                        mDatabaseKDB?.let {
                            databaseKDB.binaryCache = it.binaryCache
                        }
                        DatabaseInputKDB(databaseKDB)
                            .openDatabase(databaseInputStream,
                                masterKey,
                                progressTaskUpdater)
                        databaseKDB
                    },
                    { databaseInputStream ->
                        val databaseKDBX = DatabaseKDBX()
                        mDatabaseKDBX?.let {
                            databaseKDBX.binaryCache = it.binaryCache
                        }
                        DatabaseInputKDBX(databaseKDBX).apply {
                            setMethodToCheckIfRAMIsSufficient(isRAMSufficient)
                            openDatabase(databaseInputStream,
                                masterKey,
                                progressTaskUpdater)
                        }
                        databaseKDBX
                    }
                )
            } ?: run {
                throw IODatabaseException("Database URI is null, database cannot be reloaded")
            }
        } catch (e: FileNotFoundException) {
            throw FileNotFoundDatabaseException("Unable to load the keyfile")
        } catch (e: LoadDatabaseException) {
            throw e
        } catch (e: Exception) {
            throw LoadDatabaseException(e)
        } finally {
            dataModifiedSinceLastLoading = false
        }
    }

    fun groupIsInRecycleBin(group: Group): Boolean {
        val groupKDB = group.groupKDB
        val groupKDBX = group.groupKDBX
        if (groupKDB != null) {
            return mDatabaseKDB?.isInRecycleBin(groupKDB) ?: false
        } else if (groupKDBX != null) {
            return mDatabaseKDBX?.isInRecycleBin(groupKDBX) ?: false
        }
        return false
    }

    fun groupIsInTemplates(group: Group): Boolean {
        val groupKDBX = group.groupKDBX
        if (groupKDBX != null) {
            return mDatabaseKDBX?.getTemplatesGroup() == groupKDBX
        }
        return false
    }

    fun createVirtualGroupFromSearch(searchParameters: SearchParameters,
                                     fromGroup: NodeId<*>? = null,
                                     max: Int = Integer.MAX_VALUE): Group? {
        return mSearchHelper?.createVirtualGroupWithSearchResult(this,
            searchParameters, fromGroup, max)
    }

    fun createVirtualGroupFromSearchInfo(searchInfoString: String,
                                         max: Int = Integer.MAX_VALUE): Group? {
        return mSearchHelper?.createVirtualGroupWithSearchResult(this,
                SearchParameters().apply {
                    searchQuery = searchInfoString
                    searchInTitles = true
                    searchInUsernames = false
                    searchInPasswords = false
                    searchInUrls = true
                    searchInNotes = true
                    searchInOTP = false
                    searchInOther = true
                    searchInUUIDs = false
                    searchInTags = false
                    searchInCurrentGroup = false
                    searchInSearchableGroup = true
                    searchInRecycleBin = false
                    searchInTemplates = false
                }, null, max)
    }

    val tagPool: Tags
        get() {
            return mDatabaseKDBX?.tagPool ?: Tags()
        }

    val attachmentPool: AttachmentPool
        get() {
            return mDatabaseKDB?.attachmentPool ?: mDatabaseKDBX?.attachmentPool ?: AttachmentPool()
        }

    val allowMultipleAttachments: Boolean
        get() {
            if (mDatabaseKDB != null)
                return false
            if (mDatabaseKDBX != null)
                return true
            return false
        }

    fun buildNewBinaryAttachment(): BinaryData? {
        return mDatabaseKDB?.buildNewBinaryAttachment()
                ?: mDatabaseKDBX?.buildNewBinaryAttachment( false,
                        compressionForNewEntry(),
                        false)
    }

    fun removeAttachmentIfNotUsed(attachment: Attachment) {
        // No need in KDB database because unique attachment by entry
        // Don't clear to fix upload multiple times
        mDatabaseKDBX?.removeUnlinkedAttachment(attachment.binaryData, false)
    }

    fun removeUnlinkedAttachments() {
        // No check in database KDB because unique attachment by entry
        mDatabaseKDBX?.removeUnlinkedAttachments(true)
        dataModifiedSinceLastLoading = true
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
                outputStream = contentResolver.openOutputStream(uri, "rwt")
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
        this.dataModifiedSinceLastLoading = false
    }

    fun clearIndexesAndBinaries(filesDirectory: File? = null) {
        this.mDatabaseKDB?.clearIndexes()
        this.mDatabaseKDBX?.clearIndexes()

        this.mDatabaseKDB?.clearIconsCache()
        this.mDatabaseKDBX?.clearIconsCache()

        this.mDatabaseKDB?.clearAttachmentsCache()
        this.mDatabaseKDBX?.clearAttachmentsCache()

        this.mDatabaseKDB?.clearBinaries()
        this.mDatabaseKDBX?.clearBinaries()

        iconDrawableFactory.clearCache()

        // delete all the files in the temp dir if allowed
        try {
            filesDirectory?.let { directory ->
                cleanDirectory(directory)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to clear the directory cache.", e)
        }
    }

    fun clearAndClose(context: Context? = null) {
        clearIndexesAndBinaries(context?.let { UriUtil.getBinaryDir(context) })
        this.mDatabaseKDB = null
        this.mDatabaseKDBX = null
        this.fileUri = null
        this.loaded = false
    }

    private fun cleanDirectory(directory: File) {
        directory.listFiles()?.let { files ->
            for (file in files) {
                if (file.isDirectory) {
                    cleanDirectory(file)
                }
                file.delete()
            }
        }
    }

    fun validatePasswordEncoding(mainCredential: MainCredential): Boolean {
        val password = mainCredential.masterPassword
        val containsKeyFile = mainCredential.keyFileUri != null
        return mDatabaseKDB?.validatePasswordEncoding(password, containsKeyFile)
                ?: mDatabaseKDBX?.validatePasswordEncoding(password, containsKeyFile)
                ?: false
    }

    @Throws(IOException::class)
    fun assignMasterKey(key: String?, keyInputStream: InputStream?) {
        mDatabaseKDB?.retrieveMasterKey(key, keyInputStream)
        mDatabaseKDBX?.retrieveMasterKey(key, keyInputStream)
        mDatabaseKDBX?.keyLastChanged = DateInstant()
    }

    fun rootCanContainsEntry(): Boolean {
        return mDatabaseKDB?.rootCanContainsEntry() ?: mDatabaseKDBX?.rootCanContainsEntry() ?: false
    }

    fun createEntry(): Entry? {
        dataModifiedSinceLastLoading = true
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

    fun createGroup(virtual: Boolean = false): Group? {
        if (!virtual) {
            dataModifiedSinceLastLoading = true
        }
        var group: Group? = null
        mDatabaseKDB?.let { database ->
            group = Group(database.createGroup()).apply {
                setNodeId(database.newGroupId())
            }
        }
        mDatabaseKDBX?.let { database ->
            group = Group(database.createGroup()).apply {
                setNodeId(database.newGroupId())
            }
        }
        if (virtual)
            group?.isVirtual = virtual

        return group
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
        dataModifiedSinceLastLoading = true
        entry.entryKDB?.let { entryKDB ->
            mDatabaseKDB?.addEntryTo(entryKDB, parent.groupKDB)
        }
        entry.entryKDBX?.let { entryKDBX ->
            mDatabaseKDBX?.addEntryTo(entryKDBX, parent.groupKDBX)
        }
        entry.afterAssignNewParent()
    }

    fun updateEntry(entry: Entry) {
        dataModifiedSinceLastLoading = true
        entry.entryKDB?.let { entryKDB ->
            mDatabaseKDB?.updateEntry(entryKDB)
        }
        entry.entryKDBX?.let { entryKDBX ->
            mDatabaseKDBX?.updateEntry(entryKDBX)
        }
    }

    fun removeEntryFrom(entry: Entry, parent: Group) {
        dataModifiedSinceLastLoading = true
        entry.entryKDB?.let { entryKDB ->
            mDatabaseKDB?.removeEntryFrom(entryKDB, parent.groupKDB)
        }
        entry.entryKDBX?.let { entryKDBX ->
            mDatabaseKDBX?.removeEntryFrom(entryKDBX, parent.groupKDBX)
        }
        entry.afterAssignNewParent()
    }

    fun addGroupTo(group: Group, parent: Group) {
        dataModifiedSinceLastLoading = true
        group.groupKDB?.let { groupKDB ->
            mDatabaseKDB?.addGroupTo(groupKDB, parent.groupKDB)
        }
        group.groupKDBX?.let { groupKDBX ->
            mDatabaseKDBX?.addGroupTo(groupKDBX, parent.groupKDBX)
        }
        group.afterAssignNewParent()
    }

    fun updateGroup(group: Group) {
        dataModifiedSinceLastLoading = true
        group.groupKDB?.let { entryKDB ->
            mDatabaseKDB?.updateGroup(entryKDB)
        }
        group.groupKDBX?.let { entryKDBX ->
            mDatabaseKDBX?.updateGroup(entryKDBX)
        }
    }

    fun removeGroupFrom(group: Group, parent: Group) {
        dataModifiedSinceLastLoading = true
        group.groupKDB?.let { groupKDB ->
            mDatabaseKDB?.removeGroupFrom(groupKDB, parent.groupKDB)
        }
        group.groupKDBX?.let { groupKDBX ->
            mDatabaseKDBX?.removeGroupFrom(groupKDBX, parent.groupKDBX)
        }
        group.afterAssignNewParent()
    }

    /**
     * @return A duplicate entry with the same values, a new random UUID and a new parent
     * @param entryToCopy
     * @param newParent
     */
    fun copyEntryTo(entryToCopy: Entry, newParent: Group): Entry {
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
        dataModifiedSinceLastLoading = true
        entry.entryKDBX?.id?.let { entryId ->
            mDatabaseKDBX?.addDeletedObject(entryId)
        }
        entry.parent?.let {
            removeEntryFrom(entry, it)
        }
    }

    fun deleteGroup(group: Group) {
        dataModifiedSinceLastLoading = true
        group.doForEachChildAndForIt(
                object : NodeHandler<Entry>() {
                    override fun operate(node: Entry): Boolean {
                        deleteEntry(node)
                        return true
                    }
                },
                object : NodeHandler<Group>() {
                    override fun operate(node: Group): Boolean {
                        node.groupKDBX?.id?.let { groupId ->
                            mDatabaseKDBX?.addDeletedObject(groupId)
                        }
                        node.parent?.let {
                            removeGroupFrom(node, it)
                        }
                        return true
                    }
                })
    }

    fun ensureRecycleBinExists(resources: Resources) {
        mDatabaseKDB?.ensureBackupExists()
        mDatabaseKDBX?.ensureRecycleBinExists(resources)
    }

    fun canRecycle(entry: Entry): Boolean {
        var canRecycle: Boolean? = null
        entry.entryKDB?.let {
            canRecycle = mDatabaseKDB?.canRecycle(it)
        }
        entry.entryKDBX?.let {
            canRecycle = mDatabaseKDBX?.canRecycle(it)
        }
        return canRecycle ?: false
    }

    fun canRecycle(group: Group): Boolean {
        var canRecycle: Boolean? = null
        group.groupKDB?.let {
            canRecycle = mDatabaseKDB?.canRecycle(it)
        }
        group.groupKDBX?.let {
            canRecycle = mDatabaseKDBX?.canRecycle(it)
        }
        return canRecycle ?: false
    }

    fun recycle(entry: Entry, resources: Resources) {
        ensureRecycleBinExists(resources)
        entry.parent?.let { parent ->
            removeEntryFrom(entry, parent)
        }
        recycleBin?.let {
            addEntryTo(entry, it)
        }
        entry.afterAssignNewParent()
    }

    fun recycle(group: Group, resources: Resources) {
        ensureRecycleBinExists(resources)
        group.parent?.let { parent ->
            removeGroupFrom(group, parent)
        }
        recycleBin?.let {
            addGroupTo(group, it)
        }
        group.afterAssignNewParent()
    }

    fun undoRecycle(entry: Entry, parent: Group) {
        recycleBin?.let { it ->
            removeEntryFrom(entry, it)
        }
        addEntryTo(entry, parent)
        entry.afterAssignNewParent()
    }

    fun undoRecycle(group: Group, parent: Group) {
        recycleBin?.let {
            removeGroupFrom(group, it)
        }
        addGroupTo(group, parent)
        group.afterAssignNewParent()
    }

    fun startManageEntry(entry: Entry?) {
        mDatabaseKDBX?.let {
            entry?.startToManageFieldReferences(it)
        }
    }

    fun stopManageEntry(entry: Entry?) {
        mDatabaseKDBX?.let {
            entry?.stopToManageFieldReferences()
        }
    }

    /**
     * @return true if database allows custom field
     */
    fun allowEntryCustomFields(): Boolean {
        return mDatabaseKDBX != null
    }

    fun allowCustomSearchableGroup(): Boolean {
        return mDatabaseKDBX != null
    }

    /**
     * Remove oldest history for each entry if more than max items or max memory
     */
    fun removeOldestHistoryForEachEntry() {
        rootGroup?.doForEachChildAndForIt(
                object : NodeHandler<Entry>() {
                    override fun operate(node: Entry): Boolean {
                        removeOldestEntryHistory(node, attachmentPool)
                        return true
                    }
                },
                object : NodeHandler<Group>() {
                    override fun operate(node: Group): Boolean {
                        return true
                    }
                }
        )
    }

    /**
     * Remove oldest history if more than max items or max memory
     */
    fun removeOldestEntryHistory(entry: Entry, attachmentPool: AttachmentPool) {
        mDatabaseKDBX?.let {
            val maxItems = historyMaxItems
            if (maxItems >= 0) {
                while (entry.getHistory().size > maxItems) {
                    removeOldestEntryHistory(entry)
                }
            }

            val maxSize = historyMaxSize
            if (maxSize >= 0) {
                while (true) {
                    var historySize: Long = 0
                    for (entryHistory in entry.getHistory()) {
                        historySize += entryHistory.getSize(attachmentPool)
                    }
                    if (historySize > maxSize) {
                        removeOldestEntryHistory(entry)
                    } else {
                        break
                    }
                }
            }
        }
    }

    private fun removeOldestEntryHistory(entry: Entry) {
        entry.removeOldestEntryFromHistory()?.let {
            it.getAttachments(attachmentPool, false).forEach { attachmentToRemove ->
                removeAttachmentIfNotUsed(attachmentToRemove)
            }
        }
    }

    fun removeEntryHistory(entry: Entry, entryHistoryPosition: Int) {
        entry.removeEntryFromHistory(entryHistoryPosition)?.let {
            it.getAttachments(attachmentPool, false).forEach { attachmentToRemove ->
                removeAttachmentIfNotUsed(attachmentToRemove)
            }
        }
    }

    companion object : SingletonHolder<Database>(::Database) {

        private val TAG = Database::class.java.name
    }
}
