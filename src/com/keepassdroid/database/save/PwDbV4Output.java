/*
 * Copyright 2013 Brian Pellin.
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
package com.keepassdroid.database.save;

import static com.keepassdroid.database.PwDatabaseV4XML.*;

import java.io.IOException;
import java.io.OutputStream;
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

import org.bouncycastle.crypto.StreamCipher;
import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;
import biz.source_code.base64Coder.Base64Coder;

import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.crypto.PwStreamCipherFactory;
import com.keepassdroid.database.BinaryPool;
import com.keepassdroid.database.CrsAlgorithm;
import com.keepassdroid.database.EntryHandler;
import com.keepassdroid.database.GroupHandler;
import com.keepassdroid.database.ITimeLogger;
import com.keepassdroid.database.PwCompressionAlgorithm;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDatabaseV4.MemoryProtectionConfig;
import com.keepassdroid.database.PwDatabaseV4XML;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.PwDefsV4;
import com.keepassdroid.database.PwDeletedObject;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.PwEntryV4.AutoType;
import com.keepassdroid.database.PwGroupV4;
import com.keepassdroid.database.PwIconCustom;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.database.security.ProtectedBinary;
import com.keepassdroid.database.security.ProtectedString;
import com.keepassdroid.stream.HashedBlockOutputStream;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.MemUtil;
import com.keepassdroid.utils.Types;

public class PwDbV4Output extends PwDbOutput {

	PwDatabaseV4 mPM;
	private StreamCipher randomStream;
	private BinaryPool binPool;
	private XmlSerializer xml;
	private PwDbHeaderV4 header;
	private byte[] hashOfHeader;
	
	protected PwDbV4Output(PwDatabaseV4 pm, OutputStream os) {
		super(os);
		
		mPM = pm;
	}

	@Override
	public void output() throws PwDbOutputException {
		header = (PwDbHeaderV4 ) outputHeader(mOS);
		
		CipherOutputStream cos = attachStreamEncryptor(header, mOS);
		
		OutputStream compressed;
		try {
			cos.write(header.streamStartBytes);
		
			HashedBlockOutputStream hashed = new HashedBlockOutputStream(cos);
			
			if ( mPM.compressionAlgorithm == PwCompressionAlgorithm.Gzip ) {
				compressed = new GZIPOutputStream(hashed); 
			} else {
				compressed = hashed;
			}
			
		} catch (IOException e) {
			throw new PwDbOutputException("Failed to set up output stream.");
		}
	
		try {
			outputDatabase(compressed);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private class GroupWriter extends GroupHandler {
		private Stack<PwGroupV4> groupStack;
		
		public GroupWriter(Stack<PwGroupV4> gs) {
			groupStack = gs;
		}

		@Override
		public boolean operate(PwGroupV4 group) {
			assert(group != null);
			
			while(true) {
				try {
					if (group.parent == groupStack.peek()) {
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
	
	private class EntryWriter extends EntryHandler {

		@Override
		public boolean operate(PwEntryV4 entry) {
			assert(entry != null);
			
			try {
				writeEntry(entry, false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			return true;
		}
		
	}
	
	private void outputDatabase(OutputStream os) throws IllegalArgumentException, IllegalStateException, IOException {
		binPool = new BinaryPool((PwGroupV4)mPM.rootGroup);
		
		xml = Xml.newSerializer();
		
		xml.setOutput(os, "UTF-8");
		xml.startDocument("UTF-8", true);
		
		xml.startTag(null, ElemDocNode);
		
		writeMeta();
		
		PwGroupV4 root = (PwGroupV4) mPM.rootGroup;
		xml.startTag(null, ElemRoot);
		startGroup(root);
		Stack<PwGroupV4> groupStack = new Stack<PwGroupV4>();
		groupStack.push(root);
		
		if (!root.preOrderTraverseTree(new GroupWriter(groupStack), new EntryWriter())) throw new RuntimeException("Writing groups failed");
		
		while (groupStack.size() > 1) {
			xml.endTag(null, ElemGroup);
			groupStack.pop();
		}
		
		endGroup();
		
		writeList(ElemDeletedObjects, mPM.deletedObjects);
		
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
		
		writeObject(ElemDbName, mPM.name, true);
		writeObject(ElemDbNameChanged, mPM.nameChanged);
		writeObject(ElemDbDesc, mPM.description, true);
		writeObject(ElemDbDescChanged, mPM.descriptionChanged);
		writeObject(ElemDbDefaultUser, mPM.defaultUserName, true);
		writeObject(ElemDbDefaultUserChanged, mPM.defaultUserNameChanged);
		writeObject(ElemDbMntncHistoryDays, mPM.maintenanceHistoryDays);
		writeObject(ElemDbColor, mPM.color);
		writeObject(ElemDbKeyChanged, mPM.keyLastChanged);
		writeObject(ElemDbKeyChangeRec, mPM.keyChangeRecDays);
		writeObject(ElemDbKeyChangeForce, mPM.keyChangeForceDays);
		
		
		writeList(ElemMemoryProt, mPM.memoryProtection);
		
		writeCustomIconList();
		
		writeObject(ElemRecycleBinEnabled, mPM.recycleBinEnabled);
		writeObject(ElemRecycleBinUuid, mPM.recycleBinUUID);
		writeObject(ElemRecycleBinChanged, mPM.recycleBinChanged);
		writeObject(ElemEntryTemplatesGroup, mPM.entryTemplatesGroup);
		writeObject(ElemEntryTemplatesGroupChanged, mPM.entryTemplatesGroupChanged);
		writeObject(ElemHistoryMaxItems, mPM.historyMaxItems);
		writeObject(ElemHistoryMaxItems, mPM.historyMaxSize);
		writeObject(ElemLastSelectedGroup, mPM.lastSelectedGroup);
		writeObject(ElemLastTopVisibleGroup, mPM.lastTopVisibleGroup);
		
		writeBinPool();
		writeList(ElemCustomData, mPM.customData);
		
		xml.endTag(null, ElemMeta);
		
	}
	
	private CipherOutputStream attachStreamEncryptor(PwDbHeaderV4 header, OutputStream os) throws PwDbOutputException {
		Cipher cipher;
		try {
			mPM.makeFinalKey(header.masterSeed, header.transformSeed, (int)mPM.numKeyEncRounds);
			cipher = CipherFactory.getInstance(mPM.dataCipher, mPM.finalKey, header.encryptionIV);
		} catch (Exception e) {
			throw new PwDbOutputException("Invalid algorithm.");
		}
		
		CipherOutputStream cos = new CipherOutputStream(os, cipher);
		
		return cos;
	}

	@Override
	protected SecureRandom setIVs(PwDbHeader header) throws PwDbOutputException {
		SecureRandom random = super.setIVs(header);
		
		PwDbHeaderV4 h = (PwDbHeaderV4) header;
		random.nextBytes(h.masterSeed);
		random.nextBytes(h.transformSeed);
		random.nextBytes(h.encryptionIV);
		
		random.nextBytes(h.protectedStreamKey);
		h.innerRandomStream = CrsAlgorithm.Salsa20;
		randomStream = PwStreamCipherFactory.getInstance(h.innerRandomStream, h.protectedStreamKey);
		if (randomStream == null) {
			throw new PwDbOutputException("Invalid random cipher");
		}
		random.nextBytes(h.streamStartBytes);
		
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
			throw new PwDbOutputException("Failed to output the header.");
		}
		
		hashOfHeader = pho.getHashOfHeader();
		
		return header;
	}
	
	private void startGroup(PwGroupV4 group) throws IllegalArgumentException, IllegalStateException, IOException {
		xml.startTag(null, ElemGroup);
		writeObject(ElemUuid, group.uuid);
		writeObject(ElemName, group.name);
		writeObject(ElemNotes, group.notes);
		writeObject(ElemIcon, group.icon.iconId);
		
		if (!group.customIcon.equals(PwIconCustom.ZERO)) {
			writeObject(ElemCustomIconID, group.customIcon.uuid);
		}
		
		writeList(ElemTimes, group);
		writeObject(ElemIsExpanded, group.isExpanded);
		writeObject(ElemGroupDefaultAutoTypeSeq, group.defaultAutoTypeSequence);
		writeObject(ElemEnableAutoType, group.enableAutoType);
		writeObject(ElemEnableSearching, group.enableSearching);
		writeObject(ElemLastTopVisibleEntry, group.lastTopVisibleEntry);
		
	}
	
	private void endGroup() throws IllegalArgumentException, IllegalStateException, IOException {
		xml.endTag(null, ElemGroup);
	}
	
	private void writeEntry(PwEntryV4 entry, boolean isHistory) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(entry != null);
		
		xml.startTag(null, ElemEntry);
		
		writeObject(ElemUuid, entry.uuid);
		writeObject(ElemIcon, entry.icon.iconId);
		
		if (entry.customIcon.equals(PwIconCustom.ZERO)) {
			writeObject(ElemCustomIconID, entry.customIcon.uuid);
		}
		
		writeObject(ElemFgColor, entry.foregroundColor);
		writeObject(ElemBgColor, entry.backgroupColor);
		writeObject(ElemOverrideUrl, entry.overrideURL);
		writeObject(ElemTags, entry.tags);
		
		writeList(ElemTimes, entry);
		
		writeList(entry.strings, true);
		writeList(entry.binaries);
		writeList(ElemAutoType, entry.autoType);
		
		if (!isHistory) {
			writeList(ElemHistory, entry.history, true);
		} else {
			assert(entry.history.size() == 0);
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
			strRef = binPool.poolFind(value);
		}
		
		if (strRef != null) {
			xml.attribute(null, AttrRef, strRef);
		}
		else {
			subWriteValue(value);
		}
		
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
			if (mPM.compressionAlgorithm == PwCompressionAlgorithm.Gzip) {
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
		writeObject(name, PwDatabaseV4XML.dateFormat.format(value));
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
				protect = mPM.memoryProtection.protectTitle;
			}
			else if (key.equals(PwDefsV4.USERNAME_FIELD)) {
				protect = mPM.memoryProtection.protectUserName;
			}
			else if (key.equals(PwDefsV4.PASSWORD_FIELD)) {
				protect = mPM.memoryProtection.protectPassword;
			}
			else if (key.equals(PwDefsV4.URL_FIELD)) {
				protect = mPM.memoryProtection.protectUrl;
			}
			else if (key.equals(PwDefsV4.NOTES_FIELD)) {
				protect = mPM.memoryProtection.protectNotes;
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
			else {
				xml.text(value.toString());
			}
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
		
		writeObject(ElemLastModTime, it.getLastModificationTime());
		writeObject(ElemCreationTime, it.getCreationTime());
		writeObject(ElemLastAccessTime, it.getLastAccessTime());
		writeObject(ElemExpiryTime, it.getExpiryTime());
		writeObject(ElemExpires, it.expires());
		writeObject(ElemUsageCount, it.getUsageCount());
		writeObject(ElemLocationChanged, it.getLocationChanged());
		
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
		List<PwIconCustom> customIcons = mPM.customIcons;
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
		
		for (Entry<String, ProtectedBinary> pair : binPool.entrySet()) {
			xml.startTag(null, ElemBinary);
			xml.attribute(null, AttrId, pair.getKey());
			
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
