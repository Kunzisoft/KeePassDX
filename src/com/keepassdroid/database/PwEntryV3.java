/*
KeePass for J2ME

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This file was derived from 

Java clone of KeePass - A KeePass file viewer for Java
Copyright 2006 Bill Zwicky <billzwicky@users.sourceforge.net>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.keepassdroid.database;

// PhoneID
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;


import android.util.Log;

import com.keepassdroid.Database;


/**
 * Structure containing information about one entry.
 * 
 * <PRE>
 * One entry: [FIELDTYPE(FT)][FIELDSIZE(FS)][FIELDDATA(FD)]
 *            [FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)][FT+FS+(FD)]...
 *            
 * [ 2 bytes] FIELDTYPE
 * [ 4 bytes] FIELDSIZE, size of FIELDDATA in bytes
 * [ n bytes] FIELDDATA, n = FIELDSIZE
 * 
 * Notes:
 *  - Strings are stored in UTF-8 encoded form and are null-terminated.
 *  - FIELDTYPE can be one of the FT_ constants.
 * </PRE>
 *
 * @author Naomaru Itoi <nao@phoneid.org>
 * @author Bill Zwicky <wrzwicky@pobox.com>
 * @author Dominik Reichl <dominik.reichl@t-online.de>
 */
public class PwEntryV3 extends PwEntry {

	public static final Date NEVER_EXPIRE = getNeverExpire();
	

	/** Size of byte buffer needed to hold this struct. */
	public static final String PMS_ID_BINDESC = "bin-stream";
	public static final String PMS_ID_TITLE   = "Meta-Info";
	public static final String PMS_ID_USER    = "SYSTEM";
	public static final String PMS_ID_URL     = "$";
	private static final String PMS_TAN_ENTRY ="<TAN>";



	public int              groupId;
	public int              imageId;

	public String           username;

	private byte[]          password;

	public PwDate             tCreation;
	public PwDate             tLastMod;
	public PwDate             tLastAccess;
	public PwDate             tExpire;

	/** A string describing what is in pBinaryData */
	public String           binaryDesc;
	private byte[]          binaryData;

	private static Date getNeverExpire() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 2999);
		cal.set(Calendar.MONTH, 11);
		cal.set(Calendar.DAY_OF_MONTH, 28);
		cal.set(Calendar.HOUR, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);

		return cal.getTime();
	}

	public static boolean IsNever(Date date) {
		Calendar never = Calendar.getInstance();
		never.setTime(NEVER_EXPIRE);
		never.set(Calendar.MILLISECOND, 0);

		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.MILLISECOND, 0);

		Log.d("never", "L="+ never.get(Calendar.YEAR) + " R=" + cal.get(Calendar.YEAR));
		Log.d("never", "L="+ never.get(Calendar.MONTH) + " R=" + cal.get(Calendar.MONTH));
		Log.d("never", "L="+ never.get(Calendar.DAY_OF_MONTH) + " R=" + cal.get(Calendar.DAY_OF_MONTH));
		Log.d("never", "L="+ never.get(Calendar.HOUR) + " R=" + cal.get(Calendar.HOUR));
		Log.d("never", "L="+ never.get(Calendar.MINUTE) + " R=" + cal.get(Calendar.MINUTE));
		Log.d("never", "L="+ never.get(Calendar.SECOND) + " R=" + cal.get(Calendar.SECOND));

		return (never.get(Calendar.YEAR) == cal.get(Calendar.YEAR)) && 
		(never.get(Calendar.MONTH) == cal.get(Calendar.MONTH)) &&
		(never.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH)) &&
		(never.get(Calendar.HOUR) == cal.get(Calendar.HOUR)) &&
		(never.get(Calendar.MINUTE) == cal.get(Calendar.MINUTE)) &&
		(never.get(Calendar.SECOND) == cal.get(Calendar.SECOND));

	}

	// for tree traversing
	public PwGroupV3 parent = null;

	public PwEntryV3() {
		super();
	}

	/*
	public PwEntryV3(PwEntryV3 source) {
		assign(source);
	}
	*/

	public PwEntryV3(Database db, int parentId) {

		WeakReference<PwGroup> wPw = db.groups.get(parentId);

		parent = (PwGroupV3) wPw.get();
		groupId = parentId;

		Random random = new Random();
		uuid = new byte[16];
		random.nextBytes(uuid);

		Calendar cal = Calendar.getInstance();
		Date now = cal.getTime();
		tCreation = new PwDate(now);
		tLastAccess = new PwDate(now);
		tLastMod = new PwDate(now);
		tExpire = new PwDate(NEVER_EXPIRE);

	}
	
	public boolean isTan() {
		return title.equals(PMS_TAN_ENTRY);
	}
	
	@Override
	public String getDisplayTitle() {
		if ( isTan() ) {
			return PMS_TAN_ENTRY + " " + username;
		} else {
			return title;	
		}
	}

	/**
	 * @return the actual password byte array.
	 */
	@Override
	public String getPassword() {
		return new String(password);
	}
	
	public byte[] getPasswordBytes() {
		return password;
	}


	/**
	 * fill byte array
	 */
	private static void fill(byte[] array, byte value)
	{
		for (int i=0; i<array.length; i++)
			array[i] = value;
		return;
	}

	/** Securely erase old password before copying new. */
	public void setPassword( byte[] buf, int offset, int len ) {
		if( password != null ) {
			fill( password, (byte)0 );
			password = null;
		}
		password = new byte[len];
		System.arraycopy( buf, offset, password, 0, len );
	}



	/**
	 * @return the actual binaryData byte array.
	 */
	public byte[] getBinaryData() {
		return binaryData;
	}



	/** Securely erase old data before copying new. */
	public void setBinaryData( byte[] buf, int offset, int len ) {
		if( binaryData != null ) {
			fill( binaryData, (byte)0 );
			binaryData = null;
		}
		binaryData = new byte[len];
		System.arraycopy( buf, offset, binaryData, 0, len );
	}

	// Determine if this is a MetaStream entrie
	public boolean isMetaStream() {
		if ( binaryData == null ) return false;
		if ( additional == null || additional.length() == 0 ) return false;
		if ( ! binaryDesc.equals(PMS_ID_BINDESC) ) return false;
		if ( title == null ) return false;
		if ( ! title.equals(PMS_ID_TITLE) ) return false;
		if ( username == null ) return false;
		if ( ! username.equals(PMS_ID_USER) ) return false;
		if ( url == null ) return false;
		if ( ! url.equals(PMS_ID_URL)) return false;
		if ( imageId != 0 ) return false;

		return true;
	}

	
	@Override
	public void assign(PwEntry source) {
		
		if ( ! (source instanceof PwEntryV3) ) {
			throw new RuntimeException("DB version mix");
		}
		
		super.assign(source);
		
		PwEntryV3 src = (PwEntryV3) source;
		assign(src);
	
	}

	private void assign(PwEntryV3 source) {
		
		groupId = source.groupId;
		imageId = source.imageId;
		username = source.username;

		int passLen = source.password.length;
		password = new byte[passLen]; 
		System.arraycopy(source.password, 0, password, 0, passLen);

		tCreation = (PwDate) source.tCreation.clone();
		tLastMod = (PwDate) source.tLastMod.clone();
		tLastAccess = (PwDate) source.tLastAccess.clone();
		tExpire = (PwDate) source.tExpire.clone();

		binaryDesc = source.binaryDesc;

		if ( source.binaryData != null ) {
			int descLen = source.binaryData.length;
			binaryData = new byte[descLen]; 
			System.arraycopy(source.binaryData, 0, binaryData, 0, descLen);
		}

		parent = source.parent;

	}
	
	@Override
	public Object clone() {
		PwEntryV3 newEntry = (PwEntryV3) super.clone();
		
		int passLen = password.length;
		password = new byte[passLen]; 
		System.arraycopy(password, 0, newEntry.password, 0, passLen);

		newEntry.tCreation = (PwDate) tCreation.clone();
		newEntry.tLastMod = (PwDate) tLastMod.clone();
		newEntry.tLastAccess = (PwDate) tLastAccess.clone();
		newEntry.tExpire = (PwDate) tExpire.clone();
		
		newEntry.binaryDesc = binaryDesc;

		if ( binaryData != null ) {
			int descLen = binaryData.length;
			newEntry.binaryData = new byte[descLen]; 
			System.arraycopy(binaryData, 0, newEntry.binaryData, 0, descLen);
		}

		newEntry.parent = parent;

		
		return newEntry;
	}

	@Override
	public void stampLastAccess() {
		Calendar cal = Calendar.getInstance();
		tLastAccess = new PwDate(cal.getTime());
		
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public Date getAccess() {
		return tLastAccess.getJDate();
	}

	@Override
	public Date getCreate() {
		return tCreation.getJDate();
	}

	@Override
	public Date getExpire() {
		return tExpire.getJDate();
	}

	@Override
	public Date getMod() {
		return tLastMod.getJDate();
	}

	@Override
	public PwGroupV3 getParent() {
		return parent;
	}

}
