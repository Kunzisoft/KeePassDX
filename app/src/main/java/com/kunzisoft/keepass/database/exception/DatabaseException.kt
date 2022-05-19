/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
 *
 */
package com.kunzisoft.keepass.database.exception

import android.content.res.Resources
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.Type
import java.io.PrintStream
import java.io.PrintWriter

abstract class DatabaseException : Exception {

    var innerMessage: String? = null
    abstract var errorId: Int
    var parameters: (Array<out String>)? = null
    var mThrowable: Throwable? = null

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable) {
        mThrowable = throwable
        innerMessage = StringBuilder().apply {
            append(message)
            if (throwable.localizedMessage != null) {
                append(" ")
                append(throwable.localizedMessage)
            }
        }.toString()
    }
    constructor(throwable: Throwable) {
        mThrowable = throwable
        innerMessage = throwable.localizedMessage
    }

    fun getLocalizedMessage(resources: Resources): String {
        val throwable = mThrowable
        if (throwable is DatabaseException)
            errorId = throwable.errorId
        val localMessage = parameters?.let {
            resources.getString(errorId, *it)
        } ?: resources.getString(errorId)
        return StringBuilder().apply {
            append(localMessage)
            if (innerMessage != null) {
                append(" ")
                append(innerMessage)
            }
        }.toString()
    }

    override fun printStackTrace() {
        mThrowable?.printStackTrace()
        super.printStackTrace()
    }

    override fun printStackTrace(s: PrintStream) {
        mThrowable?.printStackTrace(s)
        super.printStackTrace(s)
    }

    override fun printStackTrace(s: PrintWriter) {
        mThrowable?.printStackTrace(s)
        super.printStackTrace(s)
    }
}

class FileNotFoundDatabaseException : DatabaseInputException() {
    @StringRes
    override var errorId: Int = R.string.file_not_found_content
}

class CorruptedDatabaseException : DatabaseInputException() {
    @StringRes
    override var errorId: Int = R.string.corrupted_file
}

class InvalidAlgorithmDatabaseException : DatabaseInputException {
    @StringRes
    override var errorId: Int = R.string.invalid_algorithm
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class UnknownDatabaseLocationException : DatabaseException() {
    @StringRes
    override var errorId: Int = R.string.error_location_unknown
}

class SignatureDatabaseException : DatabaseInputException() {
    @StringRes
    override var errorId: Int = R.string.invalid_db_sig
}

class VersionDatabaseException : DatabaseInputException() {
    @StringRes
    override var errorId: Int = R.string.unsupported_db_version
}

class InvalidCredentialsDatabaseException : DatabaseInputException {
    @StringRes
    override var errorId: Int = R.string.invalid_credentials
    constructor() : super()
    constructor(string: String) : super(string)
}

class KDFMemoryDatabaseException(exception: Throwable) : DatabaseInputException(exception) {
    @StringRes
    override var errorId: Int = R.string.error_load_database_KDF_memory
}

class NoMemoryDatabaseException(exception: Throwable) : DatabaseInputException(exception) {
    @StringRes
    override var errorId: Int = R.string.error_out_of_memory
}

class DuplicateUuidDatabaseException(type: Type, uuid: NodeId<*>) : DatabaseInputException() {
    @StringRes
    override var errorId: Int = R.string.invalid_db_same_uuid
    init {
        parameters = arrayOf(type.name, uuid.toString())
    }
}

class XMLMalformedDatabaseException : DatabaseInputException {
    @StringRes
    override var errorId: Int = R.string.error_XML_malformed
    constructor() : super()
    constructor(string: String) : super(string)
}

class MergeDatabaseKDBException : DatabaseInputException() {
    @StringRes
    override var errorId: Int = R.string.error_unable_merge_database_kdb
}

class MoveEntryDatabaseException : DatabaseException() {
    @StringRes
    override var errorId: Int = R.string.error_move_entry_here
}

class MoveGroupDatabaseException : DatabaseException() {
    @StringRes
    override var errorId: Int = R.string.error_move_group_here
}

class CopyEntryDatabaseException : DatabaseException() {
    @StringRes
    override var errorId: Int = R.string.error_copy_entry_here
}

class CopyGroupDatabaseException : DatabaseException() {
    @StringRes
    override var errorId: Int = R.string.error_copy_group_here
}

open class DatabaseInputException : DatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_load_database
    constructor() : super()
    constructor(string: String) : super(string)
    constructor(throwable: Throwable) : super(throwable)
}

open class DatabaseOutputException : DatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_save_database
    constructor(string: String) : super(string)
    constructor(string: String, e: Exception) : super(string, e)
    constructor(e: Exception) : super(e)
}