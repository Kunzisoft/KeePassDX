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

import com.keepassdroid.utils.Types;

public class PwDbHeaderV3 extends PwDbHeader {

	// DB sig from KeePass 1.03
	public static final int DBSIG_2               = 0xB54BFB65;
	// DB sig from KeePass 1.03
	public static final int DBVER_DW              = 0x00030002;

	public static final int FLAG_SHA2             = 1;
	public static final int FLAG_RIJNDAEL         = 2;
	public static final int FLAG_ARCFOUR          = 4;
	public static final int FLAG_TWOFISH          = 8;

	public static final int ALGO_AES                  = 0;
	public static final int ALGO_TWOFISH              = 1;

	/** Size of byte buffer needed to hold this struct. */
	public static final int BUF_SIZE        = 124;



	public int              signature1;                  // = PWM_DBSIG_1
	public int              signature2;                  // = DBSIG_2
	public int              flags;
	public int              version;

	/** IV used for content encryption */
	public byte             encryptionIV[] = new byte[16];

	/** Number of groups in the database */
	public int              numGroups;
	/** Number of entries in the database */
	public int              numEntries;

	/** SHA-256 hash of the database, used for integrity check */
	public byte             contentsHash[] = new byte[32];

	public int              numKeyEncRounds;

	/**
	 * Parse given buf, as read from file.
	 * @param buf
	 */
	public void loadFromFile( byte buf[], int offset ) {
		signature1 = Types.readInt( buf, offset + 0 );
		signature2 = Types.readInt( buf, offset + 4 );
		flags = Types.readInt( buf, offset + 8 );
		version = Types.readInt( buf, offset + 12 );

		System.arraycopy( buf, offset + 16, mMasterSeed, 0, 16 );
		System.arraycopy( buf, offset + 32, encryptionIV, 0, 16 );

		numGroups = Types.readInt( buf, offset + 48 );
		numEntries = Types.readInt( buf, offset + 52 );

		System.arraycopy( buf, offset + 56, contentsHash, 0, 32 );

		System.arraycopy( buf, offset + 88, mTransformSeed, 0, 32 );
		numKeyEncRounds = Types.readInt( buf, offset + 120 );
	}

	public PwDbHeaderV3() {
		mMasterSeed = new byte[16];
		mTransformSeed = new byte[32];
	}

	public static boolean matchesHeader(int sig1, int sig2) {
		return (sig1 == PWM_DBSIG_1) && (sig2 == DBSIG_2);
	}


}
