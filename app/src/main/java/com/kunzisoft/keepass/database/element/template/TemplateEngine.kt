package com.kunzisoft.keepass.database.element.template

import android.content.res.Resources
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.utils.UuidUtil
import java.util.*
import kotlin.collections.HashMap

class TemplateEngine(private val mDatabase: DatabaseKDBX) {

    private val mCacheTemplates = HashMap<UUID, Template>()

    fun getTemplates(): List<Template> {
        val templates = mutableListOf<Template>()
        try {
            val templateGroup = mDatabase.getTemplatesGroup()
            mCacheTemplates.clear()
            if (templateGroup != null) {
                templates.add(Template.STANDARD)
                templateGroup.getChildEntries().forEach { templateEntry ->
                    getTemplateFromTemplateEntry(templateEntry)?.let {
                        mCacheTemplates[templateEntry.id] = it
                    }
                }
                templates.addAll(mCacheTemplates.values)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to get templates from group", e)
        }
        return templates
    }

    fun getTemplateCreation(): Template {
        return Template(Template.CREATION)
    }

    fun createNewTemplatesGroup(resources: Resources): GroupKDBX {
        return mDatabase.createGroup().apply {
            title = getDefaultTemplateGroupName(resources)
            icon.standard = mDatabase.getStandardIcon(IconImageStandard.FOLDER_ID)
            enableAutoType = false
            enableSearching = false
            isExpanded = false
        }
    }

    fun clearCache() {
        mCacheTemplates.clear()
    }

    private fun getTemplateByCache(uuid: UUID): Template? {
        try {
            if (mCacheTemplates.containsKey(uuid))
                return mCacheTemplates[uuid]
            else {
                mDatabase.getEntryById(uuid)?.let { templateEntry ->
                    getTemplateFromTemplateEntry(templateEntry)?.let { newTemplate ->
                        mCacheTemplates[uuid] = newTemplate
                        return newTemplate
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to get Entry by $uuid", e)
        }
        return null
    }

    fun getTemplate(entryKDBX: EntryKDBX): Template? {
        UuidUtil.fromHexString(entryKDBX.getCustomFieldValue(TEMPLATE_ENTRY_UUID))?.let { templateUUID ->
            return getTemplateByCache(templateUUID)
        }
        return null
    }

    private fun getOrRetrieveAttributeFromName(attributes: HashMap<String, TemplateAttributePosition>, name: String): TemplateAttributePosition {
        return if (attributes.containsKey(name)) {
            attributes[name]!!
        } else {
            val newAttribute = TemplateAttributePosition(-1, TemplateAttribute("", TemplateAttributeType.INLINE))
            attributes[name] = newAttribute
            newAttribute
        }
    }

    fun decodeTemplateEntry(templateEntry: EntryKDBX): EntryKDBX {
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
                                attribute.attribute.type = TemplateAttributeType.INLINE
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_MULTILINE, true) ||
                                    value.contains(TEMPLATE_ATTRIBUTE_TYPE_RICH_TEXTBOX, true) -> {
                                attribute.attribute.type = TemplateAttributeType.MULTILINE
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATETIME
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_DATE, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATE
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_TIME, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TIME
                            }
                            value.contains(TEMPLATE_ATTRIBUTE_TYPE_LISTBOX, true) -> {
                                // TODO List box
                                attribute.attribute.type = TemplateAttributeType.INLINE
                            }
                        }
                        attribute.attribute.defaultValue = field.protectedValue.stringValue
                        entryCopy.removeField(field.name)
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template type", e)
                    }
                }
            }
        }

        val newFields = arrayOfNulls<Field>(attributes.size)
        attributes.values.forEach {
            // PREFIX_DECODED_TEMPLATE to fix same label as standard fields
            newFields[it.position] = Field(PREFIX_DECODED_TEMPLATE + it.attribute.label + SUFFIX_DECODED_TEMPLATE,
                    ProtectedString(false, it.attribute.defaultValue))
        }
        newFields.forEach { field ->
            field?.let {
                entryCopy.putField(field)
            }
        }

        return entryCopy
    }

    fun encodeTemplateEntry(templateEntry: EntryKDBX): EntryKDBX {
        val entryCopy = EntryKDBX().apply {
            updateWith(templateEntry)
        }
        // Add template version
        entryCopy.putField(TEMPLATE_LABEL_VERSION, ProtectedString(false, "1"))
        // Dynamic attributes
        var index = 0
        templateEntry.doForEachDecodedCustomField { field ->
            val label = field.name.removePrefix(PREFIX_DECODED_TEMPLATE).removeSuffix(SUFFIX_DECODED_TEMPLATE)
            val value = field.protectedValue
            when {
                label.equals(TEMPLATE_LABEL_VERSION, true) -> {
                    // Keep template version as is
                }
                else -> {
                    entryCopy.removeField(field.name)
                    entryCopy.putField(TEMPLATE_ATTRIBUTE_POSITION_PREFIX+'_'+label,
                            ProtectedString(false, index.toString()))
                    entryCopy.putField(TEMPLATE_ATTRIBUTE_TITLE_PREFIX+'_'+label,
                            ProtectedString(false, label))
                    // Add protected string if needed
                    var typeString = value.stringValue
                    if (value.isProtected
                            && !typeString.contains(TEMPLATE_ATTRIBUTE_TYPE_PROTECTED)) {
                        typeString = "$TEMPLATE_ATTRIBUTE_TYPE_PROTECTED $typeString"
                    }
                    entryCopy.putField(TEMPLATE_ATTRIBUTE_TYPE_PREFIX+'_'+label,
                            ProtectedString(false, typeString))
                    index++
                }
            }
        }
        return entryCopy
    }

    private fun getTemplateUUIDField(template: Template): Field? {
        UuidUtil.toHexString(template.uuid)?.let { uuidString ->
            return Field(TEMPLATE_ENTRY_UUID,
                    ProtectedString(false, uuidString))
        }
        return null
    }

    fun removeMetaTemplateRecognitionFromEntry(entry: EntryKDBX): EntryKDBX {
        val entryCopy = EntryKDBX().apply {
            updateWith(entry)
        }
        entryCopy.removeField(TEMPLATE_ENTRY_UUID)
        return entryCopy
    }

    fun addMetaTemplateRecognitionToEntry(template: Template, entry: EntryKDBX): EntryKDBX {
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

    fun createTemplateEntry(template: Template): EntryKDBX {
        val newEntry = EntryKDBX().apply {
            nodeId = NodeIdUUID(template.uuid)
            title = template.title
            icon = template.icon
            template.sections.forEachIndexed { index, section ->
                section.attributes.forEach { attribute ->
                    if (index > 0) {
                        // Label is not important with section => [Section_X]: Divider
                        putField("$PREFIX_DECODED_TEMPLATE$SECTION_DECODED_TEMPLATE${index-1}$SUFFIX_DECODED_TEMPLATE",
                            ProtectedString(false, TEMPLATE_ATTRIBUTE_TYPE_DIVIDER))
                    }

                    putField(buildFieldFromTemplateAttribute(attribute))
                }
            }
        }
        return encodeTemplateEntry(newEntry)
    }

    private fun getTemplateFromTemplateEntry(templateEntry: EntryKDBX): Template? {

        val templateEntryDecoded = decodeTemplateEntry(templateEntry)

        val templateSections = mutableListOf<TemplateSection>()
        val sectionFields = mutableListOf<Field>()
        templateEntryDecoded.doForEachDecodedCustomField { field ->

            if (field.name.contains(SECTION_DECODED_TEMPLATE)) {

                val sectionAttributes = mutableListOf<TemplateAttribute>()
                sectionFields.forEach {
                    sectionAttributes.add(buildTemplateAttributeFromField(it))
                }
                templateSections.add(TemplateSection(sectionAttributes))
                sectionFields.clear()
            } else {
                sectionFields.add(field)
            }
        }

        val sectionAttributes = mutableListOf<TemplateAttribute>()
        sectionFields.forEach {
            sectionAttributes.add(buildTemplateAttributeFromField(it))
        }
        templateSections.add(TemplateSection(sectionAttributes))

        // TODO Add decoder version
        return Template(templateEntry.id, templateEntry.title, templateEntry.icon, templateSections, 1)
    }

    private fun buildTemplateAttributeFromField(field: Field): TemplateAttribute {
        val type = when {
            field.protectedValue.stringValue.equals(TEMPLATE_ATTRIBUTE_TYPE_INLINE, true) -> TemplateAttributeType.INLINE
            field.protectedValue.stringValue.equals(TEMPLATE_ATTRIBUTE_TYPE_MULTILINE, true) -> TemplateAttributeType.MULTILINE
            field.protectedValue.stringValue.equals(TEMPLATE_ATTRIBUTE_TYPE_DATE, true) -> TemplateAttributeType.DATE
            field.protectedValue.stringValue.equals(TEMPLATE_ATTRIBUTE_TYPE_TIME, true) -> TemplateAttributeType.TIME
            field.protectedValue.stringValue.equals(TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME, true) -> TemplateAttributeType.DATETIME
            else -> TemplateAttributeType.INLINE
        }
        return TemplateAttribute(
            field.name.removePrefix(PREFIX_DECODED_TEMPLATE).removeSuffix(SUFFIX_DECODED_TEMPLATE),
            type,
            field.protectedValue.stringValue.contains(TEMPLATE_ATTRIBUTE_TYPE_PROTECTED, true)
        )
    }

    private fun buildFieldFromTemplateAttribute(attribute: TemplateAttribute): Field {
        var fieldValue = if (attribute.protected) "$TEMPLATE_ATTRIBUTE_TYPE_PROTECTED " else ""
        fieldValue += when (attribute.type) {
            TemplateAttributeType.INLINE -> TEMPLATE_ATTRIBUTE_TYPE_INLINE
            TemplateAttributeType.SMALL_MULTILINE -> TEMPLATE_ATTRIBUTE_TYPE_INLINE
            TemplateAttributeType.MULTILINE -> TEMPLATE_ATTRIBUTE_TYPE_MULTILINE
            TemplateAttributeType.DATE -> TEMPLATE_ATTRIBUTE_TYPE_DATE
            TemplateAttributeType.TIME -> TEMPLATE_ATTRIBUTE_TYPE_TIME
            TemplateAttributeType.DATETIME -> TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME
        }
        return Field("$PREFIX_DECODED_TEMPLATE${attribute.label}$SUFFIX_DECODED_TEMPLATE",
            ProtectedString(false, fieldValue))
    }

    companion object {
        private data class TemplateAttributePosition(var position: Int, var attribute: TemplateAttribute)

        private val TAG = TemplateEngine::class.java.name

        private const val PREFIX_DECODED_TEMPLATE = "["
        private const val SUFFIX_DECODED_TEMPLATE = "]"
        private const val SECTION_DECODED_TEMPLATE = "Divider_"

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

        fun getDefaultTemplateGroupName(resources: Resources): String {
            return resources.getString(R.string.templates)
        }

        fun isTemplateNameAttribute(name: String): Boolean {
            return name.startsWith(PREFIX_DECODED_TEMPLATE)
                    && name.endsWith(SUFFIX_DECODED_TEMPLATE)
        }

        // TODO template attribute
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
        
        fun getDefaults(): List<Template> {
            val templateBuilder = TemplateBuilder()
            return listOf(
                templateBuilder.email,
                templateBuilder.wifi,
                templateBuilder.notes,
                templateBuilder.idCard,
                templateBuilder.creditCard,
                templateBuilder.bank,
                templateBuilder.cryptocurrency)
        }
    }
}