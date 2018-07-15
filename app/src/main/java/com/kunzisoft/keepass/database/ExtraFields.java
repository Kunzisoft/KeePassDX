/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database;

import android.os.Parcel;
import android.os.Parcelable;

import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.utils.MemUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.kunzisoft.keepass.database.PwEntryV4.STR_NOTES;
import static com.kunzisoft.keepass.database.PwEntryV4.STR_PASSWORD;
import static com.kunzisoft.keepass.database.PwEntryV4.STR_TITLE;
import static com.kunzisoft.keepass.database.PwEntryV4.STR_URL;
import static com.kunzisoft.keepass.database.PwEntryV4.STR_USERNAME;

public class ExtraFields implements Parcelable, Cloneable {

    private Map<String, ProtectedString> fields;

    public ExtraFields() {
        fields = new HashMap<>();
    }

    public ExtraFields(Parcel in) {
        fields = MemUtil.readStringParcelableMap(in, ProtectedString.class);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        MemUtil.writeStringParcelableMap(dest, flags, fields);
    }

    public static final Parcelable.Creator<ExtraFields> CREATOR = new Parcelable.Creator<ExtraFields>() {
        @Override
        public ExtraFields createFromParcel(Parcel in) {
            return new ExtraFields(in);
        }

        @Override
        public ExtraFields[] newArray(int size) {
            return new ExtraFields[size];
        }
    };

    public boolean containsCustomFields() {
        return !getCustomProtectedFields().keySet().isEmpty();
    }

    public boolean containsCustomFieldsProtected() {
        for (Map.Entry<String, ProtectedString> field : getCustomProtectedFields().entrySet()) {
            if (field.getValue().isProtected())
                return true;
        }
        return false;
    }

    public boolean containsCustomFieldsNotProtected() {
        for (Map.Entry<String, ProtectedString> field : getCustomProtectedFields().entrySet()) {
            if (!field.getValue().isProtected())
                return true;
        }
        return false;
    }

    public String getProtectedStringValue(String key) {
        ProtectedString value = fields.get(key);
        if ( value == null ) return "";
        return value.toString();
    }

    public void putProtectedString(String key, ProtectedString protectedString) {
        fields.put(key, protectedString);
    }

    public void putProtectedString(String key, String value, boolean protect) {
        ProtectedString ps = new ProtectedString(protect, value);
        fields.put(key, ps);
    }

    /**
     * @return list of standard and customized fields
     */
    public Map<String, ProtectedString> getListOfAllFields() {
        return fields;
    }

    public void doActionToAllCustomProtectedField(ActionProtected actionProtected) {
        for (Map.Entry<String, ProtectedString> field : getCustomProtectedFields().entrySet()) {
            actionProtected.doAction(field.getKey(), field.getValue());
        }
    }

    public interface ActionProtected {
        void doAction(String key, ProtectedString value);
    }

    private Map<String, ProtectedString> getCustomProtectedFields() {
        Map<String, ProtectedString> protectedFields = new HashMap<>();
        if (fields.size() > 0) {
            for (Map.Entry<String, ProtectedString> pair : fields.entrySet()) {
                String key = pair.getKey();
                if (isNotStandardField(key)) {
                    protectedFields.put(key, pair.getValue());
                }
            }
        }
        return protectedFields;
    }

    public void removeAllCustomFields() {
        Iterator<Map.Entry<String, ProtectedString>> iter = fields.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ProtectedString> pair = iter.next();
            if (isNotStandardField(pair.getKey())) {
                iter.remove();
            }
        }
    }

    private static boolean isNotStandardField(String key) {
        return !key.equals(STR_TITLE) && !key.equals(STR_USERNAME)
                && !key.equals(STR_PASSWORD) && !key.equals(STR_URL)
                && !key.equals(STR_NOTES);
    }

    @Override
    public ExtraFields clone() {
        try {
            ExtraFields clone = (ExtraFields) super.clone();
            clone.fields = copyMap(this.fields);
            return clone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, ProtectedString> copyMap(
            Map<String, ProtectedString> original) {
        HashMap<String, ProtectedString> copy = new HashMap<>();
        for (Map.Entry<String, ProtectedString> entry : original.entrySet()) {
            copy.put(entry.getKey(), new ProtectedString(entry.getValue()));
        }
        return copy;
    }
}
