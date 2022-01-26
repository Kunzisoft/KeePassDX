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

import android.util.Log
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.utils.UuidUtil

class TemplateEngineCompatible(database: DatabaseKDBX): TemplateEngine(database) {

    override fun getVersion(): Int {
        return 1
    }

    override fun getTemplate(entryKDBX: EntryKDBX): Template? {
        UuidUtil.fromHexString(entryKDBX.getCustomFieldValue(TEMPLATE_ENTRY_UUID))?.let { templateUUID ->
            return getTemplateByCache(templateUUID)
        }
        return null
    }

    override fun removeMetaTemplateRecognitionFromEntry(entry: EntryKDBX): EntryKDBX {
        val entryCopy = EntryKDBX().apply {
            updateWith(entry)
        }
        entryCopy.removeField(TEMPLATE_ENTRY_UUID)
        return entryCopy
    }

    private fun getTemplateUUIDField(template: Template): Field? {
        UuidUtil.toHexString(template.uuid)?.let { uuidString ->
            return Field(TEMPLATE_ENTRY_UUID,
                ProtectedString(false, uuidString))
        }
        return null
    }

    override fun addMetaTemplateRecognitionToEntry(template: Template, entry: EntryKDBX): EntryKDBX {
        val entryCopy = EntryKDBX().apply {
            updateWith(entry)
        }
        // Add template field
        if (template != Template.STANDARD
            && template != CREATION) {
            getTemplateUUIDField(template)?.let { templateField ->
                entryCopy.putField(templateField)
            }
        } else {
            entryCopy.removeField(TEMPLATE_ENTRY_UUID)
        }
        return entryCopy
    }

    private fun getOrRetrieveAttributeFromName(attributes: HashMap<String, TemplateAttributePosition>, name: String): TemplateAttributePosition {
        return if (attributes.containsKey(name)) {
            attributes[name]!!
        } else {
            val newAttribute = TemplateAttributePosition(
                -1,
                TemplateAttribute(name, TemplateAttributeType.TEXT)
            )
            attributes[name] = newAttribute
            newAttribute
        }
    }

    override fun buildTemplateEntryField(attribute: TemplateAttribute): Field {
        val typeAndOptions = attribute.type.typeString +
                TemplateAttributeOption.getStringFromOptions(attribute.options)
        // PREFIX_DECODED_TEMPLATE to fix same label as standard fields
        return Field(addTemplateDecorator(decodeTemplateAttribute(attribute.label)),
            ProtectedString(attribute.protected, typeAndOptions))
    }

    override fun decodeTemplateEntry(templateEntry: EntryKDBX): EntryKDBX {
        val attributes = HashMap<String, TemplateAttributePosition>()
        val defaultValues = HashMap<String, String>()
        val entryCopy = EntryKDBX().apply {
            updateWith(templateEntry)
        }
        // Remove template version
        entryCopy.getFieldValue(TEMPLATE_LABEL_VERSION)
        try {
            // value.toIntOrNull()
            // At the moment, only the version 1 is known
        } catch (e: Exception) {
            Log.e(TAG, "Unable to retrieve template version", e)
        }
        entryCopy.removeField(TEMPLATE_LABEL_VERSION)
        // Dynamic attributes
        templateEntry.doForEachDecodedCustomField { field ->

            val label = field.name
            val value = field.protectedValue.stringValue
            when {
                label.startsWith(TEMPLATE_ATTRIBUTE_POSITION_PREFIX, true) -> {
                    try {
                        val attributeName = label.substring(TEMPLATE_ATTRIBUTE_POSITION_PREFIX.length)
                        val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                        attribute.position = value.toInt()
                        entryCopy.removeField(field.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template position", e)
                    }
                }
                label.startsWith(TEMPLATE_ATTRIBUTE_TITLE_PREFIX, true) -> {
                    try {
                        val attributeName = label.substring(TEMPLATE_ATTRIBUTE_TITLE_PREFIX.length)
                        val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                        // Here title is an alias if different (often the same)
                        if (attributeName != value) {
                            attribute.attribute.alias = value
                        }
                        entryCopy.removeField(field.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template title", e)
                    }
                }
                label.startsWith(TEMPLATE_ATTRIBUTE_TYPE_PREFIX, true) -> {
                    try {
                        val attributeName = label.substring(TEMPLATE_ATTRIBUTE_TYPE_PREFIX.length)
                        val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                        if (value.contains(TEMPLATE_ATTRIBUTE_TYPE_PROTECTED, true)) {
                            attribute.attribute.protected = true
                        }
                        when {
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_INLINE_URL, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TEXT
                                attribute.attribute.options.setLink(true)
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_INLINE, true) ||
                                    value.contains(TEMPLATE_ATTRIBUTE_TYPE_POPOUT, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TEXT
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_MULTILINE, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TEXT
                                attribute.attribute.options.setNumberLinesToMany()
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_RICH_TEXTBOX, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TEXT
                                attribute.attribute.options.setNumberLinesToMany()
                                attribute.attribute.options.setLink(true)
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_LISTBOX, true) -> {
                                attribute.attribute.type = TemplateAttributeType.LIST
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATETIME
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_DATE, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATETIME
                                attribute.attribute.options.setDateFormatToDate()
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_TIME, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATETIME
                                attribute.attribute.options.setDateFormatToTime()
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_DIVIDER, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DIVIDER
                            }
                        }
                        entryCopy.removeField(field.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template type", e)
                    }
                }
                label.startsWith(TEMPLATE_ATTRIBUTE_OPTIONS_PREFIX, true) -> {
                    try {
                        val attributeName = label.substring(TEMPLATE_ATTRIBUTE_OPTIONS_PREFIX.length)
                        val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                        if (value.isNotEmpty()) {
                            attribute.attribute.options.put(TEMPLATE_ATTRIBUTE_OPTIONS_TEMP, value)
                        }
                        entryCopy.removeField(field.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template options", e)
                    }
                }
                else -> {
                    // To retrieve default values
                    if (value.isNotEmpty()) {
                        defaultValues[label] = value
                    }
                    entryCopy.removeField(label)
                }
            }
        }

        val newFields = arrayOfNulls<Field>(attributes.size)
        attributes.values.forEach {

            val attribute = it.attribute

            // Add password generator
            if (attribute.label.equals(TEMPLATE_ATTRIBUTE_PASSWORD, true)) {
                attribute.options.associatePasswordGenerator()
            }

            // Add default value
            if (defaultValues.containsKey(attribute.label)) {
                attribute.options.default = defaultValues[attribute.label]!!
            }

            // Recognize each temp option
            attribute.options.get(TEMPLATE_ATTRIBUTE_OPTIONS_TEMP)?.let { defaultOption ->
                when (attribute.type) {
                    TemplateAttributeType.TEXT -> {
                        try {
                            // It's always a number of lines...
                            attribute.options.setNumberLines(defaultOption.toInt())
                        } catch (e: Exception) {
                            Log.e(TAG, "Unable to transform default text option", e)
                        }
                    }
                    TemplateAttributeType.LIST -> {
                        try {
                            // Default attribute option is items of the list
                            val items = defaultOption.split(",")
                            attribute.options.setListItems(*items.toTypedArray())
                        } catch (e: Exception) {
                            Log.e(TAG, "Unable to transform default list option", e)
                        }
                    }
                    TemplateAttributeType.DATETIME -> {
                        try {
                            // Default attribute option is datetime, date or time
                            when {
                                defaultOption.equals(TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME, true) -> {
                                    // Do not add option if it's datetime
                                }
                                defaultOption.equals(TEMPLATE_ATTRIBUTE_TYPE_DATE, true) -> {
                                    attribute.options.setDateFormatToDate()
                                }
                                defaultOption.equals(TEMPLATE_ATTRIBUTE_TYPE_TIME, true) -> {
                                    attribute.options.setDateFormatToTime()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Unable to transform default datetime", e)
                        }
                    }
                    TemplateAttributeType.DIVIDER -> {
                        // No option here
                    }
                }
                attribute.options.remove(TEMPLATE_ATTRIBUTE_OPTIONS_TEMP)
            }

            // Add position for each attribute
            newFields[it.position] = Field(buildTemplateEntryField(attribute))
        }
        // Add custom fields to entry
        newFields.forEach { field ->
            field?.let {
                entryCopy.putField(field)
            }
        }
        // Add colors
        entryCopy.foregroundColor = templateEntry.foregroundColor
        entryCopy.backgroundColor = templateEntry.backgroundColor

        return entryCopy
    }

    override fun encodeTemplateEntry(templateEntry: EntryKDBX): EntryKDBX {
        val entryCopy = EntryKDBX().apply {
            updateWith(templateEntry)
        }
        // Add template version
        entryCopy.putField(TEMPLATE_LABEL_VERSION, ProtectedString(false, "1"))
        // Dynamic attributes
        var index = 0
        templateEntry.doForEachDecodedCustomField { field ->
            val label = removeTemplateDecorator(encodeTemplateAttribute(field.name))
            val value = field.protectedValue
            when {
                label.equals(TEMPLATE_LABEL_VERSION, true) -> {
                    // Keep template version as is
                }
                else -> {
                    entryCopy.removeField(field.name)
                    val options = TemplateAttributeOption.getOptionsFromString(value.stringValue)

                    // Add Position attribute
                    entryCopy.putField(
                        TEMPLATE_ATTRIBUTE_POSITION_PREFIX + label,
                        ProtectedString(false, index.toString())
                    )
                    // Add Title attribute (or alias if defined)
                    val title = options.alias ?: label
                    entryCopy.putField(
                        TEMPLATE_ATTRIBUTE_TITLE_PREFIX + label,
                        ProtectedString(false, title)
                    )
                    // Add Type attribute
                    var typeString: String = when {
                        value.stringValue.contains(TemplateAttributeType.TEXT.typeString, true) -> {
                            when (options.getNumberLines()) {
                                1 -> TEMPLATE_ATTRIBUTE_TYPE_INLINE
                                else -> TEMPLATE_ATTRIBUTE_TYPE_MULTILINE
                            }
                        }
                        value.stringValue.contains(TemplateAttributeType.LIST.typeString, true) -> {
                            TEMPLATE_ATTRIBUTE_TYPE_LISTBOX

                        }
                        value.stringValue.contains(TemplateAttributeType.DATETIME.typeString, true) -> {
                            when (options.getDateFormat()) {
                                DateInstant.Type.DATE -> TEMPLATE_ATTRIBUTE_TYPE_DATE
                                DateInstant.Type.TIME -> TEMPLATE_ATTRIBUTE_TYPE_TIME
                                else -> TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME
                            }
                        }
                        value.stringValue.contains(TemplateAttributeType.DIVIDER.typeString, true) -> {
                            TEMPLATE_ATTRIBUTE_TYPE_DIVIDER
                        }
                        else -> TEMPLATE_ATTRIBUTE_TYPE_INLINE
                    }
                    // Add protected string if needed
                    if (value.isProtected) {
                        typeString = "$TEMPLATE_ATTRIBUTE_TYPE_PROTECTED $typeString"
                    }
                    entryCopy.putField(
                        TEMPLATE_ATTRIBUTE_TYPE_PREFIX + label,
                        ProtectedString(false, typeString)
                    )
                    // Add Options attribute
                    // Here only number of chars, lines and list items are supported
                    var defaultOption = ""
                    try {
                        val listItems = options.getListItems()
                        if (listItems.isNotEmpty()) {
                            defaultOption = listItems.joinToString(",")
                        } else {
                            val numberChars = options.getNumberChars()
                            if (numberChars > 1) {
                                defaultOption = numberChars.toString()
                            } else {
                                val numberLines = options.getNumberLines()
                                if (numberLines > 1) {
                                    defaultOption = numberLines.toString()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore, default option is defined
                    }
                    entryCopy.putField(
                        TEMPLATE_ATTRIBUTE_OPTIONS_PREFIX + label,
                        ProtectedString(false, defaultOption)
                    )
                    // Add default elements
                    if (options.default.isNotEmpty()) {
                        entryCopy.putField(
                            label,
                            ProtectedString(value.isProtected, options.default)
                        )
                    }
                    index++
                }
            }
        }
        // Add colors
        entryCopy.foregroundColor = templateEntry.foregroundColor
        entryCopy.backgroundColor = templateEntry.backgroundColor
        return entryCopy
    }

    private data class TemplateAttributePosition(var position: Int, var attribute: TemplateAttribute)

    companion object {

        private val TAG = TemplateEngineCompatible::class.java.name

        // Custom template ref
        private const val TEMPLATE_ATTRIBUTE_TITLE = "Title"
        private const val TEMPLATE_ATTRIBUTE_USERNAME = "UserName"
        private const val TEMPLATE_ATTRIBUTE_PASSWORD = "Password"
        private const val TEMPLATE_ATTRIBUTE_URL = "URL"
        private const val TEMPLATE_ATTRIBUTE_EXP_DATE = "@exp_date"
        private const val TEMPLATE_ATTRIBUTE_EXPIRES = "Expires"
        private const val TEMPLATE_ATTRIBUTE_NOTES = "Notes"

        private const val TEMPLATE_LABEL_VERSION = "_etm_template"
        private const val TEMPLATE_ENTRY_UUID = "_etm_template_uuid"
        private const val TEMPLATE_ATTRIBUTE_POSITION_PREFIX = "_etm_position_"
        private const val TEMPLATE_ATTRIBUTE_TITLE_PREFIX = "_etm_title_"
        private const val TEMPLATE_ATTRIBUTE_TYPE_PREFIX = "_etm_type_"
        private const val TEMPLATE_ATTRIBUTE_OPTIONS_PREFIX = "_etm_options_"
        private const val TEMPLATE_ATTRIBUTE_OPTIONS_TEMP = "temp_option"
        private const val TEMPLATE_ATTRIBUTE_TYPE_PROTECTED = "Protected"
        private const val TEMPLATE_ATTRIBUTE_TYPE_INLINE = "Inline"
        private const val TEMPLATE_ATTRIBUTE_TYPE_INLINE_URL = "Inline URL"
        private const val TEMPLATE_ATTRIBUTE_TYPE_MULTILINE = "Multiline"
        private const val TEMPLATE_ATTRIBUTE_TYPE_LISTBOX = "Listbox"
        private const val TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME = "Date Time"
        private const val TEMPLATE_ATTRIBUTE_TYPE_DATE = "Date"
        private const val TEMPLATE_ATTRIBUTE_TYPE_TIME = "Time"
        private const val TEMPLATE_ATTRIBUTE_TYPE_DIVIDER = "Divider"
        private const val TEMPLATE_ATTRIBUTE_TYPE_POPOUT = "Popout"
        private const val TEMPLATE_ATTRIBUTE_TYPE_RICH_TEXTBOX = "Rich Textbox"

        private fun decodeTemplateAttribute(name: String): String {
            return when {
                TEMPLATE_LABEL_VERSION.equals(name, true) -> TemplateField.LABEL_VERSION
                TEMPLATE_ATTRIBUTE_TITLE.equals(name, true) -> TemplateField.LABEL_TITLE
                TEMPLATE_ATTRIBUTE_USERNAME.equals(name, true) -> TemplateField.LABEL_USERNAME
                TEMPLATE_ATTRIBUTE_PASSWORD.equals(name, true) -> TemplateField.LABEL_PASSWORD
                TEMPLATE_ATTRIBUTE_URL.equals(name, true) -> TemplateField.LABEL_URL
                TEMPLATE_ATTRIBUTE_EXP_DATE.equals(name, true) -> TemplateField.LABEL_EXPIRATION
                TEMPLATE_ATTRIBUTE_EXPIRES.equals(name, true) -> TemplateField.LABEL_EXPIRATION
                TEMPLATE_ATTRIBUTE_NOTES.equals(name, true) -> TemplateField.LABEL_NOTES
                else -> name
            }
        }

        private fun encodeTemplateAttribute(name: String): String {
            return when {
                TemplateField.LABEL_VERSION.equals(name, true) -> TEMPLATE_LABEL_VERSION
                TemplateField.LABEL_TITLE.equals(name, true) -> TEMPLATE_ATTRIBUTE_TITLE
                TemplateField.LABEL_USERNAME.equals(name, true) -> TEMPLATE_ATTRIBUTE_USERNAME
                TemplateField.LABEL_PASSWORD.equals(name, true) -> TEMPLATE_ATTRIBUTE_PASSWORD
                TemplateField.LABEL_URL.equals(name, true) -> TEMPLATE_ATTRIBUTE_URL
                TemplateField.LABEL_EXPIRATION.equals(name, true) -> TEMPLATE_ATTRIBUTE_EXP_DATE
                TemplateField.LABEL_NOTES.equals(name, true) -> TEMPLATE_ATTRIBUTE_NOTES
                else -> name
            }
        }
    }
}