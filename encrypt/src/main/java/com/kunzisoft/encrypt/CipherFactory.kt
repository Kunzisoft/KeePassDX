package com.kunzisoft.encrypt

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CipherFactory {

    init {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
    fun getAES(opmode: Int, key: ByteArray, IV: ByteArray, forceNative: Boolean = false): Cipher {
        val transformation = "AES/CBC/PKCS5Padding"
        val cipher = if (forceNative || (!NativeBlockList.isBlocked && NativeLib.loaded())) {
            Cipher.getInstance(transformation, AESProvider())
        } else {
            Cipher.getInstance(transformation)
        }
        cipher.init(opmode, SecretKeySpec(key, "AES"), IvParameterSpec(IV))
        return cipher
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
    fun getTwofish(opmode: Int, key: ByteArray, IV: ByteArray): Cipher {
        val cipher: Cipher = if (opmode == Cipher.ENCRYPT_MODE) {
            Cipher.getInstance("Twofish/CBC/ZeroBytePadding")
        } else {
            Cipher.getInstance("Twofish/CBC/NoPadding")
        }
        // TODO Verify KDB TwoFish
        // CipherFactory.getInstance("Twofish/CBC/PKCS7PADDING")
        cipher.init(opmode, SecretKeySpec(key, "AES"), IvParameterSpec(IV))
        return cipher
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
    fun getChacha20(opmode: Int, key: ByteArray, IV: ByteArray): Cipher {
        val cipher = Cipher.getInstance("Chacha7539", BouncyCastleProvider())
        cipher.init(opmode, SecretKeySpec(key, "ChaCha7539"), IvParameterSpec(IV))
        return cipher
    }
}