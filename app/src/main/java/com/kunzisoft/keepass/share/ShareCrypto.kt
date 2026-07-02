/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.share

import com.kunzisoft.encrypt.argon2.Argon2Transformer
import com.kunzisoft.encrypt.argon2.Argon2Type
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object ShareCrypto {

    private const val ARGON2_MEMORY_KIB = 65536L
    private const val ARGON2_ITERATIONS = 3L
    private const val ARGON2_PARALLELISM = 1L
    private const val ARGON2_VERSION = 0x13
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val GCM_TAG_BITS = 128
    private const val VERSION_BYTE: Byte = 1

    private val secureRandom = SecureRandom()

    /**
     * Encrypts the [plaintext] using AES/GCM/NoPadding with a key derived from [pin].
     *
     * @param plaintext The data to encrypt.
     * @param pin The user-provided PIN for derivation.
     * @param aad Optional Additional Authenticated Data to bind the ciphertext to a specific context.
     * @return A byte array containing version, salt, IV, and ciphertext.
     */
    fun encrypt(
        plaintext: ByteArray,
        pin: CharArray,
        aad: ByteArray? = null
    ): ByteArray {
        val salt = ByteArray(SALT_SIZE).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        val key = deriveKey(pin, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE,
                    SecretKeySpec(key, "AES"),
                    GCMParameterSpec(GCM_TAG_BITS, iv)
                )
                aad?.let { updateAAD(it) }
            }
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
    /**
     * Decrypts the [payload] using AES/GCM/NoPadding with a key derived from [pin].
     *
     * @param payload The encrypted data package.
     * @param pin The user-provided PIN for derivation.
     * @param aad Optional Additional Authenticated Data that must match the data provided during encryption.
     * @throws javax.crypto.AEADBadTagException if the tag is invalid (wrong PIN or tampered data).
     * @throws IllegalArgumentException if the payload is malformed or version is unsupported.
     * @return The decrypted plaintext.
     */
    fun decrypt(
        payload: ByteArray,
        pin: CharArray,
        aad: ByteArray? = null
    ): ByteArray {
        val minSize = 1 + SALT_SIZE + IV_SIZE + GCM_TAG_BITS / 8
        if (payload.size < minSize)
            throw IllegalArgumentException("Payload too short")
        val buf = ByteBuffer.wrap(payload)
        val versionByte = buf.get()
        if (versionByte != VERSION_BYTE)
            throw IllegalArgumentException("Unknown version: $versionByte")
        val salt = ByteArray(SALT_SIZE).also { buf.get(it) }
        val iv = ByteArray(IV_SIZE).also { buf.get(it) }
        val ciphertext = ByteArray(buf.remaining()).also { buf.get(it) }
        val key = deriveKey(pin, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            aad?.let { cipher.updateAAD(it) }
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