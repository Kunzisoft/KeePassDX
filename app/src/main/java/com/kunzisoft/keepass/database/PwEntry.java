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
 */
package com.kunzisoft.keepass.database;

import android.os.Parcel;

import com.kunzisoft.keepass.database.iterator.EntrySearchStringIterator;
import com.kunzisoft.keepass.database.security.ProtectedString;

import java.util.UUID;

public abstract class PwEntry<Parent extends PwGroup> extends PwNode<Parent> {

	private static final String PMS_TAN_ENTRY = "<TAN>";

	protected UUID uuid = PwDatabase.UUID_ZERO;

	public PwEntry() {}

	public PwEntry(Parcel in) {
		super(in);
		uuid = (UUID) in.readSerializable();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		super.writeToParcel(dest, flags);
		dest.writeSerializable(uuid);
	}

	@Override
	protected void construct(Parent parent) {
	    super.construct(parent);
        uuid = UUID.randomUUID();
    }

	@Override
	public PwEntry clone() {
		// uuid is clone automatically (IMMUTABLE)
		return (PwEntry) super.clone();
	}

	@Override
	public Type getType() {
		return Type.ENTRY;
	}

    protected void assign(PwEntry<Parent> source) {
	    super.assign(source);
        uuid = source.uuid;
	}

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

	public void startToManageFieldReferences(PwDatabase db) {}
	public void stopToManageFieldReferences() {}

	public abstract String getTitle();
    public abstract void setTitle(String title);

	public abstract String getUsername();
    public abstract void setUsername(String user);

	public abstract String getPassword();
    public abstract void setPassword(String pass);

	public abstract String getUrl();
    public abstract void setUrl(String url);

	public abstract String getNotes();
	public abstract void setNotes(String notes);
	
	private boolean isTan() {
		return getTitle().equals(PMS_TAN_ENTRY) && (getUsername().length() > 0);
	}

    /**
     * {@inheritDoc}
     * Get the display title from an entry, <br />
     * {@link #startToManageFieldReferences(PwDatabase)} and {@link #stopToManageFieldReferences()} must be called
     * before and after {@link #getVisualTitle()}
     */
	public String getVisualTitle() {
	    // only used to compare, don't car if it's a reference
	    return getVisualTitle(isTan(), getTitle(), getUsername(), getUrl(), getUUID());
	}

	public static String getVisualTitle(boolean isTAN, String title, String username, String url, UUID uuid) {
        if ( isTAN ) {
            return PMS_TAN_ENTRY + " " + username;
        } else {
            if (title.isEmpty())
                if (url.isEmpty())
                    return uuid.toString();
                else
                    return url;
            else
                return title;
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
     * @return Map of label/value
     */
	public ExtraFields getFields() {
		return new ExtraFields();
	}

	/**
	 * If entry contains extra fields
	 * @return true if there is extra fields
	 */
	public boolean containsCustomFields() {
		return getFields().containsCustomFields();
	}

	/**
	 * If entry contains extra fields that are protected
	 * @return true if there is extra fields protected
	 */
	public boolean containsCustomFieldsProtected() {
		return getFields().containsCustomFieldsProtected();
	}

	/**
	 * If entry contains extra fields that are not protected
	 * @return true if there is extra fields not protected
	 */
	public boolean containsCustomFieldsNotProtected() {
		return getFields().containsCustomFieldsNotProtected();
	}

    /**
     * Add an extra field to the list (standard or custom)
     * @param label Label of field, must be unique
     * @param value Value of field
     */
    public void addExtraField(String label, ProtectedString value) {}

    /**
     * Delete all custom fields
     */
    public void removeAllCustomFields() {}

	/**
	 * If it's a node with only meta information like Meta-info SYSTEM Database Color
	 * @return false by default, true if it's a meta stream
	 */
	public boolean isMetaStream() {
		return false;
	}

    /**
     * Create a backup of entry
     */
    public void createBackup(PwDatabase db) {}

	public EntrySearchStringIterator stringIterator() {
		return EntrySearchStringIterator.getInstance(this);
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
}
