package com.kunzisoft.keepass.database.cursor;

import com.kunzisoft.keepass.database.element.PwEntryV4;
import com.kunzisoft.keepass.database.element.PwIconCustom;
import com.kunzisoft.keepass.database.element.PwIconFactory;

import java.util.UUID;

public class EntryCursorV4 extends EntryCursor<PwEntryV4> {

    private ExtraFieldCursor extraFieldCursor;

    public EntryCursorV4() {
        super();
        extraFieldCursor = new ExtraFieldCursor();
    }

    public void addEntry(PwEntryV4 entry) {
        addRow(new Object[] {entryId,
                entry.getNodeId().getId().getMostSignificantBits(),
                entry.getNodeId().getId().getLeastSignificantBits(),
                entry.getTitle(),
                entry.getIcon().getIconId(),
                entry.getIconCustom().getUuid().getMostSignificantBits(),
                entry.getIconCustom().getUuid().getLeastSignificantBits(),
                entry.getUsername(),
                entry.getPassword(),
                entry.getUrl(),
                entry.getNotes()});

        entry.getFields().doActionToAllCustomProtectedField((key, value) -> {
            extraFieldCursor.addExtraField(entryId, key, value);
        });

        entryId++;
    }

    public void populateEntry(PwEntryV4 pwEntry, PwIconFactory iconFactory) {
        super.populateEntry(pwEntry, iconFactory);

        // Retrieve custom icon
        PwIconCustom iconCustom = iconFactory.getIcon(
                new UUID(getLong(getColumnIndex(EntryCursor.COLUMN_INDEX_ICON_CUSTOM_UUID_MOST_SIGNIFICANT_BITS)),
                        getLong(getColumnIndex(EntryCursor.COLUMN_INDEX_ICON_CUSTOM_UUID_LEAST_SIGNIFICANT_BITS))));
        pwEntry.setIconCustom(iconCustom);

        // Retrieve extra fields
        if (extraFieldCursor.moveToFirst()) {
            while (!extraFieldCursor.isAfterLast()) {
                // Add a new extra field only if entryId is the one we want
                if (extraFieldCursor.getLong(extraFieldCursor.getColumnIndex(ExtraFieldCursor.FOREIGN_KEY_ENTRY_ID))
                        == getLong(getColumnIndex(EntryCursor._ID))) {
                    extraFieldCursor.populateExtraFieldInEntry(pwEntry);
                }
                extraFieldCursor.moveToNext();
            }
        }
    }
}
