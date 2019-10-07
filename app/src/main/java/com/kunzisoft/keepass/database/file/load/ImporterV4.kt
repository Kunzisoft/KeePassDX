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
package com.kunzisoft.keepass.database.file.load

import biz.source_code.base64Coder.Base64Coder
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.crypto.CipherFactory
import com.kunzisoft.keepass.crypto.StreamCipherFactory
import com.kunzisoft.keepass.crypto.engine.CipherEngine
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.security.ProtectedBinary
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.exception.*
import com.kunzisoft.keepass.database.file.KDBX4DateUtil
import com.kunzisoft.keepass.database.file.PwDbHeaderV4
import com.kunzisoft.keepass.stream.BetterCipherInputStream
import com.kunzisoft.keepass.stream.HashedBlockInputStream
import com.kunzisoft.keepass.stream.HmacBlockInputStream
import com.kunzisoft.keepass.stream.LEDataInputStream
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.MemoryUtil
import com.kunzisoft.keepass.utils.Types
import org.spongycastle.crypto.StreamCipher
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.*
import java.nio.charset.Charset
import java.text.ParseException
import java.util.*
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import kotlin.math.min

class ImporterV4(private val streamDir: File,
                 private val fixDuplicateUUID: Boolean = false) : Importer<PwDatabaseV4>() {

    private var randomStream: StreamCipher? = null
    private lateinit var mDatabase: PwDatabaseV4

    private var hashOfHeader: ByteArray? = null
    private var version: Long = 0

    private val unusedCacheFileName: String
        get() = mDatabase.binPool.findUnusedKey().toString()

    private var readNextNode = true
    private val ctxGroups = Stack<PwGroupV4>()
    private var ctxGroup: PwGroupV4? = null
    private var ctxEntry: PwEntryV4? = null
    private var ctxStringName: String? = null
    private var ctxStringValue: ProtectedString? = null
    private var ctxBinaryName: String? = null
    private var ctxBinaryValue: ProtectedBinary? = null
    private var ctxATName: String? = null
    private var ctxATSeq: String? = null
    private var entryInHistory = false
    private var ctxHistoryBase: PwEntryV4? = null
    private var ctxDeletedObject: PwDeletedObject? = null
    private var customIconID = PwDatabase.UUID_ZERO
    private var customIconData: ByteArray? = null
    private var customDataKey: String? = null
    private var customDataValue: String? = null
    private var groupCustomDataKey: String? = null
    private var groupCustomDataValue: String? = null
    private var entryCustomDataKey: String? = null
    private var entryCustomDataValue: String? = null

    @Throws(LoadDatabaseException::class)
    override fun openDatabase(databaseInputStream: InputStream,
                              password: String?,
                              keyInputStream: InputStream?,
                              progressTaskUpdater: ProgressTaskUpdater?): PwDatabaseV4 {

        try {
            // TODO performance
            progressTaskUpdater?.updateMessage(R.string.retrieving_db_key)

            mDatabase = PwDatabaseV4()

            mDatabase.changeDuplicateId = fixDuplicateUUID

            val header = PwDbHeaderV4(mDatabase)

            val headerAndHash = header.loadFromFile(databaseInputStream)
            version = header.version

            hashOfHeader = headerAndHash.hash
            val pbHeader = headerAndHash.header

            mDatabase.retrieveMasterKey(password, keyInputStream)
            mDatabase.makeFinalKey(header.masterSeed)
            // TODO performance

            progressTaskUpdater?.updateMessage(R.string.decrypting_db)
            val engine: CipherEngine
            val cipher: Cipher
            try {
                engine = CipherFactory.getInstance(mDatabase.dataCipher)
                mDatabase.setDataEngine(engine)
                mDatabase.encryptionAlgorithm = engine.getPwEncryptionAlgorithm()
                cipher = engine.getCipher(Cipher.DECRYPT_MODE, mDatabase.finalKey!!, header.encryptionIV)
            } catch (e: Exception) {
                throw LoadDatabaseInvalidAlgorithmException(e)
            }

            val isPlain: InputStream
            if (version < PwDbHeaderV4.FILE_VERSION_32_4) {

                val decrypted = attachCipherStream(databaseInputStream, cipher)
                val dataDecrypted = LEDataInputStream(decrypted)
                val storedStartBytes: ByteArray?
                try {
                    storedStartBytes = dataDecrypted.readBytes(32)
                    if (storedStartBytes == null || storedStartBytes.size != 32) {
                        throw LoadDatabaseInvalidCredentialsException()
                    }
                } catch (e: IOException) {
                    throw LoadDatabaseInvalidCredentialsException()
                }

                if (!Arrays.equals(storedStartBytes, header.streamStartBytes)) {
                    throw LoadDatabaseInvalidCredentialsException()
                }

                isPlain = HashedBlockInputStream(dataDecrypted)
            } else { // KDBX 4
                val isData = LEDataInputStream(databaseInputStream)
                val storedHash = isData.readBytes(32)
                if (!Arrays.equals(storedHash, hashOfHeader)) {
                    throw LoadDatabaseInvalidCredentialsException()
                }

                val hmacKey = mDatabase.hmacKey ?: throw LoadDatabaseException()
                val headerHmac = PwDbHeaderV4.computeHeaderHmac(pbHeader, hmacKey)
                val storedHmac = isData.readBytes(32)
                if (storedHmac == null || storedHmac.size != 32) {
                    throw LoadDatabaseInvalidCredentialsException()
                }
                // Mac doesn't match
                if (!Arrays.equals(headerHmac, storedHmac)) {
                    throw LoadDatabaseInvalidCredentialsException()
                }

                val hmIs = HmacBlockInputStream(isData, true, hmacKey)

                isPlain = attachCipherStream(hmIs, cipher)
            }

            val inputStreamXml: InputStream
            inputStreamXml = when (mDatabase.compressionAlgorithm) {
                PwCompressionAlgorithm.GZip -> GZIPInputStream(isPlain)
                else -> isPlain
            }

            if (version >= PwDbHeaderV4.FILE_VERSION_32_4) {
                loadInnerHeader(inputStreamXml, header)
            }

            randomStream = StreamCipherFactory.getInstance(header.innerRandomStream, header.innerRandomStreamKey)

            if (randomStream == null) {
                throw LoadDatabaseArcFourException()
            }

            readDocumentStreamed(createPullParser(inputStreamXml))

        } catch (e: LoadDatabaseException) {
            throw e
        } catch (e: XmlPullParserException) {
            throw LoadDatabaseIOException(e)
        } catch (e: IOException) {
            if (e.message?.contains("Hash failed with code") == true)
                throw LoadDatabaseKDFMemoryException(e)
            else
                throw LoadDatabaseIOException(e)
        } catch (e: OutOfMemoryError) {
            throw LoadDatabaseNoMemoryException(e)
        } catch (e: Exception) {
            throw LoadDatabaseException(e)
        }

        return mDatabase
    }

    private fun attachCipherStream(inputStream: InputStream, cipher: Cipher): InputStream {
        return BetterCipherInputStream(inputStream, cipher, 50 * 1024)
    }

    @Throws(IOException::class)
    private fun loadInnerHeader(inputStream: InputStream, header: PwDbHeaderV4) {
        val lis = LEDataInputStream(inputStream)

        while (true) {
            if (!readInnerHeader(lis, header)) break
        }
    }

    @Throws(IOException::class)
    private fun readInnerHeader(lis: LEDataInputStream, header: PwDbHeaderV4): Boolean {
        val fieldId = lis.read().toByte()

        val size = lis.readInt()
        if (size < 0) throw IOException("Corrupted file")

        var data = ByteArray(0)
        if (size > 0) {
            if (fieldId != PwDbHeaderV4.PwDbInnerHeaderV4Fields.Binary)
                data = lis.readBytes(size)
        }

        var result = true
        when (fieldId) {
            PwDbHeaderV4.PwDbInnerHeaderV4Fields.EndOfHeader -> result = false
            PwDbHeaderV4.PwDbInnerHeaderV4Fields.InnerRandomStreamID -> header.setRandomStreamID(data)
            PwDbHeaderV4.PwDbInnerHeaderV4Fields.InnerRandomstreamKey -> header.innerRandomStreamKey = data
            PwDbHeaderV4.PwDbInnerHeaderV4Fields.Binary -> {
                val flag = lis.readBytes(1)[0].toInt() != 0
                val protectedFlag = flag && PwDbHeaderV4.KdbxBinaryFlags.Protected.toInt() != PwDbHeaderV4.KdbxBinaryFlags.None.toInt()
                val byteLength = size - 1
                // Read in a file
                val file = File(streamDir, unusedCacheFileName)
                FileOutputStream(file).use { outputStream -> lis.readBytes(byteLength) { outputStream.write(it) } }
                val protectedBinary = ProtectedBinary(protectedFlag, file, byteLength)
                mDatabase.binPool.add(protectedBinary)
            }

            else -> {
            }
        }

        return result
    }

    private enum class KdbContext {
        Null,
        KeePassFile,
        Meta,
        Root,
        MemoryProtection,
        CustomIcons,
        CustomIcon,
        CustomData,
        CustomDataItem,
        RootDeletedObjects,
        DeletedObject,
        Group,
        GroupTimes,
        GroupCustomData,
        GroupCustomDataItem,
        Entry,
        EntryTimes,
        EntryString,
        EntryBinary,
        EntryAutoType,
        EntryAutoTypeItem,
        EntryHistory,
        EntryCustomData,
        EntryCustomDataItem,
        Binaries
    }

    @Throws(XmlPullParserException::class, IOException::class, LoadDatabaseException::class)
    private fun readDocumentStreamed(xpp: XmlPullParser) {

        ctxGroups.clear()

        var ctx = KdbContext.Null

        readNextNode = true

        while (true) {
            if (readNextNode) {
                if (xpp.next() == XmlPullParser.END_DOCUMENT) break
            } else {
                readNextNode = true
            }

            when (xpp.eventType) {
                XmlPullParser.START_TAG -> ctx = readXmlElement(ctx, xpp)

                XmlPullParser.END_TAG -> ctx = endXmlElement(ctx, xpp)

                else -> {
                }
            }
        }

        // Error checks
        if (ctx != KdbContext.Null) throw IOException("Malformed")
        if (ctxGroups.size != 0) throw IOException("Malformed")
    }

    @Throws(XmlPullParserException::class, IOException::class, LoadDatabaseException::class)
    private fun readXmlElement(ctx: KdbContext, xpp: XmlPullParser): KdbContext {
        val name = xpp.name
        when (ctx) {
            KdbContext.Null -> if (name.equals(PwDatabaseV4XML.ElemDocNode, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.KeePassFile, xpp)
            } else
                readUnknown(xpp)

            KdbContext.KeePassFile -> if (name.equals(PwDatabaseV4XML.ElemMeta, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.Meta, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemRoot, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.Root, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.Meta -> if (name.equals(PwDatabaseV4XML.ElemGenerator, ignoreCase = true)) {
                readString(xpp) // Ignore
            } else if (name.equals(PwDatabaseV4XML.ElemHeaderHash, ignoreCase = true)) {
                val encodedHash = readString(xpp)
                if (encodedHash.isNotEmpty() && hashOfHeader != null) {
                    val hash = Base64Coder.decode(encodedHash)
                    if (!Arrays.equals(hash, hashOfHeader)) {
                        throw LoadDatabaseException()
                    }
                }
            } else if (name.equals(PwDatabaseV4XML.ElemSettingsChanged, ignoreCase = true)) {
                mDatabase.settingsChanged = readPwTime(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemDbName, ignoreCase = true)) {
                mDatabase.name = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemDbNameChanged, ignoreCase = true)) {
                mDatabase.nameChanged = readPwTime(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemDbDesc, ignoreCase = true)) {
                mDatabase.description = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemDbDescChanged, ignoreCase = true)) {
                mDatabase.descriptionChanged = readPwTime(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemDbDefaultUser, ignoreCase = true)) {
                mDatabase.defaultUserName = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemDbDefaultUserChanged, ignoreCase = true)) {
                mDatabase.defaultUserNameChanged = readPwTime(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemDbColor, ignoreCase = true)) {
                mDatabase.color = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemDbMntncHistoryDays, ignoreCase = true)) {
                mDatabase.maintenanceHistoryDays = readUInt(xpp, DEFAULT_HISTORY_DAYS)
            } else if (name.equals(PwDatabaseV4XML.ElemDbKeyChanged, ignoreCase = true)) {
                mDatabase.keyLastChanged = readPwTime(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemDbKeyChangeRec, ignoreCase = true)) {
                mDatabase.keyChangeRecDays = readLong(xpp, -1)
            } else if (name.equals(PwDatabaseV4XML.ElemDbKeyChangeForce, ignoreCase = true)) {
                mDatabase.keyChangeForceDays = readLong(xpp, -1)
            } else if (name.equals(PwDatabaseV4XML.ElemDbKeyChangeForceOnce, ignoreCase = true)) {
                mDatabase.isKeyChangeForceOnce = readBool(xpp, false)
            } else if (name.equals(PwDatabaseV4XML.ElemMemoryProt, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.MemoryProtection, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemCustomIcons, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.CustomIcons, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemRecycleBinEnabled, ignoreCase = true)) {
                mDatabase.isRecycleBinEnabled = readBool(xpp, true)
            } else if (name.equals(PwDatabaseV4XML.ElemRecycleBinUuid, ignoreCase = true)) {
                mDatabase.recycleBinUUID = readUuid(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemRecycleBinChanged, ignoreCase = true)) {
                mDatabase.recycleBinChanged = readTime(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemEntryTemplatesGroup, ignoreCase = true)) {
                mDatabase.entryTemplatesGroup = readUuid(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemEntryTemplatesGroupChanged, ignoreCase = true)) {
                mDatabase.entryTemplatesGroupChanged = readPwTime(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemHistoryMaxItems, ignoreCase = true)) {
                mDatabase.historyMaxItems = readInt(xpp, -1)
            } else if (name.equals(PwDatabaseV4XML.ElemHistoryMaxSize, ignoreCase = true)) {
                mDatabase.historyMaxSize = readLong(xpp, -1)
            } else if (name.equals(PwDatabaseV4XML.ElemLastSelectedGroup, ignoreCase = true)) {
                mDatabase.lastSelectedGroupUUID = readUuid(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemLastTopVisibleGroup, ignoreCase = true)) {
                mDatabase.lastTopVisibleGroupUUID = readUuid(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemBinaries, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.Binaries, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemCustomData, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.CustomData, xpp)
            }

            KdbContext.MemoryProtection -> if (name.equals(PwDatabaseV4XML.ElemProtTitle, ignoreCase = true)) {
                mDatabase.memoryProtection.protectTitle = readBool(xpp, false)
            } else if (name.equals(PwDatabaseV4XML.ElemProtUserName, ignoreCase = true)) {
                mDatabase.memoryProtection.protectUserName = readBool(xpp, false)
            } else if (name.equals(PwDatabaseV4XML.ElemProtPassword, ignoreCase = true)) {
                mDatabase.memoryProtection.protectPassword = readBool(xpp, false)
            } else if (name.equals(PwDatabaseV4XML.ElemProtURL, ignoreCase = true)) {
                mDatabase.memoryProtection.protectUrl = readBool(xpp, false)
            } else if (name.equals(PwDatabaseV4XML.ElemProtNotes, ignoreCase = true)) {
                mDatabase.memoryProtection.protectNotes = readBool(xpp, false)
            } else if (name.equals(PwDatabaseV4XML.ElemProtAutoHide, ignoreCase = true)) {
                mDatabase.memoryProtection.autoEnableVisualHiding = readBool(xpp, false)
            } else {
                readUnknown(xpp)
            }

            KdbContext.CustomIcons -> if (name.equals(PwDatabaseV4XML.ElemCustomIconItem, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.CustomIcon, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.CustomIcon -> if (name.equals(PwDatabaseV4XML.ElemCustomIconItemID, ignoreCase = true)) {
                customIconID = readUuid(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemCustomIconItemData, ignoreCase = true)) {
                val strData = readString(xpp)
                if (strData.isNotEmpty()) {
                    customIconData = Base64Coder.decode(strData)
                } else {
                    assert(false)
                }
            } else {
                readUnknown(xpp)
            }

            KdbContext.Binaries -> if (name.equals(PwDatabaseV4XML.ElemBinary, ignoreCase = true)) {
                val key = xpp.getAttributeValue(null, PwDatabaseV4XML.AttrId)
                if (key != null) {
                    val pbData = readProtectedBinary(xpp)
                    val id = Integer.parseInt(key)
                    mDatabase.binPool.put(id, pbData!!)
                } else {
                    readUnknown(xpp)
                }
            } else {
                readUnknown(xpp)
            }

            KdbContext.CustomData -> if (name.equals(PwDatabaseV4XML.ElemStringDictExItem, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.CustomDataItem, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.CustomDataItem -> if (name.equals(PwDatabaseV4XML.ElemKey, ignoreCase = true)) {
                customDataKey = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemValue, ignoreCase = true)) {
                customDataValue = readString(xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.Root -> if (name.equals(PwDatabaseV4XML.ElemGroup, ignoreCase = true)) {
                if (ctxGroups.size != 0)
                    throw IOException("Group list should be empty.")

                mDatabase.rootGroup = mDatabase.createGroup()
                ctxGroups.push(mDatabase.rootGroup)
                ctxGroup = ctxGroups.peek()

                return switchContext(ctx, KdbContext.Group, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemDeletedObjects, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.RootDeletedObjects, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.Group -> if (name.equals(PwDatabaseV4XML.ElemUuid, ignoreCase = true)) {
                ctxGroup?.nodeId = PwNodeIdUUID(readUuid(xpp))
                ctxGroup?.let { mDatabase.addGroupIndex(it) }
            } else if (name.equals(PwDatabaseV4XML.ElemName, ignoreCase = true)) {
                ctxGroup?.title = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemNotes, ignoreCase = true)) {
                ctxGroup?.notes = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemIcon, ignoreCase = true)) {
                ctxGroup?.icon = mDatabase.iconFactory.getIcon(readUInt(xpp, 0).toInt())
            } else if (name.equals(PwDatabaseV4XML.ElemCustomIconID, ignoreCase = true)) {
                ctxGroup?.iconCustom = mDatabase.iconFactory.getIcon(readUuid(xpp))
            } else if (name.equals(PwDatabaseV4XML.ElemTimes, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.GroupTimes, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemIsExpanded, ignoreCase = true)) {
                ctxGroup?.isExpanded = readBool(xpp, true)
            } else if (name.equals(PwDatabaseV4XML.ElemGroupDefaultAutoTypeSeq, ignoreCase = true)) {
                ctxGroup?.defaultAutoTypeSequence = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemEnableAutoType, ignoreCase = true)) {
                ctxGroup?.enableAutoType = readOptionalBool(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemEnableSearching, ignoreCase = true)) {
                ctxGroup?.enableSearching = readOptionalBool(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemLastTopVisibleEntry, ignoreCase = true)) {
                ctxGroup?.lastTopVisibleEntry = readUuid(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemCustomData, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.GroupCustomData, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemGroup, ignoreCase = true)) {
                ctxGroup = mDatabase.createGroup()
                val groupPeek = ctxGroups.peek()
                ctxGroup?.let {
                    groupPeek.addChildGroup(it)
                    it.parent = groupPeek
                    ctxGroups.push(it)
                }

                return switchContext(ctx, KdbContext.Group, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemEntry, ignoreCase = true)) {
                ctxEntry = mDatabase.createEntry()
                ctxEntry?.let {
                    ctxGroup?.addChildEntry(it)
                    it.parent = ctxGroup
                }

                entryInHistory = false
                return switchContext(ctx, KdbContext.Entry, xpp)
            } else {
                readUnknown(xpp)
            }
            KdbContext.GroupCustomData -> if (name.equals(PwDatabaseV4XML.ElemStringDictExItem, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.GroupCustomDataItem, xpp)
            } else {
                readUnknown(xpp)
            }
            KdbContext.GroupCustomDataItem -> when {
                name.equals(PwDatabaseV4XML.ElemKey, ignoreCase = true) -> groupCustomDataKey = readString(xpp)
                name.equals(PwDatabaseV4XML.ElemValue, ignoreCase = true) -> groupCustomDataValue = readString(xpp)
                else -> readUnknown(xpp)
            }


            KdbContext.Entry -> if (name.equals(PwDatabaseV4XML.ElemUuid, ignoreCase = true)) {
                ctxEntry?.nodeId = PwNodeIdUUID(readUuid(xpp))
            } else if (name.equals(PwDatabaseV4XML.ElemIcon, ignoreCase = true)) {
                ctxEntry?.icon = mDatabase.iconFactory.getIcon(readUInt(xpp, 0).toInt())
            } else if (name.equals(PwDatabaseV4XML.ElemCustomIconID, ignoreCase = true)) {
                ctxEntry?.iconCustom = mDatabase.iconFactory.getIcon(readUuid(xpp))
            } else if (name.equals(PwDatabaseV4XML.ElemFgColor, ignoreCase = true)) {
                ctxEntry?.foregroundColor = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemBgColor, ignoreCase = true)) {
                ctxEntry?.backgroundColor = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemOverrideUrl, ignoreCase = true)) {
                ctxEntry?.overrideURL = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemTags, ignoreCase = true)) {
                ctxEntry?.tags = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemTimes, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryTimes, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemString, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryString, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemBinary, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryBinary, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemAutoType, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryAutoType, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemCustomData, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryCustomData, xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemHistory, ignoreCase = true)) {
                if (!entryInHistory) {
                    ctxHistoryBase = ctxEntry
                    return switchContext(ctx, KdbContext.EntryHistory, xpp)
                } else {
                    readUnknown(xpp)
                }
            } else {
                readUnknown(xpp)
            }
            KdbContext.EntryCustomData -> if (name.equals(PwDatabaseV4XML.ElemStringDictExItem, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryCustomDataItem, xpp)
            } else {
                readUnknown(xpp)
            }
            KdbContext.EntryCustomDataItem -> when {
                name.equals(PwDatabaseV4XML.ElemKey, ignoreCase = true) -> entryCustomDataKey = readString(xpp)
                name.equals(PwDatabaseV4XML.ElemValue, ignoreCase = true) -> entryCustomDataValue = readString(xpp)
                else -> readUnknown(xpp)
            }

            KdbContext.GroupTimes, KdbContext.EntryTimes -> {
                val tl: PwNodeV4Interface? =
                        if (ctx == KdbContext.GroupTimes) {
                            ctxGroup
                        } else {
                            ctxEntry
                        }

                when {
                    name.equals(PwDatabaseV4XML.ElemLastModTime, ignoreCase = true) -> tl?.lastModificationTime = readPwTime(xpp)
                    name.equals(PwDatabaseV4XML.ElemCreationTime, ignoreCase = true) -> tl?.creationTime = readPwTime(xpp)
                    name.equals(PwDatabaseV4XML.ElemLastAccessTime, ignoreCase = true) -> tl?.lastAccessTime = readPwTime(xpp)
                    name.equals(PwDatabaseV4XML.ElemExpiryTime, ignoreCase = true) -> tl?.expiryTime = readPwTime(xpp)
                    name.equals(PwDatabaseV4XML.ElemExpires, ignoreCase = true) -> tl?.expires = readBool(xpp, false)
                    name.equals(PwDatabaseV4XML.ElemUsageCount, ignoreCase = true) -> tl?.usageCount = readULong(xpp, 0)
                    name.equals(PwDatabaseV4XML.ElemLocationChanged, ignoreCase = true) -> tl?.locationChanged = readPwTime(xpp)
                    else -> readUnknown(xpp)
                }
            }

            KdbContext.EntryString -> if (name.equals(PwDatabaseV4XML.ElemKey, ignoreCase = true)) {
                ctxStringName = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemValue, ignoreCase = true)) {
                ctxStringValue = readProtectedString(xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.EntryBinary -> if (name.equals(PwDatabaseV4XML.ElemKey, ignoreCase = true)) {
                ctxBinaryName = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemValue, ignoreCase = true)) {
                ctxBinaryValue = readProtectedBinary(xpp)
            }

            KdbContext.EntryAutoType -> if (name.equals(PwDatabaseV4XML.ElemAutoTypeEnabled, ignoreCase = true)) {
                ctxEntry?.autoType?.enabled = readBool(xpp, true)
            } else if (name.equals(PwDatabaseV4XML.ElemAutoTypeObfuscation, ignoreCase = true)) {
                ctxEntry?.autoType?.obfuscationOptions = readUInt(xpp, 0)
            } else if (name.equals(PwDatabaseV4XML.ElemAutoTypeDefaultSeq, ignoreCase = true)) {
                ctxEntry?.autoType?.defaultSequence = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemAutoTypeItem, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryAutoTypeItem, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.EntryAutoTypeItem -> if (name.equals(PwDatabaseV4XML.ElemWindow, ignoreCase = true)) {
                ctxATName = readString(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemKeystrokeSequence, ignoreCase = true)) {
                ctxATSeq = readString(xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.EntryHistory -> if (name.equals(PwDatabaseV4XML.ElemEntry, ignoreCase = true)) {
                ctxEntry = PwEntryV4()
                ctxEntry?.let { ctxHistoryBase?.addEntryToHistory(it) }

                entryInHistory = true
                return switchContext(ctx, KdbContext.Entry, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.RootDeletedObjects -> if (name.equals(PwDatabaseV4XML.ElemDeletedObject, ignoreCase = true)) {
                ctxDeletedObject = PwDeletedObject()
                ctxDeletedObject?.let { mDatabase.addDeletedObject(it) }

                return switchContext(ctx, KdbContext.DeletedObject, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.DeletedObject -> if (name.equals(PwDatabaseV4XML.ElemUuid, ignoreCase = true)) {
                ctxDeletedObject?.uuid = readUuid(xpp)
            } else if (name.equals(PwDatabaseV4XML.ElemDeletionTime, ignoreCase = true)) {
                ctxDeletedObject?.deletionTime = readTime(xpp)
            } else {
                readUnknown(xpp)
            }
        }

        return ctx
    }

    @Throws(XmlPullParserException::class)
    private fun endXmlElement(ctx: KdbContext?, xpp: XmlPullParser): KdbContext {
        // (xpp.getEventType() == XmlPullParser.END_TAG);

        val name = xpp.name
        if (ctx == KdbContext.KeePassFile && name.equals(PwDatabaseV4XML.ElemDocNode, ignoreCase = true)) {
            return KdbContext.Null
        } else if (ctx == KdbContext.Meta && name.equals(PwDatabaseV4XML.ElemMeta, ignoreCase = true)) {
            return KdbContext.KeePassFile
        } else if (ctx == KdbContext.Root && name.equals(PwDatabaseV4XML.ElemRoot, ignoreCase = true)) {
            return KdbContext.KeePassFile
        } else if (ctx == KdbContext.MemoryProtection && name.equals(PwDatabaseV4XML.ElemMemoryProt, ignoreCase = true)) {
            return KdbContext.Meta
        } else if (ctx == KdbContext.CustomIcons && name.equals(PwDatabaseV4XML.ElemCustomIcons, ignoreCase = true)) {
            return KdbContext.Meta
        } else if (ctx == KdbContext.CustomIcon && name.equals(PwDatabaseV4XML.ElemCustomIconItem, ignoreCase = true)) {
            if (customIconID != PwDatabase.UUID_ZERO && customIconData != null) {
                val icon = PwIconCustom(customIconID, customIconData!!)
                mDatabase.addCustomIcon(icon)
                mDatabase.iconFactory.put(icon)
            }

            customIconID = PwDatabase.UUID_ZERO
            customIconData = null

            return KdbContext.CustomIcons
        } else if (ctx == KdbContext.Binaries && name.equals(PwDatabaseV4XML.ElemBinaries, ignoreCase = true)) {
            return KdbContext.Meta
        } else if (ctx == KdbContext.CustomData && name.equals(PwDatabaseV4XML.ElemCustomData, ignoreCase = true)) {
            return KdbContext.Meta
        } else if (ctx == KdbContext.CustomDataItem && name.equals(PwDatabaseV4XML.ElemStringDictExItem, ignoreCase = true)) {
            if (customDataKey != null && customDataValue != null) {
                mDatabase.putCustomData(customDataKey!!, customDataValue!!)
            }

            customDataKey = null
            customDataValue = null

            return KdbContext.CustomData
        } else if (ctx == KdbContext.Group && name.equals(PwDatabaseV4XML.ElemGroup, ignoreCase = true)) {
            if (ctxGroup != null && ctxGroup?.id == PwDatabase.UUID_ZERO) {
                ctxGroup?.nodeId = mDatabase.newGroupId()
                mDatabase.addGroupIndex(ctxGroup!!)
            }

            ctxGroups.pop()

            if (ctxGroups.size == 0) {
                ctxGroup = null
                return KdbContext.Root
            } else {
                ctxGroup = ctxGroups.peek()
                return KdbContext.Group
            }
        } else if (ctx == KdbContext.GroupTimes && name.equals(PwDatabaseV4XML.ElemTimes, ignoreCase = true)) {
            return KdbContext.Group
        } else if (ctx == KdbContext.GroupCustomData && name.equals(PwDatabaseV4XML.ElemCustomData, ignoreCase = true)) {
            return KdbContext.Group
        } else if (ctx == KdbContext.GroupCustomDataItem && name.equals(PwDatabaseV4XML.ElemStringDictExItem, ignoreCase = true)) {
            if (groupCustomDataKey != null && groupCustomDataValue != null) {
                ctxGroup?.putCustomData(groupCustomDataKey!!, groupCustomDataValue!!)
            }

            groupCustomDataKey = null
            groupCustomDataValue = null

            return KdbContext.GroupCustomData

        } else if (ctx == KdbContext.Entry && name.equals(PwDatabaseV4XML.ElemEntry, ignoreCase = true)) {

            if (ctxEntry?.id == PwDatabase.UUID_ZERO)
                ctxEntry?.nodeId = mDatabase.newEntryId()

            if (entryInHistory) {
                ctxEntry = ctxHistoryBase
                return KdbContext.EntryHistory
            }
            else if (ctxEntry != null) {
                // Add entry to the index only when close the XML element
                mDatabase.addEntryIndex(ctxEntry!!)
            }

            return KdbContext.Group
        } else if (ctx == KdbContext.EntryTimes && name.equals(PwDatabaseV4XML.ElemTimes, ignoreCase = true)) {
            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryString && name.equals(PwDatabaseV4XML.ElemString, ignoreCase = true)) {
            if (ctxStringName != null && ctxStringValue != null)
                ctxEntry?.addExtraField(ctxStringName!!, ctxStringValue!!)
            ctxStringName = null
            ctxStringValue = null

            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryBinary && name.equals(PwDatabaseV4XML.ElemBinary, ignoreCase = true)) {
            if (ctxBinaryName != null && ctxBinaryValue != null)
                ctxEntry?.putProtectedBinary(ctxBinaryName!!, ctxBinaryValue!!)
            ctxBinaryName = null
            ctxBinaryValue = null

            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryAutoType && name.equals(PwDatabaseV4XML.ElemAutoType, ignoreCase = true)) {
            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryAutoTypeItem && name.equals(PwDatabaseV4XML.ElemAutoTypeItem, ignoreCase = true)) {
            if (ctxATName != null && ctxATSeq != null)
                ctxEntry?.autoType?.put(ctxATName!!, ctxATSeq!!)
            ctxATName = null
            ctxATSeq = null

            return KdbContext.EntryAutoType
        } else if (ctx == KdbContext.EntryCustomData && name.equals(PwDatabaseV4XML.ElemCustomData, ignoreCase = true)) {
            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryCustomDataItem && name.equals(PwDatabaseV4XML.ElemStringDictExItem, ignoreCase = true)) {
            if (entryCustomDataKey != null && entryCustomDataValue != null) {
                ctxEntry?.putCustomData(entryCustomDataKey!!, entryCustomDataValue!!)
            }

            entryCustomDataKey = null
            entryCustomDataValue = null

            return KdbContext.EntryCustomData
        } else if (ctx == KdbContext.EntryHistory && name.equals(PwDatabaseV4XML.ElemHistory, ignoreCase = true)) {
            entryInHistory = false
            return KdbContext.Entry
        } else if (ctx == KdbContext.RootDeletedObjects && name.equals(PwDatabaseV4XML.ElemDeletedObjects, ignoreCase = true)) {
            return KdbContext.Root
        } else if (ctx == KdbContext.DeletedObject && name.equals(PwDatabaseV4XML.ElemDeletedObject, ignoreCase = true)) {
            ctxDeletedObject = null
            return KdbContext.RootDeletedObjects
        } else {
            var contextName = ""
            if (ctx != null) {
                contextName = ctx.name
            }
            throw RuntimeException("Invalid end element: Context " + contextName + "End element: " + name)
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readPwTime(xpp: XmlPullParser): PwDate {
        return PwDate(readTime(xpp))
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTime(xpp: XmlPullParser): Date {
        val sDate = readString(xpp)
        var utcDate: Date? = null

        if (version >= PwDbHeaderV4.FILE_VERSION_32_4) {
            var buf = Base64Coder.decode(sDate)
            if (buf.size != 8) {
                val buf8 = ByteArray(8)
                System.arraycopy(buf, 0, buf8, 0, min(buf.size, 8))
                buf = buf8
            }

            val seconds = LEDataInputStream.readLong(buf, 0)
            utcDate = KDBX4DateUtil.convertKDBX4Time(seconds)

        } else {

            try {
                utcDate = PwDatabaseV4XML.dateFormatter.get()?.parse(sDate)
            } catch (e: ParseException) {
                // Catch with null test below
            }
        }

        return utcDate ?: Date(0L)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readUnknown(xpp: XmlPullParser) {
        if (xpp.isEmptyElementTag) return

        processNode(xpp)
        while (xpp.next() != XmlPullParser.END_DOCUMENT) {
            if (xpp.eventType == XmlPullParser.END_TAG) break
            if (xpp.eventType == XmlPullParser.START_TAG) continue

            readUnknown(xpp)
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readBool(xpp: XmlPullParser, bDefault: Boolean): Boolean {
        val str = readString(xpp)

        return when {
            str.equals(PwDatabaseV4XML.ValTrue, ignoreCase = true) -> true
            str.equals(PwDatabaseV4XML.ValFalse, ignoreCase = true) -> false
            else -> bDefault
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readOptionalBool(xpp: XmlPullParser, bDefault: Boolean? = null): Boolean? {
        val str = readString(xpp)

        return when {
            str.equals(PwDatabaseV4XML.ValTrue, ignoreCase = true) -> true
            str.equals(PwDatabaseV4XML.ValFalse, ignoreCase = true) -> false
            str.equals(PwDatabaseV4XML.ValNull, ignoreCase = true) -> null
            else -> bDefault
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readPwNodeIdUuid(xpp: XmlPullParser): PwNodeIdUUID {
        return PwNodeIdUUID(readUuid(xpp))
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readUuid(xpp: XmlPullParser): UUID {
        val encoded = readString(xpp)

        if (encoded.isEmpty()) {
            return PwDatabase.UUID_ZERO
        }

        // TODO: Switch to framework Base64 once API level 8 is the minimum
        val buf = Base64Coder.decode(encoded)

        return Types.bytestoUUID(buf)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readInt(xpp: XmlPullParser, def: Int): Int {
        val str = readString(xpp)

        return try {
            Integer.parseInt(str)
        } catch (e: NumberFormatException) {
            def
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readUInt(xpp: XmlPullParser, uDefault: Long): Long {
        val u: Long = readULong(xpp, uDefault)

        if (u < 0 || u > MAX_UINT) {
            throw NumberFormatException("Outside of the uint size")
        }

        return u

    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLong(xpp: XmlPullParser, def: Long): Long {
        val str = readString(xpp)

        return try {
            java.lang.Long.parseLong(str)
        } catch (e: NumberFormatException) {
            def
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readULong(xpp: XmlPullParser, uDefault: Long): Long {
        var u = readLong(xpp, uDefault)

        if (u < 0) {
            u = uDefault
        }

        return u
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readProtectedString(xpp: XmlPullParser): ProtectedString {
        val buf = processNode(xpp)

        if (buf != null) {
            try {
                return ProtectedString(true, String(buf, Charset.forName("UTF-8")))
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                throw IOException(e.localizedMessage)
            }

        }

        return ProtectedString(false, readString(xpp))
    }

    @Throws(IOException::class)
    private fun createProtectedBinaryFromData(protection: Boolean, data: ByteArray): ProtectedBinary {
        return if (data.size > MemoryUtil.BUFFER_SIZE_BYTES) {
            val file = File(streamDir, unusedCacheFileName)
            FileOutputStream(file).use { outputStream -> outputStream.write(data) }
            ProtectedBinary(protection, file, data.size)
        } else {
            ProtectedBinary(protection, data)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readProtectedBinary(xpp: XmlPullParser): ProtectedBinary? {
        val ref = xpp.getAttributeValue(null, PwDatabaseV4XML.AttrRef)
        if (ref != null) {
            xpp.next() // Consume end tag

            val id = Integer.parseInt(ref)
            return mDatabase.binPool[id]
        }

        var compressed = false
        val comp = xpp.getAttributeValue(null, PwDatabaseV4XML.AttrCompressed)
        if (comp != null) {
            compressed = comp.equals(PwDatabaseV4XML.ValTrue, ignoreCase = true)
        }

        val buf = processNode(xpp)

        if (buf != null) {
            createProtectedBinaryFromData(true, buf)
        }

        val base64 = readString(xpp)
        if (base64.isEmpty())
            return ProtectedBinary()

        var data = Base64Coder.decode(base64)

        if (compressed) {
            data = MemoryUtil.decompress(data)
        }

        return createProtectedBinaryFromData(false, data)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readString(xpp: XmlPullParser): String {
        val buf = processNode(xpp)

        if (buf != null) {
            try {
                return String(buf, Charset.forName("UTF-8"))
            } catch (e: UnsupportedEncodingException) {
                throw IOException(e)
            }

        }

        return xpp.safeNextText()

    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readBase64String(xpp: XmlPullParser): ByteArray {

        //readNextNode = false;
        Base64Coder.decode(xpp.safeNextText())?.let { buffer ->
            val plainText = ByteArray(buffer.size)
            randomStream?.processBytes(buffer, 0, buffer.size, plainText, 0)
            return plainText
        }
        return ByteArray(0)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun processNode(xpp: XmlPullParser): ByteArray? {
        //(xpp.getEventType() == XmlPullParser.START_TAG);

        if (xpp.attributeCount > 0) {
            val protect = xpp.getAttributeValue(null, PwDatabaseV4XML.AttrProtected)
            if (protect != null && protect.equals(PwDatabaseV4XML.ValTrue, ignoreCase = true)) {
                return readBase64String(xpp)
            }
        }

        return null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun switchContext(ctxCurrent: KdbContext, ctxNew: KdbContext,
                              xpp: XmlPullParser): KdbContext {

        if (xpp.isEmptyElementTag) {
            xpp.next()  // Consume the end tag
            return ctxCurrent
        }
        return ctxNew
    }

    companion object {

        private const val DEFAULT_HISTORY_DAYS: Long = 365

        @Throws(XmlPullParserException::class)
        private fun createPullParser(readerStream: InputStream): XmlPullParser {
            val xmlPullParserFactory = XmlPullParserFactory.newInstance()
            xmlPullParserFactory.isNamespaceAware = false

            val xpp = xmlPullParserFactory.newPullParser()
            xpp.setInput(readerStream, null)

            return xpp
        }

        private const val MAX_UINT = 4294967296L // 2^32
    }
}

@Throws(IOException::class, XmlPullParserException::class)
fun XmlPullParser.safeNextText(): String {
    val result = nextText()
    if (eventType != XmlPullParser.END_TAG) {
        nextTag()
    }
    return result
}
