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

import com.kunzisoft.keepass.utils.MemUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AutoType implements Parcelable {
    private static final long OBF_OPT_NONE = 0;

    public boolean enabled = true;
    public long obfuscationOptions = OBF_OPT_NONE;
    public String defaultSequence = "";
    private HashMap<String, String> windowSeqPairs = new HashMap<>();

    public AutoType() {}

    public AutoType(AutoType autoType) {
        this.enabled = autoType.enabled;
        this.obfuscationOptions = autoType.obfuscationOptions;
        this.defaultSequence = autoType.defaultSequence;
        for (Map.Entry<String, String> entry: autoType.windowSeqPairs.entrySet()) {
            this.windowSeqPairs.put(entry.getKey(), entry.getValue());
        }
    }

    public AutoType(Parcel in) {
        this.enabled = in.readByte() != 0;
        this.obfuscationOptions = in.readLong();
        this.defaultSequence = in.readString();
        this.windowSeqPairs = MemUtil.readStringParcelableMap(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (enabled ? 1 : 0));
        dest.writeLong(obfuscationOptions);
        dest.writeString(defaultSequence);
        MemUtil.writeStringParcelableMap(dest, windowSeqPairs);
    }

    public static final Parcelable.Creator<AutoType> CREATOR = new Parcelable.Creator<AutoType>() {
        @Override
        public AutoType createFromParcel(Parcel in) {
            return new AutoType(in);
        }

        @Override
        public AutoType[] newArray(int size) {
            return new AutoType[size];
        }
    };

    public void put(String key, String value) {
        windowSeqPairs.put(key, value);
    }

    public Set<Map.Entry<String, String>> entrySet() {
        return windowSeqPairs.entrySet();
    }
}
