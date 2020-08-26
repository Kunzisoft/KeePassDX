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
package com.kunzisoft.keepass.database.file.input

import android.util.Base64
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.crypto.CipherFactory
import com.kunzisoft.keepass.crypto.StreamCipherFactory
import com.kunzisoft.keepass.crypto.engine.CipherEngine
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.DeletedObject
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX.Companion.BASE_64_FLAG
import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.NodeKDBXInterface
import com.kunzisoft.keepass.database.element.database.BinaryAttachment
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.exception.*
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX
import com.kunzisoft.keepass.database.file.DatabaseKDBXXML
import com.kunzisoft.keepass.database.file.DateKDBXUtil
import com.kunzisoft.keepass.stream.*
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.UnsignedInt
import com.kunzisoft.keepass.utils.UnsignedLong
import org.bouncycastle.crypto.StreamCipher
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.text.ParseException
import java.util.*
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import kotlin.math.min

class DatabaseInputKDBX(cacheDirectory: File,
                        private val fixDuplicateUUID: Boolean = false)
    : DatabaseInput<DatabaseKDBX>(cacheDirectory) {

    private var randomStream: StreamCipher? = null
    private lateinit var mDatabase: DatabaseKDBX

    private var hashOfHeader: ByteArray? = null

    private var readNextNode = true
    private val ctxGroups = Stack<GroupKDBX>()
    private var ctxGroup: GroupKDBX? = null
    private var ctxEntry: EntryKDBX? = null
    private var ctxStringName: String? = null
    private var ctxStringValue: ProtectedString? = null
    private var ctxBinaryName: String? = null
    private var ctxBinaryValue: BinaryAttachment? = null
    private var ctxATName: String? = null
    private var ctxATSeq: String? = null
    private var entryInHistory = false
    private var ctxHistoryBase: EntryKDBX? = null
    private var ctxDeletedObject: DeletedObject? = null
    private var customIconID = DatabaseVersioned.UUID_ZERO
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
                              progressTaskUpdater: ProgressTaskUpdater?): DatabaseKDBX {

        try {
            // TODO performance
            progressTaskUpdater?.updateMessage(R.string.retrieving_db_key)

            mDatabase = DatabaseKDBX()

            mDatabase.changeDuplicateId = fixDuplicateUUID

            val header = DatabaseHeaderKDBX(mDatabase)

            val headerAndHash = header.loadFromFile(databaseInputStream)
            mDatabase.kdbxVersion = header.version

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
                throw InvalidAlgorithmDatabaseException(e)
            }

            val isPlain: InputStream
            if (mDatabase.kdbxVersion.toKotlinLong() < DatabaseHeaderKDBX.FILE_VERSION_32_4.toKotlinLong()) {

                val decrypted = attachCipherStream(databaseInputStream, cipher)
                val dataDecrypted = LittleEndianDataInputStream(decrypted)
                val storedStartBytes: ByteArray?
                try {
                    storedStartBytes = dataDecrypted.readBytes(32)
                    if (storedStartBytes.size != 32) {
                        throw InvalidCredentialsDatabaseException()
                    }
                } catch (e: IOException) {
                    throw InvalidCredentialsDatabaseException()
                }

                if (!Arrays.equals(storedStartBytes, header.streamStartBytes)) {
                    throw InvalidCredentialsDatabaseException()
                }

                isPlain = HashedBlockInputStream(dataDecrypted)
            } else { // KDBX 4
                val isData = LittleEndianDataInputStream(databaseInputStream)
                val storedHash = isData.readBytes(32)
                if (!Arrays.equals(storedHash, hashOfHeader)) {
                    throw InvalidCredentialsDatabaseException()
                }

                val hmacKey = mDatabase.hmacKey ?: throw LoadDatabaseException()
                val headerHmac = DatabaseHeaderKDBX.computeHeaderHmac(pbHeader, hmacKey)
                val storedHmac = isData.readBytes(32)
                if (storedHmac.size != 32) {
                    throw InvalidCredentialsDatabaseException()
                }
                // Mac doesn't match
                if (!Arrays.equals(headerHmac, storedHmac)) {
                    throw InvalidCredentialsDatabaseException()
                }

                val hmIs = HmacBlockInputStream(isData, true, hmacKey)

                isPlain = attachCipherStream(hmIs, cipher)
            }

            val inputStreamXml: InputStream
            inputStreamXml = when (mDatabase.compressionAlgorithm) {
                CompressionAlgorithm.GZip -> GZIPInputStream(isPlain)
                else -> isPlain
            }

            if (mDatabase.kdbxVersion.toKotlinLong() >= DatabaseHeaderKDBX.FILE_VERSION_32_4.toKotlinLong()) {
                loadInnerHeader(inputStreamXml, header)
            }

            randomStream = StreamCipherFactory.getInstance(header.innerRandomStream, header.innerRandomStreamKey)

            if (randomStream == null) {
                throw ArcFourDatabaseException()
            }

            readDocumentStreamed(createPullParser(inputStreamXml))

        } catch (e: LoadDatabaseException) {
            throw e
        } catch (e: XmlPullParserException) {
            throw IODatabaseException(e)
        } catch (e: IOException) {
            if (e.message?.contains("Hash failed with code") == true)
                throw KDFMemoryDatabaseException(e)
            else
                throw IODatabaseException(e)
        } catch (e: OutOfMemoryError) {
            throw NoMemoryDatabaseException(e)
        } catch (e: Exception) {
            throw LoadDatabaseException(e)
        }

        return mDatabase
    }

    private fun attachCipherStream(inputStream: InputStream, cipher: Cipher): InputStream {
        return CipherInputStream(inputStream, cipher)
    }

    @Throws(IOException::class)
    private fun loadInnerHeader(inputStream: InputStream, header: DatabaseHeaderKDBX) {
        val lis = LittleEndianDataInputStream(inputStream)

        while (true) {
            if (!readInnerHeader(lis, header)) break
        }
    }

    @Throws(IOException::class)
    private fun readInnerHeader(dataInputStream: LittleEndianDataInputStream,
                                header: DatabaseHeaderKDBX): Boolean {
        val fieldId = dataInputStream.read().toByte()

        val size = dataInputStream.readUInt().toKotlinInt()
        if (size < 0) throw IOException("Corrupted file")

        var data = ByteArray(0)
        if (size > 0) {
            if (fieldId != DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.Binary)
                data = dataInputStream.readBytes(size)
        }

        var result = true
        when (fieldId) {
            DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.EndOfHeader -> {
                result = false
            }
            DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.InnerRandomStreamID -> {
                header.setRandomStreamID(data)
            }
            DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.InnerRandomstreamKey -> {
                header.innerRandomStreamKey = data
            }
            DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.Binary -> {
                val byteLength = size - 1
                // Read in a file
                val protectedFlag = dataInputStream.readBytes(1)[0].toInt() != 0
                // Unknown compression at this level
                val compression = mDatabase.compressionAlgorithm == CompressionAlgorithm.GZip
                val protectedBinary = mDatabase.buildNewBinary(cacheDirectory, protectedFlag, compression)
                protectedBinary.getOutputDataStream().use { outputStream ->
                    dataInputStream.readBytes(byteLength, DatabaseKDBX.BUFFER_SIZE_BYTES) { buffer ->
                        outputStream.write(buffer)
                    }
                }
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
            KdbContext.Null -> if (name.equals(DatabaseKDBXXML.ElemDocNode, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.KeePassFile, xpp)
            } else
                readUnknown(xpp)

            KdbContext.KeePassFile -> if (name.equals(DatabaseKDBXXML.ElemMeta, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.Meta, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemRoot, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.Root, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.Meta -> if (name.equals(DatabaseKDBXXML.ElemGenerator, ignoreCase = true)) {
                readString(xpp) // Ignore
            } else if (name.equals(DatabaseKDBXXML.ElemHeaderHash, ignoreCase = true)) {
                val encodedHash = readString(xpp)
                if (encodedHash.isNotEmpty() && hashOfHeader != null) {
                    val hash = Base64.decode(encodedHash, BASE_64_FLAG)
                    if (!Arrays.equals(hash, hashOfHeader)) {
                        throw LoadDatabaseException()
                    }
                }
            } else if (name.equals(DatabaseKDBXXML.ElemSettingsChanged, ignoreCase = true)) {
                mDatabase.settingsChanged = readPwTime(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbName, ignoreCase = true)) {
                mDatabase.name = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbNameChanged, ignoreCase = true)) {
                mDatabase.nameChanged = readPwTime(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbDesc, ignoreCase = true)) {
                mDatabase.description = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbDescChanged, ignoreCase = true)) {
                mDatabase.descriptionChanged = readPwTime(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbDefaultUser, ignoreCase = true)) {
                mDatabase.defaultUserName = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbDefaultUserChanged, ignoreCase = true)) {
                mDatabase.defaultUserNameChanged = readPwTime(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbColor, ignoreCase = true)) {
                mDatabase.color = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbMntncHistoryDays, ignoreCase = true)) {
                mDatabase.maintenanceHistoryDays = readUInt(xpp, DEFAULT_HISTORY_DAYS)
            } else if (name.equals(DatabaseKDBXXML.ElemDbKeyChanged, ignoreCase = true)) {
                mDatabase.keyLastChanged = readPwTime(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbKeyChangeRec, ignoreCase = true)) {
                mDatabase.keyChangeRecDays = readLong(xpp, -1)
            } else if (name.equals(DatabaseKDBXXML.ElemDbKeyChangeForce, ignoreCase = true)) {
                mDatabase.keyChangeForceDays = readLong(xpp, -1)
            } else if (name.equals(DatabaseKDBXXML.ElemDbKeyChangeForceOnce, ignoreCase = true)) {
                mDatabase.isKeyChangeForceOnce = readBool(xpp, false)
            } else if (name.equals(DatabaseKDBXXML.ElemMemoryProt, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.MemoryProtection, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemCustomIcons, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.CustomIcons, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemRecycleBinEnabled, ignoreCase = true)) {
                mDatabase.isRecycleBinEnabled = readBool(xpp, true)
            } else if (name.equals(DatabaseKDBXXML.ElemRecycleBinUuid, ignoreCase = true)) {
                mDatabase.recycleBinUUID = readUuid(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemRecycleBinChanged, ignoreCase = true)) {
                mDatabase.recycleBinChanged = readTime(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemEntryTemplatesGroup, ignoreCase = true)) {
                mDatabase.entryTemplatesGroup = readUuid(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemEntryTemplatesGroupChanged, ignoreCase = true)) {
                mDatabase.entryTemplatesGroupChanged = readPwTime(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemHistoryMaxItems, ignoreCase = true)) {
                mDatabase.historyMaxItems = readInt(xpp, -1)
            } else if (name.equals(DatabaseKDBXXML.ElemHistoryMaxSize, ignoreCase = true)) {
                mDatabase.historyMaxSize = readLong(xpp, -1)
            } else if (name.equals(DatabaseKDBXXML.ElemLastSelectedGroup, ignoreCase = true)) {
                mDatabase.lastSelectedGroupUUID = readUuid(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemLastTopVisibleGroup, ignoreCase = true)) {
                mDatabase.lastTopVisibleGroupUUID = readUuid(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemBinaries, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.Binaries, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemCustomData, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.CustomData, xpp)
            }

            KdbContext.MemoryProtection -> if (name.equals(DatabaseKDBXXML.ElemProtTitle, ignoreCase = true)) {
                mDatabase.memoryProtection.protectTitle = readBool(xpp, false)
            } else if (name.equals(DatabaseKDBXXML.ElemProtUserName, ignoreCase = true)) {
                mDatabase.memoryProtection.protectUserName = readBool(xpp, false)
            } else if (name.equals(DatabaseKDBXXML.ElemProtPassword, ignoreCase = true)) {
                mDatabase.memoryProtection.protectPassword = readBool(xpp, false)
            } else if (name.equals(DatabaseKDBXXML.ElemProtURL, ignoreCase = true)) {
                mDatabase.memoryProtection.protectUrl = readBool(xpp, false)
            } else if (name.equals(DatabaseKDBXXML.ElemProtNotes, ignoreCase = true)) {
                mDatabase.memoryProtection.protectNotes = readBool(xpp, false)
            } else if (name.equals(DatabaseKDBXXML.ElemProtAutoHide, ignoreCase = true)) {
                mDatabase.memoryProtection.autoEnableVisualHiding = readBool(xpp, false)
            } else {
                readUnknown(xpp)
            }

            KdbContext.CustomIcons -> if (name.equals(DatabaseKDBXXML.ElemCustomIconItem, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.CustomIcon, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.CustomIcon -> if (name.equals(DatabaseKDBXXML.ElemCustomIconItemID, ignoreCase = true)) {
                customIconID = readUuid(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemCustomIconItemData, ignoreCase = true)) {
                val strData = readString(xpp)
                if (strData.isNotEmpty()) {
                    customIconData = Base64.decode(strData, BASE_64_FLAG)
                } else {
                    assert(false)
                }
            } else {
                readUnknown(xpp)
            }

            KdbContext.Binaries -> if (name.equals(DatabaseKDBXXML.ElemBinary, ignoreCase = true)) {
                readBinary(xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.CustomData -> if (name.equals(DatabaseKDBXXML.ElemStringDictExItem, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.CustomDataItem, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.CustomDataItem -> if (name.equals(DatabaseKDBXXML.ElemKey, ignoreCase = true)) {
                customDataKey = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemValue, ignoreCase = true)) {
                customDataValue = readString(xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.Root -> if (name.equals(DatabaseKDBXXML.ElemGroup, ignoreCase = true)) {
                if (ctxGroups.size != 0)
                    throw IOException("Group list should be empty.")

                mDatabase.rootGroup = mDatabase.createGroup()
                ctxGroups.push(mDatabase.rootGroup)
                ctxGroup = ctxGroups.peek()

                return switchContext(ctx, KdbContext.Group, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDeletedObjects, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.RootDeletedObjects, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.Group -> if (name.equals(DatabaseKDBXXML.ElemUuid, ignoreCase = true)) {
                ctxGroup?.nodeId = NodeIdUUID(readUuid(xpp))
                ctxGroup?.let { mDatabase.addGroupIndex(it) }
            } else if (name.equals(DatabaseKDBXXML.ElemName, ignoreCase = true)) {
                ctxGroup?.title = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemNotes, ignoreCase = true)) {
                ctxGroup?.notes = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemIcon, ignoreCase = true)) {
                ctxGroup?.icon = mDatabase.iconFactory.getIcon(readUInt(xpp, UnsignedInt(0)).toKotlinInt())
            } else if (name.equals(DatabaseKDBXXML.ElemCustomIconID, ignoreCase = true)) {
                ctxGroup?.iconCustom = mDatabase.iconFactory.getIcon(readUuid(xpp))
            } else if (name.equals(DatabaseKDBXXML.ElemTimes, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.GroupTimes, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemIsExpanded, ignoreCase = true)) {
                ctxGroup?.isExpanded = readBool(xpp, true)
            } else if (name.equals(DatabaseKDBXXML.ElemGroupDefaultAutoTypeSeq, ignoreCase = true)) {
                ctxGroup?.defaultAutoTypeSequence = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemEnableAutoType, ignoreCase = true)) {
                ctxGroup?.enableAutoType = readOptionalBool(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemEnableSearching, ignoreCase = true)) {
                ctxGroup?.enableSearching = readOptionalBool(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemLastTopVisibleEntry, ignoreCase = true)) {
                ctxGroup?.lastTopVisibleEntry = readUuid(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemCustomData, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.GroupCustomData, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemGroup, ignoreCase = true)) {
                ctxGroup = mDatabase.createGroup()
                val groupPeek = ctxGroups.peek()
                ctxGroup?.let {
                    groupPeek.addChildGroup(it)
                    it.parent = groupPeek
                    ctxGroups.push(it)
                }

                return switchContext(ctx, KdbContext.Group, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemEntry, ignoreCase = true)) {
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
            KdbContext.GroupCustomData -> if (name.equals(DatabaseKDBXXML.ElemStringDictExItem, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.GroupCustomDataItem, xpp)
            } else {
                readUnknown(xpp)
            }
            KdbContext.GroupCustomDataItem -> when {
                name.equals(DatabaseKDBXXML.ElemKey, ignoreCase = true) -> groupCustomDataKey = readString(xpp)
                name.equals(DatabaseKDBXXML.ElemValue, ignoreCase = true) -> groupCustomDataValue = readString(xpp)
                else -> readUnknown(xpp)
            }


            KdbContext.Entry -> if (name.equals(DatabaseKDBXXML.ElemUuid, ignoreCase = true)) {
                ctxEntry?.nodeId = NodeIdUUID(readUuid(xpp))
            } else if (name.equals(DatabaseKDBXXML.ElemIcon, ignoreCase = true)) {
                ctxEntry?.icon = mDatabase.iconFactory.getIcon(readUInt(xpp, UnsignedInt(0)).toKotlinInt())
            } else if (name.equals(DatabaseKDBXXML.ElemCustomIconID, ignoreCase = true)) {
                ctxEntry?.iconCustom = mDatabase.iconFactory.getIcon(readUuid(xpp))
            } else if (name.equals(DatabaseKDBXXML.ElemFgColor, ignoreCase = true)) {
                ctxEntry?.foregroundColor = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemBgColor, ignoreCase = true)) {
                ctxEntry?.backgroundColor = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemOverrideUrl, ignoreCase = true)) {
                ctxEntry?.overrideURL = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemTags, ignoreCase = true)) {
                ctxEntry?.tags = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemTimes, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryTimes, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemString, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryString, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemBinary, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryBinary, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemAutoType, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryAutoType, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemCustomData, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryCustomData, xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemHistory, ignoreCase = true)) {
                if (!entryInHistory) {
                    ctxHistoryBase = ctxEntry
                    return switchContext(ctx, KdbContext.EntryHistory, xpp)
                } else {
                    readUnknown(xpp)
                }
            } else {
                readUnknown(xpp)
            }
            KdbContext.EntryCustomData -> if (name.equals(DatabaseKDBXXML.ElemStringDictExItem, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryCustomDataItem, xpp)
            } else {
                readUnknown(xpp)
            }
            KdbContext.EntryCustomDataItem -> when {
                name.equals(DatabaseKDBXXML.ElemKey, ignoreCase = true) -> entryCustomDataKey = readString(xpp)
                name.equals(DatabaseKDBXXML.ElemValue, ignoreCase = true) -> entryCustomDataValue = readString(xpp)
                else -> readUnknown(xpp)
            }

            KdbContext.GroupTimes, KdbContext.EntryTimes -> {
                val tl: NodeKDBXInterface? =
                        if (ctx == KdbContext.GroupTimes) {
                            ctxGroup
                        } else {
                            ctxEntry
                        }

                when {
                    name.equals(DatabaseKDBXXML.ElemLastModTime, ignoreCase = true) -> tl?.lastModificationTime = readPwTime(xpp)
                    name.equals(DatabaseKDBXXML.ElemCreationTime, ignoreCase = true) -> tl?.creationTime = readPwTime(xpp)
                    name.equals(DatabaseKDBXXML.ElemLastAccessTime, ignoreCase = true) -> tl?.lastAccessTime = readPwTime(xpp)
                    name.equals(DatabaseKDBXXML.ElemExpiryTime, ignoreCase = true) -> tl?.expiryTime = readPwTime(xpp)
                    name.equals(DatabaseKDBXXML.ElemExpires, ignoreCase = true) -> tl?.expires = readBool(xpp, false)
                    name.equals(DatabaseKDBXXML.ElemUsageCount, ignoreCase = true) -> tl?.usageCount = readULong(xpp, UnsignedLong(0))
                    name.equals(DatabaseKDBXXML.ElemLocationChanged, ignoreCase = true) -> tl?.locationChanged = readPwTime(xpp)
                    else -> readUnknown(xpp)
                }
            }

            KdbContext.EntryString -> if (name.equals(DatabaseKDBXXML.ElemKey, ignoreCase = true)) {
                ctxStringName = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemValue, ignoreCase = true)) {
                ctxStringValue = readProtectedString(xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.EntryBinary -> if (name.equals(DatabaseKDBXXML.ElemKey, ignoreCase = true)) {
                ctxBinaryName = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemValue, ignoreCase = true)) {
                ctxBinaryValue = readBinary(xpp)
            }

            KdbContext.EntryAutoType -> if (name.equals(DatabaseKDBXXML.ElemAutoTypeEnabled, ignoreCase = true)) {
                ctxEntry?.autoType?.enabled = readBool(xpp, true)
            } else if (name.equals(DatabaseKDBXXML.ElemAutoTypeObfuscation, ignoreCase = true)) {
                ctxEntry?.autoType?.obfuscationOptions = readUInt(xpp, UnsignedInt(0))
            } else if (name.equals(DatabaseKDBXXML.ElemAutoTypeDefaultSeq, ignoreCase = true)) {
                ctxEntry?.autoType?.defaultSequence = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemAutoTypeItem, ignoreCase = true)) {
                return switchContext(ctx, KdbContext.EntryAutoTypeItem, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.EntryAutoTypeItem -> if (name.equals(DatabaseKDBXXML.ElemWindow, ignoreCase = true)) {
                ctxATName = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemKeystrokeSequence, ignoreCase = true)) {
                ctxATSeq = readString(xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.EntryHistory -> if (name.equals(DatabaseKDBXXML.ElemEntry, ignoreCase = true)) {
                ctxEntry = EntryKDBX()
                ctxEntry?.let { ctxHistoryBase?.addEntryToHistory(it) }

                entryInHistory = true
                return switchContext(ctx, KdbContext.Entry, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.RootDeletedObjects -> if (name.equals(DatabaseKDBXXML.ElemDeletedObject, ignoreCase = true)) {
                ctxDeletedObject = DeletedObject()
                ctxDeletedObject?.let { mDatabase.addDeletedObject(it) }

                return switchContext(ctx, KdbContext.DeletedObject, xpp)
            } else {
                readUnknown(xpp)
            }

            KdbContext.DeletedObject -> if (name.equals(DatabaseKDBXXML.ElemUuid, ignoreCase = true)) {
                ctxDeletedObject?.uuid = readUuid(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDeletionTime, ignoreCase = true)) {
                ctxDeletedObject?.setDeletionTime(readTime(xpp))
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
        if (ctx == KdbContext.KeePassFile && name.equals(DatabaseKDBXXML.ElemDocNode, ignoreCase = true)) {
            return KdbContext.Null
        } else if (ctx == KdbContext.Meta && name.equals(DatabaseKDBXXML.ElemMeta, ignoreCase = true)) {
            return KdbContext.KeePassFile
        } else if (ctx == KdbContext.Root && name.equals(DatabaseKDBXXML.ElemRoot, ignoreCase = true)) {
            return KdbContext.KeePassFile
        } else if (ctx == KdbContext.MemoryProtection && name.equals(DatabaseKDBXXML.ElemMemoryProt, ignoreCase = true)) {
            return KdbContext.Meta
        } else if (ctx == KdbContext.CustomIcons && name.equals(DatabaseKDBXXML.ElemCustomIcons, ignoreCase = true)) {
            return KdbContext.Meta
        } else if (ctx == KdbContext.CustomIcon && name.equals(DatabaseKDBXXML.ElemCustomIconItem, ignoreCase = true)) {
            if (customIconID != DatabaseVersioned.UUID_ZERO && customIconData != null) {
                val icon = IconImageCustom(customIconID, customIconData!!)
                mDatabase.addCustomIcon(icon)
                mDatabase.iconFactory.put(icon)
            }

            customIconID = DatabaseVersioned.UUID_ZERO
            customIconData = null

            return KdbContext.CustomIcons
        } else if (ctx == KdbContext.Binaries && name.equals(DatabaseKDBXXML.ElemBinaries, ignoreCase = true)) {
            return KdbContext.Meta
        } else if (ctx == KdbContext.CustomData && name.equals(DatabaseKDBXXML.ElemCustomData, ignoreCase = true)) {
            return KdbContext.Meta
        } else if (ctx == KdbContext.CustomDataItem && name.equals(DatabaseKDBXXML.ElemStringDictExItem, ignoreCase = true)) {
            if (customDataKey != null && customDataValue != null) {
                mDatabase.putCustomData(customDataKey!!, customDataValue!!)
            }

            customDataKey = null
            customDataValue = null

            return KdbContext.CustomData
        } else if (ctx == KdbContext.Group && name.equals(DatabaseKDBXXML.ElemGroup, ignoreCase = true)) {
            if (ctxGroup != null && ctxGroup?.id == DatabaseVersioned.UUID_ZERO) {
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
        } else if (ctx == KdbContext.GroupTimes && name.equals(DatabaseKDBXXML.ElemTimes, ignoreCase = true)) {
            return KdbContext.Group
        } else if (ctx == KdbContext.GroupCustomData && name.equals(DatabaseKDBXXML.ElemCustomData, ignoreCase = true)) {
            return KdbContext.Group
        } else if (ctx == KdbContext.GroupCustomDataItem && name.equals(DatabaseKDBXXML.ElemStringDictExItem, ignoreCase = true)) {
            if (groupCustomDataKey != null && groupCustomDataValue != null) {
                ctxGroup?.putCustomData(groupCustomDataKey!!, groupCustomDataValue!!)
            }

            groupCustomDataKey = null
            groupCustomDataValue = null

            return KdbContext.GroupCustomData

        } else if (ctx == KdbContext.Entry && name.equals(DatabaseKDBXXML.ElemEntry, ignoreCase = true)) {

            if (ctxEntry?.id == DatabaseVersioned.UUID_ZERO)
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
        } else if (ctx == KdbContext.EntryTimes && name.equals(DatabaseKDBXXML.ElemTimes, ignoreCase = true)) {
            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryString && name.equals(DatabaseKDBXXML.ElemString, ignoreCase = true)) {
            if (ctxStringName != null && ctxStringValue != null)
                ctxEntry?.putExtraField(ctxStringName!!, ctxStringValue!!)
            ctxStringName = null
            ctxStringValue = null

            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryBinary && name.equals(DatabaseKDBXXML.ElemBinary, ignoreCase = true)) {
            if (ctxBinaryName != null && ctxBinaryValue != null) {
                ctxEntry?.putAttachment(Attachment(ctxBinaryName!!, ctxBinaryValue!!), mDatabase.binaryPool)
            }
            ctxBinaryName = null
            ctxBinaryValue = null

            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryAutoType && name.equals(DatabaseKDBXXML.ElemAutoType, ignoreCase = true)) {
            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryAutoTypeItem && name.equals(DatabaseKDBXXML.ElemAutoTypeItem, ignoreCase = true)) {
            if (ctxATName != null && ctxATSeq != null)
                ctxEntry?.autoType?.put(ctxATName!!, ctxATSeq!!)
            ctxATName = null
            ctxATSeq = null

            return KdbContext.EntryAutoType
        } else if (ctx == KdbContext.EntryCustomData && name.equals(DatabaseKDBXXML.ElemCustomData, ignoreCase = true)) {
            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryCustomDataItem && name.equals(DatabaseKDBXXML.ElemStringDictExItem, ignoreCase = true)) {
            if (entryCustomDataKey != null && entryCustomDataValue != null) {
                ctxEntry?.putCustomData(entryCustomDataKey!!, entryCustomDataValue!!)
            }

            entryCustomDataKey = null
            entryCustomDataValue = null

            return KdbContext.EntryCustomData
        } else if (ctx == KdbContext.EntryHistory && name.equals(DatabaseKDBXXML.ElemHistory, ignoreCase = true)) {
            entryInHistory = false
            return KdbContext.Entry
        } else if (ctx == KdbContext.RootDeletedObjects && name.equals(DatabaseKDBXXML.ElemDeletedObjects, ignoreCase = true)) {
            return KdbContext.Root
        } else if (ctx == KdbContext.DeletedObject && name.equals(DatabaseKDBXXML.ElemDeletedObject, ignoreCase = true)) {
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
    private fun readPwTime(xpp: XmlPullParser): DateInstant {
        return DateInstant(readTime(xpp))
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTime(xpp: XmlPullParser): Date {
        val sDate = readString(xpp)
        var utcDate: Date? = null

        if (mDatabase.kdbxVersion.toKotlinLong() >= DatabaseHeaderKDBX.FILE_VERSION_32_4.toKotlinLong()) {
            var buf = Base64.decode(sDate, BASE_64_FLAG)
            if (buf.size != 8) {
                val buf8 = ByteArray(8)
                System.arraycopy(buf, 0, buf8, 0, min(buf.size, 8))
                buf = buf8
            }

            val seconds = bytes64ToLong(buf)
            utcDate = DateKDBXUtil.convertKDBX4Time(seconds)

        } else {

            try {
                utcDate = DatabaseKDBXXML.DateFormatter.parse(sDate)
            } catch (e: ParseException) {
                // Catch with null test below
            }
        }

        return utcDate ?: Date(0L)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readUnknown(xpp: XmlPullParser) {
        if (xpp.isEmptyElementTag) return

        readProtectedBase64String(xpp)
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
            str.equals(DatabaseKDBXXML.ValTrue, ignoreCase = true) -> true
            str.equals(DatabaseKDBXXML.ValFalse, ignoreCase = true) -> false
            else -> bDefault
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readOptionalBool(xpp: XmlPullParser, bDefault: Boolean? = null): Boolean? {
        val str = readString(xpp)

        return when {
            str.equals(DatabaseKDBXXML.ValTrue, ignoreCase = true) -> true
            str.equals(DatabaseKDBXXML.ValFalse, ignoreCase = true) -> false
            str.equals(DatabaseKDBXXML.ValNull, ignoreCase = true) -> null
            else -> bDefault
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readUuid(xpp: XmlPullParser): UUID {
        val encoded = readString(xpp)

        if (encoded.isEmpty()) {
            return DatabaseVersioned.UUID_ZERO
        }
        val buf = Base64.decode(encoded, BASE_64_FLAG)

        return bytes16ToUuid(buf)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readInt(xpp: XmlPullParser, default: Int): Int {
        return try {
            readString(xpp).toInt()
        } catch (e: Exception) {
            default
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readUInt(xpp: XmlPullParser, default: UnsignedInt): UnsignedInt {
        return try {
            UnsignedInt(readString(xpp).toInt())
        } catch (e: Exception) {
            default
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLong(xpp: XmlPullParser, default: Long): Long {
        return try {
            readString(xpp).toLong()
        } catch (e: Exception) {
            default
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readULong(xpp: XmlPullParser, default: UnsignedLong): UnsignedLong {
        return try {
            UnsignedLong(readString(xpp).toLong())
        } catch (e: Exception) {
            default
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readProtectedString(xpp: XmlPullParser): ProtectedString {
        val buf = readProtectedBase64String(xpp)

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

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readBinary(xpp: XmlPullParser): BinaryAttachment? {

        // Reference Id to a binary already present in binary pool
        val ref = xpp.getAttributeValue(null, DatabaseKDBXXML.AttrRef)
        if (ref != null) {
            xpp.next() // Consume end tag

            val id = Integer.parseInt(ref)
            return mDatabase.binaryPool[id]
        }

        val key = xpp.getAttributeValue(null, DatabaseKDBXXML.AttrId)
        return if (key != null) {
            createBinary(key.toIntOrNull(), xpp)
        }

        // New binary to retrieve
        else {
            createBinary(null, xpp)
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun createBinary(binaryId: Int?, xpp: XmlPullParser): BinaryAttachment? {
        var compressed: Boolean = mDatabase.compressionAlgorithm == CompressionAlgorithm.GZip
        var protected = false

        if (xpp.attributeCount > 0) {
            val compress = xpp.getAttributeValue(null, DatabaseKDBXXML.AttrCompressed)
            if (compress != null) {
                compressed = compress.equals(DatabaseKDBXXML.ValTrue, ignoreCase = true)
            }

            val protect = xpp.getAttributeValue(null, DatabaseKDBXXML.AttrProtected)
            if (protect != null) {
                protected = protect.equals(DatabaseKDBXXML.ValTrue, ignoreCase = true)
            }
        }

        val base64 = readString(xpp)
        if (base64.isEmpty())
            return null
        val data = Base64.decode(base64, BASE_64_FLAG)

        // Force compression in this specific case
        val binaryAttachment = mDatabase.buildNewBinary(cacheDirectory, protected, compressed, binaryId)
        binaryAttachment.getOutputDataStream().use { outputStream ->
                outputStream.write(data)
        }
        return binaryAttachment
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readString(xpp: XmlPullParser): String {
        val buf = readProtectedBase64String(xpp)

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
        Base64.decode(xpp.safeNextText(), BASE_64_FLAG)?.let { data ->
            val plainText = ByteArray(data.size)
            randomStream?.processBytes(data, 0, data.size, plainText, 0)
            return plainText
        }
        return ByteArray(0)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readProtectedBase64String(xpp: XmlPullParser): ByteArray? {
        //(xpp.getEventType() == XmlPullParser.START_TAG);

        if (xpp.attributeCount > 0) {
            val protect = xpp.getAttributeValue(null, DatabaseKDBXXML.AttrProtected)
            if (protect != null && protect.equals(DatabaseKDBXXML.ValTrue, ignoreCase = true)) {
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

        private val DEFAULT_HISTORY_DAYS = UnsignedInt(365)

        @Throws(XmlPullParserException::class)
        private fun createPullParser(readerStream: InputStream): XmlPullParser {
            val xmlPullParserFactory = XmlPullParserFactory.newInstance()
            xmlPullParserFactory.isNamespaceAware = false

            val xpp = xmlPullParserFactory.newPullParser()
            xpp.setInput(readerStream, null)

            return xpp
        }
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
