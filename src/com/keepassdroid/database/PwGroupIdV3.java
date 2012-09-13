/*
 * Copyright 2010-2012 Brian Pellin.
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

public class PwGroupIdV3 extends PwGroupId {

	private int id;
	
	public PwGroupIdV3(int i) {
		id = i;
	}
	
	@Override
	public boolean equals(Object compare) {
		if ( ! (compare instanceof PwGroupIdV3) ) {
			return false;
		}
		
		PwGroupIdV3 cmp = (PwGroupIdV3) compare;
		return id == cmp.id;
	}

	@Override
	public int hashCode() {
		Integer i = Integer.valueOf(id);
		return i.hashCode();
	}
	
	public int getId() {
		return id;
	}
	

}
