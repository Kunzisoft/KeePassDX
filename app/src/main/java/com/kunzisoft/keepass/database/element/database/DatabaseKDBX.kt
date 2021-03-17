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

import android.content.res.Resources
import android.util.Base64
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.crypto.CryptoUtil
import com.kunzisoft.keepass.crypto.engine.AesEngine
import com.kunzisoft.keepass.crypto.engine.CipherEngine
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory
import com.kunzisoft.keepass.crypto.keyDerivation.KdfParameters
import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.DeletedObject
import com.kunzisoft.keepass.database.element.database.DatabaseKDB.Companion.BACKUP_FOLDER_TITLE
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.NodeVersioned
import com.kunzisoft.keepass.database.element.security.EncryptionAlgorithm
import com.kunzisoft.keepass.database.element.security.MemoryProtectionConfig
import com.kunzisoft.keepass.database.exception.UnknownKDF
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_32_3
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_32_4
import com.kunzisoft.keepass.utils.StringUtil.removeSpaceChars
import com.kunzisoft.keepass.utils.StringUtil.toHexString
import com.kunzisoft.keepass.utils.UnsignedInt
import com.kunzisoft.keepass.utils.VariantDictionary
import org.apache.commons.codec.binary.Hex
import org.w3c.dom.Node
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException


class DatabaseKDBX : DatabaseVersioned<UUID, UUID, GroupKDBX, EntryKDBX> {

    var hmacKey: ByteArray? = null
        private set
    var dataCipher = AesEngine.CIPHER_UUID
    private var dataEngine: CipherEngine = AesEngine()
    var compressionAlgorithm = CompressionAlgorithm.GZip
    var kdfParameters: KdfParameters? = null
    private var kdfList: MutableList<KdfEngine> = ArrayList()
    private var numKeyEncRounds: Long = 0
    var publicCustomData = VariantDictionary()

    var kdbxVersion = UnsignedInt(0)
    var name = ""
    var nameChanged = DateInstant()
    // TODO change setting date
    var settingsChanged = DateInstant()
    var description = ""
    var descriptionChanged = DateInstant()
    var defaultUserName = ""
    var defaultUserNameChanged = DateInstant()

    // TODO last change date
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
    var recycleBinChanged = Date()
    var entryTemplatesGroup = UUID_ZERO
    var entryTemplatesGroupChanged = DateInstant()
    var historyMaxItems = DEFAULT_HISTORY_MAX_ITEMS
    var historyMaxSize = DEFAULT_HISTORY_MAX_SIZE
    var lastSelectedGroupUUID = UUID_ZERO
    var lastTopVisibleGroupUUID = UUID_ZERO
    var memoryProtection = MemoryProtectionConfig()
    val deletedObjects = ArrayList<DeletedObject>()
    val customData = HashMap<String, String>()

    var binaryPool = AttachmentPool()

    var localizedAppName = "KeePassDX"

    init {
        kdfList.add(KdfFactory.aesKdf)
        kdfList.add(KdfFactory.argon2dKdf)
        kdfList.add(KdfFactory.argon2idKdf)
    }

    constructor()

    /**
     * Create a new database with a root group
     */
    constructor(databaseName: String, rootName: String) {
        name = databaseName
        kdbxVersion = FILE_VERSION_32_3
        val group = createGroup().apply {
            title = rootName
            icon.standard = getStandardIcon(IconImageStandard.FOLDER_ID)
        }
        rootGroup = group
        addGroupIndex(group)
    }

    override val version: String
        get() {
            val kdbxStringVersion = when(kdbxVersion) {
                FILE_VERSION_32_3 -> "3.1"
                FILE_VERSION_32_4 -> "4.0"
                else -> "UNKNOWN"
            }
            return "KeePass 2 - KDBX$kdbxStringVersion"
        }

    override val kdfEngine: KdfEngine?
        get() = try {
            getEngineKDBX4(kdfParameters)
        } catch (unknownKDF: UnknownKDF) {
            Log.i(TAG, "Unable to retrieve KDF engine", unknownKDF)
            null
        }

    override val kdfAvailableList: List<KdfEngine>
        get() = kdfList

    @Throws(UnknownKDF::class)
    fun getEngineKDBX4(kdfParameters: KdfParameters?): KdfEngine {
        val unknownKDFException = UnknownKDF()
        if (kdfParameters == null) {
            throw unknownKDFException
        }
        for (engine in kdfList) {
            if (engine.uuid == kdfParameters.uuid) {
                return engine
            }
        }
        throw unknownKDFException
    }

    val availableCompressionAlgorithms: List<CompressionAlgorithm>
        get() {
            val list = ArrayList<CompressionAlgorithm>()
            list.add(CompressionAlgorithm.None)
            list.add(CompressionAlgorithm.GZip)
            return list
        }

    fun changeBinaryCompression(oldCompression: CompressionAlgorithm,
                                newCompression: CompressionAlgorithm) {
        when (oldCompression) {
            CompressionAlgorithm.None -> {
                when (newCompression) {
                    CompressionAlgorithm.None -> {
                    }
                    CompressionAlgorithm.GZip -> {
                        // Only in databaseV3.1, in databaseV4 the header is zipped during the save
                        if (kdbxVersion.toKotlinLong() < FILE_VERSION_32_4.toKotlinLong()) {
                            compressAllBinaries()
                        }
                    }
                }
            }
            CompressionAlgorithm.GZip -> {
                // In databaseV4 the header is zipped during the save, so not necessary here
                if (kdbxVersion.toKotlinLong() >= FILE_VERSION_32_4.toKotlinLong()) {
                    decompressAllBinaries()
                } else {
                    when (newCompression) {
                        CompressionAlgorithm.None -> {
                            decompressAllBinaries()
                        }
                        CompressionAlgorithm.GZip -> {
                        }
                    }
                }
            }
        }
    }

    private fun compressAllBinaries() {
        binaryPool.doForEachBinary { _, binary ->
            try {
                val cipherKey = loadedCipherKey
                        ?: throw IOException("Unable to retrieve cipher key to compress binaries")
                // To compress, create a new binary with file
                binary.compress(cipherKey)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to compress $binary", e)
            }
        }
    }

    private fun decompressAllBinaries() {
        binaryPool.doForEachBinary { _, binary ->
            try {
                val cipherKey = loadedCipherKey
                        ?: throw IOException("Unable to retrieve cipher key to decompress binaries")
                binary.decompress(cipherKey)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to decompress $binary", e)
            }
        }
    }

    override val availableEncryptionAlgorithms: List<EncryptionAlgorithm>
        get() {
            val list = ArrayList<EncryptionAlgorithm>()
            list.add(EncryptionAlgorithm.AESRijndael)
            list.add(EncryptionAlgorithm.Twofish)
            list.add(EncryptionAlgorithm.ChaCha20)
            return list
        }

    override var numberKeyEncryptionRounds: Long
        get() {
            val kdfEngine = kdfEngine
            if (kdfEngine != null && kdfParameters != null)
                numKeyEncRounds = kdfEngine.getKeyRounds(kdfParameters!!)
            return numKeyEncRounds
        }
        set(rounds) {
            val kdfEngine = kdfEngine
            if (kdfEngine != null && kdfParameters != null)
                kdfEngine.setKeyRounds(kdfParameters!!, rounds)
            numKeyEncRounds = rounds
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

    override val passwordEncoding: String
        get() = "UTF-8"

    private fun getGroupByUUID(groupUUID: UUID): GroupKDBX? {
        if (groupUUID == UUID_ZERO)
            return null
        return getGroupById(NodeIdUUID(groupUUID))
    }

    // Retrieve recycle bin in index
    val recycleBin: GroupKDBX?
        get() = if (recycleBinUUID == UUID_ZERO) null else getGroupByUUID(recycleBinUUID)

    val lastSelectedGroup: GroupKDBX?
        get() = getGroupByUUID(lastSelectedGroupUUID)

    val lastTopVisibleGroup: GroupKDBX?
        get() = getGroupByUUID(lastTopVisibleGroupUUID)

    fun setDataEngine(dataEngine: CipherEngine) {
        this.dataEngine = dataEngine
    }

    override fun getStandardIcon(iconId: Int): IconImageStandard {
        return this.iconsManager.getIcon(iconId)
    }

    fun buildNewCustomIcon(cacheDirectory: File,
                           customIconId: UUID? = null,
                           result: (IconImageCustom, BinaryFile?) -> Unit) {
        iconsManager.buildNewCustomIcon(cacheDirectory, customIconId, result)
    }

    fun isCustomIconBinaryDuplicate(binary: BinaryFile): Boolean {
        return iconsManager.isCustomIconBinaryDuplicate(binary)
    }

    fun getCustomIcon(iconUuid: UUID): IconImageCustom {
        return this.iconsManager.getIcon(iconUuid)
    }

    fun putCustomData(label: String, value: String) {
        this.customData[label] = value
    }

    override fun containsCustomData(): Boolean {
        return customData.isNotEmpty()
    }

    @Throws(IOException::class)
    public override fun getMasterKey(key: String?, keyInputStream: InputStream?): ByteArray {

        var masterKey = byteArrayOf()

        if (key != null && keyInputStream != null) {
            return getCompositeKey(key, keyInputStream)
        } else if (key != null) { // key.length() >= 0
            masterKey = getPasswordKey(key)
        } else if (keyInputStream != null) { // key == null
            masterKey = getFileKey(keyInputStream)
        }

        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("No SHA-256 implementation")
        }

        return messageDigest.digest(masterKey)
    }

    @Throws(IOException::class)
    fun makeFinalKey(masterSeed: ByteArray) {

        kdfParameters?.let { keyDerivationFunctionParameters ->
            val kdfEngine = getEngineKDBX4(keyDerivationFunctionParameters)

            var transformedMasterKey = kdfEngine.transform(masterKey, keyDerivationFunctionParameters)
            if (transformedMasterKey.size != 32) {
                transformedMasterKey = CryptoUtil.hashSha256(transformedMasterKey)
            }

            val cmpKey = ByteArray(65)
            System.arraycopy(masterSeed, 0, cmpKey, 0, 32)
            System.arraycopy(transformedMasterKey, 0, cmpKey, 32, 32)
            finalKey = CryptoUtil.resizeKey(cmpKey, 0, 64, dataEngine.keyLength())

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

    override fun loadXmlKeyFile(keyInputStream: InputStream): ByteArray? {
        try {
            val documentBuilderFactory = DocumentBuilderFactory.newInstance()

            // Disable certain unsecure XML-Parsing DocumentBuilderFactory features
            try {
                documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            } catch (e : ParserConfigurationException) {
                Log.w(TAG, "Unable to add FEATURE_SECURE_PROCESSING to prevent XML eXternal Entity injection (XXE)")
            }

            val documentBuilder = documentBuilderFactory.newDocumentBuilder()
            val doc = documentBuilder.parse(keyInputStream)

            var xmlKeyFileVersion = 1F

            val docElement = doc.documentElement
            val keyFileChildNodes = docElement.childNodes
            // <KeyFile> Root node
            if (docElement == null
                    || !docElement.nodeName.equals(XML_NODE_ROOT_NAME, ignoreCase = true)) {
                return null
            }
            if (keyFileChildNodes.length < 2)
                return null
            for (keyFileChildPosition in 0 until keyFileChildNodes.length) {
                val keyFileChildNode = keyFileChildNodes.item(keyFileChildPosition)
                // <Meta>
                if (keyFileChildNode.nodeName.equals(XML_NODE_META_NAME, ignoreCase = true)) {
                    val metaChildNodes = keyFileChildNode.childNodes
                    for (metaChildPosition in 0 until metaChildNodes.length) {
                        val metaChildNode = metaChildNodes.item(metaChildPosition)
                        // <Version>
                        if (metaChildNode.nodeName.equals(XML_NODE_VERSION_NAME, ignoreCase = true)) {
                            val versionChildNodes = metaChildNode.childNodes
                            for (versionChildPosition in 0 until versionChildNodes.length) {
                                val versionChildNode = versionChildNodes.item(versionChildPosition)
                                if (versionChildNode.nodeType == Node.TEXT_NODE) {
                                    val versionText = versionChildNode.textContent.removeSpaceChars()
                                    try {
                                        xmlKeyFileVersion = versionText.toFloat()
                                        Log.i(TAG, "Reading XML KeyFile version : $xmlKeyFileVersion")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "XML Keyfile version cannot be read : $versionText")
                                    }
                                }
                            }
                        }
                    }
                }
                // <Key>
                if (keyFileChildNode.nodeName.equals(XML_NODE_KEY_NAME, ignoreCase = true)) {
                    val keyChildNodes = keyFileChildNode.childNodes
                    for (keyChildPosition in 0 until keyChildNodes.length) {
                        val keyChildNode = keyChildNodes.item(keyChildPosition)
                        // <Data>
                        if (keyChildNode.nodeName.equals(XML_NODE_DATA_NAME, ignoreCase = true)) {
                            var hashString : String? = null
                            if (keyChildNode.hasAttributes()) {
                                val dataNodeAttributes = keyChildNode.attributes
                                hashString = dataNodeAttributes
                                        .getNamedItem(XML_ATTRIBUTE_DATA_HASH).nodeValue
                            }
                            val dataChildNodes = keyChildNode.childNodes
                            for (dataChildPosition in 0 until dataChildNodes.length) {
                                val dataChildNode = dataChildNodes.item(dataChildPosition)
                                if (dataChildNode.nodeType == Node.TEXT_NODE) {
                                    val dataString = dataChildNode.textContent.removeSpaceChars()
                                    when (xmlKeyFileVersion) {
                                        1F -> {
                                            // No hash in KeyFile XML version 1
                                            return Base64.decode(dataString, BASE_64_FLAG)
                                        }
                                        2F -> {
                                            return if (hashString != null
                                                    && checkKeyFileHash(dataString, hashString)) {
                                                Log.i(TAG, "Successful key file hash check.")
                                                Hex.decodeHex(dataString.toCharArray())
                                            } else {
                                                Log.e(TAG, "Unable to check the hash of the key file.")
                                                null
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    private fun checkKeyFileHash(data: String, hash: String): Boolean {
        val digest: MessageDigest?
        var success = false
        try {
            digest = MessageDigest.getInstance("SHA-256")
            digest?.reset()
            // hexadecimal encoding of the first 4 bytes of the SHA-256 hash of the key.
            val dataDigest = digest.digest(Hex.decodeHex(data.toCharArray()))
                    .copyOfRange(0, 4)
                    .toHexString()
            success = dataDigest == hash
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return success
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
                    && currentGroup.title.equals(BACKUP_FOLDER_TITLE, ignoreCase = true)) {
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
    fun ensureRecycleBinExists(resources: Resources) {
        if (recycleBin == null) {
            // Create recycle bin
            val recycleBinGroup = createGroup().apply {
                title = resources.getString(R.string.recycle_bin)
                icon.standard = getStandardIcon(IconImageStandard.TRASH_ID)
                enableAutoType = false
                enableSearching = false
                isExpanded = false
            }
            addGroupTo(recycleBinGroup, rootGroup)
            recycleBinUUID = recycleBinGroup.id
            recycleBinChanged = recycleBinGroup.lastModificationTime.date
        }
    }

    fun removeRecycleBin() {
        if (recycleBin != null) {
            recycleBinUUID = UUID_ZERO
            recycleBinChanged = DateInstant().date
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
        if (!node.isContainedIn(recycleBin!!))
            return true
        return false
    }

    fun recycle(group: GroupKDBX, resources: Resources) {
        ensureRecycleBinExists(resources)
        removeGroupFrom(group, group.parent)
        addGroupTo(group, recycleBin)
        group.afterAssignNewParent()
    }

    fun recycle(entry: EntryKDBX, resources: Resources) {
        ensureRecycleBinExists(resources)
        removeEntryFrom(entry, entry.parent)
        addEntryTo(entry, recycleBin)
        entry.afterAssignNewParent()
    }

    fun undoRecycle(group: GroupKDBX, origParent: GroupKDBX) {
        removeGroupFrom(group, recycleBin)
        addGroupTo(group, origParent)
    }

    fun undoRecycle(entry: EntryKDBX, origParent: GroupKDBX) {
        removeEntryFrom(entry, recycleBin)
        addEntryTo(entry, origParent)
    }

    fun getDeletedObjects(): List<DeletedObject> {
        return deletedObjects
    }

    fun addDeletedObject(deletedObject: DeletedObject) {
        this.deletedObjects.add(deletedObject)
    }

    override fun removeEntryFrom(entryToRemove: EntryKDBX, parent: GroupKDBX?) {
        super.removeEntryFrom(entryToRemove, parent)
        deletedObjects.add(DeletedObject(entryToRemove.id))
    }

    override fun undoDeleteEntryFrom(entry: EntryKDBX, origParent: GroupKDBX?) {
        super.undoDeleteEntryFrom(entry, origParent)
        deletedObjects.remove(DeletedObject(entry.id))
    }

    fun containsPublicCustomData(): Boolean {
        return publicCustomData.size() > 0
    }

    fun buildNewAttachment(cacheDirectory: File,
                           compression: Boolean,
                           protection: Boolean,
                           binaryPoolId: Int? = null): BinaryFile {
        return binaryPool.put(cacheDirectory, binaryPoolId, compression, protection).binary
    }

    fun removeUnlinkedAttachment(binary: BinaryFile, clear: Boolean) {
        val listBinaries = ArrayList<BinaryFile>()
        listBinaries.add(binary)
        removeUnlinkedAttachments(listBinaries, clear)
    }

    fun removeUnlinkedAttachments(clear: Boolean) {
        removeUnlinkedAttachments(emptyList(), clear)
    }

    private fun removeUnlinkedAttachments(binaries: List<BinaryFile>, clear: Boolean) {
        // Build binaries to remove with all binaries known
        val binariesToRemove = ArrayList<BinaryFile>()
        if (binaries.isEmpty()) {
            binaryPool.doForEachBinary { _, binary ->
                binariesToRemove.add(binary)
            }
        } else {
            binariesToRemove.addAll(binaries)
        }
        // Remove binaries from the list
        rootGroup?.doForEachChild(object : NodeHandler<EntryKDBX>() {
            override fun operate(node: EntryKDBX): Boolean {
                node.getAttachments(binaryPool, true).forEach {
                    binariesToRemove.remove(it.binaryFile)
                }
                return binariesToRemove.isNotEmpty()
            }
        }, null)
        // Effective removing
        binariesToRemove.forEach {
            try {
                binaryPool.remove(it)
                if (clear)
                    it.clear()
            } catch (e: Exception) {
                Log.w(TAG, "Unable to clean binaries", e)
            }
        }
    }

    override fun validatePasswordEncoding(password: String?, containsKeyFile: Boolean): Boolean {
        if (password == null)
            return true
        return super.validatePasswordEncoding(password, containsKeyFile)
    }

    override fun clearCache() {
        try {
            super.clearCache()
            binaryPool.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to clear cache", e)
        }
    }

    companion object {
        val TYPE = DatabaseKDBX::class.java
        private val TAG = DatabaseKDBX::class.java.name

        private const val DEFAULT_HISTORY_MAX_ITEMS = 10 // -1 unlimited
        private const val DEFAULT_HISTORY_MAX_SIZE = (6 * 1024 * 1024).toLong() // -1 unlimited

        private const val XML_NODE_ROOT_NAME = "KeyFile"
        private const val XML_NODE_META_NAME = "Meta"
        private const val XML_NODE_VERSION_NAME = "Version"
        private const val XML_NODE_KEY_NAME = "Key"
        private const val XML_NODE_DATA_NAME = "Data"
        private const val XML_ATTRIBUTE_DATA_HASH = "Hash"

        const val BASE_64_FLAG = Base64.NO_WRAP
    }
}