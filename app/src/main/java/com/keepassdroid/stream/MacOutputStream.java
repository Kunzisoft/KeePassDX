/*
 * Copyright 2017 Brian Pellin.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.stream;

import java.io.IOException;
import java.io.OutputStream;

import javax.crypto.Mac;

public class MacOutputStream extends OutputStream {
    private Mac mac;
    private OutputStream os;

    public MacOutputStream(OutputStream os, Mac mac) {
        this.mac = mac;
        this.os = os;
    }

    @Override
    public void flush() throws IOException {
        os.flush();
    }

    @Override
    public void close() throws IOException {
        os.close();
    }

    @Override
    public void write(int oneByte) throws IOException {
        mac.update((byte) oneByte);
        os.write(oneByte);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        mac.update(buffer, offset, count);
        os.write(buffer, offset, count);
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        mac.update(buffer, 0, buffer.length);
        os.write(buffer);
    }

    public byte[] getMac() {
        return mac.doFinal();
    }
}
