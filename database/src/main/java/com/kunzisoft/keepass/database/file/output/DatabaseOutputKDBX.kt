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
import com.kunzisoft.encrypt.StreamCipher
import com.kunzisoft.keepass.database.crypto.CrsAlgorithm
import com.kunzisoft.keepass.database.crypto.kdf.KdfFactory
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.binary.BinaryData.Companion.BASE64_FLAG
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import com.kunzisoft.keepass.database.element.entry.AutoType
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.node.NodeHandler
import com.kunzisoft.keepass.database.element.node.NodeKDBXInterface
import com.kunzisoft.keepass.database.element.security.MemoryProtectionConfig
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_40
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_41
import com.kunzisoft.keepass.database.file.DatabaseKDBXXML
import com.kunzisoft.keepass.database.file.DateKDBXUtil
import com.kunzisoft.keepass.stream.HashedBlockOutputStream
import com.kunzisoft.keepass.stream.HmacBlockOutputStream
import com.kunzisoft.keepass.utils.*
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.OutputStream
import java.security.SecureRandom
import java.util.*
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import kotlin.experimental.or


class DatabaseOutputKDBX(private val mDatabaseKDBX: DatabaseKDBX)
    : DatabaseOutput<DatabaseHeaderKDBX>() {

    private var randomStream: StreamCipher? = null
    private lateinit var xml: XmlSerializer
    private var header: DatabaseHeaderKDBX? = null
    private var hashOfHeader: ByteArray? = null
    private var headerHmac: ByteArray? = null

    @Throws(DatabaseOutputException::class)
    override fun writeDatabase(outputStream: OutputStream,
                               assignMasterKey: () -> Unit) {

        try {
            header = outputHeader(outputStream, assignMasterKey)

            val osPlain: OutputStream = if (header!!.version.isBefore(FILE_VERSION_40)) {
                val cos = attachStreamEncryptor(header!!, outputStream)
                cos.write(header!!.streamStartBytes)

                HashedBlockOutputStream(cos)
            } else {
                outputStream.write(hashOfHeader!!)
                outputStream.write(headerHmac!!)

                attachStreamEncryptor(header!!, HmacBlockOutputStream(outputStream, mDatabaseKDBX.hmacKey!!))
            }

            when(mDatabaseKDBX.compressionAlgorithm) {
                CompressionAlgorithm.GZIP -> GZIPOutputStream(osPlain)
                else -> osPlain
            }.use { xmlOutputStream ->
                if (!header!!.version.isBefore(FILE_VERSION_40)) {
                    outputInnerHeader(mDatabaseKDBX, header!!, xmlOutputStream)
                }
                outputDatabase(xmlOutputStream)
            }
        } catch (e: Exception) {
            throw DatabaseOutputException(e)
        }
    }

    @Throws(IOException::class)
    private fun outputInnerHeader(database: DatabaseKDBX,
                                  header: DatabaseHeaderKDBX,
                                  dataOutputStream: OutputStream) {
        dataOutputStream.writeByte(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.InnerRandomStreamID)
        dataOutputStream.write4BytesUInt(UnsignedInt(4))
        if (header.innerRandomStream == null)
            throw IOException("Can't write innerRandomStream")
        dataOutputStream.write4BytesUInt(header.innerRandomStream!!.id)

        val streamKeySize = header.innerRandomStreamKey.size
        dataOutputStream.writeByte(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.InnerRandomstreamKey)
        dataOutputStream.write4BytesUInt(UnsignedInt(streamKeySize))
        dataOutputStream.write(header.innerRandomStreamKey)

        val binaryCache = database.binaryCache
        database.attachmentPool.doForEachOrderedBinaryWithoutDuplication { _, binary ->
            // Force decompression to add binary in header
            binary.decompress(binaryCache)
            // Write type binary
            dataOutputStream.writeByte(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.Binary)
            // Write size
            dataOutputStream.write4BytesUInt(UnsignedInt.fromKotlinLong(binary.getSize() + 1))
            // Write protected flag
            var flag = DatabaseHeaderKDBX.KdbxBinaryFlags.None
            if (binary.isProtected) {
                flag = flag or DatabaseHeaderKDBX.KdbxBinaryFlags.Protected
            }
            dataOutputStream.writeByte(flag)

            binary.getInputDataStream(binaryCache).use { inputStream ->
                inputStream.readAllBytes { buffer ->
                    dataOutputStream.write(buffer)
                }
            }
        }

        dataOutputStream.writeByte(DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.EndOfHeader)
        dataOutputStream.write4BytesUInt(UnsignedInt(0))
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

        writeString(DatabaseKDBXXML.ElemGenerator, mDatabaseKDBX.localizedAppName)

        if (hashOfHeader != null) {
            writeString(DatabaseKDBXXML.ElemHeaderHash, String(Base64.encode(hashOfHeader!!, BASE64_FLAG)))
        }

        if (!header!!.version.isBefore(FILE_VERSION_40)) {
            writeDateInstant(DatabaseKDBXXML.ElemSettingsChanged, mDatabaseKDBX.settingsChanged)
        }
        writeString(DatabaseKDBXXML.ElemDbName, mDatabaseKDBX.name, true)
        writeDateInstant(DatabaseKDBXXML.ElemDbNameChanged, mDatabaseKDBX.nameChanged)
        writeString(DatabaseKDBXXML.ElemDbDesc, mDatabaseKDBX.description, true)
        writeDateInstant(DatabaseKDBXXML.ElemDbDescChanged, mDatabaseKDBX.descriptionChanged)
        writeString(DatabaseKDBXXML.ElemDbDefaultUser, mDatabaseKDBX.defaultUserName, true)
        writeDateInstant(DatabaseKDBXXML.ElemDbDefaultUserChanged, mDatabaseKDBX.defaultUserNameChanged)
        writeLong(DatabaseKDBXXML.ElemDbMntncHistoryDays, mDatabaseKDBX.maintenanceHistoryDays.toKotlinLong())
        writeString(DatabaseKDBXXML.ElemDbColor, mDatabaseKDBX.color)
        writeDateInstant(DatabaseKDBXXML.ElemDbKeyChanged, mDatabaseKDBX.keyLastChanged)
        writeLong(DatabaseKDBXXML.ElemDbKeyChangeRec, mDatabaseKDBX.keyChangeRecDays)
        writeLong(DatabaseKDBXXML.ElemDbKeyChangeForce, mDatabaseKDBX.keyChangeForceDays)

        writeMemoryProtection(mDatabaseKDBX.memoryProtection)

        writeCustomIconList()

        writeBoolean(DatabaseKDBXXML.ElemRecycleBinEnabled, mDatabaseKDBX.isRecycleBinEnabled)
        writeUuid(DatabaseKDBXXML.ElemRecycleBinUuid, mDatabaseKDBX.recycleBinUUID)
        writeDateInstant(DatabaseKDBXXML.ElemRecycleBinChanged, mDatabaseKDBX.recycleBinChanged)
        writeUuid(DatabaseKDBXXML.ElemEntryTemplatesGroup, mDatabaseKDBX.entryTemplatesGroup)
        writeDateInstant(DatabaseKDBXXML.ElemEntryTemplatesGroupChanged, mDatabaseKDBX.entryTemplatesGroupChanged)
        writeLong(DatabaseKDBXXML.ElemHistoryMaxItems, mDatabaseKDBX.historyMaxItems.toLong())
        writeLong(DatabaseKDBXXML.ElemHistoryMaxSize, mDatabaseKDBX.historyMaxSize)
        writeUuid(DatabaseKDBXXML.ElemLastSelectedGroup, mDatabaseKDBX.lastSelectedGroupUUID)
        writeUuid(DatabaseKDBXXML.ElemLastTopVisibleGroup, mDatabaseKDBX.lastTopVisibleGroupUUID)

        // Seem to work properly if always in meta
        if (header!!.version.isBefore(FILE_VERSION_40))
            writeMetaBinaries()

        writeCustomData(mDatabaseKDBX.customData)

        xml.endTag(null, DatabaseKDBXXML.ElemMeta)
    }

    @Throws(DatabaseOutputException::class)
    private fun attachStreamEncryptor(header: DatabaseHeaderKDBX, os: OutputStream): CipherOutputStream {
        val cipher: Cipher
        try {
            cipher = mDatabaseKDBX
                .encryptionAlgorithm
                .cipherEngine
                .getCipher(Cipher.ENCRYPT_MODE, mDatabaseKDBX.finalKey!!, header.encryptionIV)
        } catch (e: Exception) {
            throw DatabaseOutputException("Invalid algorithm.", e)
        }

        return CipherOutputStream(os, cipher)
    }

    @Throws(DatabaseOutputException::class)
    override fun setIVs(header: DatabaseHeaderKDBX): SecureRandom {
        val random = super.setIVs(header)
        random.nextBytes(header.masterSeed)

        val ivLength = mDatabaseKDBX.encryptionAlgorithm.cipherEngine.ivLength()
        if (ivLength != header.encryptionIV.size) {
            header.encryptionIV = ByteArray(ivLength)
        }
        random.nextBytes(header.encryptionIV)

        if (mDatabaseKDBX.kdfParameters == null) {
            mDatabaseKDBX.kdfParameters = KdfFactory.aesKdf.defaultParameters
        }

        mDatabaseKDBX.randomizeKdfParameters()

        if (header.version.isBefore(FILE_VERSION_40)) {
            header.innerRandomStream = CrsAlgorithm.Salsa20
            header.innerRandomStreamKey = ByteArray(32)
        } else {
            header.innerRandomStream = CrsAlgorithm.ChaCha20
            header.innerRandomStreamKey = ByteArray(64)
        }
        random.nextBytes(header.innerRandomStreamKey)

        try {
            randomStream = CrsAlgorithm.getCipher(header.innerRandomStream, header.innerRandomStreamKey)
        } catch (e: Exception) {
            throw DatabaseOutputException(e)
        }

        if (header.version.isBefore(FILE_VERSION_40)) {
            random.nextBytes(header.streamStartBytes)
        }

        return random
    }

    @Throws(DatabaseOutputException::class)
    private fun outputHeader(outputStream: OutputStream,
                             assignMasterKey: () -> Unit): DatabaseHeaderKDBX {
        try {
            val header = DatabaseHeaderKDBX(mDatabaseKDBX)
            setIVs(header)

            mDatabaseKDBX.transformSeed = header.transformSeed
            assignMasterKey.invoke()

            val pho = DatabaseHeaderOutputKDBX(mDatabaseKDBX, header, outputStream)
            pho.output()

            hashOfHeader = pho.hashOfHeader
            headerHmac = pho.headerHmac

            return header
        } catch (e: IOException) {
            throw DatabaseOutputException("Failed to output the header.", e)
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun startGroup(group: GroupKDBX) {
        xml.startTag(null, DatabaseKDBXXML.ElemGroup)
        writeUuid(DatabaseKDBXXML.ElemUuid, group.id)
        writeString(DatabaseKDBXXML.ElemName, group.title)
        writeString(DatabaseKDBXXML.ElemNotes, group.notes)
        writeLong(DatabaseKDBXXML.ElemIcon, group.icon.standard.id.toLong())

        if (!group.icon.custom.isUnknown) {
            writeUuid(DatabaseKDBXXML.ElemCustomIconID, group.icon.custom.uuid)
        }

        writeTags(group.tags)
        writePreviousParentGroup(group.previousParentGroup)
        writeTimes(group)
        writeBoolean(DatabaseKDBXXML.ElemIsExpanded, group.isExpanded)
        writeString(DatabaseKDBXXML.ElemGroupDefaultAutoTypeSeq, group.defaultAutoTypeSequence)
        writeBoolean(DatabaseKDBXXML.ElemEnableAutoType, group.enableAutoType)
        writeBoolean(DatabaseKDBXXML.ElemEnableSearching, group.enableSearching)
        writeUuid(DatabaseKDBXXML.ElemLastTopVisibleEntry, group.lastTopVisibleEntry)
        writeCustomData(group.customData)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun endGroup() {
        xml.endTag(null, DatabaseKDBXXML.ElemGroup)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeEntry(entry: EntryKDBX, isHistory: Boolean) {

        xml.startTag(null, DatabaseKDBXXML.ElemEntry)

        writeUuid(DatabaseKDBXXML.ElemUuid, entry.id)
        writeLong(DatabaseKDBXXML.ElemIcon, entry.icon.standard.id.toLong())

        if (!entry.icon.custom.isUnknown) {
            writeUuid(DatabaseKDBXXML.ElemCustomIconID, entry.icon.custom.uuid)
        }

        writeString(DatabaseKDBXXML.ElemFgColor, entry.foregroundColor)
        writeString(DatabaseKDBXXML.ElemBgColor, entry.backgroundColor)
        writeString(DatabaseKDBXXML.ElemOverrideUrl, entry.overrideURL)

        // Write quality check only if false
        if (!entry.qualityCheck) {
            writeBoolean(DatabaseKDBXXML.ElemQualityCheck, entry.qualityCheck)
        }
        writeTags(entry.tags)
        writePreviousParentGroup(entry.previousParentGroup)
        writeTimes(entry)
        writeFields(entry.getFields())
        writeEntryBinaries(entry.binaries)
        writeCustomData(entry.customData)
        writeAutoType(entry.autoType)

        if (!isHistory) {
            writeEntryHistory(entry.history)
        }

        xml.endTag(null, DatabaseKDBXXML.ElemEntry)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeString(name: String, value: String, filterXmlChars: Boolean = false) {
        var xmlString = value

        xml.startTag(null, name)

        if (filterXmlChars) {
            xmlString = safeXmlString(xmlString)
        }

        xml.text(xmlString)
        xml.endTag(null, name)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeDateInstant(name: String, value: DateInstant) {
        val date = value.date
        if (header!!.version.isBefore(FILE_VERSION_40)) {
            writeString(name, DatabaseKDBXXML.DateFormatter.format(date))
        } else {
            val buf = longTo8Bytes(DateKDBXUtil.convertDateToKDBX4Time(date))
            val b64 = String(Base64.encode(buf, BASE64_FLAG))
            writeString(name, b64)
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeLong(name: String, value: Long) {
        writeString(name, value.toString())
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeBoolean(name: String, value: Boolean?) {
        val text: String = when {
            value == null -> DatabaseKDBXXML.ValNull
            value -> DatabaseKDBXXML.ValTrue
            else -> DatabaseKDBXXML.ValFalse
        }

        writeString(name, text)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeUuid(name: String, uuid: UUID) {
        val data = uuidTo16Bytes(uuid)
        writeString(name, String(Base64.encode(data, BASE64_FLAG)))
    }

    /*
    // Normally used by a single entry but obsolete because binaries are in meta tag with kdbx3.1-
    // or in file header with kdbx4
    // binary.isProtected attribute is not used to create the XML
    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeEntryBinary(binary : BinaryAttachment) {
        if (binary.length() > 0) {
            if (binary.isProtected) {
                xml.attribute(null, DatabaseKDBXXML.AttrProtected, DatabaseKDBXXML.ValTrue)
                binary.getInputDataStream().use { inputStream ->
                    inputStream.readBytes { buffer ->
                        val encoded = ByteArray(buffer.size)
                        randomStream!!.processBytes(buffer, 0, encoded.size, encoded, 0)
                        xml.text(String(Base64.encode(encoded, BASE_64_FLAG)))
                    }
                }
            } else {
                // Write the XML
                binary.getInputDataStream().use { inputStream ->
                    inputStream.readBytes { buffer ->
                        xml.text(String(Base64.encode(buffer, BASE_64_FLAG)))
                    }
                }
            }
        }
    }
    */

    // Only uses with kdbx3.1 to write binaries in meta tag
    // With kdbx4, don't use this method because binaries are in header file
    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeMetaBinaries() {
        xml.startTag(null, DatabaseKDBXXML.ElemBinaries)
        // Use indexes because necessarily (binary header ref is the order)
        val binaryCache = mDatabaseKDBX.binaryCache
        mDatabaseKDBX.attachmentPool.doForEachOrderedBinaryWithoutDuplication { index, binary ->
            xml.startTag(null, DatabaseKDBXXML.ElemBinary)
            xml.attribute(null, DatabaseKDBXXML.AttrId, index.toString())
            if (binary.getSize() > 0) {
                if (binary.isCompressed) {
                    xml.attribute(null, DatabaseKDBXXML.AttrCompressed, DatabaseKDBXXML.ValTrue)
                }
                try {
                    // Write the XML
                    binary.getInputDataStream(binaryCache).use { inputStream ->
                        inputStream.readAllBytes { buffer ->
                            xml.text(String(Base64.encode(buffer, BASE64_FLAG)))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to write binary", e)
                }
            }
            xml.endTag(null, DatabaseKDBXXML.ElemBinary)
        }
        xml.endTag(null, DatabaseKDBXXML.ElemBinaries)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeAutoType(autoType: AutoType) {
        xml.startTag(null, DatabaseKDBXXML.ElemAutoType)

        writeBoolean(DatabaseKDBXXML.ElemAutoTypeEnabled, autoType.enabled)
        writeLong(DatabaseKDBXXML.ElemAutoTypeObfuscation, autoType.obfuscationOptions.toKotlinLong())

        if (autoType.defaultSequence.isNotEmpty()) {
            writeString(DatabaseKDBXXML.ElemAutoTypeDefaultSeq, autoType.defaultSequence, true)
        }

        autoType.doForEachAutoTypeItem { key, value ->
            xml.startTag(null, DatabaseKDBXXML.ElemAutoTypeItem)

            xml.startTag(null, DatabaseKDBXXML.ElemWindow)
            xml.text(safeXmlString(key))
            xml.endTag(null, DatabaseKDBXXML.ElemWindow)

            xml.startTag(null, DatabaseKDBXXML.ElemKeystrokeSequence)
            xml.text(safeXmlString(value))
            xml.endTag(null, DatabaseKDBXXML.ElemKeystrokeSequence)

            xml.endTag(null, DatabaseKDBXXML.ElemAutoTypeItem)
        }

        xml.endTag(null, DatabaseKDBXXML.ElemAutoType)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeFields(fields: List<Field>) {
        for (field in fields) {
            writeField(field)
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeField(field: Field) {
        val label = field.name
        val value = field.protectedValue

        xml.startTag(null, DatabaseKDBXXML.ElemString)
        xml.startTag(null, DatabaseKDBXXML.ElemKey)
        xml.text(safeXmlString(label))
        xml.endTag(null, DatabaseKDBXXML.ElemKey)

        xml.startTag(null, DatabaseKDBXXML.ElemValue)
        var protect = value.isProtected

        when (label) {
            MemoryProtectionConfig.TITLE_FIELD -> protect = mDatabaseKDBX.memoryProtection.protectTitle
            MemoryProtectionConfig.USERNAME_FIELD -> protect = mDatabaseKDBX.memoryProtection.protectUserName
            MemoryProtectionConfig.PASSWORD_FIELD -> protect = mDatabaseKDBX.memoryProtection.protectPassword
            MemoryProtectionConfig.URL_FIELD -> protect = mDatabaseKDBX.memoryProtection.protectUrl
            MemoryProtectionConfig.NOTES_FIELD -> protect = mDatabaseKDBX.memoryProtection.protectNotes
        }

        if (protect) {
            xml.attribute(null, DatabaseKDBXXML.AttrProtected, DatabaseKDBXXML.ValTrue)
            val data = value.toString().toByteArray()
            val encoded = randomStream?.processBytes(data) ?: ByteArray(0)
            xml.text(String(Base64.encode(encoded, BASE64_FLAG)))
        } else {
            xml.text(value.toString())
        }

        xml.endTag(null, DatabaseKDBXXML.ElemValue)
        xml.endTag(null, DatabaseKDBXXML.ElemString)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeDeletedObject(value: DeletedObject) {
        xml.startTag(null, DatabaseKDBXXML.ElemDeletedObject)

        writeUuid(DatabaseKDBXXML.ElemUuid, value.uuid)
        writeDateInstant(DatabaseKDBXXML.ElemDeletionTime, value.deletionTime)

        xml.endTag(null, DatabaseKDBXXML.ElemDeletedObject)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeEntryBinaries(binaries: LinkedHashMap<String, Int>) {
        for ((label, poolId) in binaries) {
            // Retrieve the right index with the poolId, don't use ref because of header in DatabaseV4
            mDatabaseKDBX.attachmentPool.getBinaryIndexFromKey(poolId)?.toString()?.let { indexString ->
                xml.startTag(null, DatabaseKDBXXML.ElemBinary)
                xml.startTag(null, DatabaseKDBXXML.ElemKey)
                xml.text(safeXmlString(label))
                xml.endTag(null, DatabaseKDBXXML.ElemKey)

                xml.startTag(null, DatabaseKDBXXML.ElemValue)
                // Use only pool data in Meta to save binaries
                xml.attribute(null, DatabaseKDBXXML.AttrRef, indexString)
                xml.endTag(null, DatabaseKDBXXML.ElemValue)

                xml.endTag(null, DatabaseKDBXXML.ElemBinary)
            }
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeDeletedObjects(value: Collection<DeletedObject>) {
        xml.startTag(null, DatabaseKDBXXML.ElemDeletedObjects)

        for (pdo in value) {
            writeDeletedObject(pdo)
        }

        xml.endTag(null, DatabaseKDBXXML.ElemDeletedObjects)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeMemoryProtection(value: MemoryProtectionConfig) {
        xml.startTag(null, DatabaseKDBXXML.ElemMemoryProt)

        writeBoolean(DatabaseKDBXXML.ElemProtTitle, value.protectTitle)
        writeBoolean(DatabaseKDBXXML.ElemProtUserName, value.protectUserName)
        writeBoolean(DatabaseKDBXXML.ElemProtPassword, value.protectPassword)
        writeBoolean(DatabaseKDBXXML.ElemProtURL, value.protectUrl)
        writeBoolean(DatabaseKDBXXML.ElemProtNotes, value.protectNotes)

        xml.endTag(null, DatabaseKDBXXML.ElemMemoryProt)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeCustomData(customData: CustomData) {
        if (customData.isNotEmpty()) {
            xml.startTag(null, DatabaseKDBXXML.ElemCustomData)

            customData.doForEachItems { customDataItem ->
                writeCustomDataItem(customDataItem)
            }

            xml.endTag(null, DatabaseKDBXXML.ElemCustomData)
        }
    }

    private fun writeCustomDataItem(customDataItem: CustomDataItem) {
        xml.startTag(null, DatabaseKDBXXML.ElemStringDictExItem)

        xml.startTag(null, DatabaseKDBXXML.ElemKey)
        xml.text(safeXmlString(customDataItem.key))
        xml.endTag(null, DatabaseKDBXXML.ElemKey)

        xml.startTag(null, DatabaseKDBXXML.ElemValue)
        xml.text(safeXmlString(customDataItem.value))
        xml.endTag(null, DatabaseKDBXXML.ElemValue)

        customDataItem.lastModificationTime?.let { lastModificationTime ->
            writeDateInstant(DatabaseKDBXXML.ElemLastModTime, lastModificationTime)
        }

        xml.endTag(null, DatabaseKDBXXML.ElemStringDictExItem)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeTags(tags: Tags) {
        if (tags.isNotEmpty()) {
            writeString(DatabaseKDBXXML.ElemTags, tags.toString())
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writePreviousParentGroup(previousParentGroup: UUID) {
        if (!header!!.version.isBefore(FILE_VERSION_41)
                && previousParentGroup != DatabaseVersioned.UUID_ZERO) {
            writeUuid(DatabaseKDBXXML.ElemPreviousParentGroup, previousParentGroup)
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeTimes(node: NodeKDBXInterface) {
        xml.startTag(null, DatabaseKDBXXML.ElemTimes)

        writeDateInstant(DatabaseKDBXXML.ElemLastModTime, node.lastModificationTime)
        writeDateInstant(DatabaseKDBXXML.ElemCreationTime, node.creationTime)
        writeDateInstant(DatabaseKDBXXML.ElemLastAccessTime, node.lastAccessTime)
        writeDateInstant(DatabaseKDBXXML.ElemExpiryTime, node.expiryTime)
        writeBoolean(DatabaseKDBXXML.ElemExpires, node.expires)
        writeLong(DatabaseKDBXXML.ElemUsageCount, node.usageCount.toKotlinLong())
        writeDateInstant(DatabaseKDBXXML.ElemLocationChanged, node.locationChanged)

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
        var firstElement = true
        val binaryCache = mDatabaseKDBX.binaryCache
        mDatabaseKDBX.iconsManager.doForEachCustomIcon { iconCustom, binary ->
            if (binary.dataExists()) {
                // Write the parent tag
                if (firstElement) {
                    xml.startTag(null, DatabaseKDBXXML.ElemCustomIcons)
                    firstElement = false
                }

                xml.startTag(null, DatabaseKDBXXML.ElemCustomIconItem)

                writeUuid(DatabaseKDBXXML.ElemCustomIconItemID, iconCustom.uuid)
                var customImageData = ByteArray(0)
                try {
                    binary.getInputDataStream(binaryCache).use { inputStream ->
                        customImageData = inputStream.readBytes()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to write custom icon", e)
                } finally {
                    writeString(DatabaseKDBXXML.ElemCustomIconItemData,
                            String(Base64.encode(customImageData, BASE64_FLAG)))
                }
                if (iconCustom.name.isNotEmpty()) {
                    writeString(DatabaseKDBXXML.ElemName, iconCustom.name)
                }
                iconCustom.lastModificationTime?.let { lastModificationTime ->
                    writeDateInstant(DatabaseKDBXXML.ElemLastModTime, lastModificationTime)
                }

                xml.endTag(null, DatabaseKDBXXML.ElemCustomIconItem)
            }
        }
        // Close the parent tag
        if (!firstElement) {
            xml.endTag(null, DatabaseKDBXXML.ElemCustomIcons)
        }
    }

    private fun safeXmlString(text: String): String {
        if (text.isEmpty()) {
            return text
        }
        val stringBuilder = StringBuilder()
        var character: Char
        for (element in text) {
            character = element
            val hexChar = character.code
            if (
                    hexChar in 0x20..0xD7FF ||
                    hexChar == 0x9 ||
                    hexChar == 0xA ||
                    hexChar == 0xD ||
                    hexChar in 0xE000..0xFFFD
            ) {
                stringBuilder.append(character)
            }
        }
        return stringBuilder.toString()
    }

    companion object {
        private val TAG = DatabaseOutputKDBX::class.java.name
    }
}
