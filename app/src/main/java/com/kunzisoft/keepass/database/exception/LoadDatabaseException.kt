package com.kunzisoft.keepass.database.exception

import android.content.res.Resources
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.PwNodeId
import com.kunzisoft.keepass.database.element.Type


class LoadDatabaseArcFourException : LoadDatabaseException {
    constructor() : super(R.string.error_arc4)
    constructor(exception: Throwable) : super(exception, R.string.error_arc4)
}

class LoadDatabaseFileNotFoundException : LoadDatabaseException {
    constructor() : super(R.string.file_not_found_content)
    constructor(exception: Throwable) : super(exception, R.string.file_not_found_content)
}

class LoadDatabaseInvalidAlgorithmException : LoadDatabaseException {
    constructor() : super(R.string.invalid_algorithm)
    constructor(exception: Throwable) : super(exception, R.string.invalid_algorithm)
}

class LoadDatabaseDuplicateUuidException(type: Type, uuid: PwNodeId<*>):
        LoadDatabaseException(R.string.invalid_db_same_uuid, type.name, uuid.toString())

class LoadDatabaseIOException : LoadDatabaseException {
    constructor() : super(R.string.error_load_database)
    constructor(exception: Throwable) : super(exception, R.string.error_load_database)
}

class LoadDatabaseKDFMemoryException : LoadDatabaseException {
    constructor() : super(R.string.error_load_database_KDF_memory)
    constructor(exception: Throwable) : super(exception, R.string.error_load_database_KDF_memory)
}

class LoadDatabaseSignatureException : LoadDatabaseException {
    constructor() : super(R.string.invalid_db_sig)
    constructor(exception: Throwable) : super(exception, R.string.invalid_db_sig)
}

class LoadDatabaseVersionException : LoadDatabaseException {
    constructor() : super(R.string.unsupported_db_version)
    constructor(exception: Throwable) : super(exception, R.string.unsupported_db_version)
}

class LoadDatabaseInvalidCredentialsException : LoadDatabaseException {
    constructor() : super(R.string.invalid_credentials)
    constructor(exception: Throwable) : super(exception, R.string.invalid_credentials)
}

class LoadDatabaseKeyFileEmptyException : LoadDatabaseException {
    constructor() : super(R.string.keyfile_is_empty)
    constructor(exception: Throwable) : super(exception, R.string.keyfile_is_empty)
}

class LoadDatabaseNoMemoryException: LoadDatabaseException {
    constructor() : super(R.string.error_out_of_memory)
    constructor(exception: Throwable) : super(exception, R.string.error_out_of_memory)
}

open class LoadDatabaseException : Exception {

    @StringRes
    var errorId: Int = R.string.error_load_database
    var parameters: (Array<out String>)? = null

    constructor(errorMessageId: Int) : super() {
        errorId = errorMessageId
    }

    constructor(errorMessageId: Int, vararg params: String) : super() {
        errorId = errorMessageId
        parameters = params
    }

    constructor(throwable: Throwable, errorMessageId: Int? = null) : super(throwable) {
        errorMessageId?.let {
            errorId = it
        }
    }

    constructor() : super()

    fun getLocalizedMessage(resources: Resources): String {
        parameters?.let {
            return resources.getString(errorId, *it)
        } ?: return resources.getString(errorId)
    }
}
