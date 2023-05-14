/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.encrypt.aes;

import android.util.Log;

import com.kunzisoft.encrypt.NativeLib;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

public class NativeAESCipherSpi extends CipherSpi {

    private static final String TAG = NativeAESCipherSpi.class.getName();

    private static boolean mIsStaticInit = false;
    private static HashMap<PhantomReference<NativeAESCipherSpi>, Long> mCleanup = new HashMap<>();
    private static ReferenceQueue<NativeAESCipherSpi> mQueue = new ReferenceQueue<>();

    private final int AES_BLOCK_SIZE = 16;
    private byte[] mIV;

    private boolean mIsInit = false;
    private long mCtxPtr;

    private boolean mPadding  = false;

    private static void staticInit() {
        mIsStaticInit = true;

        // Start the cipher context cleanup thread to run forever
        (new Thread(new Cleanup())).start();
    }

    private static void addToCleanupQueue(NativeAESCipherSpi ref, long ptr) {
        Log.d(TAG, "queued cipher context: " + ptr);
        mCleanup.put(new PhantomReference<>(ref, mQueue), ptr);
    }

    /** Work with the garbage collector to clean up openssl memory when the cipher
     *  context is garbage collected.
     * @author bpellin
     */
    private static class Cleanup implements Runnable {

        public void run() {
            while (true) {
                try {
                    Reference<? extends NativeAESCipherSpi> ref = mQueue.remove();

                    long ctx = mCleanup.remove(ref);
                    nCleanup(ctx);
                    Log.d(TAG, "Cleaned up cipher context: " + ctx);

                } catch (InterruptedException e) {
                    // Do nothing, but resume looping if mQueue.remove is interrupted
                }
            }
        }
    }

    private static native void nCleanup(long ctxPtr);

    public NativeAESCipherSpi() {
        if ( !mIsStaticInit ) {
            staticInit();
        }
    }

    @Override
    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen)
            throws IllegalBlockSizeException, BadPaddingException {
        int maxSize = engineGetOutputSize(inputLen);
        byte[] output = new byte[maxSize];

        int finalSize;

        try {
            finalSize = doFinal(input, inputOffset, inputLen, output, 0);
        } catch (ShortBufferException e) {
            // This shouldn't be possible rethrow as RuntimeException
            throw new RuntimeException("Short buffer exception shouldn't be possible from here.");
        }

        if ( maxSize == finalSize ) {
            return output;
        } else {
            // TODO: Special doFinal to avoid this copy
            byte[] exact = new byte[finalSize];
            System.arraycopy(output, 0, exact, 0, finalSize);
            return exact;
        }
    }

    @Override
    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen,
                                byte[] output, int outputOffset) throws ShortBufferException,
            IllegalBlockSizeException, BadPaddingException {

        int result = doFinal(input, inputOffset, inputLen, output, outputOffset);
        if ( result == -1 ) {
            throw new ShortBufferException();
        }
        return result;
    }

    private int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {

        int outputSize = engineGetOutputSize(inputLen);
        int updateAmt;
        if (input != null && inputLen > 0) {
            updateAmt = nUpdate(mCtxPtr, input, inputOffset, inputLen, output, outputOffset, outputSize);
        } else {
            updateAmt = 0;
        }

        int finalAmt = nFinal(mCtxPtr, mPadding, output, outputOffset + updateAmt, outputSize - updateAmt);
        return updateAmt + finalAmt;
    }

    private native int nFinal(long ctxPtr, boolean usePadding, byte[] output, int outputOffest, int outputSize)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException;

    @Override
    protected int engineGetBlockSize() {
        return AES_BLOCK_SIZE;
    }

    @Override
    protected byte[] engineGetIV() {
        byte[] copyIV = new byte[0];
        if (mIV != null) {
            int lengthIV = mIV.length;
            copyIV = new byte[lengthIV];
            System.arraycopy(mIV, 0, copyIV, 0, lengthIV);
        }
        return copyIV;
    }

    @Override
    protected int engineGetOutputSize(int inputLen) {
        return inputLen + nGetCacheSize(mCtxPtr) + AES_BLOCK_SIZE;
    }

    private native int nGetCacheSize(long ctxPtr);

    @Override
    protected AlgorithmParameters engineGetParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void engineInit(int opmode, Key key, SecureRandom random)
            throws InvalidKeyException {

        byte[] ivArray = new byte[16];
        random.nextBytes(ivArray);

        init(opmode, key, new IvParameterSpec(ivArray));
    }

    @Override
    protected void engineInit(int opmode, Key key,
                              AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {

        IvParameterSpec ivparam;

        if ( params instanceof IvParameterSpec ) {
            ivparam = (IvParameterSpec) params;
        } else {
            throw new InvalidAlgorithmParameterException("params must be an IvParameterSpec.");
        }

        init(opmode, key, ivparam);
    }


    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameters params,
                              SecureRandom random) throws InvalidKeyException,
            InvalidAlgorithmParameterException {

        try {
            engineInit(opmode, key, params.getParameterSpec(AlgorithmParameterSpec.class), random);
        } catch (InvalidParameterSpecException e) {
            throw new InvalidAlgorithmParameterException(e);
        }
    }

    private void init(int opmode, Key key, IvParameterSpec params) {
        if (mIsInit) {
            // Do not allow multiple inits
            throw new RuntimeException("Don't allow multiple inits");
        } else {
            NativeLib.INSTANCE.init();
            mIsInit = true;
        }

        mIV = params.getIV();
        mCtxPtr = nInit(opmode == Cipher.ENCRYPT_MODE, key.getEncoded(), mIV);
        addToCleanupQueue(this, mCtxPtr);
    }

    private native long nInit(boolean encrypting, byte[] key, byte[] iv);

    @Override
    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        if ( ! mode.equals("CBC") ) {
            throw new NoSuchAlgorithmException("This only supports CBC mode");
        }
    }

    @Override
    protected void engineSetPadding(String padding)
            throws NoSuchPaddingException {

        if ( !mIsInit) {
            NativeLib.INSTANCE.init();
        }
        if ( padding.length() == 0 ) {
            return;
        }
        if ( !padding.equals("PKCS5Padding") ) {
            throw new NoSuchPaddingException("Only supports PKCS5Padding.");
        }

        mPadding = true;
    }

    @Override
    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        int maxSize = engineGetOutputSize(inputLen);
        byte[] output = new byte[maxSize];

        int updateSize = update(input, inputOffset, inputLen, output, 0);

        if ( updateSize == maxSize ) {
            return output;
        } else {
            // TODO: We could optimize update for this case to avoid this extra copy
            byte[] exact = new byte[updateSize];
            System.arraycopy(output, 0, exact, 0, updateSize);
            return exact;
        }

    }

    @Override
    protected int engineUpdate(byte[] input, int inputOffset, int inputLen,
                               byte[] output, int outputOffset) throws ShortBufferException {

        int result = update(input, inputOffset, inputLen, output, outputOffset);
        if ( result == -1 ) {
            throw new ShortBufferException("Insufficient buffer.");
        }
        return result;
    }

    private int update(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) {
        int outputSize = engineGetOutputSize(inputLen);
        return nUpdate(mCtxPtr, input, inputOffset, inputLen, output, outputOffset, outputSize);
    }

    private native int nUpdate(long ctxPtr, byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset, int outputSize);

}