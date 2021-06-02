package com.kunzisoft.keepass.database.element.template

import android.content.Context
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.model.Field
import com.kunzisoft.keepass.utils.UuidUtil

object TemplateField {

    const val STANDARD_TITLE = "title"
    const val STANDARD_USERNAME = "username"
    const val STANDARD_PASSWORD = "password"
    const val STANDARD_URL = "url"
    const val STANDARD_EXPIRATION = "expires"
    const val STANDARD_NOTES = "notes"

    const val CREDIT_CARD_TITLE = "Credit Card"
    const val CREDIT_CARD_CARDHOLDER = "Card holder"
    const val CREDIT_CARD_NUMBER = "Number"
    const val CREDIT_CARD_CVV = "CVV"
    const val CREDIT_CARD_PIN = "PIN"

    const val ID_CARD_TITLE = "ID Card"
    const val ID_CARD_NUMBER = "Number"
    const val ID_CARD_NAME = "Name"
    const val ID_CARD_PLACE_OF_ISSUE = "Place of issue"
    const val ID_CARD_DATE_OF_ISSUE = "Date of issue"

    const val EMAIL_TITLE = "E-mail"
    const val EMAIL_ADDRESS = "E-mail address"
    const val EMAIL_URL = "URL"
    const val EMAIL_PASSWORD = "Password"

    const val WIRELESS_LAN_TITLE = "Wireless LAN"
    const val WIRELESS_LAN_SSID = "SSID"
    const val WIRELESS_LAN_PASSWORD = "Password"

    const val SECURE_NOTE_TITLE = "Secure Note"
    const val SECURE_NOTE_NOTES = "Notes"

    const val MEMBERSHIP_TITLE = "Membership"
    const val MEMBERSHIP_Number = "Number"
    const val MEMBERSHIP_URL = "URL"
    const val MEMBERSHIP_EXPIRATION = "@exp_date"

    fun isStandardFieldName(name: String): Boolean {
        return arrayOf(
                STANDARD_TITLE,
                STANDARD_USERNAME,
                STANDARD_PASSWORD,
                STANDARD_URL,
                STANDARD_EXPIRATION,
                STANDARD_NOTES
        ).firstOrNull { it.equals(name, true) } != null
    }

    fun getLocalizedName(context: Context?, fieldName: String): String {
        if (context == null)
            return fieldName
        return when (fieldName) {
            STANDARD_TITLE -> context.getString(R.string.entry_title)
            STANDARD_USERNAME -> context.getString(R.string.entry_user_name)
            STANDARD_PASSWORD -> context.getString(R.string.entry_password)
            STANDARD_URL -> context.getString(R.string.entry_url)
            STANDARD_EXPIRATION -> context.getString(R.string.entry_expires)
            STANDARD_NOTES -> context.getString(R.string.entry_notes)

            CREDIT_CARD_CARDHOLDER -> context.getString(R.string.credit_card_cardholder)
            CREDIT_CARD_NUMBER -> context.getString(R.string.credit_card_number)
            CREDIT_CARD_CVV -> context.getString(R.string.credit_card_security_code)
            CREDIT_CARD_PIN -> context.getString(R.string.credit_card_pin)

            // TODO Others translations
            else -> fieldName
        }
    }

    fun getTemplateUUIDField(template: Template): Field? {
        UuidUtil.toHexString(template.uuid)?.let { uuidString ->
            return Field(TemplateEngine.TEMPLATE_ENTRY_UUID,
                    ProtectedString(false, uuidString))
        }
        return null
    }
}