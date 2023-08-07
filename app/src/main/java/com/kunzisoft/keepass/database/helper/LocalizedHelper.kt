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
package com.kunzisoft.keepass.database.helper

import android.content.Context
import android.content.res.Resources
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.database.element.template.TemplateEngine
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.exception.*

fun DatabaseException.getLocalizedMessage(resources: Resources): String? =
    when (this) {
        is FileNotFoundDatabaseException -> resources.getString(R.string.file_not_found_content)
        is CorruptedDatabaseException -> resources.getString(R.string.corrupted_file)
        is InvalidAlgorithmDatabaseException -> resources.getString(R.string.invalid_algorithm)
        is UnknownDatabaseLocationException -> resources.getString(R.string.error_location_unknown)
        is HardwareKeyDatabaseException -> resources.getString(R.string.error_hardware_key_unsupported)
        is EmptyKeyDatabaseException -> resources.getString(R.string.error_empty_key)
        is SignatureDatabaseException -> resources.getString(R.string.invalid_db_sig)
        is VersionDatabaseException -> resources.getString(R.string.unsupported_db_version)
        is InvalidCredentialsDatabaseException -> resources.getString(R.string.invalid_credentials)
        is KDFMemoryDatabaseException -> resources.getString(R.string.error_load_database_KDF_memory)
        is NoMemoryDatabaseException -> resources.getString(R.string.error_out_of_memory)
        is DuplicateUuidDatabaseException -> resources.getString(R.string.invalid_db_same_uuid, parameters[0], parameters[1])
        is XMLMalformedDatabaseException -> resources.getString(R.string.error_XML_malformed)
        is MergeDatabaseKDBException -> resources.getString(R.string.error_unable_merge_database_kdb)
        is MoveEntryDatabaseException -> resources.getString(R.string.error_move_entry_here)
        is MoveGroupDatabaseException -> resources.getString(R.string.error_move_group_here)
        is CopyEntryDatabaseException -> resources.getString(R.string.error_copy_entry_here)
        is CopyGroupDatabaseException -> resources.getString(R.string.error_copy_group_here)
        is DatabaseInputException -> resources.getString(R.string.error_load_database)
        is DatabaseOutputException -> resources.getString(R.string.error_save_database)
        else -> localizedMessage
    }

fun CompressionAlgorithm.getLocalizedName(resources: Resources): String {
    return when (this) {
        CompressionAlgorithm.NONE -> resources.getString(R.string.compression_none)
        CompressionAlgorithm.GZIP -> resources.getString(R.string.compression_gzip)
    }
}

fun TemplateField.isStandardPasswordName(context: Context, name: String): Boolean {
    return name.equals(LABEL_PASSWORD, true)
            || name == getLocalizedName(context, LABEL_PASSWORD)
}

fun TemplateField.getLocalizedName(context: Context?, name: String): String {
    if (context == null
        || TemplateEngine.containsTemplateDecorator(name)
    )
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


