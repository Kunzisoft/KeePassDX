/*
 * Copyright 2009-2013 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database.load;

import static com.keepassdroid.database.PwDatabaseV4XML.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Stack;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.crypto.StreamCipher;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import biz.source_code.base64Coder.Base64Coder;

import com.keepassdroid.UpdateStatus;
import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.crypto.PwStreamCipherFactory;
import com.keepassdroid.database.BinaryPool;
import com.keepassdroid.database.ITimeLogger;
import com.keepassdroid.database.PwCompressionAlgorithm;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDatabaseV4XML;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.PwDeletedObject;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.PwGroupV4;
import com.keepassdroid.database.PwIconCustom;
import com.keepassdroid.database.exception.ArcFourException;
import com.keepassdroid.database.exception.InvalidDBException;
import com.keepassdroid.database.exception.InvalidPasswordException;
import com.keepassdroid.database.security.ProtectedBinary;
import com.keepassdroid.database.security.ProtectedString;
import com.keepassdroid.stream.BetterCipherInputStream;
import com.keepassdroid.stream.HashedBlockInputStream;
import com.keepassdroid.stream.LEDataInputStream;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.MemUtil;
import com.keepassdroid.utils.Types;

public class ImporterV4 extends Importer {
	
	private StreamCipher randomStream;
	private PwDatabaseV4 db;
	private BinaryPool binPool = new BinaryPool();

		private byte[] hashOfHeader = null;
	
	protected PwDatabaseV4 createDB() {
		return new PwDatabaseV4();

	}

	@Override
	public PwDatabaseV4 openDatabase(InputStream inStream, String password,
			String keyfile) throws IOException, InvalidDBException {

		return openDatabase(inStream, password, keyfile, new UpdateStatus());
	}
	
	@Override
	public PwDatabaseV4 openDatabase(InputStream inStream, String password,
			String keyfile, UpdateStatus status) throws IOException,
			InvalidDBException {

		db = createDB();
		
		PwDbHeaderV4 header = new PwDbHeaderV4(db);
		
		hashOfHeader = header.loadFromFile(inStream);
			
		db.setMasterKey(password, keyfile);
		db.makeFinalKey(header.masterSeed, header.transformSeed, (int)db.numKeyEncRounds);
		
		// Attach decryptor
		Cipher cipher;
		try {
			cipher = CipherFactory.getInstance(db.dataCipher, Cipher.DECRYPT_MODE, db.finalKey, header.encryptionIV);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("Invalid algorithm.");
		} catch (NoSuchPaddingException e) {
			throw new IOException("Invalid algorithm.");
		} catch (InvalidKeyException e) {
			throw new IOException("Invalid algorithm.");
		} catch (InvalidAlgorithmParameterException e) {
			throw new IOException("Invalid algorithm.");
		}
		
		InputStream decrypted = new BetterCipherInputStream(inStream, cipher, 50 * 1024);
		LEDataInputStream dataDecrypted = new LEDataInputStream(decrypted);
		byte[] storedStartBytes = dataDecrypted.readBytes(32);
		if ( storedStartBytes == null || storedStartBytes.length != 32 ) {
			throw new IOException("Invalid data.");
		}
		
		if ( ! Arrays.equals(storedStartBytes, header.streamStartBytes) ) {
			throw new InvalidPasswordException();
		}

		HashedBlockInputStream hashed = new HashedBlockInputStream(dataDecrypted); 
		
		InputStream decompressed;
		if ( db.compressionAlgorithm == PwCompressionAlgorithm.Gzip ) {
			decompressed = new GZIPInputStream(hashed); 
		} else {
			decompressed = hashed;
		}
		
		if ( header.protectedStreamKey == null ) {
			assert(false);
			throw new IOException("Invalid stream key.");
		}
		
		randomStream = PwStreamCipherFactory.getInstance(header.innerRandomStream, header.protectedStreamKey);
		
		if ( randomStream == null ) {
			throw new ArcFourException();
		}
		
		ReadXmlStreamed(decompressed);

		return db;
		
		
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
        Entry,
        EntryTimes,
        EntryString,
        EntryBinary,
        EntryAutoType,
        EntryAutoTypeItem,
        EntryHistory,
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
	private UUID customIconID = PwDatabaseV4.UUID_ZERO;
	private byte[] customIconData;
	private String customDataKey = null;
	private String customDataValue = null;
	
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
			if ( name.equalsIgnoreCase(ElemDocNode) ) {
				return SwitchContext(ctx, KdbContext.KeePassFile, xpp);
			} else ReadUnknown(xpp);
			break;
			
		case KeePassFile:
			if ( name.equalsIgnoreCase(ElemMeta) ) {
				return SwitchContext(ctx, KdbContext.Meta, xpp);
			} else if ( name.equalsIgnoreCase(ElemRoot) ) {
				return SwitchContext(ctx, KdbContext.Root, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case Meta:
			if ( name.equalsIgnoreCase(ElemGenerator) ) {
				ReadString(xpp); // Ignore
			} else if ( name.equalsIgnoreCase(ElemHeaderHash) ) {
				String encodedHash = ReadString(xpp);
				if (!EmptyUtils.isNullOrEmpty(encodedHash) && (hashOfHeader != null)) {
					byte[] hash = Base64Coder.decode(encodedHash);
					if (!Arrays.equals(hash, hashOfHeader)) {
						throw new InvalidDBException();
					}
				}
			} else if ( name.equalsIgnoreCase(ElemDbName) ) {
				db.name = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemDbNameChanged) ) {
				db.nameChanged = ReadTime(xpp);
			} else if ( name.equalsIgnoreCase(ElemDbDesc) ) {
				db.description = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemDbDescChanged) ) {
				db.descriptionChanged = ReadTime(xpp);
			} else if ( name.equalsIgnoreCase(ElemDbDefaultUser) ) {
				db.defaultUserName = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemDbDefaultUserChanged) ) {
				db.defaultUserNameChanged = ReadTime(xpp);
			} else if ( name.equalsIgnoreCase(ElemDbColor)) {
				// TODO: Add support to interpret the color if we want to allow changing the database color
				db.color = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemDbMntncHistoryDays) ) {
				db.maintenanceHistoryDays = ReadUInt(xpp, DEFAULT_HISTORY_DAYS);
			} else if ( name.equalsIgnoreCase(ElemDbKeyChanged) ) {
				db.keyLastChanged = ReadTime(xpp);
			} else if ( name.equalsIgnoreCase(ElemDbKeyChangeRec) ) {
				db.keyChangeRecDays = ReadLong(xpp, -1);
			} else if ( name.equalsIgnoreCase(ElemDbKeyChangeForce) ) {
				db.keyChangeForceDays = ReadLong(xpp, -1);
			} else if ( name.equalsIgnoreCase(ElemMemoryProt) ) {
				return SwitchContext(ctx, KdbContext.MemoryProtection, xpp);
			} else if ( name.equalsIgnoreCase(ElemCustomIcons) ) {
				return SwitchContext(ctx, KdbContext.CustomIcons, xpp);
			} else if ( name.equalsIgnoreCase(ElemRecycleBinEnabled) ) {
				db.recycleBinEnabled = ReadBool(xpp, true);
			} else if ( name.equalsIgnoreCase(ElemRecycleBinUuid) ) {
				db.recycleBinUUID = ReadUuid(xpp);
			} else if ( name.equalsIgnoreCase(ElemRecycleBinChanged) ) {
				db.recycleBinChanged = ReadTime(xpp);
			} else if ( name.equalsIgnoreCase(ElemEntryTemplatesGroup) ) {
				db.entryTemplatesGroup = ReadUuid(xpp);
			} else if ( name.equalsIgnoreCase(ElemEntryTemplatesGroupChanged) ) {
				db.entryTemplatesGroupChanged = ReadTime(xpp);
			} else if ( name.equalsIgnoreCase(ElemHistoryMaxItems) ) {
				db.historyMaxItems = ReadInt(xpp, -1);
			} else if ( name.equalsIgnoreCase(ElemHistoryMaxSize) ) {
				db.historyMaxSize = ReadLong(xpp, -1);
			} else if ( name.equalsIgnoreCase(ElemEntryTemplatesGroupChanged) ) {
				db.entryTemplatesGroupChanged = ReadTime(xpp);
			} else if ( name.equalsIgnoreCase(ElemLastSelectedGroup) ) {
				db.lastSelectedGroup = ReadUuid(xpp);
			} else if ( name.equalsIgnoreCase(ElemLastTopVisibleGroup) ) {
				db.lastTopVisibleGroup = ReadUuid(xpp);
			} else if ( name.equalsIgnoreCase(ElemBinaries) ) {
				return SwitchContext(ctx, KdbContext.Binaries, xpp);
			} else if ( name.equalsIgnoreCase(ElemCustomData) ) {
				return SwitchContext(ctx, KdbContext.CustomData, xpp);
			}
			break;
			
		case MemoryProtection:
			if ( name.equalsIgnoreCase(ElemProtTitle) ) {
				db.memoryProtection.protectTitle = ReadBool(xpp, false);
			} else if ( name.equalsIgnoreCase(ElemProtUserName) ) {
				db.memoryProtection.protectUserName = ReadBool(xpp, false);
			} else if ( name.equalsIgnoreCase(ElemProtPassword) ) {
				db.memoryProtection.protectPassword = ReadBool(xpp, false);
			} else if ( name.equalsIgnoreCase(ElemProtURL) ) {
				db.memoryProtection.protectUrl = ReadBool(xpp, false);
			} else if ( name.equalsIgnoreCase(ElemProtNotes) ) {
				db.memoryProtection.protectNotes = ReadBool(xpp, false);
			} else if ( name.equalsIgnoreCase(ElemProtAutoHide) ) {
				db.memoryProtection.autoEnableVisualHiding = ReadBool(xpp, false);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case CustomIcons:
			if ( name.equalsIgnoreCase(ElemCustomIconItem) ) {
				return SwitchContext(ctx, KdbContext.CustomIcon, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case CustomIcon:
			if ( name.equalsIgnoreCase(ElemCustomIconItemID) ) {
				customIconID = ReadUuid(xpp);
			} else if ( name.equalsIgnoreCase(ElemCustomIconItemData) ) {
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
			if ( name.equalsIgnoreCase(ElemBinary) ) {
				String key = xpp.getAttributeValue(null, AttrId);
				if ( key != null ) {
					ProtectedBinary pbData = ReadProtectedBinary(xpp);
					binPool.put(key, pbData);
				} else {
					ReadUnknown(xpp);
				}
			} else {
				ReadUnknown(xpp);
			}
			
			break;

		case CustomData:
			if ( name.equalsIgnoreCase(ElemStringDictExItem) ) {
				return SwitchContext(ctx, KdbContext.CustomDataItem, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case CustomDataItem:
			if ( name.equalsIgnoreCase(ElemKey) ) {
				customDataKey = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemValue) ) {
				customDataValue = ReadString(xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case Root:
			if ( name.equalsIgnoreCase(ElemGroup) ) {
				assert(ctxGroups.size() == 0);
				if ( ctxGroups.size() != 0 ) throw new IOException("Group list should be empty.");
				
				db.rootGroup = new PwGroupV4();
				ctxGroups.push((PwGroupV4)db.rootGroup);
				ctxGroup = ctxGroups.peek();
				
				return SwitchContext(ctx, KdbContext.Group, xpp);
			} else if ( name.equalsIgnoreCase(ElemDeletedObjects) ) {
				return SwitchContext(ctx, KdbContext.RootDeletedObjects, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case Group:
			if ( name.equalsIgnoreCase(ElemUuid) ) {
				ctxGroup.uuid = ReadUuid(xpp);
			} else if ( name.equalsIgnoreCase(ElemName) ) {
				ctxGroup.name = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemNotes) ) {
				ctxGroup.notes = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemIcon) ) {
				ctxGroup.icon = db.iconFactory.getIcon((int)ReadUInt(xpp, 0));
			} else if ( name.equalsIgnoreCase(ElemCustomIconID) ) {
				ctxGroup.customIcon = db.iconFactory.getIcon(ReadUuid(xpp));
			} else if ( name.equalsIgnoreCase(ElemTimes) ) {
				return SwitchContext(ctx, KdbContext.GroupTimes, xpp);
			} else if ( name.equalsIgnoreCase(ElemIsExpanded) ) {
				ctxGroup.isExpanded = ReadBool(xpp, true);
			} else if ( name.equalsIgnoreCase(ElemGroupDefaultAutoTypeSeq) ) {
				ctxGroup.defaultAutoTypeSequence = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemEnableAutoType) ) {
				ctxGroup.enableAutoType = StringToBoolean(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(ElemEnableSearching) ) {
				ctxGroup.enableSearching = StringToBoolean(ReadString(xpp));
			} else if ( name.equalsIgnoreCase(ElemLastTopVisibleEntry) ) {
				ctxGroup.lastTopVisibleEntry = ReadUuid(xpp);
			} else if ( name.equalsIgnoreCase(ElemGroup) ) {
				ctxGroup = new PwGroupV4();
				ctxGroups.peek().AddGroup(ctxGroup, true);
				ctxGroups.push(ctxGroup);
				
				return SwitchContext(ctx, KdbContext.Group, xpp);
			} else if ( name.equalsIgnoreCase(ElemEntry) ) {
				ctxEntry = new PwEntryV4();
				ctxGroup.AddEntry(ctxEntry, true);
				
				entryInHistory = false;
				return SwitchContext(ctx, KdbContext.Entry, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case Entry:
			if ( name.equalsIgnoreCase(ElemUuid) ) {
				ctxEntry.setUUID(ReadUuid(xpp));
			} else if ( name.equalsIgnoreCase(ElemIcon) ) {
				ctxEntry.icon = db.iconFactory.getIcon((int)ReadUInt(xpp, 0));
			} else if ( name.equalsIgnoreCase(ElemCustomIconID) ) {
				ctxEntry.customIcon = db.iconFactory.getIcon(ReadUuid(xpp));
			} else if ( name.equalsIgnoreCase(ElemFgColor) ) {
				ctxEntry.foregroundColor = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemBgColor) ) {
				ctxEntry.backgroupColor = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemOverrideUrl) ) {
				ctxEntry.overrideURL = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemTags) ) {
				ctxEntry.tags = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemTimes) ) {
				return SwitchContext(ctx, KdbContext.EntryTimes, xpp);
			} else if ( name.equalsIgnoreCase(ElemString) ) {
				return SwitchContext(ctx, KdbContext.EntryString, xpp);
			} else if ( name.equalsIgnoreCase(ElemBinary) ) {
				return SwitchContext(ctx, KdbContext.EntryBinary, xpp);
			} else if ( name.equalsIgnoreCase(ElemAutoType) ) {
				return SwitchContext(ctx, KdbContext.EntryAutoType, xpp);
			} else if ( name.equalsIgnoreCase(ElemHistory) ) {
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
			
		case GroupTimes:
		case EntryTimes:
			ITimeLogger tl;
			if ( ctx == KdbContext.GroupTimes ) {
				tl = ctxGroup;
			} else {
				tl = ctxEntry;
			}
			
			if ( name.equalsIgnoreCase(ElemLastModTime) ) {
				tl.setLastModificationTime(ReadTime(xpp));
			} else if ( name.equalsIgnoreCase(ElemCreationTime) ) {
				tl.setCreationTime(ReadTime(xpp));
			} else if ( name.equalsIgnoreCase(ElemLastAccessTime) ) {
				tl.setLastAccessTime(ReadTime(xpp));
			} else if ( name.equalsIgnoreCase(ElemExpiryTime) ) {
				tl.setExpiryTime(ReadTime(xpp));
			} else if ( name.equalsIgnoreCase(ElemExpires) ) {
				tl.setExpires(ReadBool(xpp, false));
			} else if ( name.equalsIgnoreCase(ElemUsageCount) ) {
				tl.setUsageCount(ReadULong(xpp, 0));
			} else if ( name.equalsIgnoreCase(ElemLocationChanged) ) {
				tl.setLocationChanged(ReadTime(xpp));
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case EntryString:
			if ( name.equalsIgnoreCase(ElemKey) ) {
				ctxStringName = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemValue) ) {
				ctxStringValue = ReadProtectedString(xpp); 
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case EntryBinary:
			if ( name.equalsIgnoreCase(ElemKey) ) {
				ctxBinaryName = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemValue) ) {
				ctxBinaryValue = ReadProtectedBinary(xpp);
			}
			break;
			
		case EntryAutoType:
			if ( name.equalsIgnoreCase(ElemAutoTypeEnabled) ) {
				ctxEntry.autoType.enabled = ReadBool(xpp, true);
			} else if ( name.equalsIgnoreCase(ElemAutoTypeObfuscation) ) {
				ctxEntry.autoType.obfuscationOptions = ReadUInt(xpp, 0);
			} else if ( name.equalsIgnoreCase(ElemAutoTypeDefaultSeq) ) {
				ctxEntry.autoType.defaultSequence = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemAutoTypeItem) ) {
				return SwitchContext(ctx, KdbContext.EntryAutoTypeItem, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case EntryAutoTypeItem:
			if ( name.equalsIgnoreCase(ElemWindow) ) {
				ctxATName = ReadString(xpp);
			} else if ( name.equalsIgnoreCase(ElemKeystrokeSequence) ) {
				ctxATSeq = ReadString(xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case EntryHistory:
			if ( name.equalsIgnoreCase(ElemEntry) ) {
				ctxEntry = new PwEntryV4();
				ctxHistoryBase.history.add(ctxEntry);
				
				entryInHistory = true;
				return SwitchContext(ctx, KdbContext.Entry, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case RootDeletedObjects:
			if ( name.equalsIgnoreCase(ElemDeletedObject) ) {
				ctxDeletedObject = new PwDeletedObject();
				db.deletedObjects.add(ctxDeletedObject);
				
				return SwitchContext(ctx, KdbContext.DeletedObject, xpp);
			} else {
				ReadUnknown(xpp);
			}
			break;
			
		case DeletedObject:
			if ( name.equalsIgnoreCase(ElemUuid) ) {
				ctxDeletedObject.uuid = ReadUuid(xpp);
			} else if ( name.equalsIgnoreCase(ElemDeletionTime) ) {
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
		if ( ctx == KdbContext.KeePassFile && name.equalsIgnoreCase(ElemDocNode) ) {
			return KdbContext.Null;
		} else if ( ctx == KdbContext.Meta && name.equalsIgnoreCase(ElemMeta) ) {
			return KdbContext.KeePassFile;
		} else if ( ctx == KdbContext.Root && name.equalsIgnoreCase(ElemRoot) ) {
			return KdbContext.KeePassFile;
		} else if ( ctx == KdbContext.MemoryProtection && name.equalsIgnoreCase(ElemMemoryProt) ) {
			return KdbContext.Meta;
		} else if ( ctx == KdbContext.CustomIcons && name.equalsIgnoreCase(ElemCustomIcons) ) {
			return KdbContext.Meta;
		} else if ( ctx == KdbContext.CustomIcon && name.equalsIgnoreCase(ElemCustomIconItem) ) {
			if ( ! customIconID.equals(PwDatabaseV4.UUID_ZERO) ) {
				PwIconCustom icon = new PwIconCustom(customIconID, customIconData);
				db.customIcons.add(icon);
				db.iconFactory.put(icon);
			} else assert(false);
			
			customIconID = PwDatabaseV4.UUID_ZERO;
			customIconData = null;
			
			return KdbContext.CustomIcons;
		} else if ( ctx == KdbContext.Binaries && name.equalsIgnoreCase(ElemBinaries) ) {
			return KdbContext.Meta;
		} else if ( ctx == KdbContext.CustomData && name.equalsIgnoreCase(ElemCustomData) ) {
			return KdbContext.Meta;
		} else if ( ctx == KdbContext.CustomDataItem && name.equalsIgnoreCase(ElemStringDictExItem) ) {
			if ( customDataKey != null && customDataValue != null) {
				db.customData.put(customDataKey, customDataValue);
			} else assert(false);
			
			customDataKey = null;
			customDataValue = null;
			
			return KdbContext.CustomData;
		} else if ( ctx == KdbContext.Group && name.equalsIgnoreCase(ElemGroup) ) {
			if ( ctxGroup.uuid == null || ctxGroup.uuid.equals(PwDatabaseV4.UUID_ZERO) ) {
				ctxGroup.uuid = UUID.randomUUID();
			}
			
			ctxGroups.pop();
			
			if ( ctxGroups.size() == 0 ) {
				ctxGroup = null;
				return KdbContext.Root;
			} else {
				ctxGroup = ctxGroups.peek();
				return KdbContext.Group;
			}
		} else if ( ctx == KdbContext.GroupTimes && name.equalsIgnoreCase(ElemTimes) ) {
			return KdbContext.Group;
		} else if ( ctx == KdbContext.Entry && name.equalsIgnoreCase(ElemEntry) ) {
			if ( ctxEntry.uuid == null || ctxEntry.uuid.equals(PwDatabaseV4.UUID_ZERO) ) {
				ctxEntry.uuid = UUID.randomUUID();
			}
			
			if ( entryInHistory ) {
				ctxEntry = ctxHistoryBase;
				return KdbContext.EntryHistory;
			}
			
			return KdbContext.Group;
		} else if ( ctx == KdbContext.EntryTimes && name.equalsIgnoreCase(ElemTimes) ) {
			return KdbContext.Entry;
		} else if ( ctx == KdbContext.EntryString && name.equalsIgnoreCase(ElemString) ) {
			ctxEntry.strings.put(ctxStringName, ctxStringValue);
			ctxStringName = null;
			ctxStringValue = null;
			
			return KdbContext.Entry;
		} else if ( ctx == KdbContext.EntryBinary && name.equalsIgnoreCase(ElemBinary) ) {
			ctxEntry.binaries.put(ctxBinaryName, ctxBinaryValue);
			ctxBinaryName = null;
			ctxBinaryValue = null;
			
			return KdbContext.Entry;
		} else if ( ctx == KdbContext.EntryAutoType && name.equalsIgnoreCase(ElemAutoType) ) {
			return KdbContext.Entry;
		} else if ( ctx == KdbContext.EntryAutoTypeItem && name.equalsIgnoreCase(ElemAutoTypeItem) ) {
			ctxEntry.autoType.put(ctxATName, ctxATSeq);
			ctxATName = null;
			ctxATSeq = null;
			
			return KdbContext.EntryAutoType;
		} else if ( ctx == KdbContext.EntryHistory && name.equalsIgnoreCase(ElemHistory) ) {
			entryInHistory = false;
			return KdbContext.Entry;
		} else if ( ctx == KdbContext.RootDeletedObjects && name.equalsIgnoreCase(ElemDeletedObjects) ) {
			return KdbContext.Root;
		} else if ( ctx == KdbContext.DeletedObject && name.equalsIgnoreCase(ElemDeletedObject) ) {
			ctxDeletedObject = null;
			return KdbContext.RootDeletedObjects;
		} else {
			assert(false);
			
			throw new RuntimeException("Invalid end element");
		}
	}
	
	private Date ReadTime(XmlPullParser xpp) throws IOException, XmlPullParserException {
		String sDate = ReadString(xpp);
		
		Date utcDate;
		try {
			utcDate = PwDatabaseV4XML.dateFormat.parse(sDate);
		} catch (ParseException e) {
			utcDate = new Date(0L);
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
			return PwDatabaseV4.UUID_ZERO;
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
		String ref = xpp.getAttributeValue(null, AttrRef);
		if (ref != null) {
			xpp.next(); // Consume end tag
			
			return binPool.get(ref);
		} 
		
		boolean compressed = false;
		String comp = xpp.getAttributeValue(null, AttrCompressed);
		if (comp != null) {
			compressed = comp.equalsIgnoreCase(ValTrue);
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
			String protect = xpp.getAttributeValue(null, AttrProtected);
			if ( protect != null && protect.equalsIgnoreCase(ValTrue) ) {
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
