package com.kunzisoft.keepass.database.element;

import android.os.Parcelable;

import com.kunzisoft.keepass.database.SmallTimeInterface;

public interface PwNodeInterface extends SmallTimeInterface, Parcelable {

	PwNodeId getNodeId();

	void setNodeId(PwNodeId id);

	String getTitle();

	void setTitle(String title);

	/**
	 * @return Visual icon
	 */
	PwIcon getIcon();

	void setIcon(PwIcon icon);

	/**
	 * @return Type of Node
	 */
	PwNodeInterface.Type getType();

	/**
	 * Retrieve the parent node
	 * @return PwGroup parent as group
	 */
	PwGroupInterface getParent();

	/**
	 * Assign a parent to this node
	 */
	void setParent(PwGroupInterface prt);

	boolean containsParent();

	void touch(boolean modified, boolean touchParents);

	boolean isContainedIn(PwGroupInterface container);

	boolean isSearchingEnabled();

    boolean containsCustomData();

	/**
	 * Type of available Nodes
	 */
	enum Type {
		GROUP, ENTRY
	}
}
