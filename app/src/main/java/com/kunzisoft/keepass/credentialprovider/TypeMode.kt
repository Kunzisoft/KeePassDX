package com.kunzisoft.keepass.credentialprovider

enum class TypeMode(val useUserVerification: Boolean = false) {
    DEFAULT,
    MAGIKEYBOARD,
    AUTOFILL,
    PASSWORD(useUserVerification = true),
    PASSKEY(useUserVerification = true)
}