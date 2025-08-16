package com.kunzisoft.asymmetric

import android.util.Log
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
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
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.Signature
import java.security.spec.ECGenParameterSpec

object Signature {

    // see at https://www.iana.org/assignments/cose/cose.xhtml
    const val ES256_ALGORITHM: Long = -7
    const val RS256_ALGORITHM: Long = -257
    private const val RS256_KEY_SIZE_IN_BITS = 2048

    init {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())
    }

    fun sign(privateKeyPem: String, message: ByteArray): ByteArray? {
        val privateKey = createPrivateKey(privateKeyPem)
        val algorithmKey = privateKey.algorithm
        val algorithmSignature = when (algorithmKey) {
            "EC" -> "SHA256withECDSA"
            "ECDSA" -> "SHA256withECDSA"
            "RSA" -> "SHA256withRSA"
            else -> null
        }
        if (algorithmSignature == null) {
            Log.e(this::class.java.simpleName, "sign: privateKeyPem has an unknown algorithm")
            return null
        }
        val sig = Signature.getInstance(algorithmSignature, BouncyCastleProvider.PROVIDER_NAME)
        sig.initSign(privateKey)
        sig.update(message)
        return sig.sign()
    }

    fun createPrivateKey(privateKeyPem: String): PrivateKey {
        val targetReader = StringReader(privateKeyPem)
        val pemParser = PEMParser(targetReader)
        val privateKeyInfo = pemParser.readObject() as PrivateKeyInfo
        val privateKey = JcaPEMKeyConverter().getPrivateKey(privateKeyInfo)
        pemParser.close()
        targetReader.close()
        return privateKey
    }

    fun convertPrivateKeyToPem(privateKey: PrivateKey): String {
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
            if (typeId == ES256_ALGORITHM) {
                val es256CurveNameBC = "secp256r1"
                val spec = ECGenParameterSpec(es256CurveNameBC)
                val keyPairGen =
                    KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
                keyPairGen.initialize(spec)
                val keyPair = keyPairGen.genKeyPair()
                return Pair(keyPair, ES256_ALGORITHM)
            } else if (typeId == RS256_ALGORITHM) {

                val keyPairGen =
                    KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME)
                keyPairGen.initialize(RS256_KEY_SIZE_IN_BITS)
                val keyPair = keyPairGen.genKeyPair()
                return Pair(keyPair, RS256_ALGORITHM)
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
        }
        Log.e(this::class.java.simpleName, "convertPublicKey: unknown key type id found")
        return null
    }

    fun convertPublicKeyToMap(publicKeyIn: PublicKey, keyTypeId: Long): Map<Int, Any>? {
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

            publicKeyMap[1] = es256KeyTypeId
            publicKeyMap[3] = ES256_ALGORITHM
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
            publicKeyMap[1] = rs256KeyTypeId
            publicKeyMap[3] = RS256_ALGORITHM
            publicKeyMap[-1] =
                BigIntegers.asUnsignedByteArray(rs256KeySizeInBytes, publicKeyIn.modulus)
            publicKeyMap[-2] =
                BigIntegers.asUnsignedByteArray(
                    rs256ExponentSizeInBytes,
                    publicKeyIn.publicExponent
                )
            return publicKeyMap
        }

        Log.e(this::class.java.simpleName, "convertPublicKeyToMap: no known key type id found")
        return null
    }
}