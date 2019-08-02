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

import android.util.Log
import android.util.Xml
import biz.source_code.base64Coder.Base64Coder
import com.kunzisoft.keepass.crypto.CipherFactory
import com.kunzisoft.keepass.crypto.CrsAlgorithm
import com.kunzisoft.keepass.crypto.StreamCipherFactory
import com.kunzisoft.keepass.crypto.engine.CipherEngine
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory
import com.kunzisoft.keepass.database.*
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.exception.PwDbOutputException
import com.kunzisoft.keepass.database.exception.UnknownKDF
import com.kunzisoft.keepass.database.file.PwCompressionAlgorithm
import com.kunzisoft.keepass.database.file.PwDbHeaderV4
import com.kunzisoft.keepass.database.element.security.ProtectedBinary
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.stream.HashedBlockOutputStream
import com.kunzisoft.keepass.stream.HmacBlockOutputStream
import com.kunzisoft.keepass.stream.LEDataOutputStream
import com.kunzisoft.keepass.database.file.KDBX4DateUtil
import com.kunzisoft.keepass.utils.MemUtil
import com.kunzisoft.keepass.utils.Types
import org.joda.time.DateTime
import org.spongycastle.crypto.StreamCipher
import org.xmlpull.v1.XmlSerializer

import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import java.util.zip.GZIPOutputStream

class PwDbV4Output(private val mDatabaseV4: PwDatabaseV4, outputStream: OutputStream) : PwDbOutput<PwDbHeaderV4>(outputStream) {

    private var randomStream: StreamCipher? = null
    private lateinit var xml: XmlSerializer
    private var header: PwDbHeaderV4? = null
    private var hashOfHeader: ByteArray? = null
    private var headerHmac: ByteArray? = null
    private var engine: CipherEngine? = null

    @Throws(PwDbOutputException::class)
    override fun output() {

        try {
            try {
                engine = CipherFactory.getInstance(mDatabaseV4.dataCipher)
            } catch (e: NoSuchAlgorithmException) {
                throw PwDbOutputException("No such cipher", e)
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
                if (mDatabaseV4.compressionAlgorithm === PwCompressionAlgorithm.Gzip) {
                    osXml = GZIPOutputStream(osPlain)
                } else {
                    osXml = osPlain
                }

                if (header!!.version >= PwDbHeaderV4.FILE_VERSION_32_4) {
                    val ihOut = PwDbInnerHeaderOutputV4(mDatabaseV4, header!!, osXml)
                    ihOut.output()
                }

                outputDatabase(osXml)
                osXml.close()
            } catch (e: IllegalArgumentException) {
                throw PwDbOutputException(e)
            } catch (e: IllegalStateException) {
                throw PwDbOutputException(e)
            }

        } catch (e: IOException) {
            throw PwDbOutputException(e)
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

        writeList(PwDatabaseV4XML.ElemDeletedObjects, mDatabaseV4.deletedObjects)

        xml.endTag(null, PwDatabaseV4XML.ElemRoot)

        xml.endTag(null, PwDatabaseV4XML.ElemDocNode)
        xml.endDocument()
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeMeta() {
        xml.startTag(null, PwDatabaseV4XML.ElemMeta)

        writeObject(PwDatabaseV4XML.ElemGenerator, mDatabaseV4.localizedAppName)

        if (hashOfHeader != null) {
            writeObject(PwDatabaseV4XML.ElemHeaderHash, String(Base64Coder.encode(hashOfHeader!!)))
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

        writeList(PwDatabaseV4XML.ElemMemoryProt, mDatabaseV4.memoryProtection)

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
        writeList(PwDatabaseV4XML.ElemCustomData, mDatabaseV4.customData)

        xml.endTag(null, PwDatabaseV4XML.ElemMeta)
    }

    @Throws(PwDbOutputException::class)
    private fun attachStreamEncryptor(header: PwDbHeaderV4, os: OutputStream): CipherOutputStream {
        val cipher: Cipher
        try {
            //mDatabaseV4.makeFinalKey(header.masterSeed, mDatabaseV4.kdfParameters);

            cipher = engine!!.getCipher(Cipher.ENCRYPT_MODE, mDatabaseV4.finalKey!!, header.encryptionIV)
        } catch (e: Exception) {
            throw PwDbOutputException("Invalid algorithm.", e)
        }

        return CipherOutputStream(os, cipher)
    }

    @Throws(PwDbOutputException::class)
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
            val kdf = KdfFactory.getEngineV4(mDatabaseV4.kdfParameters)
            kdf.randomize(mDatabaseV4.kdfParameters)
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
            throw PwDbOutputException("Invalid random cipher")
        }

        if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
            random.nextBytes(header.streamStartBytes)
        }

        return random
    }

    @Throws(PwDbOutputException::class)
    override fun outputHeader(outputStream: OutputStream): PwDbHeaderV4 {

        val header = PwDbHeaderV4(mDatabaseV4)
        setIVs(header)

        val pho = PwDbHeaderOutputV4(mDatabaseV4, header, outputStream)
        try {
            pho.output()
        } catch (e: IOException) {
            throw PwDbOutputException("Failed to output the header.", e)
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

        writeList(PwDatabaseV4XML.ElemTimes, group)
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

        writeList(PwDatabaseV4XML.ElemTimes, entry)

        writeList(entry.fields.listOfAllFields, true)
        writeList(entry.binaries)
        writeList(PwDatabaseV4XML.ElemAutoType, entry.autoType)

        if (!isHistory) {
            writeList(PwDatabaseV4XML.ElemHistory, entry.history, true)
        }
        // else entry.sizeOfHistory() == 0

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
        val strRef = Integer.toString(ref)

        if (strRef != null) {
            xml.attribute(null, PwDatabaseV4XML.AttrRef, strRef)
        } else {
            subWriteValue(value)
        }
        xml.endTag(null, PwDatabaseV4XML.ElemValue)

        xml.endTag(null, PwDatabaseV4XML.ElemBinary)
    }

    /*
	TODO Make with pipe
	private void subWriteValue(ProtectedBinary value) throws IllegalArgumentException, IllegalStateException, IOException {
        try (InputStream inputStream = value.getData()) {
            if (inputStream == null) {
                Log.e(TAG, "Can't write a null input stream.");
                return;
            }

            if (value.isProtected()) {
                xml.attribute(null, PwDatabaseV4XML.AttrProtected, PwDatabaseV4XML.ValTrue);

                try (InputStream cypherInputStream =
                             IOUtil.pipe(inputStream,
                                     o -> new org.spongycastle.crypto.io.CipherOutputStream(o, randomStream))) {
                    writeInputStreamInBase64(cypherInputStream);
                }

            } else {
                if (mDatabaseV4.getCompressionAlgorithm() == PwCompressionAlgorithm.Gzip) {

                    xml.attribute(null, PwDatabaseV4XML.AttrCompressed, PwDatabaseV4XML.ValTrue);

                    try (InputStream gZipInputStream =
                                 IOUtil.pipe(inputStream, GZIPOutputStream::new, (int) value.length())) {
                        writeInputStreamInBase64(gZipInputStream);
                    }

                } else {
                    writeInputStreamInBase64(inputStream);
                }
            }
        }
	}

	private void writeInputStreamInBase64(InputStream inputStream) throws IOException {
        try (InputStream base64InputStream =
                     IOUtil.pipe(inputStream,
                             o -> new Base64OutputStream(o, DEFAULT))) {
            MemUtil.readBytes(base64InputStream,
                    buffer -> xml.text(Arrays.toString(buffer)));
        }
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
                    xml.text(String(Base64Coder.encode(encoded)))

                } else {
                    if (mDatabaseV4.compressionAlgorithm === PwCompressionAlgorithm.Gzip) {
                        xml.attribute(null, PwDatabaseV4XML.AttrCompressed, PwDatabaseV4XML.ValTrue)

                        val compressData = MemUtil.compress(buffer)
                        xml.text(String(Base64Coder.encode(compressData)))

                    } else {
                        xml.text(String(Base64Coder.encode(buffer)))
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
            val b64 = String(Base64Coder.encode(buf))
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
            value == null -> "null"
            value -> PwDatabaseV4XML.ValTrue
            else -> PwDatabaseV4XML.ValFalse
        }

        writeObject(name, text)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeObject(name: String, uuid: UUID) {
        val data = Types.UUIDtoBytes(uuid)
        writeObject(name, String(Base64Coder.encode(data)))
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
    private fun writeList(name: String, autoType: AutoType) {
        xml.startTag(null, name)

        writeObject(PwDatabaseV4XML.ElemAutoTypeEnabled, autoType.enabled)
        writeObject(PwDatabaseV4XML.ElemAutoTypeObfuscation, autoType.obfuscationOptions)

        if (autoType.defaultSequence.isNotEmpty()) {
            writeObject(PwDatabaseV4XML.ElemAutoTypeDefaultSeq, autoType.defaultSequence, true)
        }

        for ((key, value) in autoType.entrySet()) {
            writeObject(PwDatabaseV4XML.ElemAutoTypeItem, PwDatabaseV4XML.ElemWindow, key, PwDatabaseV4XML.ElemKeystrokeSequence, value)
        }

        xml.endTag(null, name)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeList(strings: Map<String, ProtectedString>, isEntryString: Boolean) {

        for ((key, value) in strings) {
            writeObject(key, value, isEntryString)
        }
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeObject(key: String, value: ProtectedString, isEntryString: Boolean) {

        xml.startTag(null, PwDatabaseV4XML.ElemString)
        xml.startTag(null, PwDatabaseV4XML.ElemKey)
        xml.text(safeXmlString(key))
        xml.endTag(null, PwDatabaseV4XML.ElemKey)

        xml.startTag(null, PwDatabaseV4XML.ElemValue)
        var protect = value.isProtected
        if (isEntryString) {
            when (key) {
                MemoryProtectionConfig.ProtectDefinition.TITLE_FIELD -> protect = mDatabaseV4.memoryProtection.protectTitle
                MemoryProtectionConfig.ProtectDefinition.USERNAME_FIELD -> protect = mDatabaseV4.memoryProtection.protectUserName
                MemoryProtectionConfig.ProtectDefinition.PASSWORD_FIELD -> protect = mDatabaseV4.memoryProtection.protectPassword
                MemoryProtectionConfig.ProtectDefinition.URL_FIELD -> protect = mDatabaseV4.memoryProtection.protectUrl
                MemoryProtectionConfig.ProtectDefinition.NOTES_FIELD -> protect = mDatabaseV4.memoryProtection.protectNotes
            }
        }

        if (protect) {
            xml.attribute(null, PwDatabaseV4XML.AttrProtected, PwDatabaseV4XML.ValTrue)

            val data = value.toString().toByteArray(charset("UTF-8"))
            val valLength = data.size

            if (valLength > 0) {
                val encoded = ByteArray(valLength)
                randomStream!!.processBytes(data, 0, valLength, encoded, 0)
                xml.text(String(Base64Coder.encode(encoded)))
            }
        } else {
            xml.text(safeXmlString(value.toString()))
        }

        xml.endTag(null, PwDatabaseV4XML.ElemValue)
        xml.endTag(null, PwDatabaseV4XML.ElemString)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeObject(name: String?, value: PwDeletedObject) {
        assert(name != null)

        xml.startTag(null, name)

        writeObject(PwDatabaseV4XML.ElemUuid, value.uuid)
        writeObject(PwDatabaseV4XML.ElemDeletionTime, value.deletionTime)

        xml.endTag(null, name)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeList(binaries: Map<String, ProtectedBinary>) {
        for ((key, value) in binaries) {
            writeObject(key, value)
        }
    }


    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeList(name: String?, value: List<PwDeletedObject>) {
        assert(name != null)

        xml.startTag(null, name)

        for (pdo in value) {
            writeObject(PwDatabaseV4XML.ElemDeletedObject, pdo)
        }

        xml.endTag(null, name)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeList(name: String?, value: MemoryProtectionConfig) {
        assert(name != null)

        xml.startTag(null, name)

        writeObject(PwDatabaseV4XML.ElemProtTitle, value.protectTitle)
        writeObject(PwDatabaseV4XML.ElemProtUserName, value.protectUserName)
        writeObject(PwDatabaseV4XML.ElemProtPassword, value.protectPassword)
        writeObject(PwDatabaseV4XML.ElemProtURL, value.protectUrl)
        writeObject(PwDatabaseV4XML.ElemProtNotes, value.protectNotes)

        xml.endTag(null, name)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeList(name: String?, customData: Map<String, String>) {
        assert(name != null)

        xml.startTag(null, name)

        for ((key, value) in customData) {
            writeObject(PwDatabaseV4XML.ElemStringDictExItem, PwDatabaseV4XML.ElemKey, key, PwDatabaseV4XML.ElemValue, value)

        }

        xml.endTag(null, name)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeList(name: String?, it: NodeV4Interface) {
        assert(name != null)

        xml.startTag(null, name)

        writeObject(PwDatabaseV4XML.ElemLastModTime, it.lastModificationTime.date)
        writeObject(PwDatabaseV4XML.ElemCreationTime, it.creationTime.date)
        writeObject(PwDatabaseV4XML.ElemLastAccessTime, it.lastAccessTime.date)
        writeObject(PwDatabaseV4XML.ElemExpiryTime, it.expiryTime.date)
        writeObject(PwDatabaseV4XML.ElemExpires, it.isExpires)
        writeObject(PwDatabaseV4XML.ElemUsageCount, it.usageCount)
        writeObject(PwDatabaseV4XML.ElemLocationChanged, it.locationChanged.date)

        xml.endTag(null, name)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeList(name: String?, value: List<PwEntryV4>, isHistory: Boolean) {
        assert(name != null)

        xml.startTag(null, name)

        for (entry in value) {
            writeEntry(entry, isHistory)
        }

        xml.endTag(null, name)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeCustomIconList() {
        val customIcons = mDatabaseV4.customIcons
        if (customIcons.size == 0) return

        xml.startTag(null, PwDatabaseV4XML.ElemCustomIcons)

        for (icon in customIcons) {
            xml.startTag(null, PwDatabaseV4XML.ElemCustomIconItem)

            writeObject(PwDatabaseV4XML.ElemCustomIconItemID, icon.uuid)
            writeObject(PwDatabaseV4XML.ElemCustomIconItemData, String(Base64Coder.encode(icon.imageData)))

            xml.endTag(null, PwDatabaseV4XML.ElemCustomIconItem)
        }

        xml.endTag(null, PwDatabaseV4XML.ElemCustomIcons)
    }

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun writeBinPool() {
        xml.startTag(null, PwDatabaseV4XML.ElemBinaries)

        for ((key, value) in mDatabaseV4.binPool.entrySet()) {
            xml.startTag(null, PwDatabaseV4XML.ElemBinary)
            xml.attribute(null, PwDatabaseV4XML.AttrId, Integer.toString(key))

            subWriteValue(value)

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
        for (i in 0 until text.length) {
            ch = text[i]
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
    }
}
