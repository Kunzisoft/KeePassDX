package com.kunzisoft.keepass.model

import android.content.Context
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.security.ProtectedString

object CreditCardCustomFields {
    const val CC_CARDHOLDER_FIELD_NAME = "CREDIT_CARD_CARDHOLDER"
    const val CC_NUMBER_FIELD_NAME = "CREDIT_CARD_NUMBER"
    const val CC_EXP_FIELD_NAME = "CREDIT_CARD_EXPIRATION"
    const val CC_CVV_FIELD_NAME = "CREDIT_CARD_CVV"

    val CC_CUSTOM_FIELDS = arrayOf(CC_CARDHOLDER_FIELD_NAME, CC_NUMBER_FIELD_NAME,
            CC_EXP_FIELD_NAME, CC_CVV_FIELD_NAME)

    fun getLocalizedName(context: Context, fieldName: String): String {
        return when (fieldName) {
            CC_CARDHOLDER_FIELD_NAME -> context.getString(R.string.cc_cardholder)
            CC_NUMBER_FIELD_NAME -> context.getString(R.string.cc_number)
            CC_EXP_FIELD_NAME -> context.getString(R.string.cc_expiration)
            CC_CVV_FIELD_NAME -> context.getString(R.string.cc_security_code)
            else -> fieldName
        }
    }

    fun buildAllFields(cardholder: String, number: String, expiration: String, cvv: String): ArrayList<Field> {

        val ccnField = Field(CC_NUMBER_FIELD_NAME, ProtectedString(false, number))
        val expirationField = Field(CC_EXP_FIELD_NAME, ProtectedString(false, expiration))
        val cvvField = Field(CC_CVV_FIELD_NAME, ProtectedString(true, cvv))
        val ccNameField = Field(CC_CARDHOLDER_FIELD_NAME, ProtectedString(false, cardholder))

        return arrayListOf(ccNameField, ccnField, expirationField, cvvField)
    }
}