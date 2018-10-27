package com.kunzisoft.keepass.stream;

import java.io.IOException;

public interface ActionReadBytes {
    /**
     * Called after each buffer fill
     * @param buffer filled
     */
    void doAction(byte[] buffer) throws IOException;
}
