package com.kunzisoft.keepass.database.element;

import java.util.UUID;

public interface PwNodeInterface {

	UUID getUUID();

	void setUUID(UUID uuid);

	String getName();

	void setName(String title);

	/**
	 * @return Visual icon
	 */
	PwIcon getIcon();

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

	void touch(boolean modified, boolean touchParents);

	boolean isContainedIn(PwGroupInterface container);

	boolean isSearchingEnabled();

	/**
	 * Type of available Nodes
	 */
	enum Type {
		GROUP, ENTRY
	}
}
