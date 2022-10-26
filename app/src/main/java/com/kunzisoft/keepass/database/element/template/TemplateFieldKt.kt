package com.kunzisoft.keepass.database.element.template

import android.content.Context
import com.kunzisoft.keepass.R


fun TemplateField.isStandardPasswordName(context: Context, name: String): Boolean {
    return name.equals(LABEL_PASSWORD, true)
            || name == getLocalizedName(context, LABEL_PASSWORD)
}

fun TemplateField.getLocalizedName(context: Context?, name: String): String {
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
