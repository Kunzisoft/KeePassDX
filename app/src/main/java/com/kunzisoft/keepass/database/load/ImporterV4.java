/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.load;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.crypto.CipherFactory;
import com.kunzisoft.keepass.crypto.PwStreamCipherFactory;
import com.kunzisoft.keepass.crypto.engine.CipherEngine;
import com.kunzisoft.keepass.database.ITimeLogger;
import com.kunzisoft.keepass.database.PwCompressionAlgorithm;
import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwDatabaseV4;
import com.kunzisoft.keepass.database.PwDatabaseV4XML;
import com.kunzisoft.keepass.database.PwDate;
import com.kunzisoft.keepass.database.PwDbHeaderV4;
import com.kunzisoft.keepass.database.PwDeletedObject;
import com.kunzisoft.keepass.database.PwEntryV4;
import com.kunzisoft.keepass.database.PwGroupV4;
import com.kunzisoft.keepass.database.PwIconCustom;
import com.kunzisoft.keepass.database.exception.ArcFourException;
import com.kunzisoft.keepass.database.exception.InvalidDBException;
import com.kunzisoft.keepass.database.exception.InvalidPasswordException;
import com.kunzisoft.keepass.database.security.ProtectedBinary;
import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.stream.BetterCipherInputStream;
import com.kunzisoft.keepass.stream.HashedBlockInputStream;
import com.kunzisoft.keepass.stream.HmacBlockInputStream;
import com.kunzisoft.keepass.stream.LEDataInputStream;
import com.kunzisoft.keepass.tasks.ProgressTaskUpdater;
import com.kunzisoft.keepass.utils.DateUtil;
import com.kunzisoft.keepass.utils.EmptyUtils;
import com.kunzisoft.keepass.utils.MemUtil;
import com.kunzisoft.keepass.utils.Types;

import org.spongycastle.crypto.StreamCipher;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Stack;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import biz.source_code.base64Coder.Base64Coder;

public class ImporterV4 extends Importer {
	
	private StreamCipher randomStream;
	private PwDatabaseV4 db;

    private byte[] hashOfHeader = null;
	private byte[] pbHeader = null;
	private long version;
	Calendar utcCal;

	public ImporterV4() {
		utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public PwDatabaseV4 openDatabase(InputStream inStream, String password,
			InputStream keyInputStream) throws IOException, InvalidDBException {

		return openDatabase(inStream, password, keyInputStream, null);
	}
	
	@Override
    public PwDatabaseV4 openDatabase(InputStream inStream, String password,
									 InputStream keyInputStream, ProgressTaskUpdater progressTaskUpdater) throws IOException,
            InvalidDBException {

		if (progressTaskUpdater != null)
			progressTaskUpdater.updateMessage(R.string.creating_db_key);
		db = new PwDatabaseV4();
		
		PwDbHeaderV4 header = new PwDbHeaderV4(db);
        db.getBinPool().clear();

		PwDbHeaderV4.HeaderAndHash hh = header.loadFromFile(inStream);
        version = header.getVersion();

		hashOfHeader = hh.hash;
		pbHeader = hh.header;

		db.retrieveMasterKey(password, keyInputStream);
		db.makeFinalKey(header.masterSeed);

		if (progressTaskUpdater != null)
			progressTaskUpdater.updateMessage(R.string.decrypting_db);
		CipherEngine engine;
		Cipher cipher;
		try {
			engine = CipherFactory.getInstance(db.getDataCipher());
			db.setDataEngine(engine);
			db.setEncryptionAlgorithm(engine.getPwEncryptionAlgorithm());
			cipher = engine.getCipher(Cipher.DECRYPT_MODE, db.getFinalKey(), header.encryptionIV);
		} catch (NoSuchAlgorithmException|NoSuchPaddingException|InvalidKeyException|InvalidAlgorithmParameterException e) {
			throw new IOException("Invalid algorithm.", e);
		}

		InputStream isPlain;
		if (version < PwDbHeaderV4.FILE_VERSION_32_4) {

			InputStream decrypted = AttachCipherStream(inStream, cipher);
			LEDataInputStream dataDecrypted = new LEDataInputStream(decrypted);
			byte[] storedStartBytes = null;
			try {
				storedStartBytes = dataDecrypted.readBytes(32);
				if (storedStartBytes == null || storedStartBytes.length != 32) {
					throw new InvalidPasswordException();
				}
			} catch (IOException e) {
				throw new InvalidPasswordException();
			}

			if (!Arrays.equals(storedStartBytes, header.streamStartBytes)) {
				throw new InvalidPasswordException();
			}

			isPlain = new HashedBlockInputStream(dataDecrypted);
		}
		else { // KDBX 4
			LEDataInputStream isData = new LEDataInputStream(inStream);
			byte[] storedHash = isData.readBytes(32);
			if (!Arrays.equals(storedHash,hashOfHeader)) {
				throw new InvalidDBException();
			}

			byte[] hmacKey = db.getHmacKey();
			byte[] headerHmac = PwDbHeaderV4.computeHeaderHmac(pbHeader, hmacKey);
			byte[] storedHmac = isData.readBytes(32);
			if (storedHmac == null || storedHmac.length != 32) {
				throw new InvalidDBException();
			}
			// Mac doesn't match
			if (! Arrays.equals(headerHmac, storedHmac)) {
				throw new InvalidDBException();
			}

			HmacBlockInputStream hmIs = new HmacBlockInputStream(isData, true, hmacKey);

			isPlain = AttachCipherStream(hmIs, cipher);
		}

		InputStream isXml;
		if ( db.getCompressionAlgorithm() == PwCompressionAlgorithm.Gzip ) {
			isXml = new GZIPInputStream(isPlain);
		} else {
			isXml = isPlain;
		}

		if (version >= PwDbHeaderV4.FILE_VERSION_32_4) {
			LoadInnerHeader(isXml, header);
		}
		
		if ( header.innerRandomStreamKey == null ) {
			assert(false);
			throw new IOException("Invalid stream key.");
		}
		
		randomStream = PwStreamCipherFactory.getInstance(header.innerRandomStream, header.innerRandomStreamKey);
		
		if ( randomStream == null ) {
			throw new ArcFourException();
		}
		
		ReadXmlStreamed(isXml);

		return db;
		
		
	}

	private InputStream AttachCipherStream(InputStream is, Cipher cipher) {
		return new BetterCipherInputStream(is, cipher, 50 * 1024);
	}

	private void LoadInnerHeader(InputStream is, PwDbHeaderV4 header) throws IOException {
		LEDataInputStream lis = new LEDataInputStream(is);

		while(true) {
			if (!ReadInnerHeader(lis, header)) break;
		}

	}

	private boolean ReadInnerHeader(LEDataInputStream lis, PwDbHeaderV4 header) throws IOException {
		byte fieldId = (byte)lis.read();

		int size = lis.readInt();
		if (size < 0) throw new IOException("Corrupted file");

		byte[] data = new byte[0];
		if (size > 0) {
			data = lis.readBytes(size);
		}

		boolean result = true;
		switch(fieldId) {
			case PwDbHeaderV4.PwDbInnerHeaderV4Fields.EndOfHeader:
				result = false;
				break;
			case PwDbHeaderV4.PwDbInnerHeaderV4Fields.InnerRandomStreamID:
			    header.setRandomStreamID(data);
				break;
			case PwDbHeaderV4.PwDbInnerHeaderV4Fields.InnerRandomstreamKey:
			    header.innerRandomStreamKey = data;
				break;
			case PwDbHeaderV4.PwDbInnerHeaderV4Fields.Binary:
			    if (data.length < 1) throw new IOException("Invalid binary format");
				byte flag = data[0];
				boolean prot = (flag & PwDbHeaderV4.KdbxBinaryFlags.Protected) !=
						PwDbHeaderV4.KdbxBinaryFlags.None;

				byte[] bin = new byte[data.length - 1];
				System.arraycopy(data, 1, bin, 0, data.length-1);
				ProtectedBinary pb = new ProtectedBinary(prot, bin);
				db.getBinPool().poolAdd(pb);

				if (prot) {
					Arrays.fill(data, (byte)0);
				}
				break;
			default:
				assert(false);
				break;
		}

		return result;
	}

	private enum KdbContext {
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

    private static final long DEFAULT_HISTORY_DAYS = 365;
	
	private boolean readNextNode = true;
	private Stack<PwGroupV4> ctxGroups = new Stack<PwGroupV4>();
	private PwGroupV4 ctxGroup = null;
	private PwEntryV4 ctxEntry = null;
	private String ctxStringName = null;
	private ProtectedString ctxStringValue = null;
	private String ctxBinaryName = null;
	private ProtectedBinary ctxBinaryValue = null;
	private String ctxATName = null;
	private String ctxATSeq = null;
	private boolean entryInHistory = false;
	private PwEntryV4 ctxHistoryBase = null;
	private PwDeletedObject ctxDeletedObject = null;
	private UUID customIconID = PwDatabase.UUID_ZERO;
	private byte[] customIconData;
	private String customDataKey = null;
	private String customDataValue = null;
	private String groupCustomDataKey = null;
	private String groupCustomDataValue = null;
	private String entryCustomDataKey = null;
	private String entryCustomDataValue = null;

	private void ReadXmlStreamed(InputStream readerStream) throws IOException, InvalidDBException {
		
			try {
				ReadDocumentStreamed(CreatePullParser(readerStream));
			} catch (XmlPullParserException e) {
				e.printStackTrace();
				throw new IOException(e.getLocalizedMessage());
			}
	}
	
	private static XmlPullParser CreatePullParser(InputStream readerStream) throws XmlPullParserException {
		XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
		xppf.setNamespaceAware(false);
		
		XmlPullParser xpp = xppf.newPullParser();
		xpp.setInput(readerStream, null);
		
		return xpp;
	}

	private void ReadDocumentStreamed(XmlPullParser xpp) throws XmlPullParserException, IOException, InvalidDBException {

		ctxGroups.clear();
		
		KdbContext ctx = KdbContext.Null;
		
		readNextNode = true;
		
		while (true) {
			if ( readNextNode ) {
				if( xpp.next() == XmlPullParser.END_DOCUMENT ) break;
			} else {
				readNextNode = true;
			}
			
			switch ( xpp.getEventType() ) {
			case XmlPullParser.START_TAG:
				ctx = ReadXmlElement(ctx, xpp);
				break;
				
			case XmlPullParser.END_TAG:
				ctx = EndXmlElement(ctx, xpp);
				break;

			default:
				assert(false);
				break;
					
			}
			
		}
		
		// Error checks
		if ( ctx != KdbContext.Null ) throw new IOException("Malformed");
		if ( ctxGroups.size() != 0 ) throw new IOException("Malformed");
	}


	private KdbContext ReadXmlElement(KdbContext ctx, XmlPullParser xpp) throws XmlPullParserException, IOException, InvalidDBException {
		String name = xpp.getName();
		switch (ctx) {
		case Null:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDocNode) ) {
				return SwitchContext(ctx, KdbContext.KeePassFile, xpp);
			} else ReadUnknown(xpp);
			break;
			
		case KeePassFile:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemMeta) ) {
				return SwitchContext(ctx, KdbContext.Meta, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemRoot) ) {
				return SwitchContext(ctx, KdbContext.Root, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case Meta:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemGenerator) ) {
				ReadString(xpp); // Ignore
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemHeaderHash) ) {
				String encodedHash = ReadString(xpp);
				if (!EmptyUtils.isNullOrEmpty(encodedHash) && (hashOfHeader != null)) {
					byte[] hash = Base64Coder.decode(encodedHash);
					if (!Arrays.equals(hash, hashOfHeader)) {
						throw new InvalidDBException();
					}
				}
			} else if (name.equalsIgnoreCase(PwDatabaseV4XML.ElemSettingsChanged)) {
				db.setSettingsChanged(ReadPwTime(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbName) ) {
				db.setName(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbNameChanged) ) {
				db.setNameChanged(ReadPwTime(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbDesc) ) {
				db.setDescription(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbDescChanged) ) {
				db.setDescriptionChanged(ReadPwTime(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbDefaultUser) ) {
				db.setDefaultUserName(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbDefaultUserChanged) ) {
				db.setDefaultUserNameChanged(ReadPwTime(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbColor)) {
				// TODO: Add support to interpret the color if we want to allow changing the database color
				db.setColor(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbMntncHistoryDays) ) {
				db.setMaintenanceHistoryDays(ReadUInt(xpp, DEFAULT_HISTORY_DAYS));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbKeyChanged) ) {
				db.setKeyLastChanged(ReadPwTime(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbKeyChangeRec) ) {
				db.setKeyChangeRecDays(ReadLong(xpp, -1));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbKeyChangeForce) ) {
				db.setKeyChangeForceDays(ReadLong(xpp, -1));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDbKeyChangeForceOnce) ) {
				db.setKeyChangeForceOnce(ReadBool(xpp, false));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemMemoryProt) ) {
				return SwitchContext(ctx, KdbContext.MemoryProtection, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomIcons) ) {
				return SwitchContext(ctx, KdbContext.CustomIcons, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemRecycleBinEnabled) ) {
				db.setRecycleBinEnabled(ReadBool(xpp, true));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemRecycleBinUuid) ) {
				db.setRecycleBinUUID(ReadUuid(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemRecycleBinChanged) ) {
				db.setRecycleBinChanged(ReadTime(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemEntryTemplatesGroup) ) {
				db.setEntryTemplatesGroup(ReadUuid(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemEntryTemplatesGroupChanged) ) {
				db.setEntryTemplatesGroupChanged(ReadPwTime(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemHistoryMaxItems) ) {
				db.setHistoryMaxItems(ReadInt(xpp, -1));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemHistoryMaxSize) ) {
				db.setHistoryMaxSize(ReadLong(xpp, -1));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemLastSelectedGroup) ) {
				db.setLastSelectedGroup(ReadUuid(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemLastTopVisibleGroup) ) {
				db.setLastTopVisibleGroup(ReadUuid(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemBinaries) ) {
				return SwitchContext(ctx, KdbContext.Binaries, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomData) ) {
				return SwitchContext(ctx, KdbContext.CustomData, xpp);
			}
			break;
			
		case MemoryProtection:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemProtTitle) ) {
				db.getMemoryProtection().protectTitle = ReadBool(xpp, false);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemProtUserName) ) {
				db.getMemoryProtection().protectUserName = ReadBool(xpp, false);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemProtPassword) ) {
				db.getMemoryProtection().protectPassword = ReadBool(xpp, false);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemProtURL) ) {
				db.getMemoryProtection().protectUrl = ReadBool(xpp, false);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemProtNotes) ) {
				db.getMemoryProtection().protectNotes = ReadBool(xpp, false);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemProtAutoHide) ) {
				db.getMemoryProtection().autoEnableVisualHiding = ReadBool(xpp, false);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case CustomIcons:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomIconItem) ) {
				return SwitchContext(ctx, KdbContext.CustomIcon, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case CustomIcon:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomIconItemID) ) {
				customIconID = ReadUuid(xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomIconItemData) ) {
				String strData = ReadString(xpp);
				if ( strData != null && strData.length() > 0 ) {
					customIconData = Base64Coder.decode(strData);
				} else {
					assert(false);
				}
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case Binaries:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemBinary) ) {
				String key = xpp.getAttributeValue(null, PwDatabaseV4XML.AttrId);
				if ( key != null ) {
					ProtectedBinary pbData = ReadProtectedBinary(xpp);
					int id = Integer.parseInt(key);
					db.getBinPool().put(id, pbData);
				} else {
					ReadUnknown(xpp);
				}
			} else {
				ReadUnknown(xpp);
			}
			
			break;

		case CustomData:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemStringDictExItem) ) {
				return SwitchContext(ctx, KdbContext.CustomDataItem, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case CustomDataItem:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemKey) ) {
				customDataKey = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemValue) ) {
				customDataValue = ReadString(xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case Root:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemGroup) ) {
				assert(ctxGroups.size() == 0);
				if ( ctxGroups.size() != 0 ) throw new IOException("Group list should be empty.");
				
				db.setRootGroup(new PwGroupV4());
				ctxGroups.push(db.getRootGroup());
				ctxGroup = ctxGroups.peek();
				
				return SwitchContext(ctx, KdbContext.Group, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDeletedObjects) ) {
				return SwitchContext(ctx, KdbContext.RootDeletedObjects, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case Group:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemUuid) ) {
				ctxGroup.setUUID(ReadUuid(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemName) ) {
				ctxGroup.setName(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemNotes) ) {
				ctxGroup.setNotes(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemIcon) ) {
				ctxGroup.setIcon(db.getIconFactory().getIcon((int)ReadUInt(xpp, 0)));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomIconID) ) {
				ctxGroup.setCustomIcon(db.getIconFactory().getIcon(ReadUuid(xpp)));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemTimes) ) {
				return SwitchContext(ctx, KdbContext.GroupTimes, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemIsExpanded) ) {
				ctxGroup.setExpanded(ReadBool(xpp, true));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemGroupDefaultAutoTypeSeq) ) {
				ctxGroup.setDefaultAutoTypeSequence(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemEnableAutoType) ) {
				ctxGroup.setEnableAutoType(StringToBoolean(ReadString(xpp)));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemEnableSearching) ) {
				ctxGroup.setEnableSearching(StringToBoolean(ReadString(xpp)));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemLastTopVisibleEntry) ) {
				ctxGroup.setLastTopVisibleEntry(ReadUuid(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomData) ) {
                return SwitchContext(ctx, KdbContext.GroupCustomData, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemGroup) ) {
				ctxGroup = new PwGroupV4();
				ctxGroups.peek().addGroup(ctxGroup);
				ctxGroups.push(ctxGroup);
				
				return SwitchContext(ctx, KdbContext.Group, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemEntry) ) {
				ctxEntry = new PwEntryV4();
				ctxGroup.addEntry(ctxEntry);
				
				entryInHistory = false;
				return SwitchContext(ctx, KdbContext.Entry, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
        case GroupCustomData:
        	if (name.equalsIgnoreCase(PwDatabaseV4XML.ElemStringDictExItem)) {
				return SwitchContext(ctx, KdbContext.GroupCustomDataItem, xpp);
			} else {
				ReadUnknown(xpp);
			}
            break;
        case GroupCustomDataItem:
        	if (name.equalsIgnoreCase(PwDatabaseV4XML.ElemKey)) {
				groupCustomDataKey = ReadString(xpp);
			} else if (name.equalsIgnoreCase(PwDatabaseV4XML.ElemValue)) {
				groupCustomDataValue = ReadString(xpp);
            } else {
                ReadUnknown(xpp);
            }
            break;

			
		case Entry:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemUuid) ) {
				ctxEntry.setUUID(ReadUuid(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemIcon) ) {
				ctxEntry.setIcon(db.getIconFactory().getIcon((int)ReadUInt(xpp, 0)));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomIconID) ) {
				ctxEntry.setCustomIcon(db.getIconFactory().getIcon(ReadUuid(xpp)));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemFgColor) ) {
				ctxEntry.setForegroundColor(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemBgColor) ) {
				ctxEntry.setBackgroupColor(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemOverrideUrl) ) {
				ctxEntry.setOverrideURL(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemTags) ) {
				ctxEntry.setTags(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemTimes) ) {
				return SwitchContext(ctx, KdbContext.EntryTimes, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemString) ) {
				return SwitchContext(ctx, KdbContext.EntryString, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemBinary) ) {
				return SwitchContext(ctx, KdbContext.EntryBinary, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemAutoType) ) {
				return SwitchContext(ctx, KdbContext.EntryAutoType, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomData)) {
				return SwitchContext(ctx, KdbContext.EntryCustomData, xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemHistory) ) {
				assert(!entryInHistory);
				
				if ( ! entryInHistory ) {
					ctxHistoryBase = ctxEntry;
					return SwitchContext(ctx, KdbContext.EntryHistory, xpp);
				} else {
					ReadUnknown(xpp);
				}
			} else {
				ReadUnknown(xpp);
			}
			break;
        case EntryCustomData:
            if (name.equalsIgnoreCase(PwDatabaseV4XML.ElemStringDictExItem)) {
                return SwitchContext(ctx, KdbContext.EntryCustomDataItem, xpp);
            } else {
                ReadUnknown(xpp);
            }
            break;
        case EntryCustomDataItem:
            if (name.equalsIgnoreCase(PwDatabaseV4XML.ElemKey)) {
                entryCustomDataKey = ReadString(xpp);
            } else if (name.equalsIgnoreCase(PwDatabaseV4XML.ElemValue)) {
                entryCustomDataValue = ReadString(xpp);
            } else {
                ReadUnknown(xpp);
            }
            break;

		case GroupTimes:
		case EntryTimes:
			ITimeLogger tl;
			if ( ctx == KdbContext.GroupTimes ) {
				tl = ctxGroup;
			} else {
				tl = ctxEntry;
			}
			
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemLastModTime) ) {
				tl.setLastModificationTime(ReadPwTime(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemCreationTime) ) {
				tl.setCreationTime(ReadPwTime(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemLastAccessTime) ) {
				tl.setLastAccessTime(ReadPwTime(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemExpiryTime) ) {
				tl.setExpiryTime(ReadPwTime(xpp));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemExpires) ) {
				tl.setExpires(ReadBool(xpp, false));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemUsageCount) ) {
				tl.setUsageCount(ReadULong(xpp, 0));
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemLocationChanged) ) {
				tl.setLocationChanged(ReadPwTime(xpp));
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case EntryString:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemKey) ) {
				ctxStringName = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemValue) ) {
				ctxStringValue = ReadProtectedString(xpp); 
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case EntryBinary:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemKey) ) {
				ctxBinaryName = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemValue) ) {
				ctxBinaryValue = ReadProtectedBinary(xpp);
			}
			break;
			
		case EntryAutoType:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemAutoTypeEnabled) ) {
				ctxEntry.getAutoType().enabled = ReadBool(xpp, true);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemAutoTypeObfuscation) ) {
				ctxEntry.getAutoType().obfuscationOptions = ReadUInt(xpp, 0);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemAutoTypeDefaultSeq) ) {
				ctxEntry.getAutoType().defaultSequence = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemAutoTypeItem) ) {
				return SwitchContext(ctx, KdbContext.EntryAutoTypeItem, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case EntryAutoTypeItem:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemWindow) ) {
				ctxATName = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemKeystrokeSequence) ) {
				ctxATSeq = ReadString(xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case EntryHistory:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemEntry) ) {
				ctxEntry = new PwEntryV4();
				ctxHistoryBase.addToHistory(ctxEntry);
				
				entryInHistory = true;
				return SwitchContext(ctx, KdbContext.Entry, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case RootDeletedObjects:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDeletedObject) ) {
				ctxDeletedObject = new PwDeletedObject();
				db.addDeletedObject(ctxDeletedObject);
				
				return SwitchContext(ctx, KdbContext.DeletedObject, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case DeletedObject:
			if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemUuid) ) {
				ctxDeletedObject.uuid = ReadUuid(xpp);
			} else if ( name.equalsIgnoreCase(PwDatabaseV4XML.ElemDeletionTime) ) {
				ctxDeletedObject.setDeletionTime(ReadTime(xpp));
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		default:
			ReadUnknown(xpp);
			break;
		}
		
		return ctx;
	}

	private KdbContext EndXmlElement(KdbContext ctx, XmlPullParser xpp) throws XmlPullParserException {
		assert(xpp.getEventType() == XmlPullParser.END_TAG);
		
		String name = xpp.getName();
		if ( ctx == KdbContext.KeePassFile && name.equalsIgnoreCase(PwDatabaseV4XML.ElemDocNode) ) {
			return KdbContext.Null;
		} else if ( ctx == KdbContext.Meta && name.equalsIgnoreCase(PwDatabaseV4XML.ElemMeta) ) {
			return KdbContext.KeePassFile;
		} else if ( ctx == KdbContext.Root && name.equalsIgnoreCase(PwDatabaseV4XML.ElemRoot) ) {
			return KdbContext.KeePassFile;
		} else if ( ctx == KdbContext.MemoryProtection && name.equalsIgnoreCase(PwDatabaseV4XML.ElemMemoryProt) ) {
			return KdbContext.Meta;
		} else if ( ctx == KdbContext.CustomIcons && name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomIcons) ) {
			return KdbContext.Meta;
		} else if ( ctx == KdbContext.CustomIcon && name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomIconItem) ) {
			if ( ! customIconID.equals(PwDatabase.UUID_ZERO) ) {
				PwIconCustom icon = new PwIconCustom(customIconID, customIconData);
				db.addCustomIcon(icon);
				db.getIconFactory().put(icon);
			} else assert(false);
			
			customIconID = PwDatabase.UUID_ZERO;
			customIconData = null;
			
			return KdbContext.CustomIcons;
		} else if ( ctx == KdbContext.Binaries && name.equalsIgnoreCase(PwDatabaseV4XML.ElemBinaries) ) {
			return KdbContext.Meta;
		} else if ( ctx == KdbContext.CustomData && name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomData) ) {
			return KdbContext.Meta;
		} else if ( ctx == KdbContext.CustomDataItem && name.equalsIgnoreCase(PwDatabaseV4XML.ElemStringDictExItem) ) {
			if ( customDataKey != null && customDataValue != null) {
				db.putCustomData(customDataKey, customDataValue);
			} else assert(false);
			
			customDataKey = null;
			customDataValue = null;
			
			return KdbContext.CustomData;
		} else if ( ctx == KdbContext.Group && name.equalsIgnoreCase(PwDatabaseV4XML.ElemGroup) ) {
			if ( ctxGroup.getUUID() == null || ctxGroup.getUUID().equals(PwDatabase.UUID_ZERO) ) {
				ctxGroup.setUUID(UUID.randomUUID());
			}
			
			ctxGroups.pop();
			
			if ( ctxGroups.size() == 0 ) {
				ctxGroup = null;
				return KdbContext.Root;
			} else {
				ctxGroup = ctxGroups.peek();
				return KdbContext.Group;
			}
		} else if ( ctx == KdbContext.GroupTimes && name.equalsIgnoreCase(PwDatabaseV4XML.ElemTimes) ) {
			return KdbContext.Group;
		} else if ( ctx == KdbContext.GroupCustomData && name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomData) ) {
			return KdbContext.Group;
		} else if ( ctx == KdbContext.GroupCustomDataItem && name.equalsIgnoreCase(PwDatabaseV4XML.ElemStringDictExItem)) {
			if (groupCustomDataKey != null && groupCustomDataValue != null) {
				ctxGroup.putCustomData(groupCustomDataKey, groupCustomDataKey);
			} else {
				assert(false);
			}

			groupCustomDataKey = null;
			groupCustomDataValue = null;

			return KdbContext.GroupCustomData;

		} else if ( ctx == KdbContext.Entry && name.equalsIgnoreCase(PwDatabaseV4XML.ElemEntry) ) {
			if ( ctxEntry.getUUID() == null || ctxEntry.getUUID().equals(PwDatabase.UUID_ZERO) ) {
				ctxEntry.setUUID(UUID.randomUUID());
			}
			
			if ( entryInHistory ) {
				ctxEntry = ctxHistoryBase;
				return KdbContext.EntryHistory;
			}
			
			return KdbContext.Group;
		} else if ( ctx == KdbContext.EntryTimes && name.equalsIgnoreCase(PwDatabaseV4XML.ElemTimes) ) {
			return KdbContext.Entry;
		} else if ( ctx == KdbContext.EntryString && name.equalsIgnoreCase(PwDatabaseV4XML.ElemString) ) {
			ctxEntry.addExtraField(ctxStringName, ctxStringValue);
			ctxStringName = null;
			ctxStringValue = null;
			
			return KdbContext.Entry;
		} else if ( ctx == KdbContext.EntryBinary && name.equalsIgnoreCase(PwDatabaseV4XML.ElemBinary) ) {
			ctxEntry.putProtectedBinary(ctxBinaryName, ctxBinaryValue);
			ctxBinaryName = null;
			ctxBinaryValue = null;
			
			return KdbContext.Entry;
		} else if ( ctx == KdbContext.EntryAutoType && name.equalsIgnoreCase(PwDatabaseV4XML.ElemAutoType) ) {
			return KdbContext.Entry;
		} else if ( ctx == KdbContext.EntryAutoTypeItem && name.equalsIgnoreCase(PwDatabaseV4XML.ElemAutoTypeItem) ) {
			ctxEntry.getAutoType().put(ctxATName, ctxATSeq);
			ctxATName = null;
			ctxATSeq = null;

			return KdbContext.EntryAutoType;
		} else if ( ctx == KdbContext.EntryCustomData && name.equalsIgnoreCase(PwDatabaseV4XML.ElemCustomData)) {
			return KdbContext.Entry;
		} else if ( ctx == KdbContext.EntryCustomDataItem && name.equalsIgnoreCase(PwDatabaseV4XML.ElemStringDictExItem)) {
			if (entryCustomDataKey != null && entryCustomDataValue != null) {
				ctxEntry.putCustomData(entryCustomDataKey, entryCustomDataValue);
			} else {
				assert(false);
			}

			entryCustomDataKey = null;
			entryCustomDataValue = null;

			return KdbContext.EntryCustomData;
		} else if ( ctx == KdbContext.EntryHistory && name.equalsIgnoreCase(PwDatabaseV4XML.ElemHistory) ) {
			entryInHistory = false;
			return KdbContext.Entry;
		} else if ( ctx == KdbContext.RootDeletedObjects && name.equalsIgnoreCase(PwDatabaseV4XML.ElemDeletedObjects) ) {
			return KdbContext.Root;
		} else if ( ctx == KdbContext.DeletedObject && name.equalsIgnoreCase(PwDatabaseV4XML.ElemDeletedObject) ) {
			ctxDeletedObject = null;
			return KdbContext.RootDeletedObjects;
		} else {
			assert(false);

			String contextName = "";
			if (ctx != null) {
				contextName = ctx.name();
			}
			throw new RuntimeException("Invalid end element: Context " +  contextName + "End element: " + name);
		}
	}

	private PwDate ReadPwTime(XmlPullParser xpp) throws IOException, XmlPullParserException {
		return new PwDate(ReadTime(xpp));
	}
	
	private Date ReadTime(XmlPullParser xpp) throws IOException, XmlPullParserException {
		String sDate = ReadString(xpp);
		Date utcDate = null;

		if (version >= PwDbHeaderV4.FILE_VERSION_32_4) {
			byte[] buf = Base64Coder.decode(sDate);
			if (buf.length != 8) {
				byte[] buf8 = new byte[8];
				System.arraycopy(buf, 0, buf8, 0, Math.min(buf.length, 8));
				buf = buf8;
			}

			long seconds = LEDataInputStream.readLong(buf, 0);
			utcDate = DateUtil.convertKDBX4Time(seconds);

		} else {

			try {
				utcDate = PwDatabaseV4XML.dateFormatter.get().parse(sDate);
			} catch (ParseException e) {
				// Catch with null test below
			}

			if (utcDate == null) {
				utcDate = new Date(0L);
			}
		}
		
		return utcDate;
		
	}

	private void ReadUnknown(XmlPullParser xpp) throws XmlPullParserException, IOException {
		assert(false);
		
		if ( xpp.isEmptyElementTag() ) return;
		
		String unknownName = xpp.getName();
		ProcessNode(xpp);
		
		while (xpp.next() != XmlPullParser.END_DOCUMENT ) {
			if ( xpp.getEventType() == XmlPullParser.END_TAG ) break;
			if ( xpp.getEventType() == XmlPullParser.START_TAG ) continue;
			
			ReadUnknown(xpp);
		}
		
		assert(xpp.getName() == unknownName);
		
	}
	
	private boolean ReadBool(XmlPullParser xpp, boolean bDefault) throws IOException, XmlPullParserException {
		String str = ReadString(xpp);
		
		if ( str.equalsIgnoreCase("true") ) {
			return true;
		} else if ( str.equalsIgnoreCase("false") ) {
			return false;
		} else {
			return bDefault;
		}
	}
	
	private UUID ReadUuid(XmlPullParser xpp) throws IOException, XmlPullParserException {
		String encoded = ReadString(xpp);
		
		if (encoded == null || encoded.length() == 0 ) {
			return PwDatabase.UUID_ZERO;
		}
		
		// TODO: Switch to framework Base64 once API level 8 is the minimum
		byte[] buf = Base64Coder.decode(encoded);
		
		return Types.bytestoUUID(buf);
	}
	
	private int ReadInt(XmlPullParser xpp, int def) throws IOException, XmlPullParserException {
		String str = ReadString(xpp);
		
		int u;
		try {
			u = Integer.parseInt(str);
		} catch( NumberFormatException e) {
			u = def;
		}
		
		return u;
	}
	
	private static final long MAX_UINT = 4294967296L; // 2^32
	private long ReadUInt(XmlPullParser xpp, long uDefault) throws IOException, XmlPullParserException {
		long u;
		
		u = ReadULong(xpp, uDefault);
		if ( u < 0 || u > MAX_UINT ) {
			throw new NumberFormatException("Outside of the uint size");
		}

		return u;
		
	}
	
	private long ReadLong(XmlPullParser xpp, long def) throws IOException, XmlPullParserException {
		String str = ReadString(xpp);
		
		long u;
		try {
			u = Long.parseLong(str);
		} catch( NumberFormatException e) {
			u = def;
		}
		
		return u;
	}
	
	private long ReadULong(XmlPullParser xpp, long uDefault) throws IOException, XmlPullParserException {
		long u = ReadLong(xpp, uDefault);
		
		if ( u < 0 ) {
			u = uDefault;
		}
		
		return u;
		
	}
	
	private ProtectedString ReadProtectedString(XmlPullParser xpp) throws XmlPullParserException, IOException {
		byte[] buf = ProcessNode(xpp);
		
		if ( buf != null) {
			try {
				return new ProtectedString(true, new String(buf, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				throw new IOException(e.getLocalizedMessage());
			} 
		}
		
		return new ProtectedString(false, ReadString(xpp));
	}
	
	private ProtectedBinary ReadProtectedBinary(XmlPullParser xpp) throws XmlPullParserException, IOException {
		String ref = xpp.getAttributeValue(null, PwDatabaseV4XML.AttrRef);
		if (ref != null) {
			xpp.next(); // Consume end tag

			int id = Integer.parseInt(ref);
			return db.getBinPool().get(id);
		} 
		
		boolean compressed = false;
		String comp = xpp.getAttributeValue(null, PwDatabaseV4XML.AttrCompressed);
		if (comp != null) {
			compressed = comp.equalsIgnoreCase(PwDatabaseV4XML.ValTrue);
		}
		
		byte[] buf = ProcessNode(xpp);
		
		if ( buf != null ) return new ProtectedBinary(true, buf);
		
		String base64 = ReadString(xpp);
		if ( base64.length() == 0 ) return ProtectedBinary.EMPTY;
		
		byte[] data = Base64Coder.decode(base64);
		
		if (compressed) {
			data = MemUtil.decompress(data);
		}
		
		return new ProtectedBinary(false, data);
	}
	
	private String ReadString(XmlPullParser xpp) throws IOException, XmlPullParserException {
		byte[] buf = ProcessNode(xpp);
		
		if ( buf != null ) {
			try {
				return new String(buf, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new IOException(e.getLocalizedMessage());
			}
		}
		
		//readNextNode = false;
		return xpp.nextText();
		
	}
	
	private String ReadStringRaw(XmlPullParser xpp) throws XmlPullParserException, IOException {
		
		//readNextNode = false;
		return xpp.nextText();
	}

	private byte[] ProcessNode(XmlPullParser xpp) throws XmlPullParserException, IOException {
		assert(xpp.getEventType() == XmlPullParser.START_TAG);
		
		byte[] buf = null;
		
		if ( xpp.getAttributeCount() > 0 ) {
			String protect = xpp.getAttributeValue(null, PwDatabaseV4XML.AttrProtected);
			if ( protect != null && protect.equalsIgnoreCase(PwDatabaseV4XML.ValTrue) ) {
				String encrypted = ReadStringRaw(xpp);
				
				if ( encrypted.length() > 0 ) {
					buf = Base64Coder.decode(encrypted);
					byte[] plainText = new byte[buf.length];
					
					randomStream.processBytes(buf, 0, buf.length, plainText, 0);
					
					return plainText;
				} else {
					buf = new byte[0];
				}
			}
		}
		
		return buf;
	}

	private KdbContext SwitchContext(KdbContext ctxCurrent, KdbContext ctxNew,
			XmlPullParser xpp) throws XmlPullParserException, IOException {

		if ( xpp.isEmptyElementTag() ) {
			xpp.next();  // Consume the end tag
			return ctxCurrent;
		}
		return ctxNew;
	}


	private Boolean StringToBoolean(String str) {
		if ( str == null || str.length() == 0 ) {
			return null;
		}
		
		String trimmed = str.trim();
		if ( trimmed.equalsIgnoreCase("true") ) {
			return true;
		} else if ( trimmed.equalsIgnoreCase("false") ) {
			return false;
		}
		
		return null;
		
	}
}
