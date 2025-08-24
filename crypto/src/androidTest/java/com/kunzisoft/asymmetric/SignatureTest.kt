package com.kunzisoft.asymmetric

import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPrivateKey
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCXDHPrivateKey
import org.junit.Test
import java.io.File
import java.io.FileWriter
import kotlin.io.path.Path

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

    private val es256PemInKeePassDX = """
            -----BEGIN PRIVATE KEY-----
            MIGTAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBHkwdwIBAQQgaIrmuL+0IpvMpZ4O
            8+CpXEzVNoyNkhquyRqD8CtVWDmgCgYIKoZIzj0DAQehRANCAARyucecj8E9YvcA
            ZHEYgElcLjwLMWmMvQ2BDZPVL4pLG1oBZer1mPEEQV7LzwGYvTzV/eb9GlXPwj/4
            la/bpVp1
            -----END PRIVATE KEY-----          
    """.trimIndent().trim()

    @Test
    fun testEC256KeyConversionKeypassXCIn() {
        val privateKey = Signature.createPrivateKey(es256PemInKeePassXC)
        val pemOut = Signature.convertPrivateKeyToPem(privateKey)

        assert(pemOut == es256PemInKeePassDX)
    }

    @Test
    fun testEC256KeyConversionKeePassDXIn() {
        val privateKey = Signature.createPrivateKey(es256PemInKeePassDX)
        val pemOut = Signature.convertPrivateKeyToPem(privateKey)

        assert(pemOut == es256PemInKeePassDX)
    }

    @Test
    fun testEC256KeyGenAndConversion() {
        val (keyPair, keyTypeId) = Signature.generateKeyPair(listOf(Signature.ES256_ALGORITHM))!!
        val privateKeyPem = Signature.convertPrivateKeyToPem(keyPair.private)

        assert(keyTypeId == Signature.ES256_ALGORITHM)
        assert(privateKeyPem.contains("-----BEGIN PRIVATE KEY-----", true))
        assert( privateKeyPem.contains("-----BEGIN EC PRIVATE KEY-----", true).not())

    }

    // endregion

    // region RSA

    private val rsa256PemIn = """
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
        val privateKey = Signature.createPrivateKey(rsa256PemIn)
        val pemOut = Signature.convertPrivateKeyToPem(privateKey)

        assert(pemOut == rsa256PemIn)
    }

    @Test
    fun testRS256KeyGenAndConversion() {
        val (keyPair, keyTypeId) = Signature.generateKeyPair(listOf(Signature.RS256_ALGORITHM))!!
        val privateKeyPem = Signature.convertPrivateKeyToPem(keyPair.private)

        assert(keyTypeId == Signature.RS256_ALGORITHM)
        assert(privateKeyPem.contains("-----BEGIN PRIVATE KEY-----", true))
    }
    // endregion

    // region ED25519

    private val ed25519PemInShort = """
        -----BEGIN PRIVATE KEY-----
        MC4CAQAwBQYDK2VwBCIEILBoCo4+IXxIuwN36/oaEsPgbe6WYJcV9YW+xnprDF4H
        -----END PRIVATE KEY-----
    """.trimIndent()

    private val ed25519PemInLong =  """
        -----BEGIN PRIVATE KEY-----
        MFECAQEwBQYDK2VwBCIEIESP8edVGbqoR/pKNmy7j7FV8Y68zrIi/5VEuAJ281K6
        gSEAyJU1wQNaJUeyxPcWjN7xZKZUhCRoIFS/MQvbdd4QE7Q=
        -----END PRIVATE KEY-----
    """.trimIndent()

    private val ed25519PemOut = """
        -----BEGIN PRIVATE KEY-----
        MC4CAQAwBQYDK2VwBCIEIESP8edVGbqoR/pKNmy7j7FV8Y68zrIi/5VEuAJ281K6
        -----END PRIVATE KEY-----
    """.trimIndent()

    @Test
    fun testEd25519KeyConverionShortIn() {
        val privateKey = Signature.createPrivateKey(ed25519PemInShort)
        val pemOut = Signature.convertPrivateKeyToPem(privateKey)

        assert(pemOut == ed25519PemInShort)
    }

    @Test
    fun testEd25519KeyConverionLongIn() {
        val privateKey = Signature.createPrivateKey(ed25519PemInLong)
        val pemOut = Signature.convertPrivateKeyToPem(privateKey)

        assert(pemOut == ed25519PemOut)
    }

    @Test
    fun testEd25519KeyGenAndConversion() {
        val (keyPair, keyTypeId) = Signature.generateKeyPair(listOf(Signature.ED_DSA_ALGORITHM))!!
        val privateKeyPem = Signature.convertPrivateKeyToPem(keyPair.private)

        assert(keyTypeId == Signature.ED_DSA_ALGORITHM)
        assert(privateKeyPem.contains("-----BEGIN PRIVATE KEY-----", true))
        assert(privateKeyPem.contains("-----BEGIN EC PRIVATE KEY-----", true).not())
    }




    // endregion

}