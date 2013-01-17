package com.keepassdroid.database.save;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import org.bouncycastle.crypto.StreamCipher;

import com.keepassdroid.crypto.CipherFactory;
import com.keepassdroid.crypto.PwStreamCipherFactory;
import com.keepassdroid.database.CrsAlgorithm;
import com.keepassdroid.database.PwCompressionAlgorithm;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.exception.PwDbOutputException;
import com.keepassdroid.stream.HashedBlockOutputStream;


public class PwDbV4Output extends PwDbOutput {

	PwDatabaseV4 mPM;
	private StreamCipher randomStream;
	
	
	protected PwDbV4Output(PwDatabaseV4 pm, OutputStream os) {
		super(os);
		
		mPM = pm;
	}

	@Override
	public void output() throws PwDbOutputException {
		PwDbHeaderV4 header = (PwDbHeaderV4 ) outputHeader(mOS);
		
		CipherOutputStream cos = attachStreamEncryptor(header, mOS);
		
		try {
			cos.write(header.streamStartBytes);
		
			HashedBlockOutputStream hashed = new HashedBlockOutputStream(cos);
			
			OutputStream compressed;
			if ( mPM.compressionAlgorithm == PwCompressionAlgorithm.Gzip ) {
				compressed = new GZIPOutputStream(hashed); 
			} else {
				compressed = hashed;
			}
			
			outputDatabase(header, compressed);
		} catch (IOException e) {
			throw new PwDbOutputException("Failed to set up output stream.");
		}
	}
	
	private void outputDatabase(PwDbHeaderV4 header, OutputStream os) {
		// TODO: output xml document from database
		
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
		
		return header;
	}

}
