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
package com.keepassdroid.database.save;

import android.util.Xml;

import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.crypto.PwStreamCipherFactory;
import com.keepassdroid.crypto.engine.CipherEngine;
import com.keepassdroid.crypto.keyDerivation.KdfEngine;
import com.keepassdroid.crypto.keyDerivation.KdfFactory;
import com.keepassdroid.database.AutoType;
import com.keepassdroid.database.CrsAlgorithm;
import com.keepassdroid.database.EntryHandler;
import com.keepassdroid.database.GroupHandler;
import com.keepassdroid.database.ITimeLogger;
import com.keepassdroid.database.MemoryProtectionConfig;
import com.keepassdroid.database.PwCompressionAlgorithm;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDatabaseV4XML;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.PwDefsV4;
import com.keepassdroid.database.PwDeletedObject;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.PwGroupV4;
import com.keepassdroid.database.PwIconCustom;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.security.ProtectedBinary;
import com.keepassdroid.database.security.ProtectedString;
import com.keepassdroid.stream.HashedBlockOutputStream;
import com.keepassdroid.stream.HmacBlockOutputStream;
import com.keepassdroid.stream.LEDataOutputStream;
import com.keepassdroid.utils.DateUtil;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.MemUtil;
import com.keepassdroid.utils.Types;

import org.joda.time.DateTime;
import org.spongycastle.crypto.StreamCipher;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import biz.source_code.base64Coder.Base64Coder;

import static com.keepassdroid.database.PwDatabaseV4XML.AttrCompressed;
import static com.keepassdroid.database.PwDatabaseV4XML.AttrId;
import static com.keepassdroid.database.PwDatabaseV4XML.AttrProtected;
import static com.keepassdroid.database.PwDatabaseV4XML.AttrRef;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemAutoType;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemAutoTypeDefaultSeq;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemAutoTypeEnabled;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemAutoTypeItem;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemAutoTypeObfuscation;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemBgColor;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemBinaries;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemBinary;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemCreationTime;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemCustomData;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemCustomIconID;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemCustomIconItem;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemCustomIconItemData;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemCustomIconItemID;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemCustomIcons;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDbColor;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDbDefaultUser;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDbDefaultUserChanged;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDbDesc;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDbDescChanged;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDbKeyChangeForce;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDbKeyChangeRec;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDbKeyChanged;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDbMntncHistoryDays;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDbName;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDbNameChanged;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDeletedObject;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDeletedObjects;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDeletionTime;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemDocNode;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemEnableAutoType;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemEnableSearching;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemEntry;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemEntryTemplatesGroup;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemEntryTemplatesGroupChanged;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemExpires;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemExpiryTime;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemFgColor;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemGenerator;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemGroup;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemGroupDefaultAutoTypeSeq;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemHeaderHash;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemHistory;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemHistoryMaxItems;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemHistoryMaxSize;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemIcon;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemIsExpanded;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemKey;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemKeystrokeSequence;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemLastAccessTime;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemLastModTime;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemLastSelectedGroup;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemLastTopVisibleEntry;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemLastTopVisibleGroup;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemLocationChanged;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemMemoryProt;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemMeta;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemName;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemNotes;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemOverrideUrl;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemProtNotes;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemProtPassword;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemProtTitle;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemProtURL;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemProtUserName;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemRecycleBinChanged;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemRecycleBinEnabled;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemRecycleBinUuid;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemRoot;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemString;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemStringDictExItem;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemTags;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemTimes;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemUsageCount;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemUuid;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemValue;
import static com.keepassdroid.database.PwDatabaseV4XML.ElemWindow;
import static com.keepassdroid.database.PwDatabaseV4XML.ValFalse;
import static com.keepassdroid.database.PwDatabaseV4XML.ValTrue;

public class PwDbV4Output extends PwDbOutput {

	PwDatabaseV4 mPM;
	private StreamCipher randomStream;
	private XmlSerializer xml;
	private PwDbHeaderV4 header;
	private byte[] hashOfHeader;
	private byte[] headerHmac;
    private CipherEngine engine = null;

	protected PwDbV4Output(PwDatabaseV4 pm, OutputStream os) {
		super(os);
		
		mPM = pm;
	}

	@Override
	public void output() throws PwDbOutputException {

        try {
			try {
				engine = CipherFactory.getInstance(mPM.getDataCipher());
			} catch (NoSuchAlgorithmException e) {
				throw new PwDbOutputException("No such cipher", e);
			}

			header = (PwDbHeaderV4) outputHeader(mOS);

			OutputStream osPlain;
			if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
				CipherOutputStream cos = attachStreamEncryptor(header, mOS);
				cos.write(header.streamStartBytes);

				HashedBlockOutputStream hashed = new HashedBlockOutputStream(cos);
				osPlain = hashed;
			} else {
				mOS.write(hashOfHeader);
				mOS.write(headerHmac);

				HmacBlockOutputStream hbos = new HmacBlockOutputStream(mOS, mPM.getHmacKey());
				osPlain = attachStreamEncryptor(header, hbos);
			}

			OutputStream osXml;
			try {
				if (mPM.getCompressionAlgorithm() == PwCompressionAlgorithm.Gzip) {
					osXml = new GZIPOutputStream(osPlain);
				} else {
					osXml = osPlain;
				}

				if (header.version >= PwDbHeaderV4.FILE_VERSION_32_4) {
					PwDbInnerHeaderOutputV4 ihOut =  new PwDbInnerHeaderOutputV4(mPM, header, osXml);
                    ihOut.output();
				}

				outputDatabase(osXml);
				osXml.close();
			} catch (IllegalArgumentException e) {
				throw new PwDbOutputException(e);
			} catch (IllegalStateException e) {
				throw new PwDbOutputException(e);
			}
		} catch (IOException e) {
			throw new PwDbOutputException(e);
		}
	}
	
	private class GroupWriter extends GroupHandler<PwGroupV4> {
		private Stack<PwGroupV4> groupStack;
		
		public GroupWriter(Stack<PwGroupV4> gs) {
			groupStack = gs;
		}

		@Override
		public boolean operate(PwGroupV4 group) {
			assert(group != null);
			
			while(true) {
				try {
					if (group.getParent() == groupStack.peek()) {
						groupStack.push(group);
						startGroup(group);
						break;
					} else {
						groupStack.pop();
						if (groupStack.size() <= 0) return false;
						endGroup();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			
			return true;
		}
	}
	
	private class EntryWriter extends EntryHandler<PwEntryV4> {

		@Override
		public boolean operate(PwEntryV4 entry) {
			assert(entry != null);
			
			try {
				writeEntry(entry, false);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			
			return true;
		}
		
	}
	
	private void outputDatabase(OutputStream os) throws IllegalArgumentException, IllegalStateException, IOException {

		xml = Xml.newSerializer();
		
		xml.setOutput(os, "UTF-8");
		xml.startDocument("UTF-8", true);
		
		xml.startTag(null, ElemDocNode);
		
		writeMeta();
		
		PwGroupV4 root = mPM.getRootGroup();
		xml.startTag(null, ElemRoot);
		startGroup(root);
		Stack<PwGroupV4> groupStack = new Stack<>();
		groupStack.push(root);
		
		if (!root.preOrderTraverseTree(new GroupWriter(groupStack), new EntryWriter()))
			throw new RuntimeException("Writing groups failed");
		
		while (groupStack.size() > 1) {
			xml.endTag(null, ElemGroup);
			groupStack.pop();
		}
		
		endGroup();
		
		writeList(ElemDeletedObjects, mPM.getDeletedObjects());
		
		xml.endTag(null, ElemRoot);
		
		xml.endTag(null, ElemDocNode);
		xml.endDocument();
		
	}
	
	private void writeMeta() throws IllegalArgumentException, IllegalStateException, IOException {
		xml.startTag(null, ElemMeta);
		
		writeObject(ElemGenerator, mPM.localizedAppName);
		
		if (hashOfHeader != null) {
			writeObject(ElemHeaderHash, String.valueOf(Base64Coder.encode(hashOfHeader)));
		}
		
		writeObject(ElemDbName, mPM.getName(), true);
		writeObject(ElemDbNameChanged, mPM.getNameChanged().getDate());
		writeObject(ElemDbDesc, mPM.getDescription(), true);
		writeObject(ElemDbDescChanged, mPM.getDescriptionChanged().getDate());
		writeObject(ElemDbDefaultUser, mPM.getDefaultUserName(), true);
		writeObject(ElemDbDefaultUserChanged, mPM.getDefaultUserNameChanged().getDate());
		writeObject(ElemDbMntncHistoryDays, mPM.getMaintenanceHistoryDays());
		writeObject(ElemDbColor, mPM.getColor());
		writeObject(ElemDbKeyChanged, mPM.getKeyLastChanged().getDate());
		writeObject(ElemDbKeyChangeRec, mPM.getKeyChangeRecDays());
		writeObject(ElemDbKeyChangeForce, mPM.getKeyChangeForceDays());
		
		writeList(ElemMemoryProt, mPM.getMemoryProtection());
		
		writeCustomIconList();
		
		writeObject(ElemRecycleBinEnabled, mPM.isRecycleBinEnabled());
		writeObject(ElemRecycleBinUuid, mPM.getRecycleBinUUID());
		writeObject(ElemRecycleBinChanged, mPM.getRecycleBinChanged());
		writeObject(ElemEntryTemplatesGroup, mPM.getEntryTemplatesGroup());
		writeObject(ElemEntryTemplatesGroupChanged, mPM.getEntryTemplatesGroupChanged().getDate());
		writeObject(ElemHistoryMaxItems, mPM.getHistoryMaxItems());
		writeObject(ElemHistoryMaxSize, mPM.getHistoryMaxSize());
		writeObject(ElemLastSelectedGroup, mPM.getLastSelectedGroup());
		writeObject(ElemLastTopVisibleGroup, mPM.getLastTopVisibleGroup());

		if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
			writeBinPool();
		}
		writeList(ElemCustomData, mPM.getCustomData());
		
		xml.endTag(null, ElemMeta);
		
	}
	
	private CipherOutputStream attachStreamEncryptor(PwDbHeaderV4 header, OutputStream os) throws PwDbOutputException {
		Cipher cipher;
		try {
			//mPM.makeFinalKey(header.masterSeed, mPM.kdfParameters);

			cipher = engine.getCipher(Cipher.ENCRYPT_MODE, mPM.getFinalKey(), header.encryptionIV);
		} catch (Exception e) {
			throw new PwDbOutputException("Invalid algorithm.", e);
		}
		
		CipherOutputStream cos = new CipherOutputStream(os, cipher);
		
		return cos;
	}

	@Override
	protected SecureRandom setIVs(PwDbHeader header) throws PwDbOutputException {
		SecureRandom random = super.setIVs(header);
		
		PwDbHeaderV4 h = (PwDbHeaderV4) header;
		random.nextBytes(h.masterSeed);

		int ivLength = engine.ivLength();
		if (ivLength != h.encryptionIV.length) {
			h.encryptionIV = new byte[ivLength];
		}
		random.nextBytes(h.encryptionIV);

		UUID kdfUUID = mPM.getKdfParameters().kdfUUID;
		KdfEngine kdf = KdfFactory.get(kdfUUID);
		kdf.randomize(mPM.getKdfParameters());

		if (h.version < PwDbHeaderV4.FILE_VERSION_32_4) {
			h.innerRandomStream = CrsAlgorithm.Salsa20;
            h.innerRandomStreamKey = new byte[32];
		} else {
			h.innerRandomStream = CrsAlgorithm.ChaCha20;
			h.innerRandomStreamKey = new byte[64];
		}
		random.nextBytes(h.innerRandomStreamKey);

		randomStream = PwStreamCipherFactory.getInstance(h.innerRandomStream, h.innerRandomStreamKey);
		if (randomStream == null) {
			throw new PwDbOutputException("Invalid random cipher");
		}

		if ( h.version < PwDbHeaderV4.FILE_VERSION_32_4) {
			random.nextBytes(h.streamStartBytes);
		}
		
		return random;
	}
	
	@Override
	public PwDbHeader outputHeader(OutputStream os) throws PwDbOutputException {
		PwDbHeaderV4 header = new PwDbHeaderV4(mPM);
		setIVs(header);

		PwDbHeaderOutputV4 pho = new PwDbHeaderOutputV4(mPM, header, os);
		try {
			pho.output();
		} catch (IOException e) {
			throw new PwDbOutputException("Failed to output the header.", e);
		}
		
		hashOfHeader = pho.getHashOfHeader();
		headerHmac = pho.headerHmac;
		
		return header;
	}
	
	private void startGroup(PwGroupV4 group) throws IllegalArgumentException, IllegalStateException, IOException {
		xml.startTag(null, ElemGroup);
		writeObject(ElemUuid, group.getUUID());
		writeObject(ElemName, group.getName());
		writeObject(ElemNotes, group.getNotes());
		writeObject(ElemIcon, group.getIconStandard().iconId);
		
		if (!group.getCustomIcon().equals(PwIconCustom.ZERO)) {
			writeObject(ElemCustomIconID, group.getCustomIcon().uuid);
		}
		
		writeList(ElemTimes, group);
		writeObject(ElemIsExpanded, group.isExpanded());
		writeObject(ElemGroupDefaultAutoTypeSeq, group.getDefaultAutoTypeSequence());
		writeObject(ElemEnableAutoType, group.getEnableAutoType());
		writeObject(ElemEnableSearching, group.getEnableSearching());
		writeObject(ElemLastTopVisibleEntry, group.getLastTopVisibleEntry());
		
	}
	
	private void endGroup() throws IllegalArgumentException, IllegalStateException, IOException {
		xml.endTag(null, ElemGroup);
	}
	
	private void writeEntry(PwEntryV4 entry, boolean isHistory) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(entry != null);
		
		xml.startTag(null, ElemEntry);
		
		writeObject(ElemUuid, entry.getUUID());
		writeObject(ElemIcon, entry.getIconStandard().iconId);
		
		if (!entry.getCustomIcon().equals(PwIconCustom.ZERO)) {
			writeObject(ElemCustomIconID, entry.getCustomIcon().uuid);
		}
		
		writeObject(ElemFgColor, entry.getForegroundColor());
		writeObject(ElemBgColor, entry.getBackgroupColor());
		writeObject(ElemOverrideUrl, entry.getOverrideURL());
		writeObject(ElemTags, entry.getTags());
		
		writeList(ElemTimes, entry);
		
		writeList(entry.getFields().getListOfAllFields(), true);
		writeList(entry.getBinaries());
		writeList(ElemAutoType, entry.getAutoType());
		
		if (!isHistory) {
			writeList(ElemHistory, entry.getHistory(), true);
		} else {
			assert(entry.sizeOfHistory() == 0);
		}
		
		xml.endTag(null, ElemEntry);
	}
	

	private void writeObject(String key, ProtectedBinary value, boolean allowRef) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(key != null && value != null);
		
		xml.startTag(null, ElemBinary);
		xml.startTag(null, ElemKey);
		xml.text(safeXmlString(key));
		xml.endTag(null, ElemKey);
		
		xml.startTag(null, ElemValue);
		String strRef = null;
		if (allowRef) {
			int ref = mPM.getBinPool().poolFind(value);
			strRef = Integer.toString(ref);
		}
		
		if (strRef != null) {
			xml.attribute(null, AttrRef, strRef);
		}
		else {
			subWriteValue(value);
		}
		xml.endTag(null, ElemValue);
		
		xml.endTag(null, ElemBinary);
	}
	
	private void subWriteValue(ProtectedBinary value) throws IllegalArgumentException, IllegalStateException, IOException {
		if (value.isProtected()) {
			xml.attribute(null, AttrProtected, ValTrue);
			
			int valLength = value.length();
			if (valLength > 0) {
				byte[] encoded = new byte[valLength];
				randomStream.processBytes(value.getData(), 0, valLength, encoded, 0);
				
				xml.text(String.valueOf(Base64Coder.encode(encoded)));
			}
			
		} else {
			if (mPM.getCompressionAlgorithm() == PwCompressionAlgorithm.Gzip) {
				xml.attribute(null, AttrCompressed, ValTrue);
				byte[] raw = value.getData();
				byte[] compressed = MemUtil.compress(raw);
				xml.text(String.valueOf(Base64Coder.encode(compressed)));
			} else {
				byte[] raw = value.getData();
				xml.text(String.valueOf(Base64Coder.encode(raw)));
			}
			
		}
	}
	
	private void writeObject(String name, String value, boolean filterXmlChars) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && value != null);
		
		xml.startTag(null, name);
		
		if (filterXmlChars) {
			value = safeXmlString(value);
		}
		
		xml.text(value);
		xml.endTag(null, name);
	}
	
	private void writeObject(String name, String value) throws IllegalArgumentException, IllegalStateException, IOException {
		writeObject(name, value, false);
	}
	
	private void writeObject(String name, Date value) throws IllegalArgumentException, IllegalStateException, IOException {
		if (header.version < PwDbHeaderV4.FILE_VERSION_32_4) {
			writeObject(name, PwDatabaseV4XML.dateFormatter.get().format(value));
		} else {
			DateTime dt = new DateTime(value);
			long seconds = DateUtil.convertDateToKDBX4Time(dt);
			byte[] buf = LEDataOutputStream.writeLongBuf(seconds);
			String b64 = new String(Base64Coder.encode(buf));
			writeObject(name, b64);
		}

	}
	
	private void writeObject(String name, long value) throws IllegalArgumentException, IllegalStateException, IOException {
		writeObject(name, String.valueOf(value));
	}
	
	private void writeObject(String name, Boolean value) throws IllegalArgumentException, IllegalStateException, IOException {
		String text;
		if (value == null) {
			text = "null";
		}
		else if (value) {
			text = ValTrue;
		}
		else {
			text = ValFalse;
		}
		
		writeObject(name, text);
	}
	
	private void writeObject(String name, UUID uuid) throws IllegalArgumentException, IllegalStateException, IOException {
		byte[] data = Types.UUIDtoBytes(uuid);
		writeObject(name, String.valueOf(Base64Coder.encode(data)));
	}
	
	private void writeObject(String name, String keyName, String keyValue, String valueName, String valueValue) throws IllegalArgumentException, IllegalStateException, IOException {
		xml.startTag(null, name);
		
		xml.startTag(null, keyName);
		xml.text(safeXmlString(keyValue));
		xml.endTag(null, keyName);
		
		xml.startTag(null, valueName);
		xml.text(safeXmlString(valueValue));
		xml.endTag(null, valueName);
		
		xml.endTag(null, name);
	}
	
	private void writeList(String name, AutoType autoType) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && autoType != null);
		
		xml.startTag(null, name);
		
		writeObject(ElemAutoTypeEnabled, autoType.enabled);
		writeObject(ElemAutoTypeObfuscation, autoType.obfuscationOptions);
		
		if (autoType.defaultSequence.length() > 0) {
			writeObject(ElemAutoTypeDefaultSeq, autoType.defaultSequence, true);
		}
		
		for (Entry<String, String> pair : autoType.entrySet()) {
			writeObject(ElemAutoTypeItem, ElemWindow, pair.getKey(), ElemKeystrokeSequence, pair.getValue());
		}
		
		xml.endTag(null, name);
		
	}

	private void writeList(Map<String, ProtectedString> strings, boolean isEntryString) throws IllegalArgumentException, IllegalStateException, IOException {
		assert (strings != null);
		
		for (Entry<String, ProtectedString> pair : strings.entrySet()) {
			writeObject(pair.getKey(), pair.getValue(), isEntryString);
			
		}
		
	}

	private void writeObject(String key, ProtectedString value, boolean isEntryString) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(key !=null && value != null);
		
		xml.startTag(null, ElemString);
		xml.startTag(null, ElemKey);
		xml.text(safeXmlString(key));
		xml.endTag(null, ElemKey);
		
		xml.startTag(null, ElemValue);
		boolean protect = value.isProtected();
		if (isEntryString) {
			if (key.equals(PwDefsV4.TITLE_FIELD)) {
				protect = mPM.getMemoryProtection().protectTitle;
			}
			else if (key.equals(PwDefsV4.USERNAME_FIELD)) {
				protect = mPM.getMemoryProtection().protectUserName;
			}
			else if (key.equals(PwDefsV4.PASSWORD_FIELD)) {
				protect = mPM.getMemoryProtection().protectPassword;
			}
			else if (key.equals(PwDefsV4.URL_FIELD)) {
				protect = mPM.getMemoryProtection().protectUrl;
			}
			else if (key.equals(PwDefsV4.NOTES_FIELD)) {
				protect = mPM.getMemoryProtection().protectNotes;
			}
		}
		
		if (protect) {
			xml.attribute(null, AttrProtected, ValTrue);
			
			byte[] data = value.toString().getBytes("UTF-8");
			int valLength = data.length;
			
			if (valLength > 0) {
				byte[] encoded = new byte[valLength];
				randomStream.processBytes(data, 0, valLength, encoded, 0);
				xml.text(String.valueOf(Base64Coder.encode(encoded)));
			}
		}
		else {
			xml.text(safeXmlString(value.toString()));
		}
		
		xml.endTag(null, ElemValue);
		xml.endTag(null, ElemString);
		
	}

	private void writeObject(String name, PwDeletedObject value) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && value != null);
		
		xml.startTag(null, name);
		
		writeObject(ElemUuid, value.uuid);
		writeObject(ElemDeletionTime, value.getDeletionTime());
		
		xml.endTag(null, name);
	}

	private void writeList(Map<String, ProtectedBinary> binaries) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(binaries != null);
		
		for (Entry<String, ProtectedBinary> pair : binaries.entrySet()) {
			writeObject(pair.getKey(), pair.getValue(), true);
		}
	}


	private void writeList(String name, List<PwDeletedObject> value) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && value != null);
		
		xml.startTag(null, name);
		
		for (PwDeletedObject pdo : value) {
			writeObject(ElemDeletedObject, pdo);
		}
		
		xml.endTag(null, name);
		
	}

	private void writeList(String name, MemoryProtectionConfig value) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && value != null);
		
		xml.startTag(null, name);
		
		writeObject(ElemProtTitle, value.protectTitle);
		writeObject(ElemProtUserName, value.protectUserName);
		writeObject(ElemProtPassword, value.protectPassword);
		writeObject(ElemProtURL, value.protectUrl);
		writeObject(ElemProtNotes, value.protectNotes);
		
		xml.endTag(null, name);
		
	}
	
	private void writeList(String name, Map<String, String> customData) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && customData != null);
		
		xml.startTag(null, name);
		
		for (Entry<String, String> pair : customData.entrySet()) {
			writeObject(ElemStringDictExItem, ElemKey, pair.getKey(), ElemValue, pair.getValue());
			  
		}
		
		xml.endTag(null, name);
		
	}
	
	private void writeList(String name, ITimeLogger it) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && it != null);
		
		xml.startTag(null, name);
		
		writeObject(ElemLastModTime, it.getLastModificationTime().getDate());
		writeObject(ElemCreationTime, it.getCreationTime().getDate());
		writeObject(ElemLastAccessTime, it.getLastAccessTime().getDate());
		writeObject(ElemExpiryTime, it.getExpiryTime().getDate());
		writeObject(ElemExpires, it.isExpires());
		writeObject(ElemUsageCount, it.getUsageCount());
		writeObject(ElemLocationChanged, it.getLocationChanged().getDate());
		
		xml.endTag(null, name);
	}

	private void writeList(String name, List<PwEntryV4> value, boolean isHistory) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && value != null);
		
		xml.startTag(null, name);
		
		for (PwEntryV4 entry : value) {
			writeEntry(entry, isHistory);
		}
		
		xml.endTag(null, name);
		
	}

	private void writeCustomIconList() throws IllegalArgumentException, IllegalStateException, IOException {
		List<PwIconCustom> customIcons = mPM.getCustomIcons();
		if (customIcons.size() == 0) return;
		
		xml.startTag(null, ElemCustomIcons);
		
		for (PwIconCustom icon : customIcons) {
			xml.startTag(null, ElemCustomIconItem);
			
			writeObject(ElemCustomIconItemID, icon.uuid);
			writeObject(ElemCustomIconItemData, String.valueOf(Base64Coder.encode(icon.imageData)));
			
			xml.endTag(null, ElemCustomIconItem);
		}
		
		xml.endTag(null, ElemCustomIcons);
	}
	
	private void writeBinPool() throws IllegalArgumentException, IllegalStateException, IOException {
		xml.startTag(null, ElemBinaries);
		
		for (Entry<Integer, ProtectedBinary> pair : mPM.getBinPool().entrySet()) {
			xml.startTag(null, ElemBinary);
			xml.attribute(null, AttrId, Integer.toString(pair.getKey()));
			
			subWriteValue(pair.getValue());
			
			xml.endTag(null, ElemBinary);
			
		}
		
		xml.endTag(null, ElemBinaries);
		
	}

	private String safeXmlString(String text) {
		if (EmptyUtils.isNullOrEmpty(text)) {
			return text;
		}
		
		StringBuilder sb = new StringBuilder();
		
		char ch;
		for (int i = 0; i < text.length(); i++) {
			ch = text.charAt(i);
			
			if(((ch >= 0x20) && (ch <= 0xD7FF)) ||              
			        (ch == 0x9) || (ch == 0xA) || (ch == 0xD) ||
			        ((ch >= 0xE000) && (ch <= 0xFFFD))) {
				
				sb.append(ch);
			}

		}
		
		return sb.toString();
	}

}
