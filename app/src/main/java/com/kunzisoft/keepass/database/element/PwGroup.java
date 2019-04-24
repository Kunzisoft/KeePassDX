package com.kunzisoft.keepass.database.element;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;

public abstract class PwGroup<Id> extends PwNode<Id> implements PwGroupInterface {

    private String title = "";
    transient private List<PwGroupInterface> childGroups = new ArrayList<>();
    transient private List<PwEntryInterface> childEntries = new ArrayList<>();

    public PwGroup() {
        super();
    }

    public PwGroup(Parcel in) {
        super(in);
        title = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(title);
    }

    protected void updateWith(PwGroup source) {
        super.updateWith(source);
        title = source.title;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String name) {
        this.title = name;
    }

    @Override
    public List<PwGroupInterface> getChildGroups() {
        return childGroups;
    }

    @Override
    public List<PwEntryInterface> getChildEntries() {
        return childEntries;
    }

    @Override
    public void addChildGroup(PwGroupInterface group) {
        this.childGroups.add(group);
    }

    @Override
    public void addChildEntry(PwEntryInterface entry) {
        this.childEntries.add(entry);
    }

    @Override
    public void removeChildGroup(PwGroupInterface group) {
        this.childGroups.remove(group);
    }

    @Override
    public void removeChildEntry(PwEntryInterface entry) {
        this.childEntries.remove(entry);
    }

    @Override
    public int getLevel() {
        return -1;
    }

    @Override
    public void setLevel(int level) {
        // Do nothing here
    }

    @Override
    public List<PwNodeInterface> getChildrenWithoutMetastream() {
        List<PwNodeInterface> children = new ArrayList<>(childGroups);
        for(PwEntryInterface child : childEntries) {
            if (!child.isMetaStream())
                children.add(child);
        }
        return children;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
