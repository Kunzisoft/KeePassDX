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
package com.kunzisoft.keepass.database.save;

import android.util.Log;
import android.util.Xml;

import com.kunzisoft.keepass.crypto.CipherFactory;
import com.kunzisoft.keepass.crypto.PwStreamCipherFactory;
import com.kunzisoft.keepass.crypto.engine.CipherEngine;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfEngine;
import com.kunzisoft.keepass.crypto.keyDerivation.KdfFactory;
import com.kunzisoft.keepass.database.AutoType;
import com.kunzisoft.keepass.database.CrsAlgorithm;
import com.kunzisoft.keepass.database.EntryHandler;
import com.kunzisoft.keepass.database.GroupHandler;
import com.kunzisoft.keepass.database.ITimeLogger;
import com.kunzisoft.keepass.database.MemoryProtectionConfig;
import com.kunzisoft.keepass.database.PwCompressionAlgorithm;
import com.kunzisoft.keepass.database.PwDatabaseV4;
import com.kunzisoft.keepass.database.PwDatabaseV4XML;
import com.kunzisoft.keepass.database.PwDbHeaderV4;
import com.kunzisoft.keepass.database.PwDefsV4;
import com.kunzisoft.keepass.database.PwDeletedObject;
import com.kunzisoft.keepass.database.PwEntryV4;
import com.kunzisoft.keepass.database.PwGroupV4;
import com.kunzisoft.keepass.database.PwIconCustom;
import com.kunzisoft.keepass.database.exception.PwDbOutputException;
import com.kunzisoft.keepass.database.exception.UnknownKDF;
import com.kunzisoft.keepass.database.security.ProtectedBinary;
import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.stream.HashedBlockOutputStream;
import com.kunzisoft.keepass.stream.HmacBlockOutputStream;
import com.kunzisoft.keepass.stream.LEDataOutputStream;
import com.kunzisoft.keepass.utils.DateUtil;
import com.kunzisoft.keepass.utils.EmptyUtils;
import com.kunzisoft.keepass.utils.MemUtil;
import com.kunzisoft.keepass.utils.Types;

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

public class PwDbV4Output extends PwDbOutput<PwDbHeaderV4> {
    private static final String TAG = PwDbV4Output.class.getName();

	private PwDatabaseV4 mPM;
	private StreamCipher randomStream;
	private XmlSerializer xml;
	private PwDbHeaderV4 header;
	private byte[] hashOfHeader;
	private byte[] headerHmac;
    private CipherEngine engine = null;

	protected PwDbV4Output(PwDatabaseV4 pm, OutputStream os) {
		super(os);
		this.mPM = pm;
	}

	@Override
	public void output() throws PwDbOutputException {

        try {
			try {
				engine = CipherFactory.getInstance(mPM.getDataCipher());
			} catch (NoSuchAlgorithmException e) {
				throw new PwDbOutputException("No such cipher", e);
			}

			header = outputHeader(mOS);

			OutputStream osPlain;
			if (header.getVersion() < PwDbHeaderV4.FILE_VERSION_32_4) {
				CipherOutputStream cos = attachStreamEncryptor(header, mOS);
				cos.write(header.streamStartBytes);

				osPlain = new HashedBlockOutputStream(cos);
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

				if (header.getVersion() >= PwDbHeaderV4.FILE_VERSION_32_4) {
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
		
		xml.startTag(null, PwDatabaseV4XML.ElemDocNode);
		
		writeMeta();
		
		PwGroupV4 root = mPM.getRootGroup();
		xml.startTag(null, PwDatabaseV4XML.ElemRoot);
		startGroup(root);
		Stack<PwGroupV4> groupStack = new Stack<>();
		groupStack.push(root);
		
		if (!root.preOrderTraverseTree(new GroupWriter(groupStack), new EntryWriter()))
			throw new RuntimeException("Writing groups failed");
		
		while (groupStack.size() > 1) {
			xml.endTag(null, PwDatabaseV4XML.ElemGroup);
			groupStack.pop();
		}
		
		endGroup();
		
		writeList(PwDatabaseV4XML.ElemDeletedObjects, mPM.getDeletedObjects());
		
		xml.endTag(null, PwDatabaseV4XML.ElemRoot);
		
		xml.endTag(null, PwDatabaseV4XML.ElemDocNode);
		xml.endDocument();
		
	}
	
	private void writeMeta() throws IllegalArgumentException, IllegalStateException, IOException {
		xml.startTag(null, PwDatabaseV4XML.ElemMeta);
		
		writeObject(PwDatabaseV4XML.ElemGenerator, mPM.localizedAppName);
		
		if (hashOfHeader != null) {
			writeObject(PwDatabaseV4XML.ElemHeaderHash, String.valueOf(Base64Coder.encode(hashOfHeader)));
		}
		
		writeObject(PwDatabaseV4XML.ElemDbName, mPM.getName(), true);
		writeObject(PwDatabaseV4XML.ElemDbNameChanged, mPM.getNameChanged().getDate());
		writeObject(PwDatabaseV4XML.ElemDbDesc, mPM.getDescription(), true);
		writeObject(PwDatabaseV4XML.ElemDbDescChanged, mPM.getDescriptionChanged().getDate());
		writeObject(PwDatabaseV4XML.ElemDbDefaultUser, mPM.getDefaultUserName(), true);
		writeObject(PwDatabaseV4XML.ElemDbDefaultUserChanged, mPM.getDefaultUserNameChanged().getDate());
		writeObject(PwDatabaseV4XML.ElemDbMntncHistoryDays, mPM.getMaintenanceHistoryDays());
		writeObject(PwDatabaseV4XML.ElemDbColor, mPM.getColor());
		writeObject(PwDatabaseV4XML.ElemDbKeyChanged, mPM.getKeyLastChanged().getDate());
		writeObject(PwDatabaseV4XML.ElemDbKeyChangeRec, mPM.getKeyChangeRecDays());
		writeObject(PwDatabaseV4XML.ElemDbKeyChangeForce, mPM.getKeyChangeForceDays());
		
		writeList(PwDatabaseV4XML.ElemMemoryProt, mPM.getMemoryProtection());
		
		writeCustomIconList();
		
		writeObject(PwDatabaseV4XML.ElemRecycleBinEnabled, mPM.isRecycleBinEnabled());
		writeObject(PwDatabaseV4XML.ElemRecycleBinUuid, mPM.getRecycleBinUUID());
		writeObject(PwDatabaseV4XML.ElemRecycleBinChanged, mPM.getRecycleBinChanged());
		writeObject(PwDatabaseV4XML.ElemEntryTemplatesGroup, mPM.getEntryTemplatesGroup());
		writeObject(PwDatabaseV4XML.ElemEntryTemplatesGroupChanged, mPM.getEntryTemplatesGroupChanged().getDate());
		writeObject(PwDatabaseV4XML.ElemHistoryMaxItems, mPM.getHistoryMaxItems());
		writeObject(PwDatabaseV4XML.ElemHistoryMaxSize, mPM.getHistoryMaxSize());
		writeObject(PwDatabaseV4XML.ElemLastSelectedGroup, mPM.getLastSelectedGroup());
		writeObject(PwDatabaseV4XML.ElemLastTopVisibleGroup, mPM.getLastTopVisibleGroup());

		if (header.getVersion() < PwDbHeaderV4.FILE_VERSION_32_4) {
			writeBinPool();
		}
		writeList(PwDatabaseV4XML.ElemCustomData, mPM.getCustomData());
		
		xml.endTag(null, PwDatabaseV4XML.ElemMeta);
		
	}
	
	private CipherOutputStream attachStreamEncryptor(PwDbHeaderV4 header, OutputStream os) throws PwDbOutputException {
		Cipher cipher;
		try {
			//mPM.makeFinalKey(header.masterSeed, mPM.kdfParameters);

			cipher = engine.getCipher(Cipher.ENCRYPT_MODE, mPM.getFinalKey(), header.encryptionIV);
		} catch (Exception e) {
			throw new PwDbOutputException("Invalid algorithm.", e);
		}

        return new CipherOutputStream(os, cipher);
	}

	@Override
	protected SecureRandom setIVs(PwDbHeaderV4 header) throws PwDbOutputException {
		SecureRandom random = super.setIVs(header);
		random.nextBytes(header.masterSeed);

		int ivLength = engine.ivLength();
		if (ivLength != header.encryptionIV.length) {
			header.encryptionIV = new byte[ivLength];
		}
		random.nextBytes(header.encryptionIV);

		if (mPM.getKdfParameters() == null) {
			mPM.setKdfParameters(KdfFactory.aesKdf.getDefaultParameters());
		}

		try {
			KdfEngine kdf = KdfFactory.getEngineV4(mPM.getKdfParameters());
			kdf.randomize(mPM.getKdfParameters());
		} catch (UnknownKDF unknownKDF) {
            Log.e(TAG, "Unable to retrieve header", unknownKDF);
		}

		if (header.getVersion() < PwDbHeaderV4.FILE_VERSION_32_4) {
			header.innerRandomStream = CrsAlgorithm.Salsa20;
			header.innerRandomStreamKey = new byte[32];
		} else {
			header.innerRandomStream = CrsAlgorithm.ChaCha20;
			header.innerRandomStreamKey = new byte[64];
		}
		random.nextBytes(header.innerRandomStreamKey);

		randomStream = PwStreamCipherFactory.getInstance(header.innerRandomStream, header.innerRandomStreamKey);
		if (randomStream == null) {
			throw new PwDbOutputException("Invalid random cipher");
		}

		if ( header.getVersion() < PwDbHeaderV4.FILE_VERSION_32_4) {
			random.nextBytes(header.streamStartBytes);
		}
		
		return random;
	}
	
	@Override
	public PwDbHeaderV4 outputHeader(OutputStream os) throws PwDbOutputException {

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
		xml.startTag(null, PwDatabaseV4XML.ElemGroup);
		writeObject(PwDatabaseV4XML.ElemUuid, group.getUUID());
		writeObject(PwDatabaseV4XML.ElemName, group.getName());
		writeObject(PwDatabaseV4XML.ElemNotes, group.getNotes());
		writeObject(PwDatabaseV4XML.ElemIcon, group.getIconStandard().iconId);
		
		if (!group.getCustomIcon().equals(PwIconCustom.ZERO)) {
			writeObject(PwDatabaseV4XML.ElemCustomIconID, group.getCustomIcon().uuid);
		}
		
		writeList(PwDatabaseV4XML.ElemTimes, group);
		writeObject(PwDatabaseV4XML.ElemIsExpanded, group.isExpanded());
		writeObject(PwDatabaseV4XML.ElemGroupDefaultAutoTypeSeq, group.getDefaultAutoTypeSequence());
		writeObject(PwDatabaseV4XML.ElemEnableAutoType, group.getEnableAutoType());
		writeObject(PwDatabaseV4XML.ElemEnableSearching, group.getEnableSearching());
		writeObject(PwDatabaseV4XML.ElemLastTopVisibleEntry, group.getLastTopVisibleEntry());
		
	}
	
	private void endGroup() throws IllegalArgumentException, IllegalStateException, IOException {
		xml.endTag(null, PwDatabaseV4XML.ElemGroup);
	}
	
	private void writeEntry(PwEntryV4 entry, boolean isHistory) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(entry != null);
		
		xml.startTag(null, PwDatabaseV4XML.ElemEntry);
		
		writeObject(PwDatabaseV4XML.ElemUuid, entry.getUUID());
		writeObject(PwDatabaseV4XML.ElemIcon, entry.getIconStandard().iconId);
		
		if (!entry.getCustomIcon().equals(PwIconCustom.ZERO)) {
			writeObject(PwDatabaseV4XML.ElemCustomIconID, entry.getCustomIcon().uuid);
		}
		
		writeObject(PwDatabaseV4XML.ElemFgColor, entry.getForegroundColor());
		writeObject(PwDatabaseV4XML.ElemBgColor, entry.getBackgroupColor());
		writeObject(PwDatabaseV4XML.ElemOverrideUrl, entry.getOverrideURL());
		writeObject(PwDatabaseV4XML.ElemTags, entry.getTags());
		
		writeList(PwDatabaseV4XML.ElemTimes, entry);
		
		writeList(entry.getFields().getListOfAllFields(), true);
		writeList(entry.getBinaries());
		writeList(PwDatabaseV4XML.ElemAutoType, entry.getAutoType());
		
		if (!isHistory) {
			writeList(PwDatabaseV4XML.ElemHistory, entry.getHistory(), true);
		} else {
			assert(entry.sizeOfHistory() == 0);
		}
		
		xml.endTag(null, PwDatabaseV4XML.ElemEntry);
	}
	

	private void writeObject(String key, ProtectedBinary value, boolean allowRef) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(key != null && value != null);
		
		xml.startTag(null, PwDatabaseV4XML.ElemBinary);
		xml.startTag(null, PwDatabaseV4XML.ElemKey);
		xml.text(safeXmlString(key));
		xml.endTag(null, PwDatabaseV4XML.ElemKey);
		
		xml.startTag(null, PwDatabaseV4XML.ElemValue);
		String strRef = null;
		if (allowRef) {
			int ref = mPM.getBinPool().poolFind(value);
			strRef = Integer.toString(ref);
		}
		
		if (strRef != null) {
			xml.attribute(null, PwDatabaseV4XML.AttrRef, strRef);
		}
		else {
			subWriteValue(value);
		}
		xml.endTag(null, PwDatabaseV4XML.ElemValue);
		
		xml.endTag(null, PwDatabaseV4XML.ElemBinary);
	}
	
	private void subWriteValue(ProtectedBinary value) throws IllegalArgumentException, IllegalStateException, IOException {
		if (value.isProtected()) {
			xml.attribute(null, PwDatabaseV4XML.AttrProtected, PwDatabaseV4XML.ValTrue);
			
			int valLength = value.length();
			if (valLength > 0) {
				byte[] encoded = new byte[valLength];
				randomStream.processBytes(value.getData(), 0, valLength, encoded, 0);
				
				xml.text(String.valueOf(Base64Coder.encode(encoded)));
			}
			
		} else {
			if (mPM.getCompressionAlgorithm() == PwCompressionAlgorithm.Gzip) {
				xml.attribute(null, PwDatabaseV4XML.AttrCompressed, PwDatabaseV4XML.ValTrue);
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
		if (header.getVersion() < PwDbHeaderV4.FILE_VERSION_32_4) {
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
			text = PwDatabaseV4XML.ValTrue;
		}
		else {
			text = PwDatabaseV4XML.ValFalse;
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
		
		writeObject(PwDatabaseV4XML.ElemAutoTypeEnabled, autoType.enabled);
		writeObject(PwDatabaseV4XML.ElemAutoTypeObfuscation, autoType.obfuscationOptions);
		
		if (autoType.defaultSequence.length() > 0) {
			writeObject(PwDatabaseV4XML.ElemAutoTypeDefaultSeq, autoType.defaultSequence, true);
		}
		
		for (Entry<String, String> pair : autoType.entrySet()) {
			writeObject(PwDatabaseV4XML.ElemAutoTypeItem, PwDatabaseV4XML.ElemWindow, pair.getKey(), PwDatabaseV4XML.ElemKeystrokeSequence, pair.getValue());
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
		
		xml.startTag(null, PwDatabaseV4XML.ElemString);
		xml.startTag(null, PwDatabaseV4XML.ElemKey);
		xml.text(safeXmlString(key));
		xml.endTag(null, PwDatabaseV4XML.ElemKey);
		
		xml.startTag(null, PwDatabaseV4XML.ElemValue);
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
			xml.attribute(null, PwDatabaseV4XML.AttrProtected, PwDatabaseV4XML.ValTrue);
			
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
		
		xml.endTag(null, PwDatabaseV4XML.ElemValue);
		xml.endTag(null, PwDatabaseV4XML.ElemString);
		
	}

	private void writeObject(String name, PwDeletedObject value) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && value != null);
		
		xml.startTag(null, name);
		
		writeObject(PwDatabaseV4XML.ElemUuid, value.uuid);
		writeObject(PwDatabaseV4XML.ElemDeletionTime, value.getDeletionTime());
		
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
			writeObject(PwDatabaseV4XML.ElemDeletedObject, pdo);
		}
		
		xml.endTag(null, name);
		
	}

	private void writeList(String name, MemoryProtectionConfig value) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && value != null);
		
		xml.startTag(null, name);
		
		writeObject(PwDatabaseV4XML.ElemProtTitle, value.protectTitle);
		writeObject(PwDatabaseV4XML.ElemProtUserName, value.protectUserName);
		writeObject(PwDatabaseV4XML.ElemProtPassword, value.protectPassword);
		writeObject(PwDatabaseV4XML.ElemProtURL, value.protectUrl);
		writeObject(PwDatabaseV4XML.ElemProtNotes, value.protectNotes);
		
		xml.endTag(null, name);
		
	}
	
	private void writeList(String name, Map<String, String> customData) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && customData != null);
		
		xml.startTag(null, name);
		
		for (Entry<String, String> pair : customData.entrySet()) {
			writeObject(PwDatabaseV4XML.ElemStringDictExItem, PwDatabaseV4XML.ElemKey, pair.getKey(), PwDatabaseV4XML.ElemValue, pair.getValue());
			  
		}
		
		xml.endTag(null, name);
		
	}
	
	private void writeList(String name, ITimeLogger it) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && it != null);
		
		xml.startTag(null, name);
		
		writeObject(PwDatabaseV4XML.ElemLastModTime, it.getLastModificationTime().getDate());
		writeObject(PwDatabaseV4XML.ElemCreationTime, it.getCreationTime().getDate());
		writeObject(PwDatabaseV4XML.ElemLastAccessTime, it.getLastAccessTime().getDate());
		writeObject(PwDatabaseV4XML.ElemExpiryTime, it.getExpiryTime().getDate());
		writeObject(PwDatabaseV4XML.ElemExpires, it.isExpires());
		writeObject(PwDatabaseV4XML.ElemUsageCount, it.getUsageCount());
		writeObject(PwDatabaseV4XML.ElemLocationChanged, it.getLocationChanged().getDate());
		
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
		
		xml.startTag(null, PwDatabaseV4XML.ElemCustomIcons);
		
		for (PwIconCustom icon : customIcons) {
			xml.startTag(null, PwDatabaseV4XML.ElemCustomIconItem);
			
			writeObject(PwDatabaseV4XML.ElemCustomIconItemID, icon.uuid);
			writeObject(PwDatabaseV4XML.ElemCustomIconItemData, String.valueOf(Base64Coder.encode(icon.imageData)));
			
			xml.endTag(null, PwDatabaseV4XML.ElemCustomIconItem);
		}
		
		xml.endTag(null, PwDatabaseV4XML.ElemCustomIcons);
	}
	
	private void writeBinPool() throws IllegalArgumentException, IllegalStateException, IOException {
		xml.startTag(null, PwDatabaseV4XML.ElemBinaries);
		
		for (Entry<Integer, ProtectedBinary> pair : mPM.getBinPool().entrySet()) {
			xml.startTag(null, PwDatabaseV4XML.ElemBinary);
			xml.attribute(null, PwDatabaseV4XML.AttrId, Integer.toString(pair.getKey()));
			
			subWriteValue(pair.getValue());
			
			xml.endTag(null, PwDatabaseV4XML.ElemBinary);
			
		}
		
		xml.endTag(null, PwDatabaseV4XML.ElemBinaries);
		
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
