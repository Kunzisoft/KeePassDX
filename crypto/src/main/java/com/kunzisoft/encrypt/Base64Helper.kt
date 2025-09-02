package com.kunzisoft.encrypt

import android.util.Base64

class Base64Helper {

    companion object {

        fun b64Decode(encodedString: String): ByteArray {
            return Base64.decode(
                encodedString,
                Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
            )
        }

        fun b64Encode(data: ByteArray): String {
            return Base64.encodeToString(
                data,
                Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
            )
        }
    }
}