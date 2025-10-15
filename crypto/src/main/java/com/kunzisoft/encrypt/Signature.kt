/*
 * Copyright 2025 Cali-95 modified by Jeremy Jamet / Kunzisoft
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

import android.content.pm.SigningInfo
import android.os.Build
import android.util.AndroidException
import android.util.Log
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPrivateKey
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator
import org.bouncycastle.util.BigIntegers
import org.bouncycastle.util.io.pem.PemWriter
import java.io.StringReader
import java.io.StringWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Locale

object Signature {

    // see at https://www.iana.org/assignments/cose/cose.xhtml
    const val ES256_ALGORITHM: Long = -7
    const val RS256_ALGORITHM: Long = -257
    private const val RS256_KEY_SIZE_IN_BITS = 2048

    const val ED_DSA_ALGORITHM: Long = -8

    private const val BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----"
    private const val BEGIN_PRIVATE_KEY_LINE_BREAK = "$BEGIN_PRIVATE_KEY\n"
    private const val END_PRIVATE_KEY = "-----END PRIVATE KEY-----"
    private const val  END_PRIVATE_KEY_LINE_BREAK = "\n$END_PRIVATE_KEY"

    init {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())
    }

    fun sign(privateKeyPem: String, message: ByteArray): ByteArray {
        val privateKey = createPrivateKey(privateKeyPem)
        val algorithmKey = privateKey.algorithm
        val algorithmSignature = when (algorithmKey) {
            "EC" -> "SHA256withECDSA"
            "ECDSA" -> "SHA256withECDSA"
            "RSA" -> "SHA256withRSA"
            "Ed25519" -> "Ed25519"
            else -> throw SecurityException("$algorithmKey algorithm is unknown")
        }
        val sig = Signature.getInstance(
            algorithmSignature,
            BouncyCastleProvider.PROVIDER_NAME
        )
        sig.initSign(privateKey)
        sig.update(message)
        return sig.sign()
    }

    fun createPrivateKey(privateKeyPem: String): PrivateKey {
        var privateKeyString = privateKeyPem
        if (privateKeyPem.startsWith(BEGIN_PRIVATE_KEY_LINE_BREAK).not()) {
            privateKeyString = privateKeyString.removePrefix(BEGIN_PRIVATE_KEY)
            privateKeyString = "$BEGIN_PRIVATE_KEY_LINE_BREAK$privateKeyString"
        }
        if (privateKeyPem.endsWith(END_PRIVATE_KEY_LINE_BREAK).not()) {
            privateKeyString = privateKeyString.removeSuffix(END_PRIVATE_KEY)
            privateKeyString += END_PRIVATE_KEY_LINE_BREAK
        }
        val targetReader = StringReader(privateKeyString)
        val pemParser = PEMParser(targetReader)
        val privateKeyInfo = pemParser.readObject() as? PrivateKeyInfo?
        val privateKey = JcaPEMKeyConverter().getPrivateKey(privateKeyInfo)
        pemParser.close()
        targetReader.close()
        return privateKey
    }

    fun convertPrivateKeyToPem(privateKey: PrivateKey): String {
        var useV1Info = false
        if (privateKey is BCEdDSAPrivateKey) {
            // to generate PEM, which are compatible to KeepassXC
            useV1Info = true
        }
        System.setProperty(
            "org.bouncycastle.pkcs8.v1_info_only",
            useV1Info.toString().lowercase()
        )

        val noOutputEncryption = null
        val pemObjectGenerator = JcaPKCS8Generator(privateKey, noOutputEncryption)

        val writer = StringWriter()
        val pemWriter = PemWriter(writer)
        pemWriter.writeObject(pemObjectGenerator)
        pemWriter.close()

        val privateKeyInPem = writer.toString().trim()
        writer.close()
        return privateKeyInPem
    }

    fun generateKeyPair(keyTypeIdList: List<Long>): Pair<KeyPair, Long>? {

        for (typeId in keyTypeIdList) {
            when (typeId) {
                ES256_ALGORITHM -> {
                    val es256CurveNameBC = "secp256r1"
                    val spec = ECGenParameterSpec(es256CurveNameBC)
                    val keyPairGen =
                        KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
                    keyPairGen.initialize(spec)
                    val keyPair = keyPairGen.genKeyPair()
                    return Pair(keyPair, ES256_ALGORITHM)

                }
                RS256_ALGORITHM -> {
                    val keyPairGen =
                        KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
                    keyPairGen.initialize(RS256_KEY_SIZE_IN_BITS)
                    val keyPair = keyPairGen.genKeyPair()
                    return Pair(keyPair, RS256_ALGORITHM)

                }
                ED_DSA_ALGORITHM -> {
                    val keyPairGen =
                        KeyPairGenerator.getInstance("Ed25519", BouncyCastleProvider.PROVIDER_NAME)
                    val keyPair = keyPairGen.genKeyPair()
                    return Pair(keyPair, ED_DSA_ALGORITHM)
                }
            }
        }

        Log.e(this::class.java.simpleName, "generateKeyPair: no known key type id found")
        return null
    }

    fun convertPublicKey(publicKeyIn: PublicKey, keyTypeId: Long): ByteArray? {
        if (keyTypeId == ES256_ALGORITHM) {
            if (publicKeyIn is BCECPublicKey) {
                publicKeyIn.setPointFormat("UNCOMPRESSED")
                return publicKeyIn.encoded
            }
        } else if (keyTypeId == RS256_ALGORITHM) {
            return publicKeyIn.encoded
        } else if (keyTypeId == ED_DSA_ALGORITHM) {
            return publicKeyIn.encoded
        }
        Log.e(this::class.java.simpleName, "convertPublicKey: unknown key type id found")
        return null
    }

    fun convertPublicKeyToMap(publicKeyIn: PublicKey, keyTypeId: Long): Map<Int, Any>? {

        // https://www.iana.org/assignments/cose/cose.xhtml#key-common-parameters
        val keyTypeLabel = 1
        val algorithmLabel = 3

        if (keyTypeId == ES256_ALGORITHM) {
            if (publicKeyIn !is BCECPublicKey) {
                Log.e(
                    this::class.java.simpleName,
                    "publicKey object has wrong type for keyTypeId $ES256_ALGORITHM: ${publicKeyIn.javaClass.canonicalName}"
                )
                return null
            }
            // constants see at https://w3c.github.io/webauthn/#example-bdbd14cc
            val publicKeyMap = mutableMapOf<Int, Any>()

            val es256KeyTypeId = 2
            val es256EllipticCurveP256Id = 1

            publicKeyMap[keyTypeLabel] = es256KeyTypeId
            publicKeyMap[algorithmLabel] = ES256_ALGORITHM

            publicKeyMap[-1] = es256EllipticCurveP256Id

            val ecPoint = publicKeyIn.q
            publicKeyMap[-2] = ecPoint.xCoord.encoded
            publicKeyMap[-3] = ecPoint.yCoord.encoded

            return publicKeyMap

        } else if (keyTypeId == RS256_ALGORITHM) {
            if (publicKeyIn !is BCRSAPublicKey) {
                Log.e(
                    this::class.java.simpleName,
                    "publicKey object has wrong type for keyTypeId $RS256_ALGORITHM: ${publicKeyIn.javaClass.canonicalName}"
                )
                return null
            }

            // constants see at https://w3c.github.io/webauthn/#example-8dfabc00

            val rs256KeySizeInBytes = RS256_KEY_SIZE_IN_BITS / 8
            val rs256KeyTypeId = 3
            val rs256ExponentSizeInBytes = 3

            val publicKeyMap = mutableMapOf<Int, Any>()
            publicKeyMap[keyTypeLabel] = rs256KeyTypeId
            publicKeyMap[algorithmLabel] = RS256_ALGORITHM
            publicKeyMap[-1] =
                BigIntegers.asUnsignedByteArray(rs256KeySizeInBytes, publicKeyIn.modulus)
            publicKeyMap[-2] =
                BigIntegers.asUnsignedByteArray(
                    rs256ExponentSizeInBytes,
                    publicKeyIn.publicExponent
                )
            return publicKeyMap
        } else if (keyTypeId == ED_DSA_ALGORITHM) {
            if (publicKeyIn !is BCEdDSAPublicKey) {
                Log.e(
                    this::class.java.simpleName,
                    "publicKey object has wrong type for keyTypeId $ED_DSA_ALGORITHM: ${publicKeyIn.javaClass.canonicalName}"
                )
                return null
            }

            val publicKeyMap = mutableMapOf<Int, Any>()

            // https://www.rfc-editor.org/rfc/rfc9053#name-key-object-parameters
            val octetKeyPairId = 1

            val curveLabel = -1
            val ed25519CurveId = 6

            val publicKeyLabel = -2

            publicKeyMap[keyTypeLabel] = octetKeyPairId
            publicKeyMap[algorithmLabel] = ED_DSA_ALGORITHM

            publicKeyMap[curveLabel] = ed25519CurveId

            val length = Ed25519PublicKeyParameters.KEY_SIZE

            publicKeyMap[publicKeyLabel] = BigIntegers.asUnsignedByteArray(
                length,
                BigIntegers.fromUnsignedByteArray(publicKeyIn.pointEncoding)
            )

            return publicKeyMap
        }

        Log.e(this::class.java.simpleName, "convertPublicKeyToMap: no known key type id found")
        return null
    }


    const val SIGNATURE_DELIMITER = "##SIG##"

    /**
     * Converts a Signature object into its SHA-256 fingerprint string.
     * The fingerprint is typically represented as uppercase hex characters separated by colons.
     */
    private fun signatureToSha256Fingerprint(signature: android.content.pm.Signature): String? {
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
     * @return A List of SHA-256 fingerprint strings, or null if an error occurs or no signatures are found.
     */
    fun SigningInfo.getAllFingerprints(): Set<String>? {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                throw AndroidException("API level ${Build.VERSION.SDK_INT} not supported")
            val signatures = mutableSetOf<String>()
            // Includes past and current keys if rotation occurred. This is generally preferred.
            signingCertificateHistory?.forEach { signature ->
                signatureToSha256Fingerprint(signature)?.let { signatures.add(it) }
            }
            // If only one signer and history is empty (e.g. new app), this might be needed.
            // Or if multiple signers are explicitly used for the APK content.
            if (hasMultipleSigners()) {
                apkContentsSigners?.forEach { signature ->
                    signatureToSha256Fingerprint(signature)?.let { signatures.add(it) }
                }
            } else { // Fallback for single signer if history was somehow null/empty
                signingCertificateHistory?.firstOrNull()?.let {
                    signatureToSha256Fingerprint(it)?.let { fp -> signatures.add(fp) }
                }
            }
            return if (signatures.isEmpty()) null else signatures
        } catch (e: Exception) {
            Log.e(Signature::class.java.simpleName, "Error getting signatures", e)
            return null
        }
    }

    /**
     * Combines a list of signatures into a single string for database storage.
     *
     * @return A single string with fingerprints joined by a ##SIG## delimiter,
     * or null if the input list is null or empty.
     */
    fun SigningInfo.getApplicationFingerprints(): String? {
        val fingerprints = getAllFingerprints()
        if (fingerprints.isNullOrEmpty()) {
            return null
        }
        return fingerprints.singleLineFingerprints()
    }

    /**
     * Combines a set of signatures into a single string for database storage.
     */
    fun Set<String>.singleLineFingerprints(): String? {
        return this.joinToString(SIGNATURE_DELIMITER)
    }

    /**
     * Transforms a colon-separated hex fingerprint string into a URL-safe,
     * padding-removed Base64 string, mimicking the Python behavior:
     * base64.urlsafe_b64encode(binascii.a2b_hex(fingerprint.replace(':', ''))).decode('utf8').replace('=', '')
     *
     * Only check the first footprint if there are several delimited by ##SIG##.
     *
     * @param fingerprint The colon-separated hex fingerprint string (e.g., "91:F7:CB:...").
     * @return The Android App Origin string.
     * @throws IllegalArgumentException if the hex string (after removing colons) has an odd length
     *         or contains non-hex characters.
     */
    fun fingerprintToUrlSafeBase64(fingerprint: String): String {
        val firstFingerprint = fingerprint.split(SIGNATURE_DELIMITER).firstOrNull()?.trim()
        if (firstFingerprint.isNullOrEmpty()) {
            throw IllegalArgumentException("Invalid fingerprint $fingerprint")
        }
        val hexStringNoColons = firstFingerprint.replace(":", "")
        if (hexStringNoColons.length % 2 != 0) {
            throw IllegalArgumentException("Hex string must have an even number of characters: $hexStringNoColons")
        }
        if (hexStringNoColons.length != 64) {
            throw IllegalArgumentException("Expected a 64-character hex string for a SHA-256 hash, but got ${hexStringNoColons.length} characters.")
        }
        val hashBytes = ByteArray(hexStringNoColons.length / 2)
        for (i in hashBytes.indices) {
            try {
                val index = i * 2
                val byteValue = hexStringNoColons.substring(index, index + 2).toInt(16)
                hashBytes[i] = byteValue.toByte()
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Invalid hex character in fingerprint: $hexStringNoColons", e)
            }
        }
        return Base64Helper.b64Encode(hashBytes)
    }
}