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

enum class TemplateAttributeType(val typeString: String) {
    TEXT("text"),
    LIST("list"),
    DATETIME("datetime"),
    DIVIDER("divider");

    companion object {
        fun getFromString(label: String): TemplateAttributeType {
            return when {
                label.contains(TEXT.typeString, true) -> TEXT
                label.contains(LIST.typeString, true) -> LIST
                label.contains(DATETIME.typeString, true) -> DATETIME
                label.contains(DIVIDER.typeString, true) -> DIVIDER
                else -> TEXT
            }
        }
    }
}