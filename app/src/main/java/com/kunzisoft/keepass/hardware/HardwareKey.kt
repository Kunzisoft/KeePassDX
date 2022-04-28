package com.kunzisoft.keepass.hardware

enum class HardwareKey(val value: String) {
    FIDO2_SECRET("FIDO2 secret"),
    CHALLENGE_RESPONSE_YUBIKEY("Yubikey challenge-response");

    companion object {
        val DEFAULT = FIDO2_SECRET

        fun getStringValues(): List<String> {
            return values().map { it.value }
        }

        fun fromPosition(position: Int): HardwareKey {
            return when (position) {
                0 -> FIDO2_SECRET
                1 -> CHALLENGE_RESPONSE_YUBIKEY
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