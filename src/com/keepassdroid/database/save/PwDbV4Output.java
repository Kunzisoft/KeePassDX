package com.keepassdroid.database.save;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;

import org.bouncycastle.crypto.StreamCipher;

import com.keepassdroid.crypto.PwStreamCipherFactory;
import com.keepassdroid.database.CrsAlgorithm;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.PwDbHeaderV4;
import com.keepassdroid.database.exception.PwDbOutputException;


public class PwDbV4Output extends PwDbOutput {

	PwDatabaseV4 mPM;
	private StreamCipher randomStream;
	
	
	protected PwDbV4Output(PwDatabaseV4 pm, OutputStream os) {
		super(os);
		
		mPM = pm;
	}

	@Override
	public void output() throws PwDbOutputException {
		outputHeader(mOS);
	}

	@Override
	protected SecureRandom setIVs(PwDbHeader header) throws PwDbOutputException {
		SecureRandom random = super.setIVs(header);
		
		PwDbHeaderV4 h = (PwDbHeaderV4) header;
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
