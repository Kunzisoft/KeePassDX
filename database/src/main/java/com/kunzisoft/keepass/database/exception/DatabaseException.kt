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

import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.database.element.node.Type
import java.io.PrintStream
import java.io.PrintWriter

abstract class DatabaseException : Exception {

    var innerMessage: String? = null
    var parameters = mutableListOf<String>()
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

class FileNotFoundDatabaseException : DatabaseInputException()

class CorruptedDatabaseException : DatabaseInputException()

class InvalidAlgorithmDatabaseException : DatabaseInputException {
    constructor() : super()
    constructor(exception: Throwable) : super(exception)
}

class UnknownDatabaseLocationException : DatabaseException()

class HardwareKeyDatabaseException : DatabaseException()

class EmptyKeyDatabaseException : DatabaseException()

class SignatureDatabaseException : DatabaseInputException()

class VersionDatabaseException : DatabaseInputException()

class InvalidCredentialsDatabaseException : DatabaseInputException {
    constructor() : super()
    constructor(string: String) : super(string)
}

class KDFMemoryDatabaseException(exception: Throwable) : DatabaseInputException(exception)

class NoMemoryDatabaseException(exception: Throwable) : DatabaseInputException(exception)

class DuplicateUuidDatabaseException(type: Type, uuid: NodeId<*>) : DatabaseInputException() {
    init {
        parameters.apply {
            add(type.name)
            add(uuid.toString())
        }
    }
}

class XMLMalformedDatabaseException : DatabaseInputException {
    constructor() : super()
    constructor(string: String) : super(string)
}

class MergeDatabaseKDBException : DatabaseInputException()

class MoveEntryDatabaseException : DatabaseException()

class MoveGroupDatabaseException : DatabaseException()

class CopyEntryDatabaseException : DatabaseException()

class CopyGroupDatabaseException : DatabaseException()

open class DatabaseInputException : DatabaseException {
    constructor() : super()
    constructor(string: String) : super(string)
    constructor(throwable: Throwable) : super(throwable)
}

open class DatabaseOutputException : DatabaseException {
    constructor(string: String) : super(string)
    constructor(string: String, e: Exception) : super(string, e)
    constructor(e: Exception) : super(e)
}
