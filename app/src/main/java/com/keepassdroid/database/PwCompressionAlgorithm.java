/*
 * Copyright 2010 Brian Pellin.
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
package com.keepassdroid.database;

public enum PwCompressionAlgorithm {
	
	None(0),
	Gzip(1);
	
	// Note: We can get away with using int's to store unsigned 32-bit ints
	//       since we won't do arithmetic on these values (also unlikely to
	//       reach negative ids).
	public final int id;
	public static final int count = 2;
	
	private PwCompressionAlgorithm(int num) {
		id = num;
	}
	
	public static PwCompressionAlgorithm fromId(int num) {
		for ( PwCompressionAlgorithm e : PwCompressionAlgorithm.values() ) {
			if ( e.id == num ) {
				return e;
			}
		}
		
		return null;
	}
	
}
