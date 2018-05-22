/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.crypto.keyDerivation;

import android.content.res.Resources;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.utils.Types;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.UUID;

public class Argon2Kdf extends KdfEngine {

    public static final UUID CIPHER_UUID = Types.bytestoUUID(
            new byte[]{(byte) 0xEF, (byte) 0x63, (byte) 0x6D, (byte) 0xDF, (byte) 0x8C, (byte) 0x29, (byte) 0x44, (byte) 0x4B,
                    (byte) 0x91, (byte) 0xF7, (byte) 0xA9, (byte) 0xA4, (byte)0x03, (byte) 0xE3, (byte) 0x0A, (byte) 0x0C
            });

    private static final String ParamSalt = "S"; // byte[]
    private static final String ParamParallelism = "P"; // UInt32
    private static final String ParamMemory = "M"; // UInt64
    private static final String ParamIterations = "I"; // UInt64
    private static final String ParamVersion = "V"; // UInt32
    private static final String ParamSecretKey = "K"; // byte[]
    private static final String ParamAssocData = "A"; // byte[]

    private static final long MinVersion = 0x10;
    private static final long MaxVersion = 0x13;

    private static final int MinSalt = 8;
    private static final int MaxSalt = Integer.MAX_VALUE;

    private static final long MinIterations = 1;
    private static final long MaxIterations = 4294967295L;

    private static final long MinMemory = 1024 * 8;
    private static final long MaxMemory = Integer.MAX_VALUE;

    private static final int MinParallelism = 1;
    private static final int MaxParallelism = (1 << 24) - 1;

    private static final long DefaultIterations = 2;
    private static final long DefaultMemory = 1024 * 1024;
    private static final long DefaultParallelism = 2;

    private static final String DEFAULT_NAME = "Argon2";

    Argon2Kdf() {
        setUUID(CIPHER_UUID);
    }

    @Override
    public String getName(Resources resources) {
        if (resources == null)
            return DEFAULT_NAME;
        return resources.getString(R.string.kdf_Argon2);
    }

    @Override
    public KdfParameters getDefaultParameters() {
        KdfParameters p = new KdfParameters(uuid);

        p.setParamUUID();
        p.setUInt32(ParamParallelism, DefaultParallelism);
        p.setUInt64(ParamMemory, DefaultMemory);
        p.setUInt64(ParamIterations, DefaultIterations);
        p.setUInt32(ParamVersion, MaxVersion);

        return p;
    }

    @Override
    public byte[] transform(byte[] masterKey, KdfParameters p) throws IOException {

        byte[] salt = p.getByteArray(ParamSalt);
        int parallelism = (int)p.getUInt32(ParamParallelism);
        long memory = p.getUInt64(ParamMemory);
        long iterations = p.getUInt64(ParamIterations);
        long version = p.getUInt32(ParamVersion);
        byte[] secretKey = p.getByteArray(ParamSecretKey);
        byte[] assocData = p.getByteArray(ParamAssocData);

        return Argon2Native.transformKey(masterKey, salt, parallelism, memory, iterations,
                secretKey, assocData, version);
    }

    @Override
    public void randomize(KdfParameters p) {
        SecureRandom random = new SecureRandom();

        byte[] salt = new byte[32];
        random.nextBytes(salt);

        p.setByteArray(ParamSalt, salt);
    }

    @Override
    public long getKeyRounds(KdfParameters p) {
        return p.getUInt64(ParamIterations);
    }

    @Override
    public void setKeyRounds(KdfParameters p, long keyRounds) {
        p.setUInt64(ParamIterations, keyRounds);
    }

    @Override
    public long getDefaultKeyRounds() {
        return DefaultIterations;
    }


    @Override
    public long getMemoryUsage(KdfParameters p) {
        return p.getUInt64(ParamMemory);
    }

    public void setMemoryUsage(KdfParameters p, long memory) {
        p.setUInt64(ParamMemory, memory);
    }

    public long getDefaultMemoryUsage() {
        return DefaultMemory;
    }

    @Override
    public int getParallelism(KdfParameters p) {
        return (int) p.getUInt32(ParamParallelism); // TODO Verify
    }

    public void setParallelism(KdfParameters p, int parallelism) {
        p.setUInt32(ParamParallelism, parallelism);
    }

    public int getDefaultParallelism() {
        return (int) DefaultParallelism; // TODO Verify
    }
}
