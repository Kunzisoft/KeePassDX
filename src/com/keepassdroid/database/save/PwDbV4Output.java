package com.keepassdroid.database.save;

import java.io.OutputStream;

import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwDbHeader;
import com.keepassdroid.database.exception.PwDbOutputException;


public class PwDbV4Output extends PwDbOutput {

	PwDatabaseV4 mPM;
	
	protected PwDbV4Output(PwDatabaseV4 pm, OutputStream os) {
		super(os);
		
		mPM = pm;
	}

	@Override
	public void output() throws PwDbOutputException {
		outputHeader(mOS);
		
	}

	@Override
	public PwDbHeader outputHeader(OutputStream os) throws PwDbOutputException {
		// TODO Auto-generated method stub
		return null;
	}

}
