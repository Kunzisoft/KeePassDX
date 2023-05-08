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
}
