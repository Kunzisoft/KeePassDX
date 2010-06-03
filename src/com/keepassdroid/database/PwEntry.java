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

	//public byte uuid[] = new byte[16];
	public String title;
	public String url;
	public String additional;
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
		
		newEntry.setUUID(getUUID());
		newEntry.title = title;
		newEntry.url = url;
		newEntry.additional = additional;
		
		return newEntry;
	}
	
	public void assign(PwEntry source) {
		setUUID(source.getUUID());
		title = source.title;
		url = source.url;
		additional = source.additional;
	}

	public abstract void stampLastAccess();

	public abstract UUID getUUID();
	public abstract void setUUID(UUID u);
	public abstract String getUsername();
	public abstract String getPassword();
	public abstract Date getCreate();
	public abstract Date getMod();
	public abstract Date getAccess();
	public abstract Date getExpire();
	public abstract PwGroup getParent();

	public abstract String getDisplayTitle();

}
