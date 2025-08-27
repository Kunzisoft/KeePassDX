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
package com.kunzisoft.encrypt

import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import android.util.AndroidException
import android.util.Log
import org.bouncycastle.crypto.engines.ChaCha7539Engine
import org.bouncycastle.crypto.engines.Salsa20Engine
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale

object HashManager {

    private val TAG = HashManager::class.simpleName

    fun getHash256(): MessageDigest {
        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-256 not implemented here.", e)
        }
        return messageDigest
    }

    fun hashSha256(vararg data: ByteArray?): ByteArray {
        val hash: MessageDigest = getHash256()
        for (byteArray in data) {
            if (byteArray != null)
                hash.update(byteArray)
        }
        return hash.digest()
    }

    fun getHash512(): MessageDigest {
        val messageDigest: MessageDigest
        try {
            messageDigest = MessageDigest.getInstance("SHA-512")
        } catch (e: NoSuchAlgorithmException) {
            throw IOException("SHA-256 not implemented here.", e)
        }
        return messageDigest
    }

    private fun hashSha512(vararg data: ByteArray?): ByteArray {
        val hash: MessageDigest = getHash512()
        for (byteArray in data) {
            if (byteArray != null)
                hash.update(byteArray)
        }
        return hash.digest()
    }

    private val SALSA_IV = byteArrayOf(
            0xE8.toByte(),
            0x30,
            0x09,
            0x4B,
            0x97.toByte(),
            0x20,
            0x5D,
            0x2A)

    fun getSalsa20(key: ByteArray): StreamCipher {
        // Build stream cipher key
        val key32 = hashSha256(key)

        val keyParam = KeyParameter(key32)
        val ivParam = ParametersWithIV(keyParam, SALSA_IV)

        val cipher = Salsa20Engine()
        cipher.init(true, ivParam)

        return StreamCipher(cipher)
    }

    fun getChaCha20(key: ByteArray): StreamCipher {
        // Build stream cipher key
        val hash = hashSha512(key)
        val key32 = ByteArray(32)
        val iv = ByteArray(12)

        System.arraycopy(hash, 0, key32, 0, 32)
        System.arraycopy(hash, 32, iv, 0, 12)

        val keyParam = KeyParameter(key32)
        val ivParam = ParametersWithIV(keyParam, iv)

        val cipher = ChaCha7539Engine()
        cipher.init(true, ivParam)

        return StreamCipher(cipher)
    }

    private const val SIGNATURE_DELIMITER = "##SIG##"

    /**
     * Converts a Signature object into its SHA-256 fingerprint string.
     * The fingerprint is typically represented as uppercase hex characters separated by colons.
     */
    private fun signatureToSha256Fingerprint(signature: Signature): String? {
        return try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val x509Certificate = certificateFactory.generateCertificate(
                signature.toByteArray().inputStream()
            ) as X509Certificate

            val messageDigest = MessageDigest.getInstance("SHA-256")
            val digest = messageDigest.digest(x509Certificate.encoded)

            // Format as colon-separated HEX uppercase string
            digest.joinToString(separator = ":") { byte -> "%02X".format(byte) }
                .uppercase(Locale.US)
        } catch (e: Exception) {
            Log.e("SigningInfoUtil", "Error converting signature to SHA-256 fingerprint", e)
            null
        }
    }

    /**
     * Retrieves all relevant SHA-256 signature fingerprints for a given package.
     *
     * @param signingInfo The SigningInfo object to retrieve the strings signatures
     * @return A List of SHA-256 fingerprint strings, or null if an error occurs or no signatures are found.
     */
    fun getAllSignatures(signingInfo: SigningInfo?): List<String>? {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                throw AndroidException("API level ${Build.VERSION.SDK_INT} not supported")
            val signatures = mutableSetOf<String>()
            if (signingInfo != null) {
                // Includes past and current keys if rotation occurred. This is generally preferred.
                signingInfo.signingCertificateHistory?.forEach { signature ->
                    signatureToSha256Fingerprint(signature)?.let { signatures.add(it) }
                }
                // If only one signer and history is empty (e.g. new app), this might be needed.
                // Or if multiple signers are explicitly used for the APK content.
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners?.forEach { signature ->
                        signatureToSha256Fingerprint(signature)?.let { signatures.add(it) }
                    }
                } else { // Fallback for single signer if history was somehow null/empty
                    signingInfo.signingCertificateHistory?.firstOrNull()?.let {
                        signatureToSha256Fingerprint(it)?.let { fp -> signatures.add(fp) }
                    }
                }
            }
            return if (signatures.isEmpty()) null else signatures.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting signatures", e)
            return null
        }
    }

    /**
     * Combines a list of signature into a single string for database storage.
     *
     * @return A single string with fingerprints joined by a delimiter, or null if the input list is null or empty.
     */
    fun SigningInfo.getApplicationSignatures(): String? {
        val fingerprints = getAllSignatures(this)
        if (fingerprints.isNullOrEmpty()) {
            return null
        }
        return fingerprints.joinToString(SIGNATURE_DELIMITER)
    }
}
