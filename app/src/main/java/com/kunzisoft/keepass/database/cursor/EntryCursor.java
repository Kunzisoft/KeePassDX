package com.kunzisoft.keepass.database.cursor;

import android.database.MatrixCursor;
import android.provider.BaseColumns;

import com.kunzisoft.keepass.database.PwDatabase;
import com.kunzisoft.keepass.database.PwEntry;
import com.kunzisoft.keepass.database.PwEntryV3;
import com.kunzisoft.keepass.database.PwEntryV4;
import com.kunzisoft.keepass.database.PwIconCustom;
import com.kunzisoft.keepass.database.PwIconFactory;
import com.kunzisoft.keepass.database.PwIconStandard;

import java.util.UUID;

public class EntryCursor extends MatrixCursor {

    private long entryId;
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

    private ExtraFieldCursor extraFieldCursor;

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
        extraFieldCursor = new ExtraFieldCursor();
    }

    public void addEntry(PwEntryV3 entry) {
        addRow(new Object[] {entryId,
                entry.getUUID().getMostSignificantBits(),
                entry.getUUID().getLeastSignificantBits(),
                entry.getTitle(),
                entry.getIconStandard().getIconId(),
                PwDatabase.UUID_ZERO.getMostSignificantBits(),
                PwDatabase.UUID_ZERO.getLeastSignificantBits(),
                entry.getUsername(),
                entry.getPassword(),
                entry.getUrl(),
                entry.getNotes()});
        entryId++;
    }

    public void addEntry(PwEntryV4 entry) {
        addRow(new Object[] {entryId,
                entry.getUUID().getMostSignificantBits(),
                entry.getUUID().getLeastSignificantBits(),
                entry.getTitle(),
                entry.getIconStandard().getIconId(),
                entry.getIconCustom().getUUID().getMostSignificantBits(),
                entry.getIconCustom().getUUID().getLeastSignificantBits(),
                entry.getUsername(),
                entry.getPassword(),
                entry.getUrl(),
                entry.getNotes()});

        entry.getFields().doActionToAllCustomProtectedField((key, value) -> {
            extraFieldCursor.addExtraField(entryId, key, value);
        });

        entryId++;
    }

    private void populateEntryBaseVersion(PwEntry pwEntry, PwIconFactory iconFactory) {
        pwEntry.setUUID(
                new UUID(getLong(getColumnIndex(EntryCursor.COLUMN_INDEX_UUID_MOST_SIGNIFICANT_BITS)),
                        getLong(getColumnIndex(EntryCursor.COLUMN_INDEX_UUID_LEAST_SIGNIFICANT_BITS))));
        pwEntry.setTitle(getString(getColumnIndex(EntryCursor.COLUMN_INDEX_TITLE)));

        PwIconStandard iconStandard = iconFactory.getIcon(getInt(getColumnIndex(EntryCursor.COLUMN_INDEX_ICON_STANDARD)));
        pwEntry.setIconStandard(iconStandard);

        pwEntry.setUsername(getString(getColumnIndex(EntryCursor.COLUMN_INDEX_USERNAME)));
        pwEntry.setPassword(getString(getColumnIndex(EntryCursor.COLUMN_INDEX_PASSWORD)));
        pwEntry.setUrl(getString(getColumnIndex(EntryCursor.COLUMN_INDEX_URL)));
        pwEntry.setNotes(getString(getColumnIndex(EntryCursor.COLUMN_INDEX_NOTES)));
    }

    public void populateEntry(PwEntryV3 pwEntry, PwIconFactory iconFactory) {
        populateEntryBaseVersion(pwEntry, iconFactory);
    }

    public void populateEntry(PwEntryV4 pwEntry, PwIconFactory iconFactory) {
        populateEntryBaseVersion(pwEntry, iconFactory);

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
