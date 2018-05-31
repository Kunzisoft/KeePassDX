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

This file was derived from 

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

package com.kunzisoft.keepass.database;

import java.io.UnsupportedEncodingException;
import java.util.UUID;


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
 * @author Jeremy Jamet <jeremy.jamet@kunzisoft.com>
 */
public class PwEntryV3 extends PwEntry<PwGroupV3> {

	/** Size of byte buffer needed to hold this struct. */
	private static final String PMS_ID_BINDESC = "bin-stream";
	private static final String PMS_ID_TITLE   = "Meta-Info";
	private static final String PMS_ID_USER    = "SYSTEM";
	private static final String PMS_ID_URL     = "$";

    // TODO Parent ID to remove
    private int groupId;

    private String title;
	private	String username;
	private byte[] password;
	private String url;
	private String additional;

	/** A string describing what is in pBinaryData */
	private String           binaryDesc;
	private byte[]          binaryData;

	public PwEntryV3() {
		super();
	}
	
	public PwEntryV3(PwGroupV3 p) {
	    construct(p);
		groupId = ((PwGroupIdV3) this.parent.getId()).getId(); // TODO remove
	}

    protected void updateWith(PwEntryV3 source) {
        super.assign(source);
        groupId = source.groupId;

        title = source.title;
        username = source.username;
        int passLen = source.password.length;
        password = new byte[passLen];
        System.arraycopy(source.password, 0, password, 0, passLen);
        url = source.url;
        additional = source.additional;

        binaryDesc = source.binaryDesc;
        if ( source.binaryData != null ) {
            int descLen = source.binaryData.length;
            binaryData = new byte[descLen];
            System.arraycopy(source.binaryData, 0, binaryData, 0, descLen);
        }
    }

    @Override
    public PwEntryV3 clone() {
        // Attributes in parent
        PwEntryV3 newEntry = (PwEntryV3) super.clone();

        // Attributes here
        // newEntry.parent stay the same in copy
        // newEntry.groupId stay the same in copy
        // newEntry.title stay the same in copy
        // newEntry.username stay the same in copy
        if (password != null) {
            int passLen = password.length;
            password = new byte[passLen];
            System.arraycopy(password, 0, newEntry.password, 0, passLen);
        }
        // newEntry.url stay the same in copy
        // newEntry.additional stay the same in copy

        // newEntry.binaryDesc stay the same in copy
        if ( binaryData != null ) {
            int descLen = binaryData.length;
            newEntry.binaryData = new byte[descLen];
            System.arraycopy(binaryData, 0, newEntry.binaryData, 0, descLen);
        }

        return newEntry;
    }

    public int getGroupId() {
	    return groupId;
    }

    public void setGroupId(int groupId) {
	    this.groupId = groupId;
    }

    @Override
    public String getUsername() {
        if (username == null) {
            return "";
        }
        return username;
    }

    @Override
    public void setUsername(String user) {
        username = user;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getNotes() {
        return additional;
    }

    @Override
    public void setNotes(String notes) {
        additional = notes;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    public void populateBlankFields(PwDatabaseV3 db) {
	    // TODO verify and remove
        if (icon == null) {
            icon = db.getIconFactory().getKeyIcon();
        }

        if (username == null) {
            username = "";
        }

        if (password == null) {
            password = new byte[0];
        }

        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        if (title == null) {
            title = "";
        }

        if (url == null) {
            url = "";
        }

        if (additional == null) {
            additional = "";
        }

        if (binaryDesc == null) {
            binaryDesc = "";
        }

        if (binaryData == null) {
            binaryData = new byte[0];
        }
    }

	/**
	 * @return the actual password byte array.
	 */
	@Override
	public String getPassword() {
		if (password == null) {
			return "";
		}
		return new String(password);
	}
	
	public byte[] getPasswordBytes() {
		return password;
	}

	/**
	 * fill byte array
	 */
	private static void fill(byte[] array, byte value) {
		for (int i=0; i<array.length; i++)
			array[i] = value;
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

	@Override
	public void setPassword(String pass) {
		byte[] password;
		try {
			password = pass.getBytes("UTF-8");
			setPassword(password, 0, password.length);
		} catch (UnsupportedEncodingException e) {
			assert false;
			password = pass.getBytes();
			setPassword(password, 0, password.length);
		}
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

	public String getBinaryDesc() {
	    return binaryDesc;
    }

    public void setBinaryDesc(String binaryDesc) {
	    this.binaryDesc = binaryDesc;
    }

	// Determine if this is a MetaStream entry
	@Override
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
		if ( !icon.isMetaStreamIcon() ) return false;

		return true;
	}
}
