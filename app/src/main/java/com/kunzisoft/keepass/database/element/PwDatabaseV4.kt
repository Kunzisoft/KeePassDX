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

import android.content.res.Resources
import android.util.Log
import biz.source_code.base64Coder.Base64Coder
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.crypto.CryptoUtil
import com.kunzisoft.keepass.crypto.engine.AesEngine
import com.kunzisoft.keepass.crypto.engine.CipherEngine
import com.kunzisoft.keepass.crypto.keyDerivation.*
import com.kunzisoft.keepass.database.exception.InvalidKeyFileException
import com.kunzisoft.keepass.database.exception.UnknownKDF
import com.kunzisoft.keepass.database.file.PwCompressionAlgorithm
import com.kunzisoft.keepass.utils.VariantDictionary
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory


class PwDatabaseV4 : PwDatabase<PwGroupV4, PwEntryV4> {

    var hmacKey: ByteArray? = null
        private set
    var dataCipher = AesEngine.CIPHER_UUID
    private var dataEngine: CipherEngine = AesEngine()
    var compressionAlgorithm = PwCompressionAlgorithm.Gzip
    var kdfParameters: KdfParameters? = null
    private var kdfV4List: MutableList<KdfEngine> = ArrayList()
    private var numKeyEncRounds: Long = 0
    var publicCustomData = VariantDictionary()

    var name = "KeePass DX database"
    var nameChanged = PwDate()
    // TODO change setting date
    var settingsChanged = PwDate()
    var description = ""
    var descriptionChanged = PwDate()
    var defaultUserName = ""
    var defaultUserNameChanged = PwDate()

    // TODO date
    var keyLastChanged = PwDate()
    var keyChangeRecDays: Long = -1
    var keyChangeForceDays: Long = 1
    var isKeyChangeForceOnce = false

    var maintenanceHistoryDays: Long = 365
    var color = ""
    /**
     * Determine if RecycleBin is enable or not
     * @return true if RecycleBin enable, false if is not available or not enable
     */
    var isRecycleBinEnabled = true
    var recycleBinUUID: UUID = UUID_ZERO
    var recycleBinChanged = Date()
    var entryTemplatesGroup = UUID_ZERO
    var entryTemplatesGroupChanged = PwDate()
    var historyMaxItems = DEFAULT_HISTORY_MAX_ITEMS
    var historyMaxSize = DEFAULT_HISTORY_MAX_SIZE
    var lastSelectedGroupUUID = UUID_ZERO
    var lastTopVisibleGroupUUID = UUID_ZERO
    var memoryProtection = MemoryProtectionConfig()
    val deletedObjects = ArrayList<PwDeletedObject>()
    val customIcons = ArrayList<PwIconCustom>()
    val customData = HashMap<String, String>()

    var binPool = BinaryPool()

    var localizedAppName = "KeePassDX" // TODO resource

    init {
        kdfV4List.add(KdfFactory.aesKdf)
        kdfV4List.add(KdfFactory.argon2Kdf)
    }

    constructor()

    constructor(databaseName: String) {
        val groupV4 = createGroup().apply {
            title = databaseName
            icon = iconFactory.folderIcon
        }
        rootGroup = groupV4
        addGroupIndex(groupV4)
    }

    override val version: String
        get() = "KeePass 2"

    override val kdfEngine: KdfEngine?
        get() = try {
            getEngineV4(kdfParameters)
        } catch (unknownKDF: UnknownKDF) {
            Log.i(TAG, "Unable to retrieve KDF engine", unknownKDF)
            null
        }

    override val kdfAvailableList: List<KdfEngine>
        get() = kdfV4List

    @Throws(UnknownKDF::class)
    fun getEngineV4(kdfParameters: KdfParameters?): KdfEngine {
        val unknownKDFException = UnknownKDF()
        if (kdfParameters == null) {
            throw unknownKDFException
        }
        for (engine in kdfV4List) {
            if (engine.uuid == kdfParameters.uuid) {
                return engine
            }
        }
        throw unknownKDFException
    }

    override val availableEncryptionAlgorithms: List<PwEncryptionAlgorithm>
        get() {
            val list = ArrayList<PwEncryptionAlgorithm>()
            list.add(PwEncryptionAlgorithm.AESRijndael)
            list.add(PwEncryptionAlgorithm.Twofish)
            list.add(PwEncryptionAlgorithm.ChaCha20)
            return list
        }

    override var numberKeyEncryptionRounds: Long
        get() {
            val kdfEngine = kdfEngine
            if (kdfEngine != null && kdfParameters != null)
                numKeyEncRounds = kdfEngine.getKeyRounds(kdfParameters!!)
            return numKeyEncRounds
        }
        @Throws(NumberFormatException::class)
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
            } else KdfEngine.UNKNOWN_VALUE.toLong()
        }
        set(memory) {
            val kdfEngine = kdfEngine
            if (kdfEngine != null && kdfParameters != null)
                kdfEngine.setMemoryUsage(kdfParameters!!, memory)
        }

    var parallelism: Int
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

    fun getGroupByUUID(groupUUID: UUID): PwGroupV4? {
        if (groupUUID == UUID_ZERO)
            return null
        return getGroupById(PwNodeIdUUID(groupUUID))
    }

    // Retrieve recycle bin in index
    val recycleBin: PwGroupV4?
        get() = getGroupByUUID(recycleBinUUID)

    val lastSelectedGroup: PwGroupV4?
        get() = getGroupByUUID(lastSelectedGroupUUID)

    val lastTopVisibleGroup: PwGroupV4?
        get() = getGroupByUUID(lastTopVisibleGroupUUID)

    fun setDataEngine(dataEngine: CipherEngine) {
        this.dataEngine = dataEngine
    }

    fun getCustomIcons(): List<PwIconCustom> {
        return customIcons
    }

    fun addCustomIcon(customIcon: PwIconCustom) {
        this.customIcons.add(customIcon)
    }

    fun getCustomData(): Map<String, String> {
        return customData
    }

    fun putCustomData(label: String, value: String) {
        this.customData[label] = value
    }

    override fun containsCustomData(): Boolean {
        return getCustomData().isNotEmpty()
    }

    @Throws(InvalidKeyFileException::class, IOException::class)
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
            val kdfEngine = getEngineV4(keyDerivationFunctionParameters)

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
            val dbf = DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(keyInputStream)

            val el = doc.documentElement
            if (el == null || !el.nodeName.equals(RootElementName, ignoreCase = true)) {
                return null
            }

            val children = el.childNodes
            if (children.length < 2) {
                return null
            }

            for (i in 0 until children.length) {
                val child = children.item(i)

                if (child.nodeName.equals(KeyElementName, ignoreCase = true)) {
                    val keyChildren = child.childNodes
                    for (j in 0 until keyChildren.length) {
                        val keyChild = keyChildren.item(j)
                        if (keyChild.nodeName.equals(KeyDataElementName, ignoreCase = true)) {
                            val children2 = keyChild.childNodes
                            for (k in 0 until children2.length) {
                                val text = children2.item(k)
                                if (text.nodeType == Node.TEXT_NODE) {
                                    val txt = text as Text
                                    return Base64Coder.decode(txt.nodeValue)
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

    override fun newGroupId(): PwNodeIdUUID {
        var newId: PwNodeIdUUID
        do {
            newId = PwNodeIdUUID()
        } while (isGroupIdUsed(newId))

        return newId
    }

    override fun newEntryId(): PwNodeIdUUID {
        var newId: PwNodeIdUUID
        do {
            newId = PwNodeIdUUID()
        } while (isEntryIdUsed(newId))

        return newId
    }

    override fun createGroup(): PwGroupV4 {
        return PwGroupV4()
    }

    override fun createEntry(): PwEntryV4 {
        return PwEntryV4()
    }

    override fun rootCanContainsEntry(): Boolean {
        return true
    }

    override fun isBackup(group: PwGroupV4): Boolean {
        // To keep compatibility with old V1 databases
        var currentGroup: PwGroupV4? = group
        while (currentGroup != null) {
            if (currentGroup.parent == rootGroup
                    && currentGroup.title.equals("Backup", ignoreCase = true)) {
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
    private fun ensureRecycleBin(resources: Resources) {
        if (recycleBin == null) {
            // Create recycle bin
            val recycleBinGroup = createGroup().apply {
                title = resources.getString(R.string.recycle_bin)
                icon = iconFactory.trashIcon
                enableAutoType = false
                enableSearching = false
                isExpanded = false
            }
            addGroupTo(recycleBinGroup, rootGroup)
            recycleBinUUID = recycleBinGroup.id
            recycleBinChanged = recycleBinGroup.lastModificationTime.date
        }
    }

    /**
     * Define if a Node must be delete or recycle when remove action is called
     * @param node Node to remove
     * @return true if node can be recycle, false elsewhere
     */
    fun canRecycle(node: PwNode<*, PwGroupV4, PwEntryV4>): Boolean {
        if (!isRecycleBinEnabled)
            return false
        if (recycleBin == null)
            return true
        if (!node.isContainedIn(recycleBin!!))
            return true
        return false
    }

    fun recycle(group: PwGroupV4, resources: Resources) {
        ensureRecycleBin(resources)
        removeGroupFrom(group, group.parent)
        addGroupTo(group, recycleBin)
        group.afterAssignNewParent()
    }

    fun recycle(entry: PwEntryV4, resources: Resources) {
        ensureRecycleBin(resources)
        removeEntryFrom(entry, entry.parent)
        addEntryTo(entry, recycleBin)
        entry.afterAssignNewParent()
    }

    fun undoRecycle(group: PwGroupV4, origParent: PwGroupV4) {
        removeGroupFrom(group, recycleBin)
        addGroupTo(group, origParent)
    }

    fun undoRecycle(entry: PwEntryV4, origParent: PwGroupV4) {
        removeEntryFrom(entry, recycleBin)
        addEntryTo(entry, origParent)
    }

    fun getDeletedObjects(): List<PwDeletedObject> {
        return deletedObjects
    }

    fun addDeletedObject(deletedObject: PwDeletedObject) {
        this.deletedObjects.add(deletedObject)
    }

    override fun removeEntryFrom(entryToRemove: PwEntryV4, parent: PwGroupV4?) {
        super.removeEntryFrom(entryToRemove, parent)
        deletedObjects.add(PwDeletedObject(entryToRemove.id))
    }

    override fun undoDeleteEntryFrom(entry: PwEntryV4, origParent: PwGroupV4?) {
        super.undoDeleteEntryFrom(entry, origParent)
        deletedObjects.remove(PwDeletedObject(entry.id))
    }

    fun containsPublicCustomData(): Boolean {
        return publicCustomData.size() > 0
    }

    override fun validatePasswordEncoding(key: String?): Boolean {
        if (key == null)
            return true
        return super.validatePasswordEncoding(key)
    }

    override fun clearCache() {
        super.clearCache()
        binPool.clear()
    }

    companion object {
        private val TAG = PwDatabaseV4::class.java.name

        private const val DEFAULT_HISTORY_MAX_ITEMS = 10 // -1 unlimited
        private const val DEFAULT_HISTORY_MAX_SIZE = (6 * 1024 * 1024).toLong() // -1 unlimited

        private const val RootElementName = "KeyFile"
        //private const val MetaElementName = "Meta";
        //private const val VersionElementName = "Version";
        private const val KeyElementName = "Key"
        private const val KeyDataElementName = "Data"
    }
}