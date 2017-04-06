/*
 * Copyright 2010-2017 Brian Pellin.
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

public enum CrsAlgorithm {
	
	Null(0),
	ArcFourVariant(1),
	Salsa20(2),
	ChaCha20(3);

	public static final int count = 4;
	public final int id;
	
	private CrsAlgorithm(int num) {
		id = num;
	}

	public static CrsAlgorithm fromId(int num) {
		for ( CrsAlgorithm e : CrsAlgorithm.values() ) {
			if ( e.id == num ) {
				return e;
			}
		}
		
		return null;
	}

}
