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
package com.kunzisoft.keepass.database.exception

import android.content.res.Resources
import com.kunzisoft.keepass.R

fun DatabaseException.getLocalizedMessage(resources: Resources): String = parameters?.let {
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
        is DuplicateUuidDatabaseException -> resources.getString(R.string.invalid_db_same_uuid)
        is XMLMalformedDatabaseException -> resources.getString(R.string.error_XML_malformed)
        is MergeDatabaseKDBException -> resources.getString(R.string.error_unable_merge_database_kdb)
        is MoveEntryDatabaseException -> resources.getString(R.string.error_move_entry_here)
        is MoveGroupDatabaseException -> resources.getString(R.string.error_move_group_here)
        is CopyEntryDatabaseException -> resources.getString(R.string.error_copy_entry_here)
        is CopyGroupDatabaseException -> resources.getString(R.string.error_copy_group_here)
        is DatabaseInputException -> resources.getString(R.string.error_load_database)
        is DatabaseOutputException -> resources.getString(R.string.error_save_database)
        else -> (mThrowable as? DatabaseException)?.getLocalizedMessage(resources)
    }
} ?: resources.getString(R.string.error_load_database)

