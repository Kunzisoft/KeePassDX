package com.kunzisoft.keepass.database.exception

import android.content.res.Resources
import androidx.annotation.StringRes
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.PwNodeId
import com.kunzisoft.keepass.database.element.Type
import java.io.IOException


class LoadDatabaseArcFourException : LoadDatabaseException(R.string.error_arc4)

class LoadDatabaseFileNotFoundException : LoadDatabaseException(R.string.file_not_found_content)

class LoadDatabaseInvalidAlgorithmException : LoadDatabaseException(R.string.invalid_algorithm)

class LoadDatabaseDuplicateUuidException(type: Type, uuid: PwNodeId<*>):
        LoadDatabaseException("Error, a $type with the same UUID $uuid already exists")

class LoadDatabaseIOException(exception: IOException) : LoadDatabaseException(exception, R.string.error_load_database)

class LoadDatabaseKDFMemoryException(exception: IOException) : LoadDatabaseException(exception, R.string.error_load_database_KDF_memory)

class LoadDatabaseSignatureException : LoadDatabaseException(R.string.invalid_db_sig)

class LoadDatabaseVersionException : LoadDatabaseException(R.string.unsupported_db_version)

open class LoadDatabaseInvalidKeyFileException : LoadDatabaseException(R.string.keyfile_does_not_exist)

class LoadDatabaseInvalidPasswordException : LoadDatabaseException(R.string.invalid_password)

class LoadDatabaseKeyFileEmptyException : LoadDatabaseException(R.string.keyfile_is_empty)

class LoadDatabaseNoMemoryException(exception: OutOfMemoryError) : LoadDatabaseException(exception, R.string.error_out_of_memory)

open class LoadDatabaseException : Exception {

    @StringRes
    var errorId: Int = R.string.error_load_database

    constructor(errorMessage: String) : super(errorMessage)

    constructor(errorMessageId: Int) : super() {
        errorId = errorMessageId
    }

    constructor(throwable: Throwable, errorMessageId: Int? = null) : super(throwable) {
        errorMessageId?.let {
            errorId = it
        }
    }

    constructor() : super()

    companion object {
        private const val serialVersionUID = 5191964825154190923L
    }

    fun getLocalizedMessage(resources: Resources): String {
        return resources.getString(errorId)
    }
}
