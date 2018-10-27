package com.kunzisoft.keepass.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Field implements Parcelable {

    private String name;
    private String value;

    public Field(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Field(Parcel in) {
        this.name = in.readString();
        this.value = in.readString();
    }

    public static final Creator<Field> CREATOR = new Creator<Field>() {
        @Override
        public Field createFromParcel(Parcel in) {
            return new Field(in);
        }

        @Override
        public Field[] newArray(int size) {
            return new Field[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(value);
    }
}
