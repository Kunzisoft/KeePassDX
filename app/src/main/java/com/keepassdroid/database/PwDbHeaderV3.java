/*
 * Copyright 2009-2011 Brian Pellin.
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
 *

Derived from

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

import java.io.IOException;

import com.keepassdroid.stream.LEDataInputStream;

public class PwDbHeaderV3 extends PwDbHeader {

	// DB sig from KeePass 1.03
	public static final int DBSIG_2               = 0xB54BFB65;
	// DB sig from KeePass 1.03
	public static final int DBVER_DW              = 0x00030003;

	public static final int FLAG_SHA2             = 1;
	public static final int FLAG_RIJNDAEL         = 2;
	public static final int FLAG_ARCFOUR          = 4;
	public static final int FLAG_TWOFISH          = 8;

	/** Size of byte buffer needed to hold this struct. */
	public static final int BUF_SIZE        = 124;

	/** Used for the dwKeyEncRounds AES transformations */
	public byte transformSeed[] = new byte[32];

	public int              signature1;                  // = PWM_DBSIG_1
	public int              signature2;                  // = DBSIG_2
	public int              flags;
	public int              version;

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
	 * @throws IOException 
	 */
	public void loadFromFile( byte buf[], int offset ) throws IOException {
		signature1 = LEDataInputStream.readInt( buf, offset + 0 );
		signature2 = LEDataInputStream.readInt( buf, offset + 4 );
		flags = LEDataInputStream.readInt( buf, offset + 8 );
		version = LEDataInputStream.readInt( buf, offset + 12 );

		System.arraycopy( buf, offset + 16, masterSeed, 0, 16 );
		System.arraycopy( buf, offset + 32, encryptionIV, 0, 16 );

		numGroups = LEDataInputStream.readInt( buf, offset + 48 );
		numEntries = LEDataInputStream.readInt( buf, offset + 52 );

		System.arraycopy( buf, offset + 56, contentsHash, 0, 32 );

		System.arraycopy( buf, offset + 88, transformSeed, 0, 32 );
		numKeyEncRounds = LEDataInputStream.readInt( buf, offset + 120 );
		if ( numKeyEncRounds < 0 ) {
			// TODO: Really treat this like an unsigned integer
			throw new IOException("Does not support more than " + Integer.MAX_VALUE + " rounds.");
		}
	}

	public PwDbHeaderV3() {
		masterSeed = new byte[16];
	}

	public static boolean matchesHeader(int sig1, int sig2) {
		return (sig1 == PWM_DBSIG_1) && (sig2 == DBSIG_2);
	}
	
	
	/** Determine if the database version is compatible with this application
	 * @return true, if it is compatible
	 */
	public boolean matchesVersion() {
		return compatibleHeaders(version, DBVER_DW);
	}
	
	public static boolean compatibleHeaders(int one, int two) {
		return (one & 0xFFFFFF00) == (two & 0xFFFFFF00);
	}


}
