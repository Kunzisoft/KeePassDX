package com.keepassdroid.database;

public enum PwVersion {
    V3, V4;

    @Override
    public String toString() {
        switch (this) {
            case V3:
                return "KeePass 1";
            case V4:
                return "KeePass 2";
            default:
                return "unknown";
        }
    }
}
