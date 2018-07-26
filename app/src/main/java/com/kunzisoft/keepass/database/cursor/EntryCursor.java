package com.kunzisoft.keepass.database.cursor;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.BaseColumns;
import android.util.Log;

import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwEntryV3;
import com.kunzisoft.keepass.database.PwEntryV4;
import com.kunzisoft.keepass.database.PwIconCustom;
import com.kunzisoft.keepass.database.PwIconFactory;
import com.kunzisoft.keepass.database.PwIconStandard;

import java.util.UUID;

public class EntryCursor extends MatrixCursor {

    public static final String _ID = BaseColumns._ID;
    public static final String COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS = "UUIDMostSignificantBits";
    public static final String COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS = "UUIDLeastSignificantBits";
    public static final String COLUMN_INDEX_TITLE = "title";
    public static final String COLUMN_INDEX_ICON_STANDARD = "iconStandard";
    public static final String COLUMN_INDEX_ICON_CUSTOM_UUID_MOST_SIGNIFICANT_BITS = "iconCustomUUIDMostSignificantBits";
    public static final String COLUMN_INDEX_ICON_CUSTOM_UUID_LEAST_SIGNIFICANT_BITS = "iconCustomUUIDLeastSignificantBits";

    public EntryCursor() {
        super(new String[]{ _ID,
                COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS,
                COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS,
                COLUMN_INDEX_TITLE,
                COLUMN_INDEX_ICON_STANDARD,
                COLUMN_INDEX_ICON_CUSTOM_UUID_MOST_SIGNIFICANT_BITS,
                COLUMN_INDEX_ICON_CUSTOM_UUID_LEAST_SIGNIFICANT_BITS});
    }

    public static void addEntry(MatrixCursor cursor, PwEntryV3 entry, int id) {
        cursor.addRow(new Object[] {id,
                entry.getUUID().getMostSignificantBits(),
                entry.getUUID().getLeastSignificantBits(),
                entry.getTitle(),
                entry.getIconStandard().getIconId(),
                PwDatabase.UUID_ZERO.getMostSignificantBits(),
                PwDatabase.UUID_ZERO.getLeastSignificantBits()});
    }

    public static void addEntry(MatrixCursor cursor, PwEntryV4 entry, int id) {
        cursor.addRow(new Object[] {id,
                entry.getUUID().getMostSignificantBits(),
                entry.getUUID().getLeastSignificantBits(),
                entry.getTitle(),
                entry.getIconStandard().getIconId(),
                entry.getCustomIcon().getUUID().getMostSignificantBits(),
                entry.getCustomIcon().getUUID().getLeastSignificantBits()});
    }

    private static void populateEntryBaseVersion(Cursor cursor, PwEntry pwEntry, PwIconFactory iconFactory) {
        pwEntry.setUUID(
                new UUID(cursor.getLong(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS)),
                        cursor.getLong(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS))));
        pwEntry.setTitle(cursor.getString(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_TITLE)));

        PwIconStandard iconStandard = iconFactory.getIcon(cursor.getInt(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_ICON_STANDARD)));
        pwEntry.setIcon(iconStandard);
    }

    public static void populateEntry(Cursor cursor, PwEntryV3 pwEntry, PwIconFactory iconFactory) {
        populateEntryBaseVersion(cursor, pwEntry, iconFactory);
    }

    public static void populateEntry(Cursor cursor, PwEntryV4 pwEntry, PwIconFactory iconFactory) {
        populateEntryBaseVersion(cursor, pwEntry, iconFactory);

        PwIconCustom iconCustom = iconFactory.getIcon(
                new UUID(cursor.getLong(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_ICON_CUSTOM_UUID_MOST_SIGNIFICANT_BITS)),
                        cursor.getLong(cursor.getColumnIndex(EntryCursor.COLUMN_INDEX_ICON_CUSTOM_UUID_LEAST_SIGNIFICANT_BITS))));
        pwEntry.setCustomIcon(iconCustom);
    }

}
