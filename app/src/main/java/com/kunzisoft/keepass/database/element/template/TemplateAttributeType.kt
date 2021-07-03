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

enum class TemplateAttributeType(val label: String) {
    SINGLE_LINE("Single Line"),
    SMALL_MULTILINE("Small Multiline"),
    MULTILINE("Multiline"),
    DATE("Date"),
    TIME("Time"),
    DATETIME("DateTime"),
    DIVIDER("Divider");

    companion object {
        fun getFromLabel(label: String): TemplateAttributeType {
            return when {
                label.equals(SINGLE_LINE.label, true) -> SINGLE_LINE
                label.equals(SMALL_MULTILINE.label, true) -> SMALL_MULTILINE
                label.equals(MULTILINE.label, true) -> MULTILINE
                label.equals(DATE.label, true) -> DATE
                label.equals(TIME.label, true) -> TIME
                label.equals(DATETIME.label, true) -> DATETIME
                label.equals(DIVIDER.label, true) -> DIVIDER
                else -> SINGLE_LINE
            }
        }
    }
}