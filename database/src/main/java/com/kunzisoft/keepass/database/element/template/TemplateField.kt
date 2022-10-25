/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kunzisoft.keepass.database.element.template

import android.content.Context
import com.kunzisoft.keepass.database.R

object TemplateField {

    const val LABEL_STANDARD = "Standard"
    const val LABEL_TEMPLATE = "Template"
    const val LABEL_VERSION = "Version"

    const val LABEL_TITLE = "Title"
    const val LABEL_USERNAME = "Username"
    const val LABEL_PASSWORD = "Password"
    const val LABEL_URL = "URL"
    const val LABEL_EXPIRATION = "Expires"
    const val LABEL_NOTES = "Notes"

    const val LABEL_DEBIT_CREDIT_CARD = "Debit / Credit Card"
    const val LABEL_HOLDER = "Holder"
    const val LABEL_NUMBER = "Number"
    const val LABEL_CVV = "CVV"
    const val LABEL_PIN = "PIN"
    const val LABEL_ID_CARD = "ID Card"
    const val LABEL_NAME = "Name"
    const val LABEL_PLACE_OF_ISSUE = "Place of issue"
    const val LABEL_DATE_OF_ISSUE = "Date of issue"
    const val LABEL_EMAIL = "Email"
    const val LABEL_EMAIL_ADDRESS = "Email address"
    const val LABEL_WIRELESS = "Wi-Fi"
    const val LABEL_SSID = "SSID"
    const val LABEL_TYPE = "Type"
    const val LABEL_CRYPTOCURRENCY = "Cryptocurrency wallet"
    const val LABEL_TOKEN = "Token"
    const val LABEL_PUBLIC_KEY = "Public key"
    const val LABEL_PRIVATE_KEY = "Private key"
    const val LABEL_SEED = "Seed"
    const val LABEL_ACCOUNT = "Account"
    const val LABEL_BANK = "Bank"
    const val LABEL_BIC = "BIC"
    const val LABEL_IBAN = "IBAN"
    const val LABEL_SECURE_NOTE = "Secure Note"
    const val LABEL_MEMBERSHIP = "Membership"

    fun isStandardPasswordName(context: Context, name: String): Boolean {
        return name.equals(LABEL_PASSWORD, true)
            || name == getLocalizedName(context, LABEL_PASSWORD)
    }

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
                || TemplateEngine.containsTemplateDecorator(name))
            return name

        return when {
            LABEL_STANDARD.equals(name, true) -> context.getString(R.string.standard)
            LABEL_TEMPLATE.equals(name, true) -> context.getString(R.string.template)
            LABEL_VERSION.equals(name, true) -> context.getString(R.string.version)

            LABEL_TITLE.equals(name, true) -> context.getString(R.string.entry_title)
            LABEL_USERNAME.equals(name, true) -> context.getString(R.string.entry_user_name)
            LABEL_PASSWORD.equals(name, true) -> context.getString(R.string.entry_password)
            LABEL_URL.equals(name, true) -> context.getString(R.string.entry_url)
            LABEL_EXPIRATION.equals(name, true) -> context.getString(R.string.entry_expires)
            LABEL_NOTES.equals(name, true) -> context.getString(R.string.entry_notes)

            LABEL_DEBIT_CREDIT_CARD.equals(name, true) -> context.getString(R.string.debit_credit_card)
            LABEL_HOLDER.equals(name, true) -> context.getString(R.string.holder)
            LABEL_NUMBER.equals(name, true) -> context.getString(R.string.number)
            LABEL_CVV.equals(name, true) -> context.getString(R.string.card_verification_value)
            LABEL_PIN.equals(name, true) -> context.getString(R.string.personal_identification_number)
            LABEL_ID_CARD.equals(name, true) -> context.getString(R.string.id_card)
            LABEL_NAME.equals(name, true) -> context.getString(R.string.name)
            LABEL_PLACE_OF_ISSUE.equals(name, true) -> context.getString(R.string.place_of_issue)
            LABEL_DATE_OF_ISSUE.equals(name, true) -> context.getString(R.string.date_of_issue)
            LABEL_EMAIL.equals(name, true) -> context.getString(R.string.email)
            LABEL_EMAIL_ADDRESS.equals(name, true) -> context.getString(R.string.email_address)
            LABEL_WIRELESS.equals(name, true) -> context.getString(R.string.wireless)
            LABEL_SSID.equals(name, true) -> context.getString(R.string.ssid)
            LABEL_TYPE.equals(name, true) -> context.getString(R.string.type)
            LABEL_CRYPTOCURRENCY.equals(name, true) -> context.getString(R.string.cryptocurrency)
            LABEL_TOKEN.equals(name, false) -> context.getString(R.string.token)
            LABEL_PUBLIC_KEY.equals(name, true) -> context.getString(R.string.public_key)
            LABEL_PRIVATE_KEY.equals(name, true) -> context.getString(R.string.private_key)
            LABEL_SEED.equals(name, true) -> context.getString(R.string.seed)
            LABEL_ACCOUNT.equals(name, true) -> context.getString(R.string.account)
            LABEL_BANK.equals(name, true) -> context.getString(R.string.bank)
            LABEL_BIC.equals(name, true) -> context.getString(R.string.bank_identifier_code)
            LABEL_IBAN.equals(name, true) -> context.getString(R.string.international_bank_account_number)
            LABEL_SECURE_NOTE.equals(name, true) -> context.getString(R.string.secure_note)
            LABEL_MEMBERSHIP.equals(name, true) -> context.getString(R.string.membership)

            else -> name
        }
    }
}
