/*
 * Copyright 2009 Brian Pellin.
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

import java.util.Date;
import java.util.UUID;

public abstract class PwEntry implements Cloneable {

	// TODO none of these belong here.  They aren't stored this way in PwEntryV4
	
	public int imageId;

	public PwEntry() {
		
	}
	
	@Override
	public Object clone() {
		PwEntry newEntry;
		try {
			newEntry = (PwEntry) super.clone();
		} catch (CloneNotSupportedException e) {
			assert(false);
			throw new RuntimeException("Clone should be supported");
		}
		
		return newEntry;
	}
	
	public void assign(PwEntry source) {
		imageId = source.imageId;
	}
	
	
	
	public abstract void stampLastAccess();

	public abstract UUID getUUID();
	public abstract void setUUID(UUID u);
	public abstract String getTitle();
	public abstract String getUsername();
	public abstract String getPassword();
	public abstract String getUrl();
	public abstract String getNotes();
	public abstract Date getCreate();
	public abstract Date getMod();
	public abstract Date getAccess();
	public abstract Date getExpire();
	public abstract boolean expires();
	public abstract PwGroup getParent();

	public abstract String getDisplayTitle();

	public boolean isMetaStream() {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PwEntry other = (PwEntry) obj;
		if (imageId != other.imageId)
			return false;
		return true;
	}
	
	

}
