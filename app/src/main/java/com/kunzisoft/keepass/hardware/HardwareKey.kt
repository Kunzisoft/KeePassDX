package com.kunzisoft.keepass.hardware

enum class HardwareKey(val value: String) {
    HMAC_SHA1_KPXC("HMAC-SHA1 KPXC"),
    HMAC_SHA1_KP2("HMAC-SHA1 KP2"),
    OATH_HOTP("OATH HOTP"),
    HMAC_SECRET_FIDO2("HMAC-SECRET FIDO2");

    companion object {
        val DEFAULT = HMAC_SHA1_KPXC

        fun getStringValues(): List<String> {
            return values().map { it.value }
        }

        fun fromPosition(position: Int): HardwareKey {
            return when (position) {
                0 -> HMAC_SHA1_KPXC
                1 -> HMAC_SHA1_KP2
                2 -> OATH_HOTP
                3 -> HMAC_SECRET_FIDO2
                else -> DEFAULT
            }
        }

        fun getHardwareKeyFromString(text: String): HardwareKey {
            values().find { it.value == text }?.let {
                return it
            }
            return DEFAULT
        }
    }
}