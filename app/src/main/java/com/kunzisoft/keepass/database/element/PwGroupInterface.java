package com.kunzisoft.keepass.database.element;

import android.support.annotation.NonNull;

import com.kunzisoft.keepass.database.EntryHandler;
import com.kunzisoft.keepass.database.GroupHandler;

import java.util.List;

public interface PwGroupInterface extends PwNodeInterface {

	List<PwGroupInterface> getChildGroups();

	List<PwEntryInterface> getChildEntries();

	void setGroups(List<PwGroupInterface> groups);

	void setEntries(List<PwEntryInterface> entries);

	int getLevel();

	void addChildGroup(PwGroupInterface group);

	void addChildEntry(PwEntryInterface entry);

	PwGroupInterface getChildGroupAt(int number);

	PwEntryInterface getChildEntryAt(int number);

	void removeChildGroup(PwGroupInterface group);

	void removeChildEntry(PwEntryInterface entry);

	int numbersOfChildGroups();

	int numbersOfChildEntries();

	boolean containsParent();

	/**
	 * Filter MetaStream entries and return children
	 * @return List of direct children (one level below) as PwNode
	 */
	List<PwNodeInterface> getDirectChildren();

	PwGroupInterface duplicate();

	static boolean preOrderTraverseTree(@NonNull PwGroupInterface root,
										GroupHandler<PwGroupInterface> groupHandler,
										EntryHandler<PwEntryInterface> entryHandler) {
		if (entryHandler != null) {
			for (PwEntryInterface entry : root.getChildEntries()) {
				if (!entryHandler.operate(entry)) return false;
			}
		}
		for (PwGroupInterface group : root.getChildGroups()) {
			if ((groupHandler != null) && !groupHandler.operate(group)) return false;
			preOrderTraverseTree(group, groupHandler, entryHandler);
		}
		return true;
	}

	boolean allowAddEntryIfIsRoot();
}
