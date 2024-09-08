package com.kunzisoft.signature

import android.util.Log
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.StringReader
import java.security.PrivateKey
import java.security.Security
import java.security.Signature

import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter

object Signature {

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
            else -> "no signature algorithms known"
        }
        val sig = Signature.getInstance(algorithmSignature, BouncyCastleProvider.PROVIDER_NAME)
        sig.initSign(privateKey)
        sig.update(message)
        return sig.sign()
    }

    private fun createPrivateKey(privateKeyPem: String): PrivateKey {
        val targetReader = StringReader(privateKeyPem);
        val a = PEMParser(targetReader)
        val privateKeyInfo = a.readObject() as PrivateKeyInfo
        val privateKey = JcaPEMKeyConverter().getPrivateKey(privateKeyInfo)
        return privateKey
    }

}