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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacBlockOutputStream extends OutputStream {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024;
    private LEDataOutputStream baseStream;
    private byte[] key;

    private byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    private int bufferPos = 0;
    private long blockIndex = 0;

    public HmacBlockOutputStream(OutputStream os, byte[] key) {
        this.baseStream = new LEDataOutputStream(os);
        this.key = key;
    }

    @Override
    public void close() throws IOException {
        if (bufferPos == 0) {
            WriteSafeBlock();
        } else {
            WriteSafeBlock();
            WriteSafeBlock();
        }

        baseStream.flush();;
        baseStream.close();
    }

    @Override
    public void flush() throws IOException {
        baseStream.flush();
    }

    @Override
    public void write(byte[] outBuffer) throws IOException {
        write(outBuffer, 0, outBuffer.length);
    }

    @Override
    public void write(byte[] outBuffer, int offset, int count) throws IOException {
        while (count > 0) {
            if (bufferPos == buffer.length) {
                WriteSafeBlock();
            }

            int copy = Math.min(buffer.length - bufferPos, count);
            assert(copy > 0);

            System.arraycopy(outBuffer, offset, buffer, bufferPos, copy);
            offset += copy;
            bufferPos += copy;

            count -= copy;
        }
    }

    @Override
    public void write(int oneByte) throws IOException {
        byte[] outByte = new byte[1];
        write(outByte, 0, 1);
    }

    private void WriteSafeBlock() throws IOException {
        byte[] bufBlockIndex = LEDataOutputStream.writeLongBuf(blockIndex);
        byte[] blockSizeBuf = LEDataOutputStream.writeIntBuf(bufferPos);

        byte[] blockHmac;
        byte[] blockKey = HmacBlockStream.GetHmacKey64(key, blockIndex);

        Mac hmac;
        try {
            hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec signingKey = new SecretKeySpec(blockKey, "HmacSHA256");
            hmac.init(signingKey);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Invalid Hmac");
        } catch (InvalidKeyException e) {
            throw new IOException("Invalid HMAC");
        }

        hmac.update(bufBlockIndex);
        hmac.update(blockSizeBuf);

        if (bufferPos > 0) {
            hmac.update(buffer, 0, bufferPos);
        }

        blockHmac = hmac.doFinal();

        baseStream.write(blockHmac);
        baseStream.write(blockSizeBuf);

        if (bufferPos > 0) {
            baseStream.write(buffer, 0, bufferPos);
        }

        blockIndex++;
        bufferPos = 0;
    }
}
