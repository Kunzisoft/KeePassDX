package com.kunzisoft.keepass.database.exception

import android.content.res.Resources
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.PwNodeId
import com.kunzisoft.keepass.database.element.Type

abstract class DatabaseException : Exception {

    abstract var errorId: Int
    var parameters: (Array<out String>)? = null

    constructor() : super()

    constructor(throwable: Throwable) : super(throwable)

    fun getLocalizedMessage(resources: Resources): String {
        parameters?.let {
            return resources.getString(errorId, *it)
        } ?: return resources.getString(errorId)
    }
}

open class LoadDatabaseException : DatabaseException {

    @StringRes
    override var errorId: Int = R.string.error_load_database

    constructor() : super()

    constructor(vararg params: String) : super() {
        parameters = params
    }

    constructor(throwable: Throwable) : super(throwable)
}

class LoadDatabaseArcFourException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_arc4

    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class LoadDatabaseFileNotFoundException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.file_not_found_content

    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class LoadDatabaseInvalidAlgorithmException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.invalid_algorithm

    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class LoadDatabaseDuplicateUuidException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.invalid_algorithm

    constructor(type: Type, uuid: PwNodeId<*>) : super() {
        parameters = Array(2) {
            when(it) {
                1 -> type.name
                2 -> uuid.toString()
                else -> {""}
            }
        }
    }
    constructor(exception: Throwable) : super(exception)
}

class LoadDatabaseIOException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_load_database

    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class LoadDatabaseKDFMemoryException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_load_database_KDF_memory

    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class LoadDatabaseSignatureException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.invalid_db_sig

    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class LoadDatabaseVersionException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.unsupported_db_version

    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class LoadDatabaseInvalidCredentialsException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.invalid_credentials

    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class LoadDatabaseKeyFileEmptyException : LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.keyfile_is_empty
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class LoadDatabaseNoMemoryException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_out_of_memory
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class MoveDatabaseEntryException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_move_entry_here
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class MoveDatabaseGroupException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_move_folder_in_itself
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class CopyDatabaseEntryException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_copy_entry_here
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class CopyDatabaseGroupException: LoadDatabaseException {
    @StringRes
    override var errorId: Int = R.string.error_copy_group_here
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class DatabaseOutputException : Exception {
    constructor(string: String) : super(string)

    constructor(string: String, e: Exception) : super(string, e)

    constructor(e: Exception) : super(e)
}