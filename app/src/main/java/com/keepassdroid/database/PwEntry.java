/*
 * Copyright 2009-2014 Brian Pellin.
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

import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

import com.keepassdroid.database.iterator.EntrySearchStringIterator;
import com.keepassdroid.utils.SprEngine;

public abstract class PwEntry implements Cloneable {

	protected static final String PMS_TAN_ENTRY = "<TAN>";
	
	public static class EntryNameComparator implements Comparator<PwEntry> {

		public int compare(PwEntry object1, PwEntry object2) {
			return object1.getTitle().compareToIgnoreCase(object2.getTitle());
		}
		
	}
	
	public PwIconStandard icon = PwIconStandard.FIRST;

	public PwEntry() {
		
	}
	
	public static PwEntry getInstance(PwGroup parent) {
		return PwEntry.getInstance(parent, true, true);
	}
	
	public static PwEntry getInstance(PwGroup parent, boolean initId, boolean initDates) {
		if (parent instanceof PwGroupV3) {
			return new PwEntryV3((PwGroupV3)parent);
		}
		else if (parent instanceof PwGroupV4) {
			return new PwEntryV4((PwGroupV4)parent);
		}
		else {
			throw new RuntimeException("Unknown PwGroup instance.");
		}
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
	
	public PwEntry clone(boolean deepStrings) {
		return (PwEntry) clone();
	}
	
	public void assign(PwEntry source) {
		icon = source.icon;
	}
	
	public abstract UUID getUUID();
	public abstract void setUUID(UUID u);
	
	public String getTitle() {
		return getTitle(false, null);
	}
	
	public String getUsername() {
		return getUsername(false, null);
	}

	public String getPassword() {
		return getPassword(false, null);
	}
	
	public String getUrl() {
		return getUrl(false, null);
	}

	public String getNotes() {
		return getNotes(false, null);
	}

	public abstract String getTitle(boolean decodeRef, PwDatabase db);
	public abstract String getUsername(boolean decodeRef, PwDatabase db);
	public abstract String getPassword(boolean decodeRef, PwDatabase db);
	public abstract String getUrl(boolean decodeRef, PwDatabase db);
	public abstract String getNotes(boolean decodeRef, PwDatabase db);
	public abstract Date getCreationTime();
	public abstract Date getLastModificationTime();
	public abstract Date getLastAccessTime();
	public abstract Date getExpiryTime();
	public abstract boolean expires();
	public abstract PwGroup getParent();
	
	public abstract void setTitle(String title, PwDatabase db);
	public abstract void setUsername(String user, PwDatabase db);
	public abstract void setPassword(String pass, PwDatabase db);
	public abstract void setUrl(String url, PwDatabase db);
	public abstract void setNotes(String notes, PwDatabase db);
	public abstract void setCreationTime(Date create);
	public abstract void setLastModificationTime(Date mod);
	public abstract void setLastAccessTime(Date access);
	public abstract void setExpires(boolean exp);
	public abstract void setExpiryTime(Date expires);
	
	
	public PwIcon getIcon() {
		return icon;
	}
	
	public boolean isTan() {
		return getTitle().equals(PMS_TAN_ENTRY) && (getUsername().length() > 0);
	}

	public String getDisplayTitle() {
		if ( isTan() ) {
			return PMS_TAN_ENTRY + " " + getUsername();
		} else {
			return getTitle();
		}
	}


	public boolean isMetaStream() {
		return false;
	}
	
	public EntrySearchStringIterator stringIterator() {
		return EntrySearchStringIterator.getInstance(this);
	}
	
	public void touch(boolean modified, boolean touchParents) {
		Date now = new Date();
		
		setLastAccessTime(now);
		
		if (modified) {
			setLastModificationTime(now);
		}
		
		PwGroup parent = getParent();
		if (touchParents && parent != null) {
			parent.touch(modified, true);
		}
	}
	
	public void touchLocation() { }
	
	public abstract void setParent(PwGroup parent);
	
	public boolean isSearchingEnabled() {
		return false;
	}

}
