package com.kunzisoft.encrypt

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CipherEngineFactory {

    companion object {

        @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
        fun getAES(opmode: Int, key: ByteArray, IV: ByteArray): Cipher {
            // TODO native
            val androidOverride = false
            val cipher = CipherFactory.getInstance("AES/CBC/PKCS5Padding", androidOverride)
            cipher.init(opmode, SecretKeySpec(key, "AES"), IvParameterSpec(IV))
            return cipher
        }

        @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class)
        fun getTwofish(opmode: Int, key: ByteArray, IV: ByteArray): Cipher {
            // TODO native
            val androidOverride = false
            val cipher: Cipher = if (opmode == Cipher.ENCRYPT_MODE) {
                CipherFactory.getInstance("Twofish/CBC/ZeroBytePadding", androidOverride)
            } else {
                CipherFactory.getInstance("Twofish/CBC/NoPadding", androidOverride)
            }
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
}