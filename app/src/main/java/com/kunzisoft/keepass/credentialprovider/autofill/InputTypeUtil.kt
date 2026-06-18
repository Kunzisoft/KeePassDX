package com.kunzisoft.keepass.credentialprovider.autofill

import android.text.InputType

object InputTypeUtil {

    fun isPasswordInputType(inputType: Int): Boolean =
        isVariationType(inputType,
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)

    fun isUsernameInputType(inputType: Int): Boolean =
        isVariationType(inputType,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_NORMAL,
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
            InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT)

    fun isVariationType(inputType: Int, vararg types: Int): Boolean {
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return types.any { variation == it }
    }
}
