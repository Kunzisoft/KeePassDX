package com.kunzisoft.encrypt.argon2;

public enum Argon2Type {
    ARGON2_D(0),
    ARGON2_I(1),
    ARGON2_ID(2);

    int cValue = 0;

    Argon2Type(int i) {
        cValue = i;
    }
}
