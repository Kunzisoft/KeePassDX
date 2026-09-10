package com.kunzisoft.encrypt

import com.kunzisoft.encrypt.Signature.convertPrivateKeyToPem
import com.kunzisoft.encrypt.Signature.createPrivateKey
import com.kunzisoft.encrypt.Signature.fingerprintToUrlSafeBase64
import com.kunzisoft.encrypt.Signature.generateKeyPair
import com.kunzisoft.encrypt.Signature.sign
import org.junit.Assert
import org.junit.Test

class SignatureTest {

    // All private keys are for testing only.
    // DO NOT USE THEM

    // region ES256
    private val es256PemInKeePassXC =
        """
            -----BEGIN PRIVATE KEY-----
            MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgaIrmuL+0IpvMpZ4O
            8+CpXEzVNoyNkhquyRqD8CtVWDmhRANCAARyucecj8E9YvcAZHEYgElcLjwLMWmM
            vQ2BDZPVL4pLG1oBZer1mPEEQV7LzwGYvTzV/eb9GlXPwj/4la/bpVp1
            -----END PRIVATE KEY-----
        """.trimIndent().trim()

    private val es256PemInKeePassDX =
        """
            -----BEGIN PRIVATE KEY-----
            MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgaIrmuL+0IpvMpZ4O
            8+CpXEzVNoyNkhquyRqD8CtVWDmgCgYIKoZIzj0DAQehRANCAARyucecj8E9YvcA
            ZHEYgElcLjwLMWmMvQ2BDZPVL4pLG1oBZer1mPEEQV7LzwGYvTzV/eb9GlXPwj/4
            la/bpVp1
            -----END PRIVATE KEY-----          
        """.trimIndent().trim()

    @Test
    fun testEC256KeyConversionKeypassXCIn() {
        val privateKey = createPrivateKey(es256PemInKeePassXC.toCharArray())
        val pemOut = convertPrivateKeyToPem(privateKey)

        assert(pemOut.contentEquals(es256PemInKeePassDX.toCharArray()))
    }

    @Test
    fun testEC256KeyConversionKeePassDXIn() {
        val privateKey = createPrivateKey(es256PemInKeePassDX.toCharArray())
        val pemOut = convertPrivateKeyToPem(privateKey)

        assert(pemOut.contentEquals(es256PemInKeePassDX.toCharArray()))
    }

    @Test
    fun testEC256KeyGenAndConversion() {
        val (keyPair, keyTypeId) = generateKeyPair(listOf(Signature.ES256_ALGORITHM))!!
        val privateKeyPem = convertPrivateKeyToPem(keyPair.private)

        assert(keyTypeId == Signature.ES256_ALGORITHM)
        val privateKeyPemString = String(privateKeyPem)
        assert(privateKeyPemString.contains("-----BEGIN PRIVATE KEY-----", true))
        assert(privateKeyPemString.contains("-----BEGIN EC PRIVATE KEY-----", true).not())

    }

    // endregion

    // region RSA

    private val rsa256PemIn =
        """
            -----BEGIN PRIVATE KEY-----
            MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCaunVJEhLHl7/f
            NZufOmj4MY/1J/YHgMAZYFBORQVm58psUjCU7jIww+BK5aRGShdumRzbxr1Yqyh6
            yvWbv2u9l6cOdtQKFtXDsWtP9tMBqqhODhG30gE3rEt5l2k1CSzSO9sGghUxlb2i
            Q9fiSQ4HmiEc+cXbsSbeYsWwGYNYNhPdJ7vwsZsXzmD0RPpcxy0uJatASjWx3lXF
            +eprpcZUr0NLlGIob6VfCt0q2ZfoeEHcEcp4qQ9nI+hOLPFzp/x1TFLX8wKvmwRh
            ifwyAauVIaDZaQvAeoBuV2hSBl596ujiJt2Kd3pbQ4SjD1MXudbVvGkofgSZNR0f
            ai6f6POFAgMBAAECggEAFtSIVb3K85RahU7dpYLy1hxKB3xb+wNuVNA3STU59NMi
            tRTzgiYbVcKxJ5v2v0BTcMg6z9rlOV4X3PZxgwedmB32UlYKN2rjI7rcALKEs+xA
            ZTQCPUNJVrOfd1N1/JNb/7FBQhaTlftoPbcQ9Zyd61U8qY/ZN+9NsuaUEMXS8YLe
            cqlwJjRcWh3PuTQ+qeVw5l6lgK4XEyDbh/Aj9DGgwVsAkwGdXpuQRBQr8UClO/he
            2iOwkn4LJ5nnXwByMpEct03+eUj0kxlijunYbBnKJfRv6tz+ZcpZoc1EqeWbrTB0
            eKf+R6N8MHgJSemVVGZvgsUYbfMqkJA/LNOyIpQ/wQKBgQDJhowHVDtze+FQrunR
            NchOXgZNWTFf3ZITxxnWnTgumtKdg3MkxeKEBCzAqefb6n6zi2rQzP/PAZAKT/we
            YP24hwUVeFePH9/Llf5QuCOWGtkZbNRFSCHWcbfRQAL4vfPJk79bhwCoC5wQ5uk1
            atCA+dln6b3wDXq2bvBs6Rj7bQKBgQDEjZIMMgYoEq6yKCFK+11BFo3sj3rmbCcE
            tu29mXBfromgzfL0NLoqUAB5OsYKO1nl7eQp2QVIgdLLs8BwTKkel4gosK9B5T4C
            umFG0yGIOJz7twA5joLuZAFsoPazj6yHUXaFJaye2P5KwVCL9ws5V2WKgnsF/hKe
            QWwSIjtxeQKBgAbyR1NdWOtDIuIYFWErvGrPHOJ/p48JYSajX0Whh7U7ivT4+fgT
            hhpM1ooRkTdoXtOrg5QM7OhiwmdImIUnjLdWmBtEWahKTfmDgw+fOULMTB1vPeXh
            daEhrFdfIHsYeRXCrP7nqWMhe1Ct1O4Nb4BynEbTrMNgg5FUQ59NbZoFAoGAXNNb
            YSUS4UQJexwWtRnHbeDgABO3ADGdr81QtBVOC/IbD3WUQx7PuQH1Z0uJkfV7vGpA
            Mj9LDnY5fniS7rZVvJvl8wmWi3FfetxY6qD1mibahMplcclLLpjOT2YpfJ3i5jlj
            1vf28UIbvmRTzPZMN7V9wA9lWGwokNLm3h2Ko0kCgYBq0NEd+VMkuIXuGz6j2IXC
            qjKf187RZAn2B7otXoumCze+uxm4N0PyOYb1fNGHeE8/RjNQO7VmzZg/dMrk9ZJh
            ueHJgOLTbDdlQCUacSipHGmWMN9E+EjgBRiqmPZzV6dq/kGc2FUSGB22wY8gckEX
            AmqgkPgYHZ/VzFPTrp97IQ==
            -----END PRIVATE KEY-----
        """.trimIndent().trim()

    @Test
    fun testRS256KeyConversion() {
        val privateKey = createPrivateKey(rsa256PemIn.toCharArray())
        val pemOut = convertPrivateKeyToPem(privateKey)

        assert(pemOut.contentEquals(rsa256PemIn.toCharArray()))
    }

    @Test
    fun testRS256KeyGenAndConversion() {
        val (keyPair, keyTypeId) = generateKeyPair(listOf(Signature.RS256_ALGORITHM))!!
        val privateKeyPem = convertPrivateKeyToPem(keyPair.private)

        assert(keyTypeId == Signature.RS256_ALGORITHM)
        assert(String(privateKeyPem).contains("-----BEGIN PRIVATE KEY-----", true))
    }
    // endregion

    // region ED25519

    private val ed25519PemInShort =
        """
            -----BEGIN PRIVATE KEY-----
            MC4CAQAwBQYDK2VwBCIEILBoCo4+IXxIuwN36/oaEsPgbe6WYJcV9YW+xnprDF4H
            -----END PRIVATE KEY-----
        """.trimIndent()

    private val ed25519PemInLong =
        """
            -----BEGIN PRIVATE KEY-----
            MFECAQEwBQYDK2VwBCIEIESP8edVGbqoR/pKNmy7j7FV8Y68zrIi/5VEuAJ281K6
            gSEAyJU1wQNaJUeyxPcWjN7xZKZUhCRoIFS/MQvbdd4QE7Q=
            -----END PRIVATE KEY-----
        """.trimIndent()

    private val ed25519PemOut =
        """
            -----BEGIN PRIVATE KEY-----
            MC4CAQAwBQYDK2VwBCIEIESP8edVGbqoR/pKNmy7j7FV8Y68zrIi/5VEuAJ281K6
            -----END PRIVATE KEY-----
        """.trimIndent()

    @Test
    fun testEd25519KeyConverionShortIn() {
        val privateKey = createPrivateKey(ed25519PemInShort.toCharArray())
        val pemOut = convertPrivateKeyToPem(privateKey)

        assert(pemOut.contentEquals(ed25519PemInShort.toCharArray()))
    }

    @Test
    fun testEd25519KeyConverionLongIn() {
        val privateKey = createPrivateKey(ed25519PemInLong.toCharArray())
        val pemOut = convertPrivateKeyToPem(privateKey)

        assert(pemOut.contentEquals(ed25519PemOut.toCharArray()))
    }

    @Test
    fun testEd25519KeyGenAndConversion() {
        val (keyPair, keyTypeId) = generateKeyPair(listOf(Signature.ED_DSA_ALGORITHM))!!
        val privateKeyPem = convertPrivateKeyToPem(keyPair.private)

        assert(keyTypeId == Signature.ED_DSA_ALGORITHM)
        val privateKeyPemString = String(privateKeyPem)
        assert(privateKeyPemString.contains("-----BEGIN PRIVATE KEY-----", true))
        assert(privateKeyPemString.contains("-----BEGIN EC PRIVATE KEY-----", true).not())
    }

    // endregion

    // region ML-DSA

    // All private keys are for testing only.
    // DO NOT USE THEM

    // openssl genpkey -algorithm ML-DSA-44 -provparam ml-dsa.output_formats=seed-only -out mldsa44-seed-only.pem
    val mlDsa44SeedOnly =
        """
            -----BEGIN PRIVATE KEY-----
            MDQCAQAwCwYJYIZIAWUDBAMRBCKAIFcKtXgQHrAiGiaBG3Y1dUMosQJFhkMHJHZo
            RXr6/TS0
            -----END PRIVATE KEY-----
        """.trimIndent().trim()

    // openssl genpkey -algorithm ML-DSA-44 -provparam ml-dsa.output_formats=seed-priv -out mldsa44-seed-priv.pem
    val mlDsa44SeedAndPrivateKey =
        """
            -----BEGIN PRIVATE KEY-----
            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
            -----END PRIVATE KEY-----
        """.trimIndent().trim()

    // openssl genpkey -algorithm ML-DSA-44 -provparam ml-dsa.output_formats=priv-only -out mldsa44-priv-only.pem
    val mlDsa44PrivateKeyWithoutSeed =
        """
            -----BEGIN PRIVATE KEY-----
            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
            -----END PRIVATE KEY-----
        """.trimIndent().trim()

    @Test
    fun testMLDSA44KeyGenAndSign() {
        val (keyPair, keyTypeId) = generateKeyPair(listOf(Signature.ML_DSA_44_ALGORITHM))!!
        val privateKeyPem = convertPrivateKeyToPem(keyPair.private)

        Assert.assertEquals(Signature.ML_DSA_44_ALGORITHM, keyTypeId)

        val message = "test message".toByteArray()
        val signature = sign(privateKeyPem, message)

        Assert.assertNotNull(signature)
        Assert.assertTrue(signature.isNotEmpty())

        val sig = java.security.Signature.getInstance("ML-DSA-44", "BC")
        sig.initVerify(keyPair.public)
        sig.update(message)
        Assert.assertTrue(sig.verify(signature))
    }

    @Test
    fun testMLDSA65KeyGenAndSign() {
        val (keyPair, keyTypeId) = generateKeyPair(listOf(Signature.ML_DSA_65_ALGORITHM))!!
        val privateKeyPem = convertPrivateKeyToPem(keyPair.private)

        Assert.assertEquals(Signature.ML_DSA_65_ALGORITHM, keyTypeId)

        val message = "test message".toByteArray()
        val signature = sign(privateKeyPem, message)

        Assert.assertNotNull(signature)
        Assert.assertTrue(signature.isNotEmpty())

        val sig = java.security.Signature.getInstance("ML-DSA-65", "BC")
        sig.initVerify(keyPair.public)
        sig.update(message)
        Assert.assertTrue(sig.verify(signature))
    }

    @Test
    fun testMLDSA87KeyGenAndSign() {
        val (keyPair, keyTypeId) = generateKeyPair(listOf(Signature.ML_DSA_87_ALGORITHM))!!
        val privateKeyPem = convertPrivateKeyToPem(keyPair.private)

        Assert.assertEquals(Signature.ML_DSA_87_ALGORITHM, keyTypeId)

        val message = "test message".toByteArray()
        val signature = sign(privateKeyPem, message)

        Assert.assertNotNull(signature)
        Assert.assertTrue(signature.isNotEmpty())

        val sig = java.security.Signature.getInstance("ML-DSA-87", "BC")
        sig.initVerify(keyPair.public)
        sig.update(message)
        Assert.assertTrue(sig.verify(signature))
    }

    @Test
    fun testSeedOnly() {
        val privateKey = createPrivateKey(mlDsa44SeedOnly.toCharArray())
        val pem = convertPrivateKeyToPem(privateKey)
        assert(checkPem(pem))
    }

    @Test
    fun testSeedAndPrivateKeyOnly() {
        val privateKey = createPrivateKey(mlDsa44SeedAndPrivateKey.toCharArray())
        val pem = convertPrivateKeyToPem(privateKey)
        assert(checkPem(pem))
    }

    @Test
    fun testPrivateKeyWithoutSeedIsUnsupported() {
        val keyAsPem = mlDsa44PrivateKeyWithoutSeed.toCharArray()
        val privateKey = createPrivateKey(keyAsPem)
        try {
            convertPrivateKeyToPem(privateKey)

            assert(false) {
                "the convertPrivateKeyToPem should throw an Exception, " +
                        "because ML DSA private key without a seed is not supported"
            }
        } catch (e: SecurityException) {
            assert(e.message != null)
            assert("does not contain the seed" in e.message!!.lowercase())
            return
        } catch (otherException: Exception) {
            assert(false) { "wrong type of Exception: ${otherException.javaClass.name}" }
        }
        assert(false)
    }

    @Test
    fun testReadMlDsa44Pem() {
        val privateKey = createPrivateKey(mlDsa44SeedOnly.toCharArray())
        val thePackage = privateKey.javaClass.`package`!!.name.lowercase()
        val isOpenssl = "openssl" in thePackage
        assert(isOpenssl.not())
        assert("bouncycastle" in thePackage)
    }

    @Test
    fun testMlDsa44() {
        checkMlDsaGenerateAndSign(-48)
    }

    @Test
    fun testMlDsa65() {
        checkMlDsaGenerateAndSign(-49)
    }

    @Test
    fun testMlDsa87() {
        checkMlDsaGenerateAndSign(-50)
    }

    fun checkMlDsaGenerateAndSign(id: Long) {
        val keyPairNullable = generateKeyPair(listOf(id))
        assert(keyPairNullable != null) { "no keyPair for $id was generated" }

        val keyPair = keyPairNullable!!
        assert(keyPair.second == id) { "wrong algorithm id" }

        val pem = convertPrivateKeyToPem(keyPair.first.private)
        assert(checkPem(pem))

        val sig = sign(pem, "the test message".toByteArray())
        assert(sig.size > 1)
    }

    fun checkPem(pem: CharArray): Boolean {
        val pemSplited = String(pem).split("\n")

        // see https://www.rfc-editor.org/rfc/rfc9881.html#name-private-key-format
        return pemSplited.size == 4
    }

    // endregion

    @Test
    fun testSingleSignature() {
        // Generate random input
        val fingerprint = "A7:5C:63:72:A0:B6:7D:B0:16:86:B4:7D:F6:8C:91:51:6E:E1:62:29:EE:C4:C0:C6:7D:35:5E:32:20:7C:66:17"
        val expected = "p1xjcqC2fbAWhrR99oyRUW7hYinuxMDGfTVeMiB8Zhc"

        Assert.assertEquals("Check fingerprint app", expected, fingerprintToUrlSafeBase64(fingerprint))
    }

    @Test
    fun testMultipleSignature() {
        // Generate random input
        val fingerprint = "A7:5C:63:72:A0:B6:7D:B0:16:86:B4:7D:F6:8C:91:51:6E:E1:62:29:EE:C4:C0:C6:7D:35:5E:32:20:7C:66:17##SIG##DB:25:8A:A6:19:08:9B:D1:3D:BA:71:9E:5A:DA:EC:FF:7F:12:C8:8F:67:AD:68:3C:1F:BC:F2:28:B3:88:BD:91"
        val expected = "p1xjcqC2fbAWhrR99oyRUW7hYinuxMDGfTVeMiB8Zhc"

        Assert.assertEquals("Check fingerprint app", expected, fingerprintToUrlSafeBase64(fingerprint))
    }
}