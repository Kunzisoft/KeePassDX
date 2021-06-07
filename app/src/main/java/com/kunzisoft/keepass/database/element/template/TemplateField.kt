package com.kunzisoft.keepass.database.element.template

import android.content.Context
import com.kunzisoft.keepass.R

object TemplateField {

    const val LABEL_TITLE = "Title"
    const val LABEL_USERNAME = "Username"
    const val LABEL_PASSWORD = "Password"
    const val LABEL_URL = "URL"
    const val LABEL_EXPIRATION = "Expires"
    const val LABEL_NOTES = "Notes"

    const val LABEL_VERSION = "Version"
    const val LABEL_CREDIT_CARD = "Credit Card"
    const val LABEL_CARD_CARDHOLDER = "Card holder"
    const val LABEL_NUMBER = "Number"
    const val LABEL_CVV = "CVV"
    const val LABEL_PIN = "PIN"
    const val LABEL_ID_CARD = "ID Card"
    const val LABEL_NAME = "Name"
    const val LABEL_PLACE_OF_ISSUE = "Place of issue"
    const val LABEL_DATE_OF_ISSUE = "Date of issue"
    const val LABEL_EMAIL_TITLE = "E-mail"
    const val LABEL_EMAIL_ADDRESS = "E-mail address"
    const val LABEL_WIRELESS_LAN = "Wireless LAN"
    const val LABEL_SSID = "SSID"
    const val LABEL_SECURE_NOTE = "Secure Note"
    const val LABEL_MEMBERSHIP = "Membership"

    fun isStandardFieldName(name: String): Boolean {
        return arrayOf(
                LABEL_TITLE,
                LABEL_USERNAME,
                LABEL_PASSWORD,
                LABEL_URL,
                LABEL_EXPIRATION,
                LABEL_NOTES
        ).firstOrNull { it.equals(name, true) } != null
    }

    fun getLocalizedName(context: Context?, name: String): String {
        if (context == null
                || TemplateEngine.isTemplateNameAttribute(name))
            return name

        return when {
            LABEL_TITLE.equals(name, true) -> context.getString(R.string.entry_title)
            LABEL_USERNAME.equals(name, true) -> context.getString(R.string.entry_user_name)
            LABEL_PASSWORD.equals(name, true) -> context.getString(R.string.entry_password)
            LABEL_URL.equals(name, true) -> context.getString(R.string.entry_url)
            LABEL_EXPIRATION.equals(name, true) -> context.getString(R.string.entry_expires)
            LABEL_NOTES.equals(name, true) -> context.getString(R.string.entry_notes)

            LABEL_VERSION.equals(name, true) -> context.getString(R.string.version)
            LABEL_CREDIT_CARD.equals(name, true) -> context.getString(R.string.credit_card)
            LABEL_CARD_CARDHOLDER.equals(name, true) -> context.getString(R.string.credit_card_cardholder)
            LABEL_NUMBER.equals(name, true) -> context.getString(R.string.credit_card_number)
            LABEL_CVV.equals(name, true) -> context.getString(R.string.credit_card_security_code)
            LABEL_PIN.equals(name, true) -> context.getString(R.string.credit_card_pin)
            LABEL_ID_CARD.equals(name, true) -> context.getString(R.string.id_card)
            LABEL_NAME.equals(name, true) -> context.getString(R.string.name)
            LABEL_PLACE_OF_ISSUE.equals(name, true) -> context.getString(R.string.place_of_issue)
            LABEL_DATE_OF_ISSUE.equals(name, true) -> context.getString(R.string.date_of_issue)
            LABEL_EMAIL_TITLE.equals(name, true) -> context.getString(R.string.email)
            LABEL_EMAIL_ADDRESS.equals(name, true) -> context.getString(R.string.email_address)
            LABEL_WIRELESS_LAN.equals(name, true) -> context.getString(R.string.vireless_lan)
            LABEL_SSID.equals(name, true) -> context.getString(R.string.ssid)
            LABEL_SECURE_NOTE.equals(name, true) -> context.getString(R.string.secure_note)
            LABEL_MEMBERSHIP.equals(name, true) -> context.getString(R.string.membership)

            else -> name
        }
    }
}