package com.kunzisoft.keepass.database.exception

import java.io.IOException

class UnknownKDF : IOException(message) {
    companion object {
        private const val message = "Unknown key derivation function"
    }
}
