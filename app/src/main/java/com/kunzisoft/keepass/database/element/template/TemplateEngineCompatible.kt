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
                TemplateAttribute("", TemplateAttributeType.TEXT)
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
                label.startsWith(TEMPLATE_ATTRIBUTE_POSITION_PREFIX) -> {
                    try {
                        val attributeName = label.substring(TEMPLATE_ATTRIBUTE_POSITION_PREFIX.length)
                        val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                        attribute.position = value.toInt()
                        entryCopy.removeField(field.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template position", e)
                    }
                }
                label.startsWith(TEMPLATE_ATTRIBUTE_TITLE_PREFIX) -> {
                    try {
                        val attributeName = label.substring(TEMPLATE_ATTRIBUTE_TITLE_PREFIX.length)
                        val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                        attribute.attribute.label = value
                        entryCopy.removeField(field.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template title", e)
                    }
                }
                label.startsWith(TEMPLATE_ATTRIBUTE_TYPE_PREFIX) -> {
                    try {
                        val attributeName = label.substring(TEMPLATE_ATTRIBUTE_TYPE_PREFIX.length)
                        val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                        if (value.contains(TEMPLATE_ATTRIBUTE_TYPE_PROTECTED, true)) {
                            attribute.attribute.protected = true
                        }
                        when {
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_INLINE, true) ||
                                    value.contains(TEMPLATE_ATTRIBUTE_TYPE_POPOUT, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TEXT
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_MULTILINE, true) ||
                                    value.contains(TEMPLATE_ATTRIBUTE_TYPE_RICH_TEXTBOX, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TEXT
                                attribute.attribute.options[TemplateAttributeOption.NUMBER_LINES] =
                                    TemplateAttributeOption.NUMBER_LINES_MANY
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATETIME
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_DATE, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATETIME
                                attribute.attribute.options[TemplateAttributeOption.DATETIME_FORMAT] =
                                    TemplateAttributeOption.DATETIME_FORMAT_DATE
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_TIME, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATETIME
                                attribute.attribute.options[TemplateAttributeOption.DATETIME_FORMAT] =
                                    TemplateAttributeOption.DATETIME_FORMAT_TIME
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_LISTBOX, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TEXT
                                // TODO List box
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
            }
        }

        val newFields = arrayOfNulls<Field>(attributes.size)
        attributes.values.forEach {
            newFields[it.position] = Field(buildTemplateEntryField(it.attribute))
        }
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
                    entryCopy.putField(
                        TEMPLATE_ATTRIBUTE_POSITION_PREFIX +'_'+label,
                        ProtectedString(false, index.toString())
                    )
                    entryCopy.putField(
                        TEMPLATE_ATTRIBUTE_TITLE_PREFIX +'_'+label,
                        ProtectedString(false, label)
                    )
                    // Add protected string if needed
                    var typeString: String = when {
                        value.stringValue.contains(TemplateAttributeType.TEXT.label, true) -> {
                            when (TemplateAttributeOption.getOptionsFromString(field.protectedValue.stringValue)[TemplateAttributeOption.NUMBER_LINES]) {
                                TemplateAttributeOption.NUMBER_LINES_MANY -> TEMPLATE_ATTRIBUTE_TYPE_MULTILINE
                                else -> TEMPLATE_ATTRIBUTE_TYPE_INLINE
                            }
                        }
                        value.stringValue.contains(TemplateAttributeType.DATETIME.label, true) -> {
                            when (TemplateAttributeOption.getOptionsFromString(field.protectedValue.stringValue)[TemplateAttributeOption.DATETIME_FORMAT]) {
                                TemplateAttributeOption.DATETIME_FORMAT_DATE -> TEMPLATE_ATTRIBUTE_TYPE_DATE
                                TemplateAttributeOption.DATETIME_FORMAT_TIME -> TEMPLATE_ATTRIBUTE_TYPE_TIME
                                else -> TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME
                            }
                        }
                        value.stringValue.contains(TemplateAttributeType.DIVIDER.label, true) -> {
                            TEMPLATE_ATTRIBUTE_TYPE_DIVIDER
                        }
                        else -> TEMPLATE_ATTRIBUTE_TYPE_INLINE
                    }

                    if (value.isProtected) {
                        typeString = "$TEMPLATE_ATTRIBUTE_TYPE_PROTECTED $typeString"
                    }
                    entryCopy.putField(
                        TEMPLATE_ATTRIBUTE_TYPE_PREFIX +'_'+label,
                        ProtectedString(false, typeString)
                    )
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
        private const val TEMPLATE_ATTRIBUTE_TITLE = "@title"
        private const val TEMPLATE_ATTRIBUTE_USERNAME = "@username"
        private const val TEMPLATE_ATTRIBUTE_PASSWORD = "@password"
        private const val TEMPLATE_ATTRIBUTE_URL = "@url"
        private const val TEMPLATE_ATTRIBUTE_EXP_DATE = "@exp_date"
        private const val TEMPLATE_ATTRIBUTE_EXPIRES = "@expires"
        private const val TEMPLATE_ATTRIBUTE_NOTES = "@notes"

        private const val TEMPLATE_LABEL_VERSION = "_etm_template"
        private const val TEMPLATE_ENTRY_UUID = "_etm_template_uuid"
        private const val TEMPLATE_ATTRIBUTE_POSITION_PREFIX = "_etm_position"
        private const val TEMPLATE_ATTRIBUTE_TITLE_PREFIX = "_etm_title"
        private const val TEMPLATE_ATTRIBUTE_TYPE_PREFIX = "_etm_type"
        private const val TEMPLATE_ATTRIBUTE_TYPE_PROTECTED = "Protected"
        private const val TEMPLATE_ATTRIBUTE_TYPE_INLINE = "Inline"
        private const val TEMPLATE_ATTRIBUTE_TYPE_MULTILINE = "Multiline"
        private const val TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME = "Date Time"
        private const val TEMPLATE_ATTRIBUTE_TYPE_DATE = "Date"
        private const val TEMPLATE_ATTRIBUTE_TYPE_TIME = "Time"
        private const val TEMPLATE_ATTRIBUTE_TYPE_DIVIDER = "Divider"
        private const val TEMPLATE_ATTRIBUTE_TYPE_LISTBOX = "Listbox"
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