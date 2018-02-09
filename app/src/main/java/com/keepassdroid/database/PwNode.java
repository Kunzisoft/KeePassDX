package com.keepassdroid.database;

public abstract class PwNode {
    public abstract Type getType();

    public enum Type {
        GROUP, ENTRY
    }
}
