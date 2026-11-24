/*
 * Copyright 2026 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.tests.template

import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.TemplateEngineCompatible
import junit.framework.TestCase
import org.junit.Assert

class TemplateEngineCompatibleTest : TestCase() {

    fun testDecodeTemplateEntryWithNegativeOrMissingPosition() {
        val database = DatabaseKDBX()
        val engine = TemplateEngineCompatible(database)

        val templateEntry = EntryKDBX().apply {
            // Attribute 1 has position 0
            putField("_etm_position_FieldA", ProtectedString(false, "0"))
            putField("_etm_title_FieldA", ProtectedString(false, "Field A"))
            putField("_etm_type_FieldA", ProtectedString(false, "Inline"))

            // Attribute 2 has title and type, but NO position field (defaults to -1)
            putField("_etm_title_FieldB", ProtectedString(false, "Field B"))
            putField("_etm_type_FieldB", ProtectedString(false, "Inline"))
        }

        // Before fix, this threw ArrayIndexOutOfBoundsException: length=2; index=-1
        val decodedEntry = engine.decodeTemplateEntry(templateEntry)

        Assert.assertNotNull(decodedEntry)
        Assert.assertEquals(2, decodedEntry.getFields().size)
    }

    fun testDecodeTemplateEntryOrder() {
        val database = DatabaseKDBX()
        val engine = TemplateEngineCompatible(database)

        val templateEntry = EntryKDBX().apply {
            putField("_etm_position_Second", ProtectedString(false, "1"))
            putField("_etm_title_Second", ProtectedString(false, "Second"))
            putField("_etm_type_Second", ProtectedString(false, "Inline"))

            putField("_etm_position_First", ProtectedString(false, "0"))
            putField("_etm_title_First", ProtectedString(false, "First"))
            putField("_etm_type_First", ProtectedString(false, "Inline"))

            putField("_etm_title_ThirdUnpositioned", ProtectedString(false, "Third"))
            putField("_etm_type_ThirdUnpositioned", ProtectedString(false, "Inline"))
        }

        val decodedEntry = engine.decodeTemplateEntry(templateEntry)
        Assert.assertNotNull(decodedEntry)
        val fields = decodedEntry.getFields()
        Assert.assertEquals(3, fields.size)
        // Verify ordered correctly: First (pos 0), Second (pos 1), ThirdUnpositioned (pos -1)
        Assert.assertEquals("[First]", fields[0].name)
        Assert.assertEquals("[Second]", fields[1].name)
        Assert.assertEquals("[ThirdUnpositioned]", fields[2].name)
    }
}
