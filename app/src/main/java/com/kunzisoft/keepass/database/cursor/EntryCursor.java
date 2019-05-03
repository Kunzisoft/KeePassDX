package com.kunzisoft.keepass.database.cursor;

import android.database.MatrixCursor;
import android.provider.BaseColumns;

import com.kunzisoft.keepass.database.element.*;

import java.util.UUID;

public abstract class EntryCursor<PwEntryV extends PwEntry> extends MatrixCursor {

    protected long entryId;
    public static final String _ID = BaseColumns._ID;
    public static final String COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS = "UUID_most_significant_bits";
    public static final String COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS = "UUID_least_significant_bits";
    public static final String COLUMN_INDEX_TITLE = "title";
    public static final String COLUMN_INDEX_ICON_STANDARD = "icon_standard";
    public static final String COLUMN_INDEX_ICON_CUSTOM_UUID_MOST_SIGNIFICANT_BITS = "icon_custom_UUID_most_significant_bits";
    public static final String COLUMN_INDEX_ICON_CUSTOM_UUID_LEAST_SIGNIFICANT_BITS = "icon_custom_UUID_least_significant_bits";
    public static final String COLUMN_INDEX_USERNAME = "username";
    public static final String COLUMN_INDEX_PASSWORD = "password";
    public static final String COLUMN_INDEX_URL = "URL";
    public static final String COLUMN_INDEX_NOTES = "notes";

    public EntryCursor() {
        super(new String[]{ _ID,
                COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS,
                COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS,
                COLUMN_INDEX_TITLE,
                COLUMN_INDEX_ICON_STANDARD,
                COLUMN_INDEX_ICON_CUSTOM_UUID_MOST_SIGNIFICANT_BITS,
                COLUMN_INDEX_ICON_CUSTOM_UUID_LEAST_SIGNIFICANT_BITS,
                COLUMN_INDEX_USERNAME,
                COLUMN_INDEX_PASSWORD,
                COLUMN_INDEX_URL,
                COLUMN_INDEX_NOTES});
        entryId = 0;
    }

    public abstract void addEntry(PwEntryV entry);

    public void populateEntry(PwEntryV pwEntry, PwIconFactory iconFactory) {
        pwEntry.setNodeId(new PwNodeIdUUID(
                new UUID(getLong(getColumnIndex(EntryCursor.COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS)),
                        getLong(getColumnIndex(EntryCursor.COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS)))));
        pwEntry.setTitle(getString(getColumnIndex(EntryCursor.COLUMN_INDEX_TITLE)));

        PwIconStandard iconStandard = iconFactory.getIcon(getInt(getColumnIndex(EntryCursor.COLUMN_INDEX_ICON_STANDARD)));
        pwEntry.setIcon(iconStandard);

        pwEntry.setUsername(getString(getColumnIndex(EntryCursor.COLUMN_INDEX_USERNAME)));
        pwEntry.setPassword(getString(getColumnIndex(EntryCursor.COLUMN_INDEX_PASSWORD)));
        pwEntry.setUrl(getString(getColumnIndex(EntryCursor.COLUMN_INDEX_URL)));
        pwEntry.setNotes(getString(getColumnIndex(EntryCursor.COLUMN_INDEX_NOTES)));
    }

}
