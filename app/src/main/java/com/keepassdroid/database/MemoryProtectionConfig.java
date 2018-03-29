package com.keepassdroid.database;

public class MemoryProtectionConfig {

    public boolean protectTitle = false;
    public boolean protectUserName = false;
    public boolean protectPassword = false;
    public boolean protectUrl = false;
    public boolean protectNotes = false;

    public boolean autoEnableVisualHiding = false;

    public boolean isProtected(String field) {
        if (field.equalsIgnoreCase(PwDefsV4.TITLE_FIELD)) return protectTitle;
        if (field.equalsIgnoreCase(PwDefsV4.USERNAME_FIELD)) return protectUserName;
        if (field.equalsIgnoreCase(PwDefsV4.PASSWORD_FIELD)) return protectPassword;
        if (field.equalsIgnoreCase(PwDefsV4.URL_FIELD)) return protectUrl;
        if (field.equalsIgnoreCase(PwDefsV4.NOTES_FIELD)) return protectNotes;

        return false;
    }
}
