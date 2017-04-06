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

import com.keepassdroid.utils.Types;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacBlockInputStream extends InputStream {
    private LEDataInputStream baseStream;
    private boolean verify;
    private byte[] key;
    private byte[] buffer;
    private int bufferPos = 0;
    private long blockIndex = 0;
    private boolean endOfStream = false;

    public HmacBlockInputStream(InputStream baseStream, boolean verify, byte[] key) {
        super();

        this.baseStream = new LEDataInputStream(baseStream);
        this.verify = verify;
        this.key = key;
        buffer = new byte[0];
    }

    @Override
    public int read() throws IOException {
        if (endOfStream) return -1;

        if (bufferPos == buffer.length) {
            if (!readSafeBlock()) return -1;
        }

        int output = Types.readUByte(buffer, bufferPos);
        bufferPos++;

        return output;
    }

    @Override
    public int read(byte[] outBuffer, int byteOffset, int byteCount) throws IOException {
        int remaining = byteCount;
        while (remaining > 0) {
            if (bufferPos == buffer.length) {
                if (!readSafeBlock()) {
                    int read = byteCount - remaining;
                    if (read <= 0) {
                        return -1;
                    } else {
                        return byteCount - remaining;
                    }
                }
            }

            int copy = Math.min(buffer.length - bufferPos, remaining);
            assert(copy > 0);

            System.arraycopy(buffer, bufferPos, outBuffer, byteOffset, copy);
            byteOffset += copy;
            bufferPos += copy;

            remaining -= copy;
        }

        return byteCount;
    }

    @Override
    public int read(byte[] outBuffer) throws IOException {
        return read(outBuffer, 0, outBuffer.length);
    }

    private boolean readSafeBlock() throws IOException {
        if (endOfStream) return false;

        byte[] storedHmac = baseStream.readBytes(32);
        if (storedHmac == null || storedHmac.length != 32) {
            throw new IOException("File corrupted");
        }

        byte[] pbBlockIndex = LEDataOutputStream.writeLongBuf(blockIndex);
        byte[] pbBlockSize = baseStream.readBytes(4);
        if (pbBlockSize == null || pbBlockSize.length != 4) {
            throw new IOException("File corrupted");
        }
        int blockSize = LEDataInputStream.readInt(pbBlockSize, 0);
        bufferPos = 0;

        buffer = baseStream.readBytes(blockSize);

        if (verify) {
            byte[] cmpHmac;
            byte[] blockKey = HmacBlockStream.GetHmacKey64(key, blockIndex);
            Mac hmac;
            try {
                hmac = Mac.getInstance("HmacSHA256");
                SecretKeySpec signingKey = new SecretKeySpec(blockKey, "HmacSHA256");
                hmac.init(signingKey);
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Invalid Hmac");
            } catch (InvalidKeyException e) {
                throw new IOException("Invalid Hmac");
            }

            hmac.update(pbBlockIndex);
            hmac.update(pbBlockSize);

            if (buffer.length > 0) {
                hmac.update(buffer);
            }

            cmpHmac = hmac.doFinal();
            Arrays.fill(blockKey, (byte)0);

            if (!Arrays.equals(cmpHmac, storedHmac)) {
                throw new IOException("Invalid Hmac");
            }

        }

        blockIndex++;

        if (blockSize == 0) {
            endOfStream = true;
            return false;
        }

        return true;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void close() throws IOException {
        baseStream.close();
    }

    @Override
    public long skip(long byteCount) throws IOException {
        return 0;
    }

    @Override
    public int available() throws IOException {
        return buffer.length - bufferPos;
    }
}
