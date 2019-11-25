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
package com.kunzisoft.keepass.database.file.save

import android.util.Base64
import android.util.Log
import android.util.Xml
import com.kunzisoft.keepass.crypto.CipherFactory
import com.kunzisoft.keepass.crypto.CrsAlgorithm
import com.kunzisoft.keepass.crypto.StreamCipherFactory
import com.kunzisoft.keepass.crypto.engine.CipherEngine
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory
import com.kunzisoft.keepass.database.NodeHandler
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.PwDatabaseV4.Companion.BASE_64_FLAG
import com.kunzisoft.keepass.database.element.security.ProtectedBinary
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.exception.DatabaseOutputException
import com.kunzisoft.keepass.database.exception.UnknownKDF
import com.kunzisoft.keepass.database.file.KDBX4DateUtil
import com.kunzisoft.keepass.database.file.PwDbHeaderV4
import com.kunzisoft.keepass.stream.HashedBlockOutputStream
import com.kunzisoft.keepass.stream.HmacBlockOutputStream
import com.kunzisoft.keepass.stream.LEDataOutputStream
import com.kunzisoft.keepass.utils.DatabaseInputOutputUtils
import com.kunzisoft.keepass.utils.MemoryUtil
import org.joda.time.DateTime
import org.spongycastle.crypto.StreamCipher
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream


class PwDbV4Output(private val mDatabaseV4: PwDatabaseV4, outputStream: OutputStream) : PwDbOutput<PwDbHeaderV4>(outputStream) {

    private var randomStream: StreamCipher? = null
    private lateinit var xml: XmlSerializer
    private var header: PwDbHeaderV4? = null
    private var hashOfHeader: ByteArray? = null
    private var headerHmac: ByteArray? = null
    private var engine: CipherEngine? = null

    @Throws(DatabaseOutputException::class)
    override fun output() {

        try {
            try {
                engine = CipherFactory.getInstance(mDatabaseV4.dataCipher)
            } catch (e: NoSuchAlgorithmException) {
                throw DatabaseOutputException("No such cipher", e)
            }

            header = outputHeader(mOS)

            val osPlain: OutputStream
            if (header!!.version < PwDbHeaderV4.FILE_VERSION_32_4) {
                val cos = attachStreamEncryptor(header!!, mOS)
                cos.write(header!!.streamStartBytes)

                osPlain = HashedBlockOutputStream(cos)
            } else {
                mOS.write(hashOfHeader!!)
                mOS.write(headerHmac!!)

                val hbos = HmacBlockOutputStream(mOS, mDatabaseV4.hmacKey)
                osPlain = attachStreamEncryptor(header!!, hbos)
            }

            val osXml: OutputStream
            try {
                osXml = when(mDatabaseV4.compressionAlgorithm) {
                    PwCompressionAlgorithm.GZip -> GZIPOutputStream(osPlain)
                    else -> osPlain
                }

                if (header!!.version >= PwDbHeaderV4.FILE_VERSION_32_4) {
                    val ihOut = PwDbInnerHeaderOutputV4(mDatabaseV4, header!!, osXml)
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
    private fun outputDatabase(os: OutputStream) {

        xml = Xml.newSerializer()

        xml.setOutput(os, "UTF-8")
        xml.startDocument("UTF-8", true)

        xml.startTag(null, PwDatabaseV4XML.ElemDocNode)

        writeMeta()

        mDatabaseV4.rootGroup?.let { root ->
            xml.startTag(null, PwDatabaseV4XML.ElemRoot)
            startGroup(root)
            val groupStack = Stack<PwGroupV4>()
            groupStack.push(root)

            if (!root.doForEachChild(
                            object : NodeHandler<PwEntryV4>() {
                                override fun operate(node: PwEntryV4): Boolean {
                                    try {
                                        writeEntry(node, false)
                                    } catch (ex: IOException) {
                                        throw RuntimeException(ex)
                                    }

                                    return true
                                }
                            },
                            object : NodeHandler<PwGroupV4>() {
                                override fun operate(node: PwGroupV4): Boolean {
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
                xml.endTag(null, PwDatabaseV4XML.ElemGroup)
                groupStack.pop()
            }
        }

        endGroup()

        writeDeletedObjects(mDatabaseV4.deletedObjects)

        xml.endTag(null, PwDatabaseV4XML.ElemRoot)

        xml.endTag(null, PwDatabaseV4XML.ElemDocNode)
        xml.endDocument()
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeMeta() {
        xml.startTag(null, PwDatabaseV4XML.ElemMeta)

        writeObject(PwDatabaseV4XML.ElemGenerator, mDatabaseV4.localizedAppName)

        if (hashOfHeader != null) {
            writeObject(PwDatabaseV4XML.ElemHeaderHash, String(Base64.encode(hashOfHeader!!, BASE_64_FLAG)))
        }

        writeObject(PwDatabaseV4XML.ElemDbName, mDatabaseV4.name, true)
        writeObject(PwDatabaseV4XML.ElemDbNameChanged, mDatabaseV4.nameChanged.date)
        writeObject(PwDatabaseV4XML.ElemDbDesc, mDatabaseV4.description, true)
        writeObject(PwDatabaseV4XML.ElemDbDescChanged, mDatabaseV4.descriptionChanged.date)
        writeObject(PwDatabaseV4XML.ElemDbDefaultUser, mDatabaseV4.defaultUserName, true)
        writeObject(PwDatabaseV4XML.ElemDbDefaultUserChanged, mDatabaseV4.defaultUserNameChanged.date)
        writeObject(PwDatabaseV4XML.ElemDbMntncHistoryDays, mDatabaseV4.maintenanceHistoryDays)
        writeObject(PwDatabaseV4XML.ElemDbColor, mDatabaseV4.color)
        writeObject(PwDatabaseV4XML.ElemDbKeyChanged, mDatabaseV4.keyLastChanged.date)
        writeObject(PwDatabaseV4XML.ElemDbKeyChangeRec, mDatabaseV4.keyChangeRecDays)
        writeObject(PwDatabaseV4XML.ElemDbKeyChangeForce, mDatabaseV4.keyChangeForceDays)

        writeMemoryProtection(mDatabaseV4.memoryProtection)

        writeCustomIconList()

        writeObject(PwDatabaseV4XML.ElemRecycleBinEnabled, mDatabaseV4.isRecycleBinEnabled)
        writeObject(PwDatabaseV4XML.ElemRecycleBinUuid, mDatabaseV4.recycleBinUUID)
        writeObject(PwDatabaseV4XML.ElemRecycleBinChanged, mDatabaseV4.recycleBinChanged)
        writeObject(PwDatabaseV4XML.ElemEntryTemplatesGroup, mDatabaseV4.entryTemplatesGroup)
        writeObject(PwDatabaseV4XML.ElemEntryTemplatesGroupChanged, mDatabaseV4.entryTemplatesGroupChanged.date)
        writeObject(PwDatabaseV4XML.ElemHistoryMaxItems, mDatabaseV4.historyMaxItems.toLong())
        writeObject(PwDatabaseV4XML.ElemHistoryMaxSize, mDatabaseV4.historyMaxSize)
        writeObject(PwDatabaseV4XML.ElemLastSelectedGroup, mDatabaseV4.lastSelectedGroupUUID)
        writeObject(PwDatabaseV4XML.ElemLastTopVisibleGroup, mDatabaseV4.lastTopVisibleGroupUUID)

        if (header!!.version < PwDbHeaderV4.FILE_VERSION_32_4) {
            writeBinPool()
        }
        writeCustomData(mDatabaseV4.customData)

        xml.endTag(null, PwDatabaseV4XML.ElemMeta)
    }

    @Throws(DatabaseOutputException::class)
    private fun attachStreamEncryptor(header: PwDbHeaderV4, os: OutputStream): CipherOutputStream {
        val cipher: Cipher
        try {
            //mDatabaseV4.makeFinalKey(header.masterSeed, mDatabaseV4.kdfParameters);

            cipher = engine!!.getCipher(Cipher.ENCRYPT_MODE, mDatabaseV4.finalKey!!, header.encryptionIV)
        } catch (e: Exception) {
            throw DatabaseOutputException("Invalid algorithm.", e)
        }

        return CipherOutputStream(os, cipher)
    }

    @Throws(DatabaseOutputException::class)
    override fun setIVs(header: PwDbHeaderV4): SecureRandom {
        val random = super.setIVs(header)
        random.nextBytes(header.masterSeed)

        val ivLength = engine!!.ivLength()
        if (ivLength != header.encryptionIV.size) {
            header.encryptionIV = ByteArray(ivLength)
        }
        random.nextBytes(header.encryptionIV)

        if (mDatabaseV4.kdfParameters == null) {
            mDatabaseV4.kdfParameters = KdfFactory.aesKdf.defaultParameters
        }

        try {
            val kdf = mDatabaseV4.getEngineV4(mDatabaseV4.kdfParameters)
            kdf.randomize(mDatabaseV4.kdfParameters!!)
        } catch (unknownKDF: UnknownKDF) {
            Log.e(TAG, "Unable to retrieve header", unknownKDF)
        }

        if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
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

        if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
            random.nextBytes(header.streamStartBytes)
        }

        return random
    }

    @Throws(DatabaseOutputException::class)
    override fun outputHeader(outputStream: OutputStream): PwDbHeaderV4 {

        val header = PwDbHeaderV4(mDatabaseV4)
        setIVs(header)

        val pho = PwDbHeaderOutputV4(mDatabaseV4, header, outputStream)
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
    private fun startGroup(group: PwGroupV4) {
        xml.startTag(null, PwDatabaseV4XML.ElemGroup)
        writeObject(PwDatabaseV4XML.ElemUuid, group.id)
        writeObject(PwDatabaseV4XML.ElemName, group.title)
        writeObject(PwDatabaseV4XML.ElemNotes, group.notes)
        writeObject(PwDatabaseV4XML.ElemIcon, group.icon.iconId.toLong())

        if (group.iconCustom != PwIconCustom.UNKNOWN_ICON) {
            writeObject(PwDatabaseV4XML.ElemCustomIconID, group.iconCustom.uuid)
        }

        writeTimes(group)
        writeObject(PwDatabaseV4XML.ElemIsExpanded, group.isExpanded)
        writeObject(PwDatabaseV4XML.ElemGroupDefaultAutoTypeSeq, group.defaultAutoTypeSequence)
        writeObject(PwDatabaseV4XML.ElemEnableAutoType, group.enableAutoType)
        writeObject(PwDatabaseV4XML.ElemEnableSearching, group.enableSearching)
        writeObject(PwDatabaseV4XML.ElemLastTopVisibleEntry, group.lastTopVisibleEntry)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun endGroup() {
        xml.endTag(null, PwDatabaseV4XML.ElemGroup)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeEntry(entry: PwEntryV4, isHistory: Boolean) {

        xml.startTag(null, PwDatabaseV4XML.ElemEntry)

        writeObject(PwDatabaseV4XML.ElemUuid, entry.id)
        writeObject(PwDatabaseV4XML.ElemIcon, entry.icon.iconId.toLong())

        if (entry.iconCustom != PwIconCustom.UNKNOWN_ICON) {
            writeObject(PwDatabaseV4XML.ElemCustomIconID, entry.iconCustom.uuid)
        }

        writeObject(PwDatabaseV4XML.ElemFgColor, entry.foregroundColor)
        writeObject(PwDatabaseV4XML.ElemBgColor, entry.backgroundColor)
        writeObject(PwDatabaseV4XML.ElemOverrideUrl, entry.overrideURL)
        writeObject(PwDatabaseV4XML.ElemTags, entry.tags)

        writeTimes(entry)

        writeFields(entry.fields)
        writeList(entry.binaries)
        writeAutoType(entry.autoType)

        if (!isHistory) {
            writeEntryHistory(entry.history)
        }

        xml.endTag(null, PwDatabaseV4XML.ElemEntry)
    }


    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeObject(key: String, value: ProtectedBinary) {

        xml.startTag(null, PwDatabaseV4XML.ElemBinary)
        xml.startTag(null, PwDatabaseV4XML.ElemKey)
        xml.text(safeXmlString(key))
        xml.endTag(null, PwDatabaseV4XML.ElemKey)

        xml.startTag(null, PwDatabaseV4XML.ElemValue)
        val ref = mDatabaseV4.binPool.findKey(value)
        if (ref != null) {
            xml.attribute(null, PwDatabaseV4XML.AttrRef, ref.toString())
        } else {
            subWriteValue(value)
        }
        xml.endTag(null, PwDatabaseV4XML.ElemValue)

        xml.endTag(null, PwDatabaseV4XML.ElemBinary)
    }

    /*
    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
	private fun subWriteValue(value: ProtectedBinary) {
        try {
            val inputStream = value.getData()
            if (inputStream == null) {
                Log.e(TAG, "Can't write a null input stream.")
                return
            }

            if (value.isProtected) {
                xml.attribute(null, PwDatabaseV4XML.AttrProtected, PwDatabaseV4XML.ValTrue)

                try {
                    val cypherInputStream =
                    IOUtil.pipe(inputStream,
                            o -> new org.spongycastle.crypto.io.CipherOutputStream(o, randomStream))
                    writeInputStreamInBase64(cypherInputStream)
                } catch (e: Exception) {}

            } else {
                if (mDatabaseV4.compressionAlgorithm == PwCompressionAlgorithm.GZip) {
                    xml.attribute(null, PwDatabaseV4XML.AttrCompressed, PwDatabaseV4XML.ValTrue)

                    try {
                        val gZipInputStream =
                        IOUtil.pipe(inputStream, GZIPOutputStream::new, (int) value.length())
                        writeInputStreamInBase64(gZipInputStream)
                    } catch (e: Exception) {}

                } else {
                    writeInputStreamInBase64(inputStream);
                }
            }
        } catch (e: Exception) {}
	}

    @Throws(IOException::class)
	private fun writeInputStreamInBase64(inputStream: InputStream) {
        try {
            val base64InputStream = pipe(inputStream, Base64OutputStream(o, Base64OutputStream.DEFAULT))
            MemoryUtil.readBytes(base64InputStream,
                    ActionReadBytes { buffer -> xml.text(Arrays.toString(buffer)) })

        } catch (e: Exception) {}
    }
     */

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun subWriteValue(value: ProtectedBinary) {

        val valLength = value.length().toInt()
        if (valLength > 0) {
            val buffer = ByteArray(valLength)
            if (valLength == value.getData()!!.read(buffer, 0, valLength)) {

                if (value.isProtected) {
                    xml.attribute(null, PwDatabaseV4XML.AttrProtected, PwDatabaseV4XML.ValTrue)

                    val encoded = ByteArray(valLength)
                    randomStream!!.processBytes(buffer, 0, valLength, encoded, 0)
                    xml.text(String(Base64.encode(encoded, BASE_64_FLAG)))

                } else {
                    if (mDatabaseV4.compressionAlgorithm === PwCompressionAlgorithm.GZip) {
                        xml.attribute(null, PwDatabaseV4XML.AttrCompressed, PwDatabaseV4XML.ValTrue)

                        val compressData = MemoryUtil.compress(buffer)
                        xml.text(String(Base64.encode(compressData, BASE_64_FLAG)))

                    } else {
                        xml.text(String(Base64.encode(buffer, BASE_64_FLAG)))
                    }
                }
            } else {
                Log.e(TAG, "Unable to read the stream of the protected binary")
            }
        }
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
    private fun writeObject(name: String, value: Date?) {
        if (header!!.version < PwDbHeaderV4.FILE_VERSION_32_4) {
            writeObject(name, PwDatabaseV4XML.dateFormatter.get().format(value))
        } else {
            val dt = DateTime(value)
            val seconds = KDBX4DateUtil.convertDateToKDBX4Time(dt)
            val buf = LEDataOutputStream.writeLongBuf(seconds)
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
            value == null -> PwDatabaseV4XML.ValNull
            value -> PwDatabaseV4XML.ValTrue
            else -> PwDatabaseV4XML.ValFalse
        }

        writeObject(name, text)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeObject(name: String, uuid: UUID) {
        val data = DatabaseInputOutputUtils.uuidToBytes(uuid)
        writeObject(name, String(Base64.encode(data, BASE_64_FLAG)))
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
        xml.startTag(null, PwDatabaseV4XML.ElemAutoType)

        writeObject(PwDatabaseV4XML.ElemAutoTypeEnabled, autoType.enabled)
        writeObject(PwDatabaseV4XML.ElemAutoTypeObfuscation, autoType.obfuscationOptions)

        if (autoType.defaultSequence.isNotEmpty()) {
            writeObject(PwDatabaseV4XML.ElemAutoTypeDefaultSeq, autoType.defaultSequence, true)
        }

        for ((key, value) in autoType.entrySet()) {
            writeObject(PwDatabaseV4XML.ElemAutoTypeItem, PwDatabaseV4XML.ElemWindow, key, PwDatabaseV4XML.ElemKeystrokeSequence, value)
        }

        xml.endTag(null, PwDatabaseV4XML.ElemAutoType)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeFields(fields: Map<String, ProtectedString>) {

        for ((key, value) in fields) {
            writeField(key, value)
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeField(key: String, value: ProtectedString) {

        xml.startTag(null, PwDatabaseV4XML.ElemString)
        xml.startTag(null, PwDatabaseV4XML.ElemKey)
        xml.text(safeXmlString(key))
        xml.endTag(null, PwDatabaseV4XML.ElemKey)

        xml.startTag(null, PwDatabaseV4XML.ElemValue)
        var protect = value.isProtected

        when (key) {
            MemoryProtectionConfig.ProtectDefinition.TITLE_FIELD -> protect = mDatabaseV4.memoryProtection.protectTitle
            MemoryProtectionConfig.ProtectDefinition.USERNAME_FIELD -> protect = mDatabaseV4.memoryProtection.protectUserName
            MemoryProtectionConfig.ProtectDefinition.PASSWORD_FIELD -> protect = mDatabaseV4.memoryProtection.protectPassword
            MemoryProtectionConfig.ProtectDefinition.URL_FIELD -> protect = mDatabaseV4.memoryProtection.protectUrl
            MemoryProtectionConfig.ProtectDefinition.NOTES_FIELD -> protect = mDatabaseV4.memoryProtection.protectNotes
        }

        if (protect) {
            xml.attribute(null, PwDatabaseV4XML.AttrProtected, PwDatabaseV4XML.ValTrue)

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

        xml.endTag(null, PwDatabaseV4XML.ElemValue)
        xml.endTag(null, PwDatabaseV4XML.ElemString)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeDeletedObject(value: PwDeletedObject) {
        xml.startTag(null, PwDatabaseV4XML.ElemDeletedObject)

        writeObject(PwDatabaseV4XML.ElemUuid, value.uuid)
        writeObject(PwDatabaseV4XML.ElemDeletionTime, value.deletionTime)

        xml.endTag(null, PwDatabaseV4XML.ElemDeletedObject)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeList(binaries: Map<String, ProtectedBinary>) {
        for ((key, value) in binaries) {
            writeObject(key, value)
        }
    }


    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeDeletedObjects(value: List<PwDeletedObject>) {
        xml.startTag(null, PwDatabaseV4XML.ElemDeletedObjects)

        for (pdo in value) {
            writeDeletedObject(pdo)
        }

        xml.endTag(null, PwDatabaseV4XML.ElemDeletedObjects)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeMemoryProtection(value: MemoryProtectionConfig) {
        xml.startTag(null, PwDatabaseV4XML.ElemMemoryProt)

        writeObject(PwDatabaseV4XML.ElemProtTitle, value.protectTitle)
        writeObject(PwDatabaseV4XML.ElemProtUserName, value.protectUserName)
        writeObject(PwDatabaseV4XML.ElemProtPassword, value.protectPassword)
        writeObject(PwDatabaseV4XML.ElemProtURL, value.protectUrl)
        writeObject(PwDatabaseV4XML.ElemProtNotes, value.protectNotes)

        xml.endTag(null, PwDatabaseV4XML.ElemMemoryProt)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeCustomData(customData: Map<String, String>) {
        xml.startTag(null, PwDatabaseV4XML.ElemCustomData)

        for ((key, value) in customData) {
            writeObject(
                    PwDatabaseV4XML.ElemStringDictExItem,
                    PwDatabaseV4XML.ElemKey,
                    key,
                    PwDatabaseV4XML.ElemValue,
                    value
            )
        }

        xml.endTag(null, PwDatabaseV4XML.ElemCustomData)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeTimes(node: PwNodeV4Interface) {
        xml.startTag(null, PwDatabaseV4XML.ElemTimes)

        writeObject(PwDatabaseV4XML.ElemLastModTime, node.lastModificationTime.date)
        writeObject(PwDatabaseV4XML.ElemCreationTime, node.creationTime.date)
        writeObject(PwDatabaseV4XML.ElemLastAccessTime, node.lastAccessTime.date)
        writeObject(PwDatabaseV4XML.ElemExpiryTime, node.expiryTime.date)
        writeObject(PwDatabaseV4XML.ElemExpires, node.expires)
        writeObject(PwDatabaseV4XML.ElemUsageCount, node.usageCount)
        writeObject(PwDatabaseV4XML.ElemLocationChanged, node.locationChanged.date)

        xml.endTag(null, PwDatabaseV4XML.ElemTimes)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeEntryHistory(value: List<PwEntryV4>) {
        val element = PwDatabaseV4XML.ElemHistory

        xml.startTag(null, element)

        for (entry in value) {
            writeEntry(entry, true)
        }

        xml.endTag(null, element)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeCustomIconList() {
        val customIcons = mDatabaseV4.customIcons
        if (customIcons.size == 0) return

        xml.startTag(null, PwDatabaseV4XML.ElemCustomIcons)

        for (icon in customIcons) {
            xml.startTag(null, PwDatabaseV4XML.ElemCustomIconItem)

            writeObject(PwDatabaseV4XML.ElemCustomIconItemID, icon.uuid)
            writeObject(PwDatabaseV4XML.ElemCustomIconItemData, String(Base64.encode(icon.imageData, BASE_64_FLAG)))

            xml.endTag(null, PwDatabaseV4XML.ElemCustomIconItem)
        }

        xml.endTag(null, PwDatabaseV4XML.ElemCustomIcons)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeBinPool() {
        xml.startTag(null, PwDatabaseV4XML.ElemBinaries)

        mDatabaseV4.binPool.doForEachBinary { key, binary ->
            xml.startTag(null, PwDatabaseV4XML.ElemBinary)
            xml.attribute(null, PwDatabaseV4XML.AttrId, key.toString())

            subWriteValue(binary)

            xml.endTag(null, PwDatabaseV4XML.ElemBinary)
        }

        xml.endTag(null, PwDatabaseV4XML.ElemBinaries)
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
        private val TAG = PwDbV4Output::class.java.name

        @Throws(IOException::class)
        fun pipe(inputStream: InputStream, outputStream: OutputStream, buf: ByteArray) {
            while (true) {
                val amt = inputStream.read(buf)
                if (amt < 0) {
                    break
                }
                outputStream.write(buf, 0, amt)
            }
        }
    }
}
