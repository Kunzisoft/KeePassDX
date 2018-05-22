package com.kunzisoft.keepass.database.exception;

import java.io.IOException;

public class UnknownKDF extends IOException {

    private static String message = "Unknown key derivation function";

    public UnknownKDF() {
        super(message);
    }

    public UnknownKDF(Exception e) {
        super(message, e);
    }
}
