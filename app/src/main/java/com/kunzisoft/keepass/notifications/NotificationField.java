/*
 *
 * Copyright 2017 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass Libre.
 *
 *  KeePass Libre is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass Libre is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass Libre.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.notifications;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import tech.jgross.keepass.R;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Utility class to manage fields in Notifications
 */
public class NotificationField implements Parcelable {

    private static final String TAG = NotificationField.class.getName();

    private NotificationFieldId id;
    String value;
    String label;
    String copyText;

    public NotificationField(NotificationFieldId id, String value, Resources resources) {
        this.id = id;
        this.value = value;
        this.label = getLabel(resources);
        this.copyText = getCopyText(resources);
    }

    public NotificationField(NotificationFieldId id, String value, String label, Resources resources) {
        this.id = id;
        this.value = value;
        this.label = label;
        this.copyText = getCopyText(resources);
    }

    protected NotificationField(Parcel in) {
        id = NotificationFieldId.values()[in.readInt()];
        value = in.readString();
        label = in.readString();
        copyText = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id.ordinal());
        dest.writeString(value);
        dest.writeString(label);
        dest.writeString(copyText);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<NotificationField> CREATOR = new Creator<NotificationField>() {
        @Override
        public NotificationField createFromParcel(Parcel in) {
            return new NotificationField(in);
        }

        @Override
        public NotificationField[] newArray(int size) {
            return new NotificationField[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationField field = (NotificationField) o;
        return id.equals(field.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public enum NotificationFieldId {
        USERNAME, PASSWORD, FIELD_A, FIELD_B, FIELD_C;

        public static NotificationFieldId[] getAnonymousFieldId() {
            return new NotificationFieldId[] {FIELD_A, FIELD_B, FIELD_C};
        }
    }

    private static final String ACTION_COPY_PREFIX = "ACTION_COPY_";
    private static final String EXTRA_KEY_PREFIX = "EXTRA_";

    /**
     * Return EXTRA_KEY link to ACTION_KEY, or null if ACTION_KEY is unknown
     */
    public static @Nullable String getExtraKeyLinkToActionKey(String actionKey) {
        try {
            if (actionKey.startsWith(ACTION_COPY_PREFIX)) {
                String idName = actionKey.substring(ACTION_COPY_PREFIX.length(), actionKey.length());
                return getExtraKey(NotificationFieldId.valueOf(idName));
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't get Extra Key from Action Key", e);
        }
        return null;
    }

    private static String getActionKey(NotificationFieldId id) {
        return ACTION_COPY_PREFIX + id.name();
    }

    public String getActionKey() {
        return getActionKey(id);
    }

    private static String getExtraKey(NotificationFieldId id) {
        return EXTRA_KEY_PREFIX + id.name();
    }

    public String getExtraKey() {
        return getExtraKey(id);
    }

    public static List<String> getAllActionKeys() {
        List<String> actionKeys = new ArrayList<>();
        for (NotificationFieldId id : NotificationFieldId.values()) {
            actionKeys.add(getActionKey(id));
        }
        return actionKeys;
    }

    private String getLabel(Resources resources) {
        switch (id) {
            case USERNAME:
                return resources.getString(R.string.entry_user_name);
            case PASSWORD:
                return resources.getString(R.string.entry_password);
            default:
                return id.name();
        }
    }

    private String getCopyText(Resources resources) {
        return resources.getString(R.string.select_to_copy, label);
    }
}
