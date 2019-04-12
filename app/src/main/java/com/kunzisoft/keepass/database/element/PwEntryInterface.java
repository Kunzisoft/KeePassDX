package com.kunzisoft.keepass.database.element;

import com.kunzisoft.keepass.database.ExtraFields;
import com.kunzisoft.keepass.database.SmallTimeInterface;
import com.kunzisoft.keepass.database.security.ProtectedString;

public interface PwEntryInterface extends PwNodeInterface, SmallTimeInterface {

	String getTitle();
	void setTitle(String title);

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

	void touchLocation();

	PwEntryInterface duplicate();

	String PMS_TAN_ENTRY = "<TAN>";

	static boolean isTan(PwEntryInterface entry) {
		return entry.getTitle().equals(PMS_TAN_ENTRY)
				&& (entry.getUsername().length() > 0);
	}

	/**
	 * {@inheritDoc}
	 * Get the display title from an entry, <br />
	 * {@link #startManageEntry()} and {@link #stopManageEntry()} must be called
	 * before and after {@link #getVisualTitle(PwEntryInterface entry)}
	 */
	static String getVisualTitle(boolean isTan, String title, String userName, String url, String id) {
		if ( isTan ) {
			return PMS_TAN_ENTRY + " " + userName;
		} else {
			if (title.isEmpty())
				if (userName.isEmpty())
					if (url.isEmpty())
						return id;
					else
						return url;
				else
					return userName;
			else
				return title;
		}
	}

	static String getVisualTitle(PwEntryInterface entry) {
		return getVisualTitle(PwEntryInterface.isTan(entry),
				entry.getTitle(),
				entry.getUsername(),
				entry.getUrl(),
				entry.getNodeId().toString());
	}
}
