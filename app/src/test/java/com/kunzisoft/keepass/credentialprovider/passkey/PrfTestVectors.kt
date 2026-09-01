package com.kunzisoft.keepass.credentialprovider.passkey

import com.kunzisoft.encrypt.HashManager
import com.kunzisoft.keepass.credentialprovider.passkey.util.PasskeyHelper
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.util.Base64

class PrfTestVectors {

    private fun hex(value: String) =
        value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    // Test-vectors CTAP2 hmac-secret extension
    // https://www.w3.org/TR/webauthn-3/#test-vectors-extensions-prf-ctap
    @Test
    fun derivesSaltForSingleInput() {
        val first = hex("576562417574686e20505246207465737420766563746f727302")

        assertArrayEquals(
            hex("527413ebb48293772df30f031c5ac4650c7de14bf9498671ae163447b6a772b3"),
            PasskeyHelper.derivePrfSalt(first),
        )
    }

    @Test
    fun derivesSaltForSecondInput() {
        val second = hex("576562417574686e20505246207465737420766563746f727303")

        assertArrayEquals(
            hex("d68ac03329a10ee5e0ec834492bb9a96a0e547baf563bf78ccbe8789b22e776b"),
            PasskeyHelper.derivePrfSalt(second),
        )
    }

    @Test
    fun evaluatesSingleInputAgainstSpecVector() {
        val secret = hex("437e065e723a98b2f08f39d8baf7c53ecb3c363c5e5104bdaaf5d5ca2e028154")
        val first = hex("576562417574686e20505246207465737420766563746f727302")

        val resultFirst = PasskeyHelper.computePrfValue(secret, first)

        assertArrayEquals(
            hex("3c33e07d202c3b029cc21f1722767021bf27d595933b3d2b6a1b9d5dddc77fae"),
            resultFirst,
        )
    }

    @Test
    fun evaluatesTwoInputsAgainstSpecVectors() {
        val secret = hex("437e065e723a98b2f08f39d8baf7c53ecb3c363c5e5104bdaaf5d5ca2e028154")
        val first = hex("576562417574686e20505246207465737420766563746f727302")
        val second = hex("576562417574686e20505246207465737420766563746f727303")

        val resultFirst = PasskeyHelper.computePrfValue(secret, first)
        val resultSecond = PasskeyHelper.computePrfValue(secret, second)

        assertArrayEquals(
            hex("3c33e07d202c3b029cc21f1722767021bf27d595933b3d2b6a1b9d5dddc77fae"),
            resultFirst,
        )
        assertArrayEquals(
            hex("a62a8773b19cda90d7ed4ef72a80a804320dbd3997e2f663805ad1fd3293d50b"),
            resultSecond,
        )
    }

    @Test
    fun testCredentialIdDerivation() {
        // From Section 16.17.1.1: Base64Url(SHA-256(IKM || 0x00))"
        val expectedCredentialId = "e02eZ9lPp0UdkF4vGRO4-NxlhWBkL1FCmsmb1tTfRyE"
        val expectedHash = Base64.getUrlDecoder().decode(expectedCredentialId)

        val ikm = "WebAuthn PRF test vectors".toByteArray(Charsets.UTF_8)
        val actualHash = HashManager.sha256(ikm, byteArrayOf(0x00))

        assertArrayEquals("Credential ID hash must match WebAuthn spec derivation", expectedHash, actualHash)
    }

    @Test
    fun testRegistrationPrfOutput() {
        // From Section 16.17.1.1: SHA-256(IKM || 0x01)
        val expectedRegistrationResult = hex("c4172e982e9097c39a6c0cb720cb375b92e3fcad154a63e43a93f1096b1e1973")

        val ikm = "WebAuthn PRF test vectors".toByteArray(Charsets.UTF_8)
        val actualResult = HashManager.sha256(ikm, byteArrayOf(0x01))

        assertArrayEquals("Registration PRF output should match SHA-256(IKM || 0x01)", expectedRegistrationResult, actualResult)
    }
}
