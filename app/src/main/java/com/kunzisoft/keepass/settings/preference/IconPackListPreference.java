package com.kunzisoft.keepass.settings.preference;

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.icons.IconPack;
import com.kunzisoft.keepass.icons.IconPackChooser;

import java.util.ArrayList;
import java.util.List;

public class IconPackListPreference extends ListPreference {

    public IconPackListPreference(Context context) {
        this(context, null);
    }

    public IconPackListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    public IconPackListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    public IconPackListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        List<String> entries = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (IconPack iconPack : IconPackChooser.getIconPackList(context)) {
            entries.add(iconPack.getName());
            values.add(iconPack.getId());
        }

        setEntries(entries.toArray(new String[0]));
        setEntryValues(values.toArray(new String[0]));
        setDefaultValue(IconPackChooser.getSelectedIconPack(context).getId());
    }
}
