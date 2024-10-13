package com.kunzisoft.keepass.credentialprovider.util

import org.apache.commons.codec.binary.Base64

class Base64Helper {

    companion object {

        fun b64Decode(encodedString: String?): ByteArray {
            return Base64.decodeBase64(encodedString)
        }

        fun b64Encode(data: ByteArray): String {
            return android.util.Base64.encodeToString(
                data,
                android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
            )
        }
    }
}