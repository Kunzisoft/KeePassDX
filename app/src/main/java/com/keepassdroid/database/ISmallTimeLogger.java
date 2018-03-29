package com.keepassdroid.database;

public interface ISmallTimeLogger {

    PwDate getLastModificationTime();
    void setLastModificationTime(PwDate date);

    PwDate getCreationTime();
    void setCreationTime(PwDate date);

    PwDate getLastAccessTime();
    void setLastAccessTime(PwDate date);

    PwDate getExpiryTime();
    void setExpiryTime(PwDate date);

    boolean isExpires();
    void setExpires(boolean exp);
}
