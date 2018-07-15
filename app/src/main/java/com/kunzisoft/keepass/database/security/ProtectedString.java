/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.database.security;

import android.os.Parcel;
import android.os.Parcelable;

public class ProtectedString implements Parcelable {

	private boolean protect;
	private String string;
	
	public ProtectedString() {
		this(false, "");
	}

    public ProtectedString(ProtectedString toCopy) {
        this.protect = toCopy.protect;
		this.string = toCopy.string;
    }
	
	public ProtectedString(boolean enableProtection, String string) {
		this.protect = enableProtection;
		this.string = string;
	}

	public ProtectedString(Parcel in) {
		protect = in.readByte() != 0;
		string = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByte((byte) (protect ? 1 : 0));
		dest.writeString(string);
	}

	public static final Parcelable.Creator<ProtectedString> CREATOR = new Parcelable.Creator<ProtectedString>() {
		@Override
		public ProtectedString createFromParcel(Parcel in) {
			return new ProtectedString(in);
		}

		@Override
		public ProtectedString[] newArray(int size) {
			return new ProtectedString[size];
		}
	};

	public boolean isProtected() {
		return protect;
	}

	public int length() {
		if (string == null) {
			return 0;
		}

		return string.length();
	}

	@Override
	public String toString() {
		return string;
	}

}
