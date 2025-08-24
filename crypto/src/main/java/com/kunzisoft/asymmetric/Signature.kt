package com.kunzisoft.asymmetric

import android.util.Log
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
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

    const val ED_DSA_ALGORITHM: Long = -8

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
            "Ed25519" -> "Ed25519"
            else -> null
        }
        if (algorithmSignature == null) {
            Log.e(this::class.java.simpleName, "sign: the algorithm $algorithmKey is unknown")
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
        var useV1Info = false
        if (privateKey is BCEdDSAPrivateKey) {
            // to generate PEM, which are compatible to KeepassXC
            useV1Info = true
        }
        System.setProperty("org.bouncycastle.pkcs8.v1_info_only", useV1Info.toString().lowercase())

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

            } else if (typeId == ED_DSA_ALGORITHM) {
                val keyPairGen =
                    KeyPairGenerator.getInstance("Ed25519", BouncyCastleProvider.PROVIDER_NAME)
                val keyPair = keyPairGen.genKeyPair()
                return Pair(keyPair, ED_DSA_ALGORITHM)
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

            publicKeyMap[publicKeyLabel] = BigIntegers.asUnsignedByteArray(length,
                BigIntegers.fromUnsignedByteArray(publicKeyIn.pointEncoding))

            return publicKeyMap
        }

        Log.e(this::class.java.simpleName, "convertPublicKeyToMap: no known key type id found")
        return null
    }
}