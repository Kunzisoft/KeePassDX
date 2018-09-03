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

import java.io.File;
import java.util.Arrays;

public class ProtectedBinary implements Parcelable {
	
	public final static ProtectedBinary EMPTY = new ProtectedBinary();

	private boolean protect;
	private byte[] data;
	private File dataFile;
	
	public boolean isProtected() {
		return protect;
	}
	
	public int length() {
	    // TODO File length
		if (data == null) {
			return 0;
		}
		return data.length;
	}
	
	public ProtectedBinary() {
		this(false, new byte[0]);
	}
	
	public ProtectedBinary(boolean enableProtection, byte[] data) {
		this.protect = enableProtection;
		this.data = data;
		this.dataFile = null;
	}

    public ProtectedBinary(boolean enableProtection, File dataFile) {
        this.protect = enableProtection;
        this.data = new byte[0];
        this.dataFile = dataFile;
    }

	public ProtectedBinary(Parcel in) {
		protect = in.readByte() != 0;
		in.readByteArray(data);
        dataFile = new File(in.readString());
	}
	
	// TODO: replace the byte[] with something like ByteBuffer to make the return
	// value immutable, so we don't have to worry about making deep copies
	public byte[] getData() {
		return data;
	}
	
	public boolean equals(ProtectedBinary rhs) {
		return (protect == rhs.protect)
                && Arrays.equals(data, rhs.data)
                && dataFile.equals(rhs.dataFile);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByte((byte) (protect ? 1 : 0));
		dest.writeByteArray(data);
		dest.writeString(dataFile.getAbsolutePath());
	}

	public static final Creator<ProtectedBinary> CREATOR = new Creator<ProtectedBinary>() {
		@Override
		public ProtectedBinary createFromParcel(Parcel in) {
			return new ProtectedBinary(in);
		}

		@Override
		public ProtectedBinary[] newArray(int size) {
			return new ProtectedBinary[size];
		}
	};

}
