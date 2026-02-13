package com.kunzisoft.keepass.hardware

enum class HardwareKey(val value: String) {
    FIDO2_HMAC_SECRET("FIDO2 hmac-secret"),
    YUBIKEY_HMAC_SHA1("Yubikey hmac-sha1");

    override fun toString(): String {
        return value
    }

    companion object {
        val DEFAULT = FIDO2_HMAC_SECRET

        fun getStringValues(): List<String> {
            return HardwareKey.entries.map { it.value }
        }

        fun fromPosition(position: Int): HardwareKey {
            return when (position) {
                0 -> FIDO2_HMAC_SECRET
                1 -> YUBIKEY_HMAC_SHA1
                else -> DEFAULT
            }
        }

        fun getHardwareKeyFromString(text: String?): HardwareKey? {
            if (text == null)
                return null
            HardwareKey.entries.find { it.value == text }?.let {
                return it
            }
            return DEFAULT
        }
    }
}