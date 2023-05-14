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
import android.util.Log
import com.kunzisoft.encrypt.StreamCipher
import com.kunzisoft.keepass.database.crypto.CipherEngine
import com.kunzisoft.keepass.database.crypto.CrsAlgorithm
import com.kunzisoft.keepass.database.crypto.HmacBlock
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.binary.BinaryData.Companion.BASE64_FLAG
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.database.DatabaseVersioned
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.node.NodeKDBXInterface
import com.kunzisoft.keepass.database.element.security.MemoryProtectionConfig
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.exception.*
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX
import com.kunzisoft.keepass.database.file.DatabaseHeaderKDBX.Companion.FILE_VERSION_40
import com.kunzisoft.keepass.database.file.DatabaseKDBXXML
import com.kunzisoft.keepass.database.file.DateKDBXUtil
import com.kunzisoft.keepass.stream.HashedBlockInputStream
import com.kunzisoft.keepass.stream.HmacBlockInputStream
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater
import com.kunzisoft.keepass.utils.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.text.ParseException
import java.util.*
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.Mac
import kotlin.math.min

class DatabaseInputKDBX(database: DatabaseKDBX)
    : DatabaseInput<DatabaseKDBX>(database) {

    private var randomStream: StreamCipher? = null

    private var hashOfHeader: ByteArray? = null

    private var readNextNode = true
    private val ctxGroups = Stack<GroupKDBX>()
    private var ctxGroup: GroupKDBX? = null
    private var ctxEntry: EntryKDBX? = null
    private var ctxStringName: String? = null
    private var ctxStringValue: ProtectedString? = null
    private var ctxBinaryName: String? = null
    private var ctxBinaryValue: BinaryData? = null
    private var ctxATName: String? = null
    private var ctxATSeq: String? = null
    private var entryInHistory = false
    private var ctxHistoryBase: EntryKDBX? = null
    private var ctxDeletedObject: DeletedObject? = null
    private var customIconID = DatabaseVersioned.UUID_ZERO
    private var customIconName: String = ""
    private var customIconLastModificationTime: DateInstant? = null
    private var customIconData: ByteArray? = null
    private var customDataKey: String? = null
    private var customDataValue: String? = null
    private var customDataLastModificationTime: DateInstant? = null
    private var groupCustomDataKey: String? = null
    private var groupCustomDataValue: String? = null
    private var entryCustomDataKey: String? = null
    private var entryCustomDataValue: String? = null

    private var isRAMSufficient: (memoryWanted: Long) -> Boolean = {true}

    fun setMethodToCheckIfRAMIsSufficient(method: (memoryWanted: Long) -> Boolean) {
        this.isRAMSufficient = method
    }

    @Throws(DatabaseInputException::class)
    override fun openDatabase(databaseInputStream: InputStream,
                              progressTaskUpdater: ProgressTaskUpdater?,
                              assignMasterKey: (() -> Unit)): DatabaseKDBX {
        try {
            startKeyTimer(progressTaskUpdater)

            val header = DatabaseHeaderKDBX(mDatabase)

            val headerAndHash = header.loadFromFile(databaseInputStream)
            mDatabase.kdbxVersion = header.version

            hashOfHeader = headerAndHash.hash
            val pbHeader = headerAndHash.header

            val transformSeed = header.transformSeed
            mDatabase.transformSeed = transformSeed
            assignMasterKey.invoke()
            mDatabase.makeFinalKey(header.masterSeed)

            stopKeyTimer()
            startContentTimer(progressTaskUpdater)

            val cipher: Cipher
            try {
                val engine: CipherEngine = mDatabase.encryptionAlgorithm.cipherEngine
                engine.forcePaddingCompatibility = true
                cipher = engine.getCipher(Cipher.DECRYPT_MODE, mDatabase.finalKey!!, header.encryptionIV)
                engine.forcePaddingCompatibility = false
            } catch (e: Exception) {
                throw InvalidAlgorithmDatabaseException(e)
            }

            val plainInputStream: InputStream
            if (mDatabase.kdbxVersion.isBefore(FILE_VERSION_40)) {

                val dataDecrypted = CipherInputStream(databaseInputStream, cipher)
                val storedStartBytes: ByteArray?
                try {
                    storedStartBytes = dataDecrypted.readBytesLength(32)
                    if (storedStartBytes.size != 32) {
                        throw InvalidCredentialsDatabaseException()
                    }
                } catch (e: IOException) {
                    throw InvalidCredentialsDatabaseException()
                }

                if (!Arrays.equals(storedStartBytes, header.streamStartBytes)) {
                    throw InvalidCredentialsDatabaseException()
                }

                plainInputStream = HashedBlockInputStream(dataDecrypted)
            } else { // KDBX 4
                val storedHash = databaseInputStream.readBytesLength(32)
                if (!storedHash.contentEquals(hashOfHeader)) {
                    throw InvalidCredentialsDatabaseException()
                }

                val hmacKey = mDatabase.hmacKey ?: throw DatabaseInputException()

                val blockKey = HmacBlock.getHmacKey64(hmacKey, UnsignedLong.MAX_BYTES)
                val hmac: Mac = HmacBlock.getHmacSha256(blockKey)
                val headerHmac = hmac.doFinal(pbHeader)

                val storedHmac = databaseInputStream.readBytesLength(32)
                if (storedHmac.size != 32) {
                    throw InvalidCredentialsDatabaseException()
                }
                // Mac doesn't match
                if (!headerHmac.contentEquals(storedHmac)) {
                    throw InvalidCredentialsDatabaseException()
                }

                val hmIs = HmacBlockInputStream(databaseInputStream, true, hmacKey)

                plainInputStream = CipherInputStream(hmIs, cipher)
            }

            val inputStreamXml: InputStream = when (mDatabase.compressionAlgorithm) {
                CompressionAlgorithm.GZIP -> GZIPInputStream(plainInputStream)
                else -> plainInputStream
            }

            if (!mDatabase.kdbxVersion.isBefore(FILE_VERSION_40)) {
                readInnerHeader(inputStreamXml, header)
            }

            try {
                randomStream = CrsAlgorithm.getCipher(header.innerRandomStream, header.innerRandomStreamKey)
            } catch (e: Exception) {
                throw DatabaseInputException(e)
            }

            val xmlPullParserFactory = XmlPullParserFactory.newInstance().apply {
                isNamespaceAware = false
            }
            val xmlPullParser = xmlPullParserFactory.newPullParser().apply {
                setInput(inputStreamXml, null)
            }
            readDocumentStreamed(xmlPullParser)

            stopContentTimer()

        } catch (e: Error) {
            if (e is OutOfMemoryError)
                throw NoMemoryDatabaseException(e)
            if (e.message?.contains("Hash failed with code") == true)
                throw KDFMemoryDatabaseException(e)
            throw DatabaseInputException(e)
        }

        return mDatabase
    }

    @Throws(IOException::class)
    private fun readInnerHeader(dataInputStream: InputStream,
                                header: DatabaseHeaderKDBX) {

        var readStream = true
        while (readStream) {
            val fieldId = dataInputStream.read().toByte()

            val size = dataInputStream.readBytes4ToUInt().toKotlinInt()
            if (size < 0) throw CorruptedDatabaseException()

            var data = ByteArray(0)
            try {
                if (size > 0) {
                    if (fieldId != DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.Binary) {
                        data = dataInputStream.readBytesLength(size)
                    }
                }
            } catch (e: Exception) {
                // OOM only if corrupted file
                throw CorruptedDatabaseException()
            }

            readStream = true
            when (fieldId) {
                DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.EndOfHeader -> {
                    readStream = false
                }
                DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.InnerRandomStreamID -> {
                    header.setRandomStreamID(data)
                }
                DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.InnerRandomstreamKey -> {
                    header.innerRandomStreamKey = data
                }
                DatabaseHeaderKDBX.PwDbInnerHeaderV4Fields.Binary -> {
                    // Read in a file
                    val protectedFlag = dataInputStream.read().toByte() == DatabaseHeaderKDBX.KdbxBinaryFlags.Protected
                    val byteLength = size - 1
                    // No compression at this level
                    val protectedBinary = mDatabase.buildNewBinaryAttachment(
                            isRAMSufficient.invoke(byteLength.toLong()), false, protectedFlag)
                    protectedBinary.getOutputDataStream(mDatabase.binaryCache).use { outputStream ->
                        dataInputStream.readBytes(byteLength) { buffer ->
                            outputStream.write(buffer)
                        }
                    }
                }
            }
        }
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

    @Throws(XmlPullParserException::class, IOException::class, DatabaseInputException::class)
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
        if (ctx != KdbContext.Null) throw XMLMalformedDatabaseException()
        if (ctxGroups.size != 0) throw XMLMalformedDatabaseException()
    }

    @Throws(XmlPullParserException::class, IOException::class, DatabaseInputException::class)
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
                    val hash = Base64.decode(encodedHash, BASE64_FLAG)
                    if (!Arrays.equals(hash, hashOfHeader)) {
                        throw DatabaseInputException()
                    }
                }
            } else if (name.equals(DatabaseKDBXXML.ElemSettingsChanged, ignoreCase = true)) {
                mDatabase.settingsChanged = readDateInstant(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbName, ignoreCase = true)) {
                mDatabase.name = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbNameChanged, ignoreCase = true)) {
                mDatabase.nameChanged = readDateInstant(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbDesc, ignoreCase = true)) {
                mDatabase.description = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbDescChanged, ignoreCase = true)) {
                mDatabase.descriptionChanged = readDateInstant(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbDefaultUser, ignoreCase = true)) {
                mDatabase.defaultUserName = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbDefaultUserChanged, ignoreCase = true)) {
                mDatabase.defaultUserNameChanged = readDateInstant(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbColor, ignoreCase = true)) {
                mDatabase.color = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemDbMntncHistoryDays, ignoreCase = true)) {
                mDatabase.maintenanceHistoryDays = readUInt(xpp, DEFAULT_HISTORY_DAYS)
            } else if (name.equals(DatabaseKDBXXML.ElemDbKeyChanged, ignoreCase = true)) {
                mDatabase.keyLastChanged = readDateInstant(xpp)
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
                mDatabase.recycleBinChanged = readDateInstant(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemEntryTemplatesGroup, ignoreCase = true)) {
                mDatabase.entryTemplatesGroup = readUuid(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemEntryTemplatesGroupChanged, ignoreCase = true)) {
                mDatabase.entryTemplatesGroupChanged = readDateInstant(xpp)
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
                mDatabase.memoryProtection.protectTitle = readBool(xpp, MemoryProtectionConfig.DEFAULT_PROTECT_TITLE)
            } else if (name.equals(DatabaseKDBXXML.ElemProtUserName, ignoreCase = true)) {
                mDatabase.memoryProtection.protectUserName = readBool(xpp, MemoryProtectionConfig.DEFAULT_PROTECT_USERNAME)
            } else if (name.equals(DatabaseKDBXXML.ElemProtPassword, ignoreCase = true)) {
                mDatabase.memoryProtection.protectPassword = readBool(xpp, MemoryProtectionConfig.DEFAULT_PROTECT_PASSWORD)
            } else if (name.equals(DatabaseKDBXXML.ElemProtURL, ignoreCase = true)) {
                mDatabase.memoryProtection.protectUrl = readBool(xpp, MemoryProtectionConfig.DEFAULT_PROTECT_URL)
            } else if (name.equals(DatabaseKDBXXML.ElemProtNotes, ignoreCase = true)) {
                mDatabase.memoryProtection.protectNotes = readBool(xpp, MemoryProtectionConfig.DEFAULT_PROTECT_NOTES)
            } else if (name.equals(DatabaseKDBXXML.ElemProtAutoHide, ignoreCase = true)) {
                mDatabase.memoryProtection.autoEnableVisualHiding = readBool(xpp, MemoryProtectionConfig.DEFAULT_AUTO_ENABLE_VISUAL_HIDING)
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
                    customIconData = Base64.decode(strData, BASE64_FLAG)
                }
            } else if (name.equals(DatabaseKDBXXML.ElemName, ignoreCase = true)) {
                customIconName = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemLastModTime, ignoreCase = true)) {
                customIconLastModificationTime = readDateInstant(xpp)
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
            } else if (name.equals(DatabaseKDBXXML.ElemLastModTime, ignoreCase = true)) {
                customDataLastModificationTime = readDateInstant(xpp)
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
                ctxGroup?.icon?.standard = mDatabase.getStandardIcon(readUInt(xpp, UnsignedInt(0)).toKotlinInt())
            } else if (name.equals(DatabaseKDBXXML.ElemCustomIconID, ignoreCase = true)) {
                val iconUUID = readUuid(xpp)
                ctxGroup?.icon?.custom = mDatabase.getCustomIcon(iconUUID) ?: IconImageCustom(iconUUID)
            } else if (name.equals(DatabaseKDBXXML.ElemTags, ignoreCase = true)) {
                ctxGroup?.tags = readTags(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemPreviousParentGroup, ignoreCase = true)) {
                ctxGroup?.previousParentGroup = readUuid(xpp)
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
                name.equals(DatabaseKDBXXML.ElemLastModTime, ignoreCase = true) -> readDateInstant(xpp) // Ignore
                else -> readUnknown(xpp)
            }


            KdbContext.Entry -> if (name.equals(DatabaseKDBXXML.ElemUuid, ignoreCase = true)) {
                ctxEntry?.nodeId = NodeIdUUID(readUuid(xpp))
            } else if (name.equals(DatabaseKDBXXML.ElemIcon, ignoreCase = true)) {
                ctxEntry?.icon?.standard = mDatabase.getStandardIcon(readUInt(xpp, UnsignedInt(0)).toKotlinInt())
            } else if (name.equals(DatabaseKDBXXML.ElemCustomIconID, ignoreCase = true)) {
                val iconUUID = readUuid(xpp)
                ctxEntry?.icon?.custom = mDatabase.getCustomIcon(iconUUID) ?: IconImageCustom(iconUUID)
            } else if (name.equals(DatabaseKDBXXML.ElemFgColor, ignoreCase = true)) {
                ctxEntry?.foregroundColor = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemBgColor, ignoreCase = true)) {
                ctxEntry?.backgroundColor = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemOverrideUrl, ignoreCase = true)) {
                ctxEntry?.overrideURL = readString(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemQualityCheck, ignoreCase = true)) {
                ctxEntry?.qualityCheck = readBool(xpp, true)
            } else if (name.equals(DatabaseKDBXXML.ElemTags, ignoreCase = true)) {
                ctxEntry?.tags = readTags(xpp)
            } else if (name.equals(DatabaseKDBXXML.ElemPreviousParentGroup, ignoreCase = true)) {
                ctxEntry?.previousParentGroup = readUuid(xpp)
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
                name.equals(DatabaseKDBXXML.ElemLastModTime, ignoreCase = true) -> readDateInstant(xpp) // Ignore
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
                    name.equals(DatabaseKDBXXML.ElemLastModTime, ignoreCase = true) -> tl?.lastModificationTime = readDateInstant(xpp)
                    name.equals(DatabaseKDBXXML.ElemCreationTime, ignoreCase = true) -> tl?.creationTime = readDateInstant(xpp)
                    name.equals(DatabaseKDBXXML.ElemLastAccessTime, ignoreCase = true) -> tl?.lastAccessTime = readDateInstant(xpp)
                    name.equals(DatabaseKDBXXML.ElemExpiryTime, ignoreCase = true) -> tl?.expiryTime = readDateInstant(xpp)
                    name.equals(DatabaseKDBXXML.ElemExpires, ignoreCase = true) -> tl?.expires = readBool(xpp, false)
                    name.equals(DatabaseKDBXXML.ElemUsageCount, ignoreCase = true) -> tl?.usageCount = readULong(xpp, UnsignedLong(0))
                    name.equals(DatabaseKDBXXML.ElemLocationChanged, ignoreCase = true) -> tl?.locationChanged = readDateInstant(xpp)
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
                ctxDeletedObject?.deletionTime = readDateInstant(xpp)
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
            val iconData = customIconData
            if (customIconID != DatabaseVersioned.UUID_ZERO && iconData != null) {
                mDatabase.addCustomIcon(customIconID,
                        customIconName,
                        customIconLastModificationTime,
                        isRAMSufficient.invoke(iconData.size.toLong())) { _, binary ->
                    binary?.getOutputDataStream(mDatabase.binaryCache)?.use { outputStream ->
                        outputStream.write(iconData)
                    }
                }
            }
            customIconID = DatabaseVersioned.UUID_ZERO
            customIconName = ""
            customIconLastModificationTime = null
            customIconData = null
            return KdbContext.CustomIcons
        } else if (ctx == KdbContext.Binaries && name.equals(DatabaseKDBXXML.ElemBinaries, ignoreCase = true)) {
            return KdbContext.Meta
        } else if (ctx == KdbContext.CustomData && name.equals(DatabaseKDBXXML.ElemCustomData, ignoreCase = true)) {
            return KdbContext.Meta
        } else if (ctx == KdbContext.CustomDataItem && name.equals(DatabaseKDBXXML.ElemStringDictExItem, ignoreCase = true)) {
            customDataKey?.let { dataKey ->
                customDataValue?.let { dataValue ->
                    mDatabase.customData.put(CustomDataItem(dataKey,
                            dataValue, customDataLastModificationTime))
                }
            }
            customDataKey = null
            customDataValue = null
            customDataLastModificationTime = null
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
            groupCustomDataKey?.let { customDataKey ->
                groupCustomDataValue?.let { customDataValue ->
                    ctxGroup?.customData?.put(CustomDataItem(customDataKey, customDataValue))
                }
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
            } else if (ctxEntry != null) {
                // Add entry to the index only when close the XML element
                mDatabase.addEntryIndex(ctxEntry!!)
            }

            return KdbContext.Group
        } else if (ctx == KdbContext.EntryTimes && name.equals(DatabaseKDBXXML.ElemTimes, ignoreCase = true)) {
            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryString && name.equals(DatabaseKDBXXML.ElemString, ignoreCase = true)) {
            if (ctxStringName != null && ctxStringValue != null)
                ctxEntry?.putField(ctxStringName!!, ctxStringValue!!)
            ctxStringName = null
            ctxStringValue = null

            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryBinary && name.equals(DatabaseKDBXXML.ElemBinary, ignoreCase = true)) {
            if (ctxBinaryName != null && ctxBinaryValue != null) {
                ctxEntry?.putAttachment(Attachment(ctxBinaryName!!, ctxBinaryValue!!), mDatabase.attachmentPool)
            }
            ctxBinaryName = null
            ctxBinaryValue = null

            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryAutoType && name.equals(DatabaseKDBXXML.ElemAutoType, ignoreCase = true)) {
            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryAutoTypeItem && name.equals(DatabaseKDBXXML.ElemAutoTypeItem, ignoreCase = true)) {
            if (ctxATName != null && ctxATSeq != null)
                ctxEntry?.autoType?.add(ctxATName!!, ctxATSeq!!)
            ctxATName = null
            ctxATSeq = null

            return KdbContext.EntryAutoType
        } else if (ctx == KdbContext.EntryCustomData && name.equals(DatabaseKDBXXML.ElemCustomData, ignoreCase = true)) {
            return KdbContext.Entry
        } else if (ctx == KdbContext.EntryCustomDataItem && name.equals(DatabaseKDBXXML.ElemStringDictExItem, ignoreCase = true)) {
            entryCustomDataKey?.let { customDataKey ->
                entryCustomDataValue?.let { customDataValue ->
                    ctxEntry?.customData?.put(CustomDataItem(customDataKey, customDataValue))
                }
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
            throw XMLMalformedDatabaseException("Invalid end element: Context " + contextName + "End element: " + name)
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readDateInstant(xpp: XmlPullParser): DateInstant {
        val sDate = readString(xpp)
        var utcDate: Date? = null

        if (mDatabase.kdbxVersion.isBefore(FILE_VERSION_40)) {
            try {
                utcDate = DatabaseKDBXXML.DateFormatter.parse(sDate)
            } catch (e: ParseException) {
                // Catch with null test below
            }
        } else {
            var buf = Base64.decode(sDate, BASE64_FLAG)
            if (buf.size != 8) {
                val buf8 = ByteArray(8)
                System.arraycopy(buf, 0, buf8, 0, min(buf.size, 8))
                buf = buf8
            }

            val seconds = bytes64ToLong(buf)
            utcDate = DateKDBXUtil.convertKDBX4Time(seconds)
        }

        return DateInstant(utcDate ?: Date(0L))
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTags(xpp: XmlPullParser): Tags {
        val tags = Tags(readString(xpp))
        mDatabase.tagPool.put(tags)
        return tags
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

        return try {
            val buf = Base64.decode(encoded, BASE64_FLAG)
            bytes16ToUuid(buf)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to read base 64 UUID, create a random one", e)
            UUID.randomUUID()
        }
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
    private fun readBinary(xpp: XmlPullParser): BinaryData? {

        // Reference Id to a binary already present in binary pool
        val ref = xpp.getAttributeValue(null, DatabaseKDBXXML.AttrRef)
        // New id to a binary
        val key = xpp.getAttributeValue(null, DatabaseKDBXXML.AttrId)

        return when {
            ref != null -> {
                xpp.next() // Consume end tag
                val id = Integer.parseInt(ref)
                // A ref is not necessarily an index in Database V3.1
                var binaryRetrieve = mDatabase.attachmentPool[id]
                // Create empty binary if not retrieved in pool
                if (binaryRetrieve == null) {
                    binaryRetrieve = mDatabase.buildNewBinaryAttachment(
                            smallSize = false,
                            compression = false,
                            protection = false,
                            binaryPoolId = id)
                }
                return binaryRetrieve
            }
            key != null -> {
                createBinary(key.toIntOrNull(), xpp)
            }
            else -> {
                // New binary to retrieve
                createBinary(null, xpp)
            }
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun createBinary(binaryId: Int?, xpp: XmlPullParser): BinaryData? {
        var compressed = false
        var protected = true

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

        // Build the new binary and compress
        val binaryAttachment = mDatabase.buildNewBinaryAttachment(
                isRAMSufficient.invoke(base64.length.toLong()), compressed, protected, binaryId)
        try {
            binaryAttachment.getOutputDataStream(mDatabase.binaryCache).use { outputStream ->
                outputStream.write(Base64.decode(base64, BASE64_FLAG))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to read base 64 attachment", e)
            binaryAttachment.isCorrupted = true
            binaryAttachment.getOutputDataStream(mDatabase.binaryCache).use { outputStream ->
                outputStream.write(base64.toByteArray())
            }
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
    private fun readProtectedBase64String(xpp: XmlPullParser): ByteArray? {
        if (xpp.attributeCount > 0) {
            val protect = xpp.getAttributeValue(null, DatabaseKDBXXML.AttrProtected)
            if (protect != null && protect.equals(DatabaseKDBXXML.ValTrue, ignoreCase = true)) {
                Base64.decode(xpp.safeNextText(), BASE64_FLAG)?.let { data ->
                    return randomStream?.processBytes(data)
                }
                return ByteArray(0)
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

        private val TAG = DatabaseInputKDBX::class.java.name

        private val DEFAULT_HISTORY_DAYS = UnsignedInt(365)
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
