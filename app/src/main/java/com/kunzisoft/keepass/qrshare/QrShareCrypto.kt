package com.kunzisoft.keepass.qrshare

import com.kunzisoft.encrypt.argon2.Argon2Transformer
import com.kunzisoft.encrypt.argon2.Argon2Type
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object QrShareCrypto {

    private const val ARGON2_MEMORY_KIB = 65536L
    private const val ARGON2_ITERATIONS = 3L
    private const val ARGON2_PARALLELISM = 1L
    private const val ARGON2_VERSION = 0x13
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val GCM_TAG_BITS = 128
    private const val VERSION_BYTE: Byte = 1

    private val secureRandom = SecureRandom()

    fun encrypt(plaintext: ByteArray, pin: CharArray): ByteArray {
        val salt = ByteArray(SALT_SIZE).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        val key = deriveKey(pin, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            val ciphertext = cipher.doFinal(plaintext)
            return ByteBuffer.allocate(1 + SALT_SIZE + IV_SIZE + ciphertext.size)
                .put(VERSION_BYTE)
                .put(salt)
                .put(iv)
                .put(ciphertext)
                .array()
        } finally {
            key.fill(0)
        }
    }

    @Throws(AEADBadTagException::class, IllegalArgumentException::class)
    fun decrypt(payload: ByteArray, pin: CharArray): ByteArray {
        val minSize = 1 + SALT_SIZE + IV_SIZE + GCM_TAG_BITS / 8
        if (payload.size < minSize) throw IllegalArgumentException("Payload too short")
        val buf = ByteBuffer.wrap(payload)
        val version = buf.get()
        if (version != VERSION_BYTE) throw IllegalArgumentException("Unknown version: $version")
        val salt = ByteArray(SALT_SIZE).also { buf.get(it) }
        val iv = ByteArray(IV_SIZE).also { buf.get(it) }
        val ciphertext = ByteArray(buf.remaining()).also { buf.get(it) }
        val key = deriveKey(pin, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            return cipher.doFinal(ciphertext)
        } finally {
            key.fill(0)
        }
    }

    private fun deriveKey(pin: CharArray, salt: ByteArray): ByteArray {
        val pinBytes = String(pin).toByteArray(Charsets.UTF_8)
        return try {
            Argon2Transformer.transformKey(
                type = Argon2Type.ARGON2_ID,
                password = pinBytes,
                salt = salt,
                parallelism = ARGON2_PARALLELISM,
                memory = ARGON2_MEMORY_KIB,
                iterations = ARGON2_ITERATIONS,
                version = ARGON2_VERSION
            )
        } finally {
            pinBytes.fill(0)
        }
    }
}
