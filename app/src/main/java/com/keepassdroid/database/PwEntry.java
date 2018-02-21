/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
 */
package com.keepassdroid.database;

import com.keepassdroid.database.iterator.EntrySearchStringIterator;
import com.keepassdroid.database.security.ProtectedString;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class PwEntry extends PwNode implements Cloneable {

	protected static final String PMS_TAN_ENTRY = "<TAN>";
	
	public PwIconStandard icon = PwIconStandard.FIRST;
	
	public static PwEntry getInstance(PwGroup parent) {
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

	@Override
	public Type getType() {
		return Type.ENTRY;
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
	public abstract Date getLastModificationTime();
	public abstract Date getLastAccessTime();
	public abstract Date getExpiryTime();
	public abstract boolean expires();
	
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

    public void setIcon(PwIconStandard icon) {
        this.icon = icon;
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

	// TODO encapsulate extra fields

    /**
     * To redefine if version of entry allow extra field,
     * @return true if entry allows extra field
     */
	public boolean allowExtraFields() {
		return false;
	}

    /**
     * Retrieve extra fields to show, key is the label, value is the value of field
     * @param pm Database
     * @return Map of label/value
     */
	public Map<String, String> getExtraFields(PwDatabase pm) {
		return new HashMap<>();
	}

    /**
     * Retrieve extra protected fields to show, key is the label, value is the value protected of field
     * @return Map of label/value
     */
    public Map<String, ProtectedString> getExtraProtectedFields() {
        return new HashMap<>();
    }

    /**
     * Add an extra field to the list
     * @param label Label of field, must be unique
     * @param value Value of field
     */
    public void addField(String label, ProtectedString value) {}

    /**
     * Delete all extra fields
     */
    public void removeExtraFields() {}

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
	
	public boolean isSearchingEnabled() {
		return false;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PwEntry pwEntry = (PwEntry) o;
        return isSameType(pwEntry)
                && (getUUID() != null ? getUUID().equals(pwEntry.getUUID()) : pwEntry.getUUID() == null);
    }

    @Override
    public int hashCode() {
        return getUUID() != null ? getUUID().hashCode() : 0;
    }

    /**
     * Comparator of Entry by Name
     */
    public static class EntryNameComparator implements Comparator<PwEntry> {
        public int compare(PwEntry object1, PwEntry object2) {
            if (object1.equals(object2))
                return 0;

            int entryTitleComp = object1.getTitle().compareToIgnoreCase(object2.getTitle());
            // If same title, can be different
            if (entryTitleComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }
            return entryTitleComp;
        }
    }

    /**
     * Comparator of Entry by Creation
     */
    public static class EntryCreationComparator implements Comparator<PwEntry> {
        public int compare(PwEntry object1, PwEntry object2) {
            if (object1.equals(object2))
                return 0;

            int entryCreationComp = object1.getCreationTime().compareTo(object2.getCreationTime());
            // If same creation, can be different
            if (entryCreationComp == 0) {
                return object1.hashCode() - object2.hashCode();
            }
            return entryCreationComp;
        }
    }

}
