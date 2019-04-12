package com.kunzisoft.keepass.database.element;

import android.support.annotation.NonNull;

import com.kunzisoft.keepass.database.EntryHandler;
import com.kunzisoft.keepass.database.GroupHandler;

import java.util.List;

public interface PwGroupInterface extends PwNodeInterface {

    int getLevel();

    void setLevel(int level);

    List<PwGroupInterface> getChildGroups();

	List<PwEntryInterface> getChildEntries();

	void addChildGroup(PwGroupInterface group);

	void addChildEntry(PwEntryInterface entry);

	void removeChildGroup(PwGroupInterface group);

	void removeChildEntry(PwEntryInterface entry);

	boolean containsParent();

	/**
	 * Filter MetaStream entries and return children
	 * @return List of direct children (one level below) as PwNode
	 */
	List<PwNodeInterface> getChildrenWithoutMetastream();

	boolean allowAddEntryIfIsRoot();

	PwGroupInterface duplicate();

	static void doForEachChildAndForRoot(@NonNull PwGroupInterface root,
											EntryHandler<PwEntryInterface> entryHandler,
											GroupHandler<PwGroupInterface> groupHandler) {
		doForEachChild(root, entryHandler, groupHandler);
        groupHandler.operate(root);
	}

	static boolean doForEachChild(@NonNull PwGroupInterface root,
                                  EntryHandler<PwEntryInterface> entryHandler,
                                  GroupHandler<PwGroupInterface> groupHandler) {
        for (PwEntryInterface entry : root.getChildEntries()) {
            if (!entryHandler.operate(entry)) return false;
        }
		for (PwGroupInterface group : root.getChildGroups()) {
			if ((groupHandler != null) && !groupHandler.operate(group)) return false;
			doForEachChild(group, entryHandler, groupHandler);
		}
		return true;
	}
}
