package com.keepassdroid.database.save;

import static com.keepassdroid.database.PwDatabaseV4XML.*;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
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
import com.keepassdroid.database.CrsAlgorithm;
import com.keepassdroid.database.PwCompressionAlgorithm;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDatabaseV4.MemoryProtectionConfig;
import com.keepassdroid.database.PwDatabaseV4XML;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.PwIconCustom;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.stream.HashedBlockOutputStream;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.Types;

public class PwDbV4Output extends PwDbOutput {

	PwDatabaseV4 mPM;
	private StreamCipher randomStream;
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
	
	private void outputDatabase(OutputStream os) throws IllegalArgumentException, IllegalStateException, IOException {
		// TODO: BinPoolBuild
		
		xml = Xml.newSerializer();
		
		xml.setOutput(os, "UTF-8");
		xml.startDocument("UTF-8", true);
		
		xml.startTag(null, ElemDocNode);
		
		outputMeta();
		
		xml.endTag(null, ElemDocNode);
		xml.endDocument();
		
	}
	
	private void outputMeta() throws IllegalArgumentException, IllegalStateException, IOException {
		xml.startTag(null, ElemMeta);
		
		WriteObject(ElemGenerator, mPM.localizedAppName);
		
		if (hashOfHeader != null) {
			WriteObject(ElemHeaderHash, String.valueOf(Base64Coder.encode(hashOfHeader)));
		}
		
		WriteObject(ElemDbName, mPM.name, true);
		WriteObject(ElemDbNameChanged, mPM.nameChanged);
		WriteObject(ElemDbDesc, mPM.description, true);
		WriteObject(ElemDbDescChanged, mPM.descriptionChanged);
		WriteObject(ElemDbDefaultUser, mPM.defaultUserName, true);
		WriteObject(ElemDbDefaultUserChanged, mPM.defaultUserNameChanged);
		WriteObject(ElemDbMntncHistoryDays, mPM.maintenanceHistoryDays);
		WriteObject(ElemDbColor, mPM.color);
		WriteObject(ElemDbKeyChanged, mPM.keyLastChanged);
		WriteObject(ElemDbKeyChangeRec, mPM.keyChangeRecDays);
		WriteObject(ElemDbKeyChangeForce, mPM.keyChangeForceDays);
		
		
		WriteList(ElemMemoryProt, mPM.memoryProtection);
		
		WriteCustomIconList();
		
		WriteObject(ElemRecycleBinEnabled, mPM.recycleBinEnabled);
		WriteObject(ElemRecycleBinUuid, mPM.recycleBinUUID);
		WriteObject(ElemRecycleBinChanged, mPM.recycleBinChanged);
		WriteObject(ElemEntryTemplatesGroup, mPM.entryTemplatesGroup);
		WriteObject(ElemEntryTemplatesGroupChanged, mPM.entryTemplatesGroupChanged);
		WriteObject(ElemHistoryMaxItems, mPM.historyMaxItems);
		WriteObject(ElemHistoryMaxItems, mPM.historyMaxSize);
		WriteObject(ElemLastSelectedGroup, mPM.lastSelectedGroup);
		WriteObject(ElemLastTopVisibleGroup, mPM.lastTopVisibleGroup);
		
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
	
	private void WriteObject(String name, String value, boolean filterXmlChars) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && value != null);
		
		xml.startTag(null, name);
		
		if (filterXmlChars) {
			value = SafeXmlString(value);
		}
		
		xml.text(value);
		xml.endTag(null, name);
	}
	
	private void WriteObject(String name, String value) throws IllegalArgumentException, IllegalStateException, IOException {
		WriteObject(name, value, false);
	}
	
	private void WriteObject(String name, Date value) throws IllegalArgumentException, IllegalStateException, IOException {
		WriteObject(name, PwDatabaseV4XML.dateFormat.format(value));
	}
	
	private void WriteObject(String name, long value) throws IllegalArgumentException, IllegalStateException, IOException {
		WriteObject(name, String.valueOf(value));
	}
	
	private void WriteObject(String name, boolean value) throws IllegalArgumentException, IllegalStateException, IOException {
		WriteObject(name, value ? ValTrue : ValFalse);
	}
	
	private void WriteObject(String name, UUID uuid) throws IllegalArgumentException, IllegalStateException, IOException {
		byte[] data = Types.UUIDtoBytes(uuid);
		WriteObject(name, String.valueOf(Base64Coder.encode(data)));
	}
	
	private void WriteList(String name, MemoryProtectionConfig value) throws IllegalArgumentException, IllegalStateException, IOException {
		assert(name != null && value != null);
		
		xml.startTag(null, name);
		
		WriteObject(ElemProtTitle, value.protectTitle);
		WriteObject(ElemProtUserName, value.protectUserName);
		WriteObject(ElemProtPassword, value.protectPassword);
		WriteObject(ElemProtURL, value.protectUrl);
		WriteObject(ElemProtNotes, value.protectNotes);
		
		xml.endTag(null, name);
		
	}
	
	private void WriteCustomIconList() throws IllegalArgumentException, IllegalStateException, IOException {
		List<PwIconCustom> customIcons = mPM.customIcons;
		if (customIcons.size() == 0) return;
		
		xml.startTag(null, ElemCustomIcons);
		
		for (PwIconCustom icon : customIcons) {
			xml.startTag(null, ElemCustomIconItem);
			
			WriteObject(ElemCustomIconItemID, icon.uuid);
			WriteObject(ElemCustomIconItemData, String.valueOf(Base64Coder.encode(icon.imageData)));
			
			xml.endTag(null, ElemCustomIconItem);
		}
		
		xml.endTag(null, ElemCustomIcons);
	}
	
	private String SafeXmlString(String text) {
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
