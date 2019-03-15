package com.kunzisoft.keepass.database.element;

import com.kunzisoft.keepass.database.ExtraFields;
import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.model.Entry;
import com.kunzisoft.keepass.model.Field;

import java.util.UUID;

public interface PwEntryInterface extends PwNodeInterface {

	UUID getUUID();

	void setUUID(UUID uuid);

	String getName();
	void setName(String title);

	String getUsername();
	void setUsername(String user);

	String getPassword();
	void setPassword(String pass);

	String getUrl();
	void setUrl(String url);

	String getNotes();
	void setNotes(String notes);


	/**
	 * To redefine if version of entry allow extra field,
	 * @return true if entry allows extra field
	 */
	boolean allowExtraFields();

	/**
	 * Retrieve extra fields to show, key is the label, value is the value of field
	 * @return Map of label/value
	 */
	ExtraFields getFields();

	/**
	 * If entry contains extra fields
	 * @return true if there is extra fields
	 */
	boolean containsCustomFields();
	/**
	 * If entry contains extra fields that are protected
	 * @return true if there is extra fields protected
	 */
	boolean containsCustomFieldsProtected();

	/**
	 * If entry contains extra fields that are not protected
	 * @return true if there is extra fields not protected
	 */
	boolean containsCustomFieldsNotProtected();

	/**
	 * Add an extra field to the list (standard or custom)
	 * @param label Label of field, must be unique
	 * @param value Value of field
	 */
	void addExtraField(String label, ProtectedString value);

	/**
	 * Delete all custom fields
	 */
	void removeAllCustomFields();

	/**
	 * If it's a node with only meta information like Meta-info SYSTEM Database Color
	 * @return false by default, true if it's a meta stream
	 */
	boolean isMetaStream();

	/**
	 * Create a backup of entry
	 */
	void createBackup(PwDatabase db);

	void touchLocation();

	void startToManageFieldReferences(PwDatabase db);
	void stopToManageFieldReferences();

	PwEntryInterface duplicate();

	String PMS_TAN_ENTRY = "<TAN>";

	static boolean isTan(PwEntryInterface entry) {
		return entry.getName().equals(PMS_TAN_ENTRY)
				&& (entry.getUsername().length() > 0);
	}

	/**
	 * {@inheritDoc}
	 * Get the display title from an entry, <br />
	 * {@link #startToManageFieldReferences(PwDatabase)} and {@link #stopToManageFieldReferences()} must be called
	 * before and after {@link #getVisualTitle(PwEntryInterface entry)}
	 */
	static String getVisualTitle(boolean isTan, String userName, String title, String url, UUID uuid) {
		if ( isTan ) {
			return PMS_TAN_ENTRY + " " + userName;
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

	static String getVisualTitle(PwEntryInterface entry) {
		return getVisualTitle(PwEntryInterface.isTan(entry),
				entry.getUsername(),
				entry.getName(),
				entry.getUrl(),
				entry.getUUID());
	}

	static Entry getEntry(PwEntryInterface entry) {
		Entry entryModel = new Entry();
		entryModel.setTitle(entry.getName());
		entryModel.setUsername(entry.getUsername());
		entryModel.setPassword(entry.getPassword());
		entryModel.setUrl(entry.getUrl());
		if (entry.containsCustomFields()) {
			entry.getFields()
					.doActionToAllCustomProtectedField(
							(key, value) -> entryModel.addCustomField(
									new Field(key, value.toString())));
		}
		return entryModel;
	}
}
