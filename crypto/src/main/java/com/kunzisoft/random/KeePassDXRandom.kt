package com.kunzisoft.random

import java.security.SecureRandom

class KeePassDXRandom {

    companion object {

        private val internalSecureRandom: SecureRandom = SecureRandom()

        fun generateCredentialId(): ByteArray {
            // see https://w3c.github.io/webauthn/#credential-id
            val size = 16
            val credentialId = ByteArray(size)
            internalSecureRandom.nextBytes(credentialId)
            return credentialId
        }

    }

}