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
import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.crypto.AesEngine
import com.kunzisoft.keepass.database.crypto.CipherEngine
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.crypto.VariantDictionary
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.database.crypto.kdf.KdfFactory
import com.kunzisoft.keepass.database.crypto.kdf.KdfParameters
import com.kunzisoft.keepass.database.element.CustomData
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.DeletedObject
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.database.DatabaseKDB.Companion.BACKUP_FOLDER_TITLE
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.entry.FieldReferencesEngine
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.NodeVersioned
import com.kunzisoft.keepass.database.element.security.MemoryProtectionConfig
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.database.element.template.TemplateEngineCompatible
import com.kunzisoft.keepass.database.exception.UnknownKDF
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_31
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_40
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_41
import com.kunzisoft.keepass.utils.StringUtil.removeSpaceChars
import com.kunzisoft.keepass.utils.StringUtil.toHexString
import com.kunzisoft.keepass.utils.UnsignedInt
import com.kunzisoft.keepass.utils.longTo8Bytes
import org.apache.commons.codec.binary.Hex
import org.w3c.dom.Node
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import kotlin.math.min


class DatabaseKDBX : DatabaseVersioned<UUID, UUID, GroupKDBX, EntryKDBX> {

    var hmacKey: ByteArray? = null
        private set
    var cipherUuid = EncryptionAlgorithm.AESRijndael.uuid
    private var dataEngine: CipherEngine = AesEngine()
    var compressionAlgorithm = CompressionAlgorithm.GZip
    var kdfParameters: KdfParameters? = null
    private var kdfList: MutableList<KdfEngine> = ArrayList()
    private var numKeyEncRounds: Long = 0
    var publicCustomData = VariantDictionary()
    private val mFieldReferenceEngine = FieldReferencesEngine(this)
    private val mTemplateEngine = TemplateEngineCompatible(this)

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
    var recycleBinChanged = DateInstant()
    var entryTemplatesGroup = UUID_ZERO
    var entryTemplatesGroupChanged = DateInstant()
    var historyMaxItems = DEFAULT_HISTORY_MAX_ITEMS
    var historyMaxSize = DEFAULT_HISTORY_MAX_SIZE
    var lastSelectedGroupUUID = UUID_ZERO
    var lastTopVisibleGroupUUID = UUID_ZERO
    var memoryProtection = MemoryProtectionConfig()
    val deletedObjects = ArrayList<DeletedObject>()
    val customData = CustomData()

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
    constructor(databaseName: String,
                rootName: String,
                templatesGroupName: String? = null) {
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
            val kdbxStringVersion = when(kdbxVersion) {
                FILE_VERSION_31 -> "3.1"
                FILE_VERSION_40 -> "4.0"
                FILE_VERSION_41 -> "4.1"
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
                        if (kdbxVersion.isBefore(FILE_VERSION_40)) {
                            compressAllBinaries()
                        }
                    }
                }
            }
            CompressionAlgorithm.GZip -> {
                // In databaseV4 the header is zipped during the save, so not necessary here
                if (kdbxVersion.isBefore(FILE_VERSION_40)) {
                    when (newCompression) {
                        CompressionAlgorithm.None -> {
                            decompressAllBinaries()
                        }
                        CompressionAlgorithm.GZip -> {
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

    fun buildNewCustomIcon(customIconId: UUID? = null,
                           result: (IconImageCustom, BinaryData?) -> Unit) {
        iconsManager.buildNewCustomIcon(customIconId, result)
    }

    fun addCustomIcon(customIconId: UUID? = null,
                      name: String,
                      lastModificationTime: DateInstant?,
                      smallSize: Boolean,
                      result: (IconImageCustom, BinaryData?) -> Unit) {
        iconsManager.addCustomIcon(customIconId, name, lastModificationTime, smallSize, result)
    }

    fun isCustomIconBinaryDuplicate(binary: BinaryData): Boolean {
        return iconsManager.isCustomIconBinaryDuplicate(binary)
    }

    fun getCustomIcon(iconUuid: UUID): IconImageCustom {
        return this.iconsManager.getIcon(iconUuid)
    }

    fun getCustomIcon(binary: BinaryData): IconImageCustom? {
        return this.iconsManager.getIcon(binary)
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
            entryTemplatesGroupChanged = templatesGroup.lastModificationTime
        } else {
            removeTemplatesGroup()
        }
    }

    fun removeTemplatesGroup() {
        entryTemplatesGroup = UUID_ZERO
        entryTemplatesGroupChanged = DateInstant()
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

    fun decodeEntryWithTemplateConfiguration(entryKDBX: EntryKDBX, entryIsTemplate: Boolean): EntryKDBX {
        return if (entryIsTemplate) {
            mTemplateEngine.decodeTemplateEntry(entryKDBX)
        } else {
            mTemplateEngine.removeMetaTemplateRecognitionFromEntry(entryKDBX)
        }
    }

    fun encodeEntryWithTemplateConfiguration(entryKDBX: EntryKDBX, entryIsTemplate: Boolean, template: Template): EntryKDBX {
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
        return this.entryIndexes.values.find { entry ->
            entry.decodeTitleKey(recursionLevel).equals(title, true)
        }
    }

    fun getEntryByUsername(username: String, recursionLevel: Int): EntryKDBX? {
        return this.entryIndexes.values.find { entry ->
            entry.decodeUsernameKey(recursionLevel).equals(username, true)
        }
    }

    fun getEntryByURL(url: String, recursionLevel: Int): EntryKDBX? {
        return this.entryIndexes.values.find { entry ->
            entry.decodeUrlKey(recursionLevel).equals(url, true)
        }
    }

    fun getEntryByPassword(password: String, recursionLevel: Int): EntryKDBX? {
        return this.entryIndexes.values.find { entry ->
            entry.decodePasswordKey(recursionLevel).equals(password, true)
        }
    }

    fun getEntryByNotes(notes: String, recursionLevel: Int): EntryKDBX? {
        return this.entryIndexes.values.find { entry ->
            entry.decodeNotesKey(recursionLevel).equals(notes, true)
        }
    }

    fun getEntryByCustomData(customDataValue: String): EntryKDBX? {
        return entryIndexes.values.find { entry ->
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
    public override fun getMasterKey(key: String?, keyInputStream: InputStream?): ByteArray {

        var masterKey = byteArrayOf()

        if (key != null && keyInputStream != null) {
            return getCompositeKey(key, keyInputStream)
        } else if (key != null) { // key.length() >= 0
            masterKey = getPasswordKey(key)
        } else if (keyInputStream != null) { // key == null
            masterKey = getFileKey(keyInputStream)
        }

        return HashManager.hashSha256(masterKey)
    }

    @Throws(IOException::class)
    fun makeFinalKey(masterSeed: ByteArray) {

        kdfParameters?.let { keyDerivationFunctionParameters ->
            val kdfEngine = getEngineKDBX4(keyDerivationFunctionParameters)

            var transformedMasterKey = kdfEngine.transform(masterKey, keyDerivationFunctionParameters)
            if (transformedMasterKey.size != 32) {
                transformedMasterKey = HashManager.hashSha256(transformedMasterKey)
            }

            val cmpKey = ByteArray(65)
            System.arraycopy(masterSeed, 0, cmpKey, 0, 32)
            System.arraycopy(transformedMasterKey, 0, cmpKey, 32, 32)
            finalKey = resizeKey(cmpKey, dataEngine.keyLength())

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
        var success = false
        try {
            // hexadecimal encoding of the first 4 bytes of the SHA-256 hash of the key.
            val dataDigest = HashManager.hashSha256(Hex.decodeHex(data.toCharArray()))
                    .copyOfRange(0, 4).toHexString()
            success = dataDigest == hash
        } catch (e: Exception) {
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
            // Create recycle bin only if a group with a valid name don't already exists
            val firstGroupWithValidName = getGroupIndexes().firstOrNull {
                it.title == resources.getString(R.string.recycle_bin)
            }
            val recycleBinGroup = if (firstGroupWithValidName == null) {
                val newRecycleBinGroup = createGroup().apply {
                    title = resources.getString(R.string.recycle_bin)
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
            recycleBinChanged = recycleBinGroup.lastModificationTime
        }
    }

    fun removeRecycleBin() {
        if (recycleBin != null) {
            recycleBinUUID = UUID_ZERO
            recycleBinChanged = DateInstant()
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
                && recycleBin!!.isContainedIn(node))
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

    override fun addEntryTo(newEntry: EntryKDBX, parent: GroupKDBX?) {
        super.addEntryTo(newEntry, parent)
        mFieldReferenceEngine.clear()
    }

    override fun updateEntry(entry: EntryKDBX) {
        super.updateEntry(entry)
        mFieldReferenceEngine.clear()
    }

    override fun removeEntryFrom(entryToRemove: EntryKDBX, parent: GroupKDBX?) {
        super.removeEntryFrom(entryToRemove, parent)
        deletedObjects.add(DeletedObject(entryToRemove.id))
        mFieldReferenceEngine.clear()
    }

    override fun undoDeleteEntryFrom(entry: EntryKDBX, origParent: GroupKDBX?) {
        super.undoDeleteEntryFrom(entry, origParent)
        deletedObjects.remove(DeletedObject(entry.id))
    }

    fun containsPublicCustomData(): Boolean {
        return publicCustomData.size() > 0
    }

    fun buildNewAttachment(smallSize: Boolean,
                           compression: Boolean,
                           protection: Boolean,
                           binaryPoolId: Int? = null): BinaryData {
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

    override fun validatePasswordEncoding(password: String?, containsKeyFile: Boolean): Boolean {
        if (password == null)
            return true
        return super.validatePasswordEncoding(password, containsKeyFile)
    }

    override fun clearCache() {
        try {
            super.clearCache()
            mFieldReferenceEngine.clear()
            attachmentPool.clear()
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