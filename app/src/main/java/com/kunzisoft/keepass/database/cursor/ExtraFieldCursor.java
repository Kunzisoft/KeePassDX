package com.kunzisoft.keepass.database.cursor;

import android.database.MatrixCursor;
import android.provider.BaseColumns;

import com.kunzisoft.keepass.database.PwEntryV4;
import com.kunzisoft.keepass.database.security.ProtectedString;

public class ExtraFieldCursor extends MatrixCursor {

    private long fieldId;
    public static final String _ID = BaseColumns._ID;
    public static final String FOREIGN_KEY_ENTRY_ID = "entry_id";
    public static final String COLUMN_LABEL = "label";
    public static final String COLUMN_PROTECTION = "protection";
    public static final String COLUMN_VALUE = "value";

    public ExtraFieldCursor() {
        super(new String[]{ _ID,
                FOREIGN_KEY_ENTRY_ID,
                COLUMN_LABEL,
                COLUMN_PROTECTION,
                COLUMN_VALUE});
        fieldId = 0;
    }

    public synchronized void addExtraField(long entryId, String label, ProtectedString value) {
        addRow(new Object[] {fieldId,
                entryId,
                label,
                (value.isProtected()) ? 1 : 0,
                value.toString()});
        fieldId++;
    }

    public void populateExtraFieldInEntry(PwEntryV4 pwEntry) {

        pwEntry.addExtraField(getString(getColumnIndex(ExtraFieldCursor.COLUMN_LABEL)),
                new ProtectedString((getInt(getColumnIndex(ExtraFieldCursor.COLUMN_PROTECTION)) > 0),
                        getString(getColumnIndex(ExtraFieldCursor.COLUMN_VALUE))));

    }
}
