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
package com.kunzisoft.keepass.database.element.database

import android.util.Base64
import android.util.Log
import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.crypto.VariantDictionary
import com.kunzisoft.keepass.database.crypto.kdf.AesKdf
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.database.crypto.kdf.KdfFactory
import com.kunzisoft.keepass.database.crypto.kdf.KdfParameters
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.database.DatabaseKDB.Companion.BACKUP_FOLDER_TITLE
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.entry.FieldReferencesEngine
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.*
import com.kunzisoft.keepass.database.element.security.MemoryProtectionConfig
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.database.element.template.TemplateEngineCompatible
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_31
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_40
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_41
import com.kunzisoft.keepass.hardware.HardwareKey
import com.kunzisoft.keepass.utils.UnsignedInt
import com.kunzisoft.keepass.utils.longTo8Bytes
import java.io.IOException
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac
import kotlin.math.min


class DatabaseKDBX : DatabaseVersioned<UUID, UUID, GroupKDBX, EntryKDBX> {

    // To resave the database with same credential when already loaded
    private var mCompositeKey = CompositeKey()

    var hmacKey: ByteArray? = null
        private set

    override var encryptionAlgorithm: EncryptionAlgorithm = EncryptionAlgorithm.AESRijndael

    fun setEncryptionAlgorithmFromUUID(uuid: UUID) {
        encryptionAlgorithm = EncryptionAlgorithm.getFrom(uuid)
    }

    override val availableEncryptionAlgorithms: List<EncryptionAlgorithm> = listOf(
        EncryptionAlgorithm.AESRijndael,
        EncryptionAlgorithm.Twofish,
        EncryptionAlgorithm.ChaCha20
    )

    var kdfParameters: KdfParameters? = null

    override var kdfEngine: KdfEngine?
        get() = getKdfEngineFromParameters(kdfParameters)
        set(value) {
            value?.let {
                if (kdfParameters?.uuid != value.defaultParameters.uuid)
                    kdfParameters = value.defaultParameters
                numberKeyEncryptionRounds = value.defaultKeyRounds
                memoryUsage = value.defaultMemoryUsage
                parallelism = value.defaultParallelism
            }
        }

    private fun getKdfEngineFromParameters(kdfParameters: KdfParameters?): KdfEngine? {
        if (kdfParameters == null) {
            return null
        }
        for (engine in kdfAvailableList) {
            if (engine.uuid == kdfParameters.uuid) {
                return engine
            }
        }
        return null
    }

    fun randomizeKdfParameters() {
        kdfParameters?.let {
            kdfEngine?.randomize(it)
        }
    }

    override val kdfAvailableList: List<KdfEngine> = listOf(
        KdfFactory.aesKdf,
        KdfFactory.argon2dKdf,
        KdfFactory.argon2idKdf
    )

    var compressionAlgorithm = CompressionAlgorithm.GZIP

    private val mFieldReferenceEngine = FieldReferencesEngine(this)
    private val mTemplateEngine = TemplateEngineCompatible(this)

    var kdbxVersion = UnsignedInt(0)
    var name = ""
    var nameChanged = DateInstant()
    var description = ""
    var descriptionChanged = DateInstant()
    var defaultUserName = ""
    var defaultUserNameChanged = DateInstant()
    var settingsChanged = DateInstant()
    var keyLastChanged = DateInstant()
    var keyChangeRecDays: Long = -1
    var keyChangeForceDays: Long = 1
    var isKeyChangeForceOnce = false

    var maintenanceHistoryDays = UnsignedInt(365)
    var color = ""

    /**
     * Determine if RecycleBin is enable or not
     * @return true if RecycleBin enable, false if is not available or not enable
     */
    var isRecycleBinEnabled = true
    var recycleBinUUID: UUID = UUID_ZERO
    var recycleBinChanged = DateInstant()
    var entryTemplatesGroup = UUID_ZERO
    var entryTemplatesGroupChanged = DateInstant()
    var historyMaxItems = DEFAULT_HISTORY_MAX_ITEMS
    var historyMaxSize = DEFAULT_HISTORY_MAX_SIZE
    var lastSelectedGroupUUID = UUID_ZERO
    var lastTopVisibleGroupUUID = UUID_ZERO
    var memoryProtection = MemoryProtectionConfig()
    val deletedObjects = HashSet<DeletedObject>()
    var publicCustomData = VariantDictionary()
    val customData = CustomData()

    val tagPool = Tags()

    var localizedAppName = "KeePassDX"

    constructor()

    /**
     * Create a new database with a root group
     */
    constructor(
        databaseName: String,
        rootName: String,
        templatesGroupName: String? = null,
    ) {
        name = databaseName
        kdbxVersion = FILE_VERSION_31
        val group = createGroup().apply {
            title = rootName
            icon.standard = getStandardIcon(IconImageStandard.FOLDER_ID)
        }
        rootGroup = group
        if (templatesGroupName != null) {
            val templatesGroup = mTemplateEngine.createNewTemplatesGroup(templatesGroupName)
            entryTemplatesGroup = templatesGroup.id
            entryTemplatesGroupChanged = templatesGroup.lastModificationTime
        }
    }

    override val version: String
        get() {
            val kdbxStringVersion = when (kdbxVersion) {
                FILE_VERSION_31 -> "3.1"
                FILE_VERSION_40 -> "4.0"
                FILE_VERSION_41 -> "4.1"
                else -> "UNKNOWN"
            }
            return "V2 - KDBX$kdbxStringVersion"
        }

    override val defaultFileExtension: String
        get() = ".kdbx"

    private open class NodeOperationHandler<T : NodeKDBXInterface> : NodeHandler<T>() {
        var containsCustomData = false
        override fun operate(node: T): Boolean {
            if (node.customData.isNotEmpty()) {
                containsCustomData = true
            }
            return true
        }
    }

    private inner class EntryOperationHandler : NodeOperationHandler<EntryKDBX>() {
        var passwordQualityEstimationDisabled = false
        override fun operate(node: EntryKDBX): Boolean {
            if (!node.qualityCheck) {
                passwordQualityEstimationDisabled = true
            }
            return super.operate(node)
        }
    }

    private inner class GroupOperationHandler : NodeOperationHandler<GroupKDBX>() {
        var containsTags = false
        override fun operate(node: GroupKDBX): Boolean {
            if (node.tags.isNotEmpty())
                containsTags = true
            return super.operate(node)
        }
    }

    fun deriveMasterKey(
        masterCredential: MasterCredential,
        challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    ) {
        // Retrieve each plain credential
        val password = masterCredential.password
        val keyFileData = masterCredential.keyFileData
        val hardwareKey = masterCredential.hardwareKey
        val passwordBytes = if (password != null) MasterCredential.retrievePasswordKey(
            password,
            passwordEncoding
        ) else null
        val keyFileBytes = if (keyFileData != null) MasterCredential.retrieveKeyFileDecodedKey(
            keyFileData,
            true
        ) else null
        val hardwareKeyBytes = if (hardwareKey != null) MasterCredential.retrieveHardwareKey(
            challengeResponseRetriever.invoke(hardwareKey, transformSeed)
        ) else null

        // Save to rebuild master password with new seed later
        mCompositeKey = CompositeKey(passwordBytes, keyFileBytes, hardwareKey)

        // Build the master key
        this.masterKey = composedKeyToMasterKey(
            passwordBytes,
            keyFileBytes,
            hardwareKeyBytes
        )
    }

    @Throws(DatabaseOutputException::class)
    fun deriveCompositeKey(
        challengeResponseRetriever: (HardwareKey, ByteArray?) -> ByteArray,
    ) {
        val passwordBytes = mCompositeKey.passwordData
        val keyFileBytes = mCompositeKey.keyFileData
        val hardwareKey = mCompositeKey.hardwareKey
        if (hardwareKey == null) {
            // If no hardware key, simply rebuild from composed keys
            this.masterKey = composedKeyToMasterKey(
                passwordBytes,
                keyFileBytes
            )
        } else {
            val hardwareKeyBytes = MasterCredential.retrieveHardwareKey(
                challengeResponseRetriever.invoke(hardwareKey, transformSeed)
            )
            this.masterKey = composedKeyToMasterKey(
                passwordBytes,
                keyFileBytes,
                hardwareKeyBytes
            )
        }
    }

    private fun composedKeyToMasterKey(
        passwordData: ByteArray?,
        keyFileData: ByteArray?,
        hardwareKeyData: ByteArray? = null,
    ): ByteArray {
        return HashManager.hashSha256(
            passwordData,
            keyFileData,
            hardwareKeyData
        )
    }

    fun copyMasterKeyFrom(databaseVersioned: DatabaseKDBX) {
        super.copyMasterKeyFrom(databaseVersioned)
        this.mCompositeKey = databaseVersioned.mCompositeKey
    }

    fun getMinKdbxVersion(): UnsignedInt {
        val entryHandler = EntryOperationHandler()
        val groupHandler = GroupOperationHandler()
        rootGroup?.doForEachChildAndForIt(entryHandler, groupHandler)

        // https://keepass.info/help/kb/kdbx_4.1.html
        val containsGroupWithTag = groupHandler.containsTags
        val containsEntryWithPasswordQualityEstimationDisabled =
            entryHandler.passwordQualityEstimationDisabled
        val containsCustomIconWithNameOrLastModificationTime =
            iconsManager.containsCustomIconWithNameOrLastModificationTime()
        val containsHeaderCustomDataWithLastModificationTime =
            customData.containsItemWithLastModificationTime()

        // https://keepass.info/help/kb/kdbx_4.html
        // If AES is not use, it's at least 4.0
        val keyDerivationFunction = kdfEngine
        val kdfIsNotAes =
            keyDerivationFunction != null && keyDerivationFunction.uuid != AesKdf.CIPHER_UUID
        val containsHeaderCustomData = customData.isNotEmpty()
        val containsNodeCustomData =
            entryHandler.containsCustomData || groupHandler.containsCustomData

        // Check each condition to determine version
        return if (containsGroupWithTag
            || containsEntryWithPasswordQualityEstimationDisabled
            || containsCustomIconWithNameOrLastModificationTime
            || containsHeaderCustomDataWithLastModificationTime
        ) {
            FILE_VERSION_41
        } else if (kdfIsNotAes
            || containsHeaderCustomData
            || containsNodeCustomData
        ) {
            FILE_VERSION_40
        } else {
            FILE_VERSION_31
        }
    }

    val availableCompressionAlgorithms: List<CompressionAlgorithm> = listOf(
        CompressionAlgorithm.NONE,
        CompressionAlgorithm.GZIP
    )

    fun changeBinaryCompression(
        oldCompression: CompressionAlgorithm,
        newCompression: CompressionAlgorithm,
    ) {
        when (oldCompression) {
            CompressionAlgorithm.NONE -> {
                when (newCompression) {
                    CompressionAlgorithm.NONE -> {
                    }
                    CompressionAlgorithm.GZIP -> {
                        // Only in databaseV3.1, in databaseV4 the header is zipped during the save
                        if (kdbxVersion.isBefore(FILE_VERSION_40)) {
                            compressAllBinaries()
                        }
                    }
                }
            }
            CompressionAlgorithm.GZIP -> {
                // In databaseV4 the header is zipped during the save, so not necessary here
                if (kdbxVersion.isBefore(FILE_VERSION_40)) {
                    when (newCompression) {
                        CompressionAlgorithm.NONE -> {
                            decompressAllBinaries()
                        }
                        CompressionAlgorithm.GZIP -> {
                        }
                    }
                } else {
                    decompressAllBinaries()
                }
            }
        }
    }

    private fun compressAllBinaries() {
        attachmentPool.doForEachBinary { _, binary ->
            try {
                // To compress, create a new binary with file
                binary.compress(binaryCache)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to compress $binary", e)
            }
        }
    }

    private fun decompressAllBinaries() {
        attachmentPool.doForEachBinary { _, binary ->
            try {
                binary.decompress(binaryCache)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to decompress $binary", e)
            }
        }
    }

    override var numberKeyEncryptionRounds: Long
        get() {
            val kdfEngine = kdfEngine
            var numKeyEncRounds: Long = 0
            if (kdfEngine != null && kdfParameters != null)
                numKeyEncRounds = kdfEngine.getKeyRounds(kdfParameters!!)
            return numKeyEncRounds
        }
        set(rounds) {
            val kdfEngine = kdfEngine
            if (kdfEngine != null && kdfParameters != null)
                kdfEngine.setKeyRounds(kdfParameters!!, rounds)
        }

    var memoryUsage: Long
        get() {
            val kdfEngine = kdfEngine
            return if (kdfEngine != null && kdfParameters != null) {
                kdfEngine.getMemoryUsage(kdfParameters!!)
            } else KdfEngine.UNKNOWN_VALUE
        }
        set(memory) {
            val kdfEngine = kdfEngine
            if (kdfEngine != null && kdfParameters != null)
                kdfEngine.setMemoryUsage(kdfParameters!!, memory)
        }

    var parallelism: Long
        get() {
            val kdfEngine = kdfEngine
            return if (kdfEngine != null && kdfParameters != null) {
                kdfEngine.getParallelism(kdfParameters!!)
            } else KdfEngine.UNKNOWN_VALUE
        }
        set(parallelism) {
            val kdfEngine = kdfEngine
            if (kdfEngine != null && kdfParameters != null)
                kdfEngine.setParallelism(kdfParameters!!, parallelism)
        }

    override val passwordEncoding: Charset
        get() = Charsets.UTF_8

    private fun getGroupByUUID(groupUUID: UUID): GroupKDBX? {
        if (groupUUID == UUID_ZERO)
            return null
        return getGroupById(NodeIdUUID(groupUUID))
    }

    // Retrieve recycle bin in index
    val recycleBin: GroupKDBX?
        get() = getGroupByUUID(recycleBinUUID)

    val lastSelectedGroup: GroupKDBX?
        get() = getGroupByUUID(lastSelectedGroupUUID)

    val lastTopVisibleGroup: GroupKDBX?
        get() = getGroupByUUID(lastTopVisibleGroupUUID)

    override fun getStandardIcon(iconId: Int): IconImageStandard {
        return this.iconsManager.getIcon(iconId)
    }

    fun buildNewCustomIcon(
        customIconId: UUID? = null,
        result: (IconImageCustom, BinaryData?) -> Unit,
    ) {
        // Create a binary file for a brand new custom icon
        addCustomIcon(customIconId, "", null, false, result)
    }

    fun addCustomIcon(
        customIconId: UUID? = null,
        name: String,
        lastModificationTime: DateInstant?,
        smallSize: Boolean,
        result: (IconImageCustom, BinaryData?) -> Unit,
    ) {
        iconsManager.addCustomIcon(customIconId, name, lastModificationTime, { uniqueBinaryId ->
            // Create a byte array for better performance with small data
            binaryCache.getBinaryData(uniqueBinaryId, smallSize)
        }, result)
    }

    fun removeCustomIcon(iconUuid: UUID) {
        iconsManager.removeCustomIcon(iconUuid, binaryCache)
    }

    fun isCustomIconBinaryDuplicate(binary: BinaryData): Boolean {
        return iconsManager.isCustomIconBinaryDuplicate(binary)
    }

    fun getCustomIcon(iconUuid: UUID): IconImageCustom? {
        return this.iconsManager.getIcon(iconUuid)
    }

    fun isTemplatesGroupEnabled(): Boolean {
        return entryTemplatesGroup != UUID_ZERO
    }

    fun enableTemplatesGroup(enable: Boolean, templatesGroupName: String) {
        // Create templates group only if a group with a valid name don't already exists
        val firstGroupWithValidName = getGroupIndexes().firstOrNull {
            it.title == templatesGroupName
        }
        if (enable) {
            val templatesGroup = firstGroupWithValidName
                ?: mTemplateEngine.createNewTemplatesGroup(templatesGroupName)
            entryTemplatesGroup = templatesGroup.id
        } else {
            removeTemplatesGroup()
        }
    }

    fun removeTemplatesGroup() {
        entryTemplatesGroup = UUID_ZERO
        mTemplateEngine.clearCache()
    }

    fun getTemplatesGroup(): GroupKDBX? {
        if (isTemplatesGroupEnabled()) {
            return getGroupById(entryTemplatesGroup)
        }
        return null
    }

    fun getTemplates(templateCreation: Boolean): List<Template> {
        return if (templateCreation)
            listOf(mTemplateEngine.getTemplateCreation())
        else
            mTemplateEngine.getTemplates()
    }

    fun getTemplate(entry: EntryKDBX): Template? {
        return mTemplateEngine.getTemplate(entry)
    }

    fun decodeEntryWithTemplateConfiguration(
        entryKDBX: EntryKDBX,
        entryIsTemplate: Boolean,
    ): EntryKDBX {
        return if (entryIsTemplate) {
            mTemplateEngine.decodeTemplateEntry(entryKDBX)
        } else {
            mTemplateEngine.removeMetaTemplateRecognitionFromEntry(entryKDBX)
        }
    }

    fun encodeEntryWithTemplateConfiguration(
        entryKDBX: EntryKDBX,
        entryIsTemplate: Boolean,
        template: Template,
    ): EntryKDBX {
        return if (entryIsTemplate) {
            mTemplateEngine.encodeTemplateEntry(entryKDBX)
        } else {
            mTemplateEngine.addMetaTemplateRecognitionToEntry(template, entryKDBX)
        }
    }

    /*
     * Search methods
     */

    fun getGroupById(id: UUID): GroupKDBX? {
        return this.getGroupById(NodeIdUUID(id))
    }

    fun getEntryById(id: UUID): EntryKDBX? {
        return this.getEntryById(NodeIdUUID(id))
    }

    fun getEntryByTitle(title: String, recursionLevel: Int): EntryKDBX? {
        return findEntry { entry ->
            entry.decodeTitleKey(recursionLevel).equals(title, true)
        }
    }

    fun getEntryByUsername(username: String, recursionLevel: Int): EntryKDBX? {
        return findEntry { entry ->
            entry.decodeUsernameKey(recursionLevel).equals(username, true)
        }
    }

    fun getEntryByURL(url: String, recursionLevel: Int): EntryKDBX? {
        return findEntry { entry ->
            entry.decodeUrlKey(recursionLevel).equals(url, true)
        }
    }

    fun getEntryByPassword(password: String, recursionLevel: Int): EntryKDBX? {
        return findEntry { entry ->
            entry.decodePasswordKey(recursionLevel).equals(password, true)
        }
    }

    fun getEntryByNotes(notes: String, recursionLevel: Int): EntryKDBX? {
        return findEntry { entry ->
            entry.decodeNotesKey(recursionLevel).equals(notes, true)
        }
    }

    fun getEntryByCustomData(customDataValue: String): EntryKDBX? {
        return findEntry { entry ->
            entry.customData.containsItemWithValue(customDataValue)
        }
    }

    /**
     * Retrieve the value of a field reference
     */
    fun getFieldReferenceValue(textReference: String, recursionLevel: Int): String {
        return mFieldReferenceEngine.compile(textReference, recursionLevel)
    }

    @Throws(IOException::class)
    fun makeFinalKey(masterSeed: ByteArray) {

        kdfParameters?.let { keyDerivationFunctionParameters ->
            val kdfEngine = getKdfEngineFromParameters(keyDerivationFunctionParameters)
                ?: throw IOException("Unknown key derivation function")

            var transformedMasterKey =
                kdfEngine.transform(masterKey, keyDerivationFunctionParameters)
            if (transformedMasterKey.size != 32) {
                transformedMasterKey = HashManager.hashSha256(transformedMasterKey)
            }

            val cmpKey = ByteArray(65)
            System.arraycopy(masterSeed, 0, cmpKey, 0, 32)
            System.arraycopy(transformedMasterKey, 0, cmpKey, 32, 32)
            finalKey = resizeKey(cmpKey, encryptionAlgorithm.cipherEngine.keyLength())

            val messageDigest: MessageDigest
            try {
                messageDigest = MessageDigest.getInstance("SHA-512")
                cmpKey[64] = 1
                hmacKey = messageDigest.digest(cmpKey)
            } catch (e: NoSuchAlgorithmException) {
                throw IOException("No SHA-512 implementation")
            } finally {
                Arrays.fill(cmpKey, 0.toByte())
            }
        }
    }

    private fun resizeKey(inBytes: ByteArray, cbOut: Int): ByteArray {
        if (cbOut == 0) return ByteArray(0)

        val messageDigest = if (cbOut <= 32) HashManager.getHash256() else HashManager.getHash512()
        messageDigest.update(inBytes, 0, 64)
        val hash: ByteArray = messageDigest.digest()

        if (cbOut == hash.size) {
            return hash
        }

        val ret = ByteArray(cbOut)
        if (cbOut < hash.size) {
            System.arraycopy(hash, 0, ret, 0, cbOut)
        } else {
            var pos = 0
            var r: Long = 0
            while (pos < cbOut) {
                val hmac: Mac
                try {
                    hmac = Mac.getInstance("HmacSHA256")
                } catch (e: NoSuchAlgorithmException) {
                    throw RuntimeException(e)
                }

                val pbR = longTo8Bytes(r)
                val part = hmac.doFinal(pbR)

                val copy = min(cbOut - pos, part.size)
                System.arraycopy(part, 0, ret, pos, copy)
                pos += copy
                r++

                Arrays.fill(part, 0.toByte())
            }
        }

        Arrays.fill(hash, 0.toByte())
        return ret
    }

    override fun newGroupId(): NodeIdUUID {
        var newId: NodeIdUUID
        do {
            newId = NodeIdUUID()
        } while (isGroupIdUsed(newId))

        return newId
    }

    override fun newEntryId(): NodeIdUUID {
        var newId: NodeIdUUID
        do {
            newId = NodeIdUUID()
        } while (isEntryIdUsed(newId))

        return newId
    }

    override fun createGroup(): GroupKDBX {
        return GroupKDBX()
    }

    override fun createEntry(): EntryKDBX {
        return EntryKDBX()
    }

    override fun rootCanContainsEntry(): Boolean {
        return true
    }

    override fun isInRecycleBin(group: GroupKDBX): Boolean {
        // To keep compatibility with old V1 databases
        var currentGroup: GroupKDBX? = group
        while (currentGroup != null) {
            if (currentGroup.parent == rootGroup
                && currentGroup.title.equals(BACKUP_FOLDER_TITLE, ignoreCase = true)
            ) {
                return true
            }
            currentGroup = currentGroup.parent
        }

        return if (recycleBin == null)
            false
        else if (!isRecycleBinEnabled)
            false
        else
            group.isContainedIn(recycleBin!!)
    }

    /**
     * Ensure that the recycle bin tree exists, if enabled and create it
     * if it doesn't exist
     */
    fun ensureRecycleBinExists(recyclerBinTitle: String) {
        if (recycleBin == null) {
            // Create recycle bin only if a group with a valid name don't already exists
            val firstGroupWithValidName = getGroupIndexes().firstOrNull {
                it.title == recyclerBinTitle
            }
            val recycleBinGroup = if (firstGroupWithValidName == null) {
                val newRecycleBinGroup = createGroup().apply {
                    title = recyclerBinTitle
                    icon.standard = getStandardIcon(IconImageStandard.TRASH_ID)
                    enableAutoType = false
                    enableSearching = false
                    isExpanded = false
                }
                addGroupTo(newRecycleBinGroup, rootGroup)
                newRecycleBinGroup
            } else {
                firstGroupWithValidName
            }
            recycleBinUUID = recycleBinGroup.id
            recycleBinChanged = DateInstant()
        }
    }

    fun removeRecycleBin() {
        if (recycleBin != null) {
            recycleBinUUID = UUID_ZERO
        }
    }

    /**
     * Define if a Node must be delete or recycle when remove action is called
     * @param node Node to remove
     * @return true if node can be recycle, false elsewhere
     */
    fun canRecycle(node: NodeVersioned<*, GroupKDBX, EntryKDBX>): Boolean {
        if (!isRecycleBinEnabled)
            return false
        if (recycleBin == null)
            return false
        if (node is GroupKDBX
            && recycleBin!!.isContainedIn(node)
        )
            return false
        if (!node.isContainedIn(recycleBin!!))
            return true
        return false
    }

    fun getDeletedObject(nodeId: NodeId<UUID>): DeletedObject? {
        return deletedObjects.find { it.uuid == nodeId.id }
    }

    fun addDeletedObject(deletedObject: DeletedObject) {
        this.deletedObjects.add(deletedObject)
    }

    fun addDeletedObject(objectId: UUID) {
        addDeletedObject(DeletedObject(objectId))
    }

    override fun addEntryTo(newEntry: EntryKDBX, parent: GroupKDBX?) {
        super.addEntryTo(newEntry, parent)
        tagPool.put(newEntry.tags)
        mFieldReferenceEngine.clear()
    }

    override fun updateEntry(entry: EntryKDBX) {
        super.updateEntry(entry)
        tagPool.put(entry.tags)
        mFieldReferenceEngine.clear()
    }

    override fun removeEntryFrom(entryToRemove: EntryKDBX, parent: GroupKDBX?) {
        super.removeEntryFrom(entryToRemove, parent)
        // Do not remove tags from pool, it's only in temp memory
        mFieldReferenceEngine.clear()
    }

    fun buildNewBinaryAttachment(
        smallSize: Boolean,
        compression: Boolean,
        protection: Boolean,
        binaryPoolId: Int? = null,
    ): BinaryData {
        return attachmentPool.put(binaryPoolId) { uniqueBinaryId ->
            binaryCache.getBinaryData(uniqueBinaryId, smallSize, compression, protection)
        }.binary
    }

    fun removeUnlinkedAttachment(binary: BinaryData, clear: Boolean) {
        val listBinaries = ArrayList<BinaryData>()
        listBinaries.add(binary)
        removeUnlinkedAttachments(listBinaries, clear)
    }

    fun removeUnlinkedAttachments(clear: Boolean) {
        removeUnlinkedAttachments(emptyList(), clear)
    }

    private fun removeUnlinkedAttachments(binaries: List<BinaryData>, clear: Boolean) {
        // TODO check in icon pool
        // Build binaries to remove with all binaries known
        val binariesToRemove = ArrayList<BinaryData>()
        if (binaries.isEmpty()) {
            attachmentPool.doForEachBinary { _, binary ->
                binariesToRemove.add(binary)
            }
        } else {
            binariesToRemove.addAll(binaries)
        }
        // Remove binaries from the list
        rootGroup?.doForEachChild(object : NodeHandler<EntryKDBX>() {
            override fun operate(node: EntryKDBX): Boolean {
                node.getAttachments(attachmentPool, true).forEach {
                    binariesToRemove.remove(it.binaryData)
                }
                return binariesToRemove.isNotEmpty()
            }
        }, null)
        // Effective removing
        binariesToRemove.forEach {
            try {
                attachmentPool.remove(it)
                if (clear)
                    it.clear(binaryCache)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to clean binaries", e)
            }
        }
    }

    override fun isValidCredential(password: String?, containsKeyFile: Boolean): Boolean {
        if (password == null)
            return true
        return super.isValidCredential(password, containsKeyFile)
    }

    override fun clearIndexes() {
        try {
            super.clearIndexes()
            mFieldReferenceEngine.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to clear cache", e)
        }
    }

    companion object {
        val TYPE = DatabaseKDBX::class.java
        private val TAG = DatabaseKDBX::class.java.name

        private const val DEFAULT_HISTORY_MAX_ITEMS = 10 // -1 unlimited
        private const val DEFAULT_HISTORY_MAX_SIZE = (6 * 1024 * 1024).toLong() // -1 unlimited
    }
}
