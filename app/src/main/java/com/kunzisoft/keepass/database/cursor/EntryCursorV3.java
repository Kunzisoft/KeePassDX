package com.kunzisoft.keepass.database.cursor;

import com.kunzisoft.keepass.database.element.PwDatabase;
import com.kunzisoft.keepass.database.element.PwEntryV3;

public class EntryCursorV3 extends EntryCursor<PwEntryV3> {

    public void addEntry(PwEntryV3 entry) {
        addRow(new Object[] {entryId,
                entry.getNodeId().getId().getMostSignificantBits(),
                entry.getNodeId().getId().getLeastSignificantBits(),
                entry.getTitle(),
                entry.getIcon().getIconId(),
                PwDatabase.UUID_ZERO.getMostSignificantBits(),
                PwDatabase.UUID_ZERO.getLeastSignificantBits(),
                entry.getUsername(),
                entry.getPassword(),
                entry.getUrl(),
                entry.getNotes()});
        entryId++;
    }

}
