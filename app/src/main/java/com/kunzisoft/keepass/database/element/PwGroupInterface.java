package com.kunzisoft.keepass.database.element;

import com.kunzisoft.keepass.database.EntryHandler;
import com.kunzisoft.keepass.database.GroupHandler;

import java.util.List;

public interface PwGroupInterface extends PwNodeInterface {

	PwNodeId getId();
	void setId(PwNodeId id);

	List<PwGroupInterface> getChildGroups();

	List<PwEntryInterface> getChildEntries();

	void setGroups(List<PwGroupInterface> groups);

	void setEntries(List<PwEntryInterface> entries);

	void addChildGroup(PwGroupInterface group);

	void addChildEntry(PwEntryInterface entry);

	PwGroupInterface getChildGroupAt(int number);

	PwEntryInterface getChildEntryAt(int number);

	void removeChildGroup(PwGroupInterface group);

	void removeChildEntry(PwEntryInterface entry);

	int numbersOfChildGroups();

	int numbersOfChildEntries();

	/**
	 * Filter MetaStream entries and return children
	 * @return List of direct children (one level below) as PwNode
	 */
	public List<PwNodeInterface> getDirectChildren();

	PwGroupInterface duplicate();

	public boolean preOrderTraverseTree(GroupHandler<PwGroupInterface> groupHandler,
										EntryHandler<PwEntryInterface> entryHandler);

	public boolean allowAddEntryIfIsRoot();
}
