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
package com.kunzisoft.keepass.database.file.output

import android.util.Base64
import android.util.Log
import android.util.Xml
import com.kunzisoft.keepass.crypto.CipherFactory
import com.kunzisoft.keepass.crypto.CrsAlgorithm
import com.kunzisoft.keepass.crypto.StreamCipherFactory
import com.kunzisoft.keepass.crypto.engine.CipherEngine
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory
import com.kunzisoft.keepass.database.action.node.NodeHandler
import com.kunzisoft.keepass.database.element.DeletedObject
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX.Companion.BASE_64_FLAG
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX.Companion.BUFFER_SIZE_BYTES
import com.kunzisoft.keepass.database.element.entry.AutoType
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.node.NodeKDBXInterface
import com.kunzisoft.keepass.database.element.security.BinaryAttachment
import com.kunzisoft.keepass.database.element.security.MemoryProtectionConfig
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.database.exception.UnknownKDF
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX
import com.kunzisoft.keepass.database.file.DatabaseKDBXXML
import com.kunzisoft.keepass.database.file.DateKDBXUtil
import com.kunzisoft.keepass.stream.*
import org.bouncycastle.crypto.StreamCipher
import org.joda.time.DateTime
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.OutputStream
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream


class DatabaseOutputKDBX(private val mDatabaseKDBX: DatabaseKDBX,
                         outputStream: OutputStream)
    : DatabaseOutput<DatabaseHeaderKDBX>(outputStream) {

    private var randomStream: StreamCipher? = null
    private lateinit var xml: XmlSerializer
    private var header: DatabaseHeaderKDBX? = null
    private var hashOfHeader: ByteArray? = null
    private var headerHmac: ByteArray? = null
    private var engine: CipherEngine? = null

    @Throws(DatabaseOutputException::class)
    override fun output() {

        try {
            try {
                engine = CipherFactory.getInstance(mDatabaseKDBX.dataCipher)
            } catch (e: NoSuchAlgorithmException) {
                throw DatabaseOutputException("No such cipher", e)
            }

            header = outputHeader(mOS)

            val osPlain: OutputStream
            osPlain = if (header!!.version.toKotlinLong() < DatabaseHeaderKDBX.FILE_VERSION_32_4.toKotlinLong()) {
                val cos = attachStreamEncryptor(header!!, mOS)
                cos.write(header!!.streamStartBytes)

                HashedBlockOutputStream(cos)
            } else {
                mOS.write(hashOfHeader!!)
                mOS.write(headerHmac!!)


                attachStreamEncryptor(header!!, HmacBlockOutputStream(mOS, mDatabaseKDBX.hmacKey!!))
            }

            val osXml: OutputStream
            try {
                osXml = when(mDatabaseKDBX.compressionAlgorithm) {
                    CompressionAlgorithm.GZip -> GZIPOutputStream(osPlain)
                    else -> osPlain
                }

                if (header!!.version.toKotlinLong() >= DatabaseHeaderKDBX.FILE_VERSION_32_4.toKotlinLong()) {
                    val ihOut = DatabaseInnerHeaderOutputKDBX(mDatabaseKDBX, header!!, osXml)
                    ihOut.output()
                }

                outputDatabase(osXml)
                osXml.close()
            } catch (e: IllegalArgumentException) {
                throw DatabaseOutputException(e)
            } catch (e: IllegalStateException) {
                throw DatabaseOutputException(e)
            }

        } catch (e: IOException) {
            throw DatabaseOutputException(e)
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun outputDatabase(outputStream: OutputStream) {

        xml = Xml.newSerializer()

        xml.setOutput(outputStream, "UTF-8")
        xml.startDocument("UTF-8", true)

        xml.startTag(null, DatabaseKDBXXML.ElemDocNode)

        writeMeta()

        mDatabaseKDBX.rootGroup?.let { root ->
            xml.startTag(null, DatabaseKDBXXML.ElemRoot)
            startGroup(root)
            val groupStack = Stack<GroupKDBX>()
            groupStack.push(root)

            if (!root.doForEachChild(
                            object : NodeHandler<EntryKDBX>() {
                                override fun operate(node: EntryKDBX): Boolean {
                                    try {
                                        writeEntry(node, false)
                                    } catch (ex: IOException) {
                                        throw RuntimeException(ex)
                                    }

                                    return true
                                }
                            },
                            object : NodeHandler<GroupKDBX>() {
                                override fun operate(node: GroupKDBX): Boolean {
                                    while (true) {
                                        try {
                                            if (node.parent === groupStack.peek()) {
                                                groupStack.push(node)
                                                startGroup(node)
                                                break
                                            } else {
                                                groupStack.pop()
                                                if (groupStack.size <= 0) return false
                                                endGroup()
                                            }
                                        } catch (e: IOException) {
                                            throw RuntimeException(e)
                                        }

                                    }
                                    return true
                                }
                            })
            )
                throw RuntimeException("Writing groups failed")

            while (groupStack.size > 1) {
                xml.endTag(null, DatabaseKDBXXML.ElemGroup)
                groupStack.pop()
            }
        }

        endGroup()

        writeDeletedObjects(mDatabaseKDBX.deletedObjects)

        xml.endTag(null, DatabaseKDBXXML.ElemRoot)

        xml.endTag(null, DatabaseKDBXXML.ElemDocNode)
        xml.endDocument()
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeMeta() {
        xml.startTag(null, DatabaseKDBXXML.ElemMeta)

        writeObject(DatabaseKDBXXML.ElemGenerator, mDatabaseKDBX.localizedAppName)

        if (hashOfHeader != null) {
            writeObject(DatabaseKDBXXML.ElemHeaderHash, String(Base64.encode(hashOfHeader!!, BASE_64_FLAG)))
        }

        writeObject(DatabaseKDBXXML.ElemDbName, mDatabaseKDBX.name, true)
        writeObject(DatabaseKDBXXML.ElemDbNameChanged, mDatabaseKDBX.nameChanged.date)
        writeObject(DatabaseKDBXXML.ElemDbDesc, mDatabaseKDBX.description, true)
        writeObject(DatabaseKDBXXML.ElemDbDescChanged, mDatabaseKDBX.descriptionChanged.date)
        writeObject(DatabaseKDBXXML.ElemDbDefaultUser, mDatabaseKDBX.defaultUserName, true)
        writeObject(DatabaseKDBXXML.ElemDbDefaultUserChanged, mDatabaseKDBX.defaultUserNameChanged.date)
        writeObject(DatabaseKDBXXML.ElemDbMntncHistoryDays, mDatabaseKDBX.maintenanceHistoryDays.toKotlinLong())
        writeObject(DatabaseKDBXXML.ElemDbColor, mDatabaseKDBX.color)
        writeObject(DatabaseKDBXXML.ElemDbKeyChanged, mDatabaseKDBX.keyLastChanged.date)
        writeObject(DatabaseKDBXXML.ElemDbKeyChangeRec, mDatabaseKDBX.keyChangeRecDays)
        writeObject(DatabaseKDBXXML.ElemDbKeyChangeForce, mDatabaseKDBX.keyChangeForceDays)

        writeMemoryProtection(mDatabaseKDBX.memoryProtection)

        writeCustomIconList()

        writeObject(DatabaseKDBXXML.ElemRecycleBinEnabled, mDatabaseKDBX.isRecycleBinEnabled)
        writeUuid(DatabaseKDBXXML.ElemRecycleBinUuid, mDatabaseKDBX.recycleBinUUID)
        writeObject(DatabaseKDBXXML.ElemRecycleBinChanged, mDatabaseKDBX.recycleBinChanged)
        writeUuid(DatabaseKDBXXML.ElemEntryTemplatesGroup, mDatabaseKDBX.entryTemplatesGroup)
        writeObject(DatabaseKDBXXML.ElemEntryTemplatesGroupChanged, mDatabaseKDBX.entryTemplatesGroupChanged.date)
        writeObject(DatabaseKDBXXML.ElemHistoryMaxItems, mDatabaseKDBX.historyMaxItems.toLong())
        writeObject(DatabaseKDBXXML.ElemHistoryMaxSize, mDatabaseKDBX.historyMaxSize)
        writeUuid(DatabaseKDBXXML.ElemLastSelectedGroup, mDatabaseKDBX.lastSelectedGroupUUID)
        writeUuid(DatabaseKDBXXML.ElemLastTopVisibleGroup, mDatabaseKDBX.lastTopVisibleGroupUUID)

        // Seem to work properly if always in meta
        if (header!!.version.toKotlinLong() < DatabaseHeaderKDBX.FILE_VERSION_32_4.toKotlinLong())
            writeMetaBinaries()

        writeCustomData(mDatabaseKDBX.customData)

        xml.endTag(null, DatabaseKDBXXML.ElemMeta)
    }

    @Throws(DatabaseOutputException::class)
    private fun attachStreamEncryptor(header: DatabaseHeaderKDBX, os: OutputStream): CipherOutputStream {
        val cipher: Cipher
        try {
            //mDatabaseKDBX.makeFinalKey(header.masterSeed, mDatabaseKDBX.kdfParameters);

            cipher = engine!!.getCipher(Cipher.ENCRYPT_MODE, mDatabaseKDBX.finalKey!!, header.encryptionIV)
        } catch (e: Exception) {
            throw DatabaseOutputException("Invalid algorithm.", e)
        }

        return CipherOutputStream(os, cipher)
    }

    @Throws(DatabaseOutputException::class)
    override fun setIVs(header: DatabaseHeaderKDBX): SecureRandom {
        val random = super.setIVs(header)
        random.nextBytes(header.masterSeed)

        val ivLength = engine!!.ivLength()
        if (ivLength != header.encryptionIV.size) {
            header.encryptionIV = ByteArray(ivLength)
        }
        random.nextBytes(header.encryptionIV)

        if (mDatabaseKDBX.kdfParameters == null) {
            mDatabaseKDBX.kdfParameters = KdfFactory.aesKdf.defaultParameters
        }

        try {
            val kdf = mDatabaseKDBX.getEngineKDBX4(mDatabaseKDBX.kdfParameters)
            kdf.randomize(mDatabaseKDBX.kdfParameters!!)
        } catch (unknownKDF: UnknownKDF) {
            Log.e(TAG, "Unable to retrieve header", unknownKDF)
        }

        if (header.version.toKotlinLong() < DatabaseHeaderKDBX.FILE_VERSION_32_4.toKotlinLong()) {
            header.innerRandomStream = CrsAlgorithm.Salsa20
            header.innerRandomStreamKey = ByteArray(32)
        } else {
            header.innerRandomStream = CrsAlgorithm.ChaCha20
            header.innerRandomStreamKey = ByteArray(64)
        }
        random.nextBytes(header.innerRandomStreamKey)

        randomStream = StreamCipherFactory.getInstance(header.innerRandomStream, header.innerRandomStreamKey)
        if (randomStream == null) {
            throw DatabaseOutputException("Invalid random cipher")
        }

        if (header.version.toKotlinLong() < DatabaseHeaderKDBX.FILE_VERSION_32_4.toKotlinLong()) {
            random.nextBytes(header.streamStartBytes)
        }

        return random
    }

    @Throws(DatabaseOutputException::class)
    override fun outputHeader(outputStream: OutputStream): DatabaseHeaderKDBX {

        val header = DatabaseHeaderKDBX(mDatabaseKDBX)
        setIVs(header)

        val pho = DatabaseHeaderOutputKDBX(mDatabaseKDBX, header, outputStream)
        try {
            pho.output()
        } catch (e: IOException) {
            throw DatabaseOutputException("Failed to output the header.", e)
        }

        hashOfHeader = pho.hashOfHeader
        headerHmac = pho.headerHmac

        return header
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun startGroup(group: GroupKDBX) {
        xml.startTag(null, DatabaseKDBXXML.ElemGroup)
        writeUuid(DatabaseKDBXXML.ElemUuid, group.id)
        writeObject(DatabaseKDBXXML.ElemName, group.title)
        writeObject(DatabaseKDBXXML.ElemNotes, group.notes)
        writeObject(DatabaseKDBXXML.ElemIcon, group.icon.iconId.toLong())

        if (group.iconCustom != IconImageCustom.UNKNOWN_ICON) {
            writeUuid(DatabaseKDBXXML.ElemCustomIconID, group.iconCustom.uuid)
        }

        writeTimes(group)
        writeObject(DatabaseKDBXXML.ElemIsExpanded, group.isExpanded)
        writeObject(DatabaseKDBXXML.ElemGroupDefaultAutoTypeSeq, group.defaultAutoTypeSequence)
        writeObject(DatabaseKDBXXML.ElemEnableAutoType, group.enableAutoType)
        writeObject(DatabaseKDBXXML.ElemEnableSearching, group.enableSearching)
        writeUuid(DatabaseKDBXXML.ElemLastTopVisibleEntry, group.lastTopVisibleEntry)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun endGroup() {
        xml.endTag(null, DatabaseKDBXXML.ElemGroup)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeEntry(entry: EntryKDBX, isHistory: Boolean) {

        xml.startTag(null, DatabaseKDBXXML.ElemEntry)

        writeUuid(DatabaseKDBXXML.ElemUuid, entry.id)
        writeObject(DatabaseKDBXXML.ElemIcon, entry.icon.iconId.toLong())

        if (entry.iconCustom != IconImageCustom.UNKNOWN_ICON) {
            writeUuid(DatabaseKDBXXML.ElemCustomIconID, entry.iconCustom.uuid)
        }

        writeObject(DatabaseKDBXXML.ElemFgColor, entry.foregroundColor)
        writeObject(DatabaseKDBXXML.ElemBgColor, entry.backgroundColor)
        writeObject(DatabaseKDBXXML.ElemOverrideUrl, entry.overrideURL)
        writeObject(DatabaseKDBXXML.ElemTags, entry.tags)

        writeTimes(entry)

        writeFields(entry.fields)
        writeEntryBinaries(entry.binaries)
        writeAutoType(entry.autoType)

        if (!isHistory) {
            writeEntryHistory(entry.history)
        }

        xml.endTag(null, DatabaseKDBXXML.ElemEntry)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeObject(name: String, value: String, filterXmlChars: Boolean = false) {
        var xmlString = value

        xml.startTag(null, name)

        if (filterXmlChars) {
            xmlString = safeXmlString(xmlString)
        }

        xml.text(xmlString)
        xml.endTag(null, name)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeObject(name: String, value: Date) {
        if (header!!.version.toKotlinLong() < DatabaseHeaderKDBX.FILE_VERSION_32_4.toKotlinLong()) {
            writeObject(name, DatabaseKDBXXML.DateFormatter.format(value))
        } else {
            val dt = DateTime(value)
            val seconds = DateKDBXUtil.convertDateToKDBX4Time(dt)
            val buf = longTo8Bytes(seconds)
            val b64 = String(Base64.encode(buf, BASE_64_FLAG))
            writeObject(name, b64)
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeObject(name: String, value: Long) {
        writeObject(name, value.toString())
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeObject(name: String, value: Boolean?) {
        val text: String = when {
            value == null -> DatabaseKDBXXML.ValNull
            value -> DatabaseKDBXXML.ValTrue
            else -> DatabaseKDBXXML.ValFalse
        }

        writeObject(name, text)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeUuid(name: String, uuid: UUID) {
        val data = uuidTo16Bytes(uuid)
        writeObject(name, String(Base64.encode(data, BASE_64_FLAG)))
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeBinary(binary : BinaryAttachment) {
        val binaryLength = binary.length()
        if (binaryLength > 0) {

            if (binary.isProtected) {
                xml.attribute(null, DatabaseKDBXXML.AttrProtected, DatabaseKDBXXML.ValTrue)

                binary.getInputDataStream().readBytes(BUFFER_SIZE_BYTES) { buffer ->
                    val encoded = ByteArray(buffer.size)
                    randomStream!!.processBytes(buffer, 0, encoded.size, encoded, 0)
                    val charArray = String(Base64.encode(encoded, BASE_64_FLAG)).toCharArray()
                    xml.text(charArray, 0, charArray.size)
                }
            } else {
                // Force binary compression from database (compression was harmonized during import)
                if (mDatabaseKDBX.compressionAlgorithm === CompressionAlgorithm.GZip) {
                    xml.attribute(null, DatabaseKDBXXML.AttrCompressed, DatabaseKDBXXML.ValTrue)
                }

                // Force decompression in this specific case
                val binaryInputStream = if (mDatabaseKDBX.compressionAlgorithm == CompressionAlgorithm.None
                                && binary.isCompressed == true) {
                    GZIPInputStream(binary.getInputDataStream())
                } else {
                    binary.getInputDataStream()
                }

                // Write the XML
                binaryInputStream.readBytes(BUFFER_SIZE_BYTES) { buffer ->
                    val charArray = String(Base64.encode(buffer, BASE_64_FLAG)).toCharArray()
                    xml.text(charArray, 0, charArray.size)
                }
            }
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeMetaBinaries() {
        xml.startTag(null, DatabaseKDBXXML.ElemBinaries)

        mDatabaseKDBX.binaryPool.doForEachBinary { key, binary ->
            xml.startTag(null, DatabaseKDBXXML.ElemBinary)
            xml.attribute(null, DatabaseKDBXXML.AttrId, key.toString())
            writeBinary(binary)
            xml.endTag(null, DatabaseKDBXXML.ElemBinary)
        }

        xml.endTag(null, DatabaseKDBXXML.ElemBinaries)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeObject(name: String, keyName: String, keyValue: String, valueName: String, valueValue: String) {
        xml.startTag(null, name)

        xml.startTag(null, keyName)
        xml.text(safeXmlString(keyValue))
        xml.endTag(null, keyName)

        xml.startTag(null, valueName)
        xml.text(safeXmlString(valueValue))
        xml.endTag(null, valueName)

        xml.endTag(null, name)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeAutoType(autoType: AutoType) {
        xml.startTag(null, DatabaseKDBXXML.ElemAutoType)

        writeObject(DatabaseKDBXXML.ElemAutoTypeEnabled, autoType.enabled)
        writeObject(DatabaseKDBXXML.ElemAutoTypeObfuscation, autoType.obfuscationOptions.toKotlinLong())

        if (autoType.defaultSequence.isNotEmpty()) {
            writeObject(DatabaseKDBXXML.ElemAutoTypeDefaultSeq, autoType.defaultSequence, true)
        }

        for ((key, value) in autoType.entrySet()) {
            writeObject(DatabaseKDBXXML.ElemAutoTypeItem, DatabaseKDBXXML.ElemWindow, key, DatabaseKDBXXML.ElemKeystrokeSequence, value)
        }

        xml.endTag(null, DatabaseKDBXXML.ElemAutoType)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeFields(fields: Map<String, ProtectedString>) {

        for ((key, value) in fields) {
            writeField(key, value)
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeField(key: String, value: ProtectedString) {

        xml.startTag(null, DatabaseKDBXXML.ElemString)
        xml.startTag(null, DatabaseKDBXXML.ElemKey)
        xml.text(safeXmlString(key))
        xml.endTag(null, DatabaseKDBXXML.ElemKey)

        xml.startTag(null, DatabaseKDBXXML.ElemValue)
        var protect = value.isProtected

        when (key) {
            MemoryProtectionConfig.ProtectDefinition.TITLE_FIELD -> protect = mDatabaseKDBX.memoryProtection.protectTitle
            MemoryProtectionConfig.ProtectDefinition.USERNAME_FIELD -> protect = mDatabaseKDBX.memoryProtection.protectUserName
            MemoryProtectionConfig.ProtectDefinition.PASSWORD_FIELD -> protect = mDatabaseKDBX.memoryProtection.protectPassword
            MemoryProtectionConfig.ProtectDefinition.URL_FIELD -> protect = mDatabaseKDBX.memoryProtection.protectUrl
            MemoryProtectionConfig.ProtectDefinition.NOTES_FIELD -> protect = mDatabaseKDBX.memoryProtection.protectNotes
        }

        if (protect) {
            xml.attribute(null, DatabaseKDBXXML.AttrProtected, DatabaseKDBXXML.ValTrue)

            val data = value.toString().toByteArray(charset("UTF-8"))
            val valLength = data.size

            if (valLength > 0) {
                val encoded = ByteArray(valLength)
                randomStream!!.processBytes(data, 0, valLength, encoded, 0)
                xml.text(String(Base64.encode(encoded, BASE_64_FLAG)))
            }
        } else {
            xml.text(safeXmlString(value.toString()))
        }

        xml.endTag(null, DatabaseKDBXXML.ElemValue)
        xml.endTag(null, DatabaseKDBXXML.ElemString)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeDeletedObject(value: DeletedObject) {
        xml.startTag(null, DatabaseKDBXXML.ElemDeletedObject)

        writeUuid(DatabaseKDBXXML.ElemUuid, value.uuid)
        writeObject(DatabaseKDBXXML.ElemDeletionTime, value.getDeletionTime())

        xml.endTag(null, DatabaseKDBXXML.ElemDeletedObject)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeEntryBinaries(binaries: Map<String, BinaryAttachment>) {
        for ((key, binary) in binaries) {
            xml.startTag(null, DatabaseKDBXXML.ElemBinary)
            xml.startTag(null, DatabaseKDBXXML.ElemKey)
            xml.text(safeXmlString(key))
            xml.endTag(null, DatabaseKDBXXML.ElemKey)

            xml.startTag(null, DatabaseKDBXXML.ElemValue)
            val ref = mDatabaseKDBX.binaryPool.findKey(binary)
            if (ref != null) {
                xml.attribute(null, DatabaseKDBXXML.AttrRef, ref.toString())
            } else {
                writeBinary(binary)
            }
            xml.endTag(null, DatabaseKDBXXML.ElemValue)

            xml.endTag(null, DatabaseKDBXXML.ElemBinary)
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeDeletedObjects(value: List<DeletedObject>) {
        xml.startTag(null, DatabaseKDBXXML.ElemDeletedObjects)

        for (pdo in value) {
            writeDeletedObject(pdo)
        }

        xml.endTag(null, DatabaseKDBXXML.ElemDeletedObjects)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeMemoryProtection(value: MemoryProtectionConfig) {
        xml.startTag(null, DatabaseKDBXXML.ElemMemoryProt)

        writeObject(DatabaseKDBXXML.ElemProtTitle, value.protectTitle)
        writeObject(DatabaseKDBXXML.ElemProtUserName, value.protectUserName)
        writeObject(DatabaseKDBXXML.ElemProtPassword, value.protectPassword)
        writeObject(DatabaseKDBXXML.ElemProtURL, value.protectUrl)
        writeObject(DatabaseKDBXXML.ElemProtNotes, value.protectNotes)

        xml.endTag(null, DatabaseKDBXXML.ElemMemoryProt)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeCustomData(customData: Map<String, String>) {
        xml.startTag(null, DatabaseKDBXXML.ElemCustomData)

        for ((key, value) in customData) {
            writeObject(
                    DatabaseKDBXXML.ElemStringDictExItem,
                    DatabaseKDBXXML.ElemKey,
                    key,
                    DatabaseKDBXXML.ElemValue,
                    value
            )
        }

        xml.endTag(null, DatabaseKDBXXML.ElemCustomData)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeTimes(node: NodeKDBXInterface) {
        xml.startTag(null, DatabaseKDBXXML.ElemTimes)

        writeObject(DatabaseKDBXXML.ElemLastModTime, node.lastModificationTime.date)
        writeObject(DatabaseKDBXXML.ElemCreationTime, node.creationTime.date)
        writeObject(DatabaseKDBXXML.ElemLastAccessTime, node.lastAccessTime.date)
        writeObject(DatabaseKDBXXML.ElemExpiryTime, node.expiryTime.date)
        writeObject(DatabaseKDBXXML.ElemExpires, node.expires)
        writeObject(DatabaseKDBXXML.ElemUsageCount, node.usageCount.toKotlinLong())
        writeObject(DatabaseKDBXXML.ElemLocationChanged, node.locationChanged.date)

        xml.endTag(null, DatabaseKDBXXML.ElemTimes)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeEntryHistory(value: List<EntryKDBX>) {
        val element = DatabaseKDBXXML.ElemHistory

        xml.startTag(null, element)

        for (entry in value) {
            writeEntry(entry, true)
        }

        xml.endTag(null, element)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeCustomIconList() {
        val customIcons = mDatabaseKDBX.customIcons
        if (customIcons.size == 0) return

        xml.startTag(null, DatabaseKDBXXML.ElemCustomIcons)

        for (icon in customIcons) {
            xml.startTag(null, DatabaseKDBXXML.ElemCustomIconItem)

            writeUuid(DatabaseKDBXXML.ElemCustomIconItemID, icon.uuid)
            writeObject(DatabaseKDBXXML.ElemCustomIconItemData, String(Base64.encode(icon.imageData, BASE_64_FLAG)))

            xml.endTag(null, DatabaseKDBXXML.ElemCustomIconItem)
        }

        xml.endTag(null, DatabaseKDBXXML.ElemCustomIcons)
    }

    private fun safeXmlString(text: String): String {
        if (text.isEmpty()) {
            return text
        }

        val stringBuilder = StringBuilder()
        var ch: Char
        for (element in text) {
            ch = element
            if (
                ch.toInt() in 0x20..0xD7FF ||
                ch.toInt() == 0x9 || ch.toInt() == 0xA || ch.toInt() == 0xD ||
                ch.toInt() in 0xE000..0xFFFD
            ) {
                stringBuilder.append(ch)
            }
        }
        return stringBuilder.toString()
    }

    companion object {
        private val TAG = DatabaseOutputKDBX::class.java.name
    }
}
