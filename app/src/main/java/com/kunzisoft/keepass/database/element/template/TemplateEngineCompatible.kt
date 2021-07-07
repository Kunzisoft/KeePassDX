package com.kunzisoft.keepass.database.element.template

import android.util.Log
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.utils.UuidUtil

class TemplateEngineCompatible(database: DatabaseKDBX): TemplateEngine(database) {

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
            && template != Template.CREATION) {
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

    override fun decodeTemplateEntry(templateEntry: EntryKDBX): EntryKDBX {
        val attributes = HashMap<String, TemplateAttributePosition>()
        val entryCopy = EntryKDBX().apply {
            updateWith(templateEntry)
        }
        // Remove template version
        entryCopy.getFieldValue(TEMPLATE_LABEL_VERSION)
        try {
            // value.toIntOrNull()
            // TODO template decoder version
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
                            attribute.attribute.options[TemplateAttributeOption.ALIAS_ATTR] = value
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
                                attribute.attribute.options[TemplateAttributeOption.TEXT_LINK_ATTR] =
                                    true.toString()
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_INLINE, true) ||
                                    value.contains(TEMPLATE_ATTRIBUTE_TYPE_POPOUT, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TEXT
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_MULTILINE, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TEXT
                                attribute.attribute.options[TemplateAttributeOption.TEXT_NUMBER_LINES_ATTR] =
                                    TemplateAttributeOption.TEXT_NUMBER_LINES_VALUE_MANY
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_RICH_TEXTBOX, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TEXT
                                attribute.attribute.options[TemplateAttributeOption.TEXT_NUMBER_LINES_ATTR] =
                                    TemplateAttributeOption.TEXT_NUMBER_LINES_VALUE_MANY
                                attribute.attribute.options[TemplateAttributeOption.TEXT_LINK_ATTR] =
                                    true.toString()
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_LISTBOX, true) -> {
                                attribute.attribute.type = TemplateAttributeType.LIST
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATETIME
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_DATE, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATETIME
                                attribute.attribute.options[TemplateAttributeOption.DATETIME_FORMAT_ATTR] =
                                    TemplateAttributeOption.DATETIME_FORMAT_VALUE_DATE
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_TIME, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATETIME
                                attribute.attribute.options[TemplateAttributeOption.DATETIME_FORMAT_ATTR] =
                                    TemplateAttributeOption.DATETIME_FORMAT_VALUE_TIME
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
                            attribute.attribute.options[TEMPLATE_ATTRIBUTE_OPTIONS_TEMP] = value
                        }
                        entryCopy.removeField(field.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template options", e)
                    }
                }
                else -> {
                    // To retrieve default values
                    if (attributes.containsKey(label)) {
                        if (value.isNotEmpty()) {
                            val attribute = attributes[label]!!
                            attribute.attribute.options[TemplateAttributeOption.DEFAULT_ATTR] =
                                value
                        }
                    }
                    entryCopy.removeField(label)
                }
            }
        }

        val newFields = arrayOfNulls<Field>(attributes.size)
        attributes.values.forEach {

            val attribute = it.attribute

            // Recognize each temp option
            attribute.options[TEMPLATE_ATTRIBUTE_OPTIONS_TEMP]?.let { defaultOption ->
                when (attribute.type) {
                    TemplateAttributeType.TEXT -> {
                        try {
                            val linesString = attribute.options[TemplateAttributeOption.TEXT_NUMBER_LINES_ATTR]
                            if (linesString == null || linesString == "1") {
                                // If one line, default attribute option is number of chars
                                attribute.options[TemplateAttributeOption.TEXT_NUMBER_CHARS_ATTR] =
                                    defaultOption
                            } else {
                                // else it's number of lines
                                attribute.options[TemplateAttributeOption.TEXT_NUMBER_LINES_ATTR] =
                                    defaultOption
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Unable to transform default text option", e)
                        }
                    }
                    TemplateAttributeType.LIST -> {
                        try {
                            // Default attribute option is items of the list
                            val items = defaultOption.split(",")
                            attribute.options[TemplateAttributeOption.LIST_ITEMS] =
                                TemplateAttributeOption.stringFromListItems(items)
                            // TODO Add default item
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
                                    attribute.options[TemplateAttributeOption.DATETIME_FORMAT_ATTR] =
                                        TemplateAttributeOption.DATETIME_FORMAT_VALUE_DATE
                                }
                                defaultOption.equals(TEMPLATE_ATTRIBUTE_TYPE_TIME, true) -> {
                                    attribute.options[TemplateAttributeOption.DATETIME_FORMAT_ATTR] =
                                        TemplateAttributeOption.DATETIME_FORMAT_VALUE_TIME
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
            val label = encodeTemplateAttribute(
                field.name
                    .removePrefix(PREFIX_DECODED_TEMPLATE)
                    .removeSuffix(SUFFIX_DECODED_TEMPLATE)
            )
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
                    val title = options[TemplateAttributeOption.ALIAS_ATTR] ?: label
                    entryCopy.putField(
                        TEMPLATE_ATTRIBUTE_TITLE_PREFIX + label,
                        ProtectedString(false, title)
                    )
                    // Add Type attribute
                    var typeString: String = when {
                        value.stringValue.contains(TemplateAttributeType.TEXT.typeString, true) -> {
                            when (options[TemplateAttributeOption.TEXT_NUMBER_LINES_ATTR]) {
                                TemplateAttributeOption.TEXT_NUMBER_LINES_VALUE_MANY -> TEMPLATE_ATTRIBUTE_TYPE_MULTILINE
                                else -> TEMPLATE_ATTRIBUTE_TYPE_INLINE
                            }
                        }
                        value.stringValue.contains(TemplateAttributeType.LIST.typeString, true) -> {
                            TEMPLATE_ATTRIBUTE_TYPE_LISTBOX
                        }
                        value.stringValue.contains(TemplateAttributeType.DATETIME.typeString, true) -> {
                            when (options[TemplateAttributeOption.DATETIME_FORMAT_ATTR]) {
                                TemplateAttributeOption.DATETIME_FORMAT_VALUE_DATE -> TEMPLATE_ATTRIBUTE_TYPE_DATE
                                TemplateAttributeOption.DATETIME_FORMAT_VALUE_TIME -> TEMPLATE_ATTRIBUTE_TYPE_TIME
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
                    // Add Options attribute (here only number of chars and lines are supported)
                    var defaultOption = ""
                    try {
                        options[TemplateAttributeOption.TEXT_NUMBER_CHARS_ATTR]?.let { numberChars ->
                            defaultOption = if (numberChars.toInt() > 1) numberChars else defaultOption
                        }
                        options[TemplateAttributeOption.TEXT_NUMBER_LINES_ATTR]?.let { numberLines ->
                            defaultOption = if (numberLines.toInt() > 1) numberLines else defaultOption
                        }
                    } catch (e: Exception) {
                        // Ignore, can be "many"
                    }
                    entryCopy.putField(
                        TEMPLATE_ATTRIBUTE_OPTIONS_PREFIX + label,
                        ProtectedString(false, defaultOption)
                    )
                    // Add default elements
                    options[TemplateAttributeOption.DEFAULT_ATTR]?.let { defaultValue ->
                        entryCopy.putField(
                            label,
                            ProtectedString(value.isProtected, defaultValue)
                        )
                    }
                    index++
                }
            }
        }
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

        fun decodeTemplateAttribute(name: String): String {
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

        fun encodeTemplateAttribute(name: String): String {
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