package com.kunzisoft.keepass.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Entry implements Parcelable {

    private String title;
    private String username;
    private String password;
    private String url;
    private List<Field> customFields;

    public Entry() {
        this.title = "";
        this.username = "";
        this.password = "";
        this.url = "";
        this.customFields = new ArrayList<>();
    }

    protected Entry(Parcel in) {
        title = in.readString();
        username = in.readString();
        password = in.readString();
        url = in.readString();
        //noinspection unchecked
        customFields = in.readArrayList(Field.class.getClassLoader());
    }

    public static final Creator<Entry> CREATOR = new Creator<Entry>() {
        @Override
        public Entry createFromParcel(Parcel in) {
            return new Entry(in);
        }

        @Override
        public Entry[] newArray(int size) {
            return new Entry[size];
        }
    };

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Field> getCustomFields() {
        return customFields;
    }

    public void addCustomField(Field field) {
        this.customFields.add(field);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeString(username);
        parcel.writeString(password);
        parcel.writeString(url);
        parcel.writeArray(customFields.toArray());
    }
}
