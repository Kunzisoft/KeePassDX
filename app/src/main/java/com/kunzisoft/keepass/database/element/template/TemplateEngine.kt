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

    private fun conditionCustomFields(attributes: HashMap<String, TemplateAttributePosition>,
                                      field: Field,
                                      actionForEachAttributePrefix: ((TemplateAttributePosition) -> Unit)?,
                                      retrieveTemplateVersion: (Int?) -> Unit) {
        val label = field.name
        val value = field.protectedValue.stringValue
        when {
            label.equals(TEMPLATE_LABEL_VERSION, true) -> {
                try {
                    retrieveTemplateVersion.invoke(value.toIntOrNull())
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to retrieve template version", e)
                }
            }
            label.startsWith(TEMPLATE_ATTRIBUTE_POSITION_PREFIX) -> {
                try {
                    val attributeName = label.substring(TEMPLATE_ATTRIBUTE_POSITION_PREFIX.length)
                    val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                    attribute.position = value.toInt()
                    actionForEachAttributePrefix?.invoke(attribute)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to retrieve template position", e)
                }
            }
            label.startsWith(TEMPLATE_ATTRIBUTE_TITLE_PREFIX) -> {
                try {
                    val attributeName = label.substring(TEMPLATE_ATTRIBUTE_TITLE_PREFIX.length)
                    val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                    attribute.attribute.label = value
                    actionForEachAttributePrefix?.invoke(attribute)
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
                    actionForEachAttributePrefix?.invoke(attribute)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to retrieve template type", e)
                }
            }
            // TODO section
        }
    }

    private fun getTemplateFromTemplateEntry(templateEntry: EntryKDBX): Template? {
        var templateVersion: Int? = null
        val attributes = HashMap<String, TemplateAttributePosition>()
        templateEntry.doForEachDecodedCustomField { field ->
            conditionCustomFields(attributes, field, null) {
                templateVersion = it
            }
        }

        return templateVersion?.let { version ->
            val templateAttributes = arrayOfNulls<TemplateAttribute>(attributes.size)
            attributes.values.forEach {
                templateAttributes[it.position] = it.attribute
            }
            val templateSections = mutableListOf<TemplateSection>()
            val templateSection = TemplateSection(templateAttributes.filterNotNull())
            templateSections.add(templateSection)
            Template(templateEntry.id, templateEntry.title, templateEntry.icon, templateSections, version)
        }
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
        templateEntry.doForEachDecodedCustomField { field ->
            conditionCustomFields(attributes, field, {
                if (field.name.startsWith(TEMPLATE_ATTRIBUTE_TYPE_PREFIX)) {
                    it.attribute.defaultValue = field.protectedValue.stringValue
                }
                entryCopy.removeField(field)
            }, { })
        }

        val newFields = arrayOfNulls<Field>(attributes.size)
        attributes.values.forEach {
            newFields[it.position] = Field(it.attribute.label, ProtectedString(false, it.attribute.defaultValue))
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
        var index = 0
        templateEntry.doForEachDecodedCustomField { field ->
            val label = field.name
            val value = field.protectedValue
            when {
                label.equals(TEMPLATE_LABEL_VERSION, true) -> {
                    // Keep template version as is
                }
                else -> {
                    entryCopy.removeField(field)
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

    companion object {
        private data class TemplateAttributePosition(var position: Int, var attribute: TemplateAttribute)

        private val TAG = TemplateEngine::class.java.name
        const val TEMPLATE_LABEL_VERSION = "_etm_template"
        const val TEMPLATE_ENTRY_UUID = "_etm_template_uuid"
        private const val TEMPLATE_ATTRIBUTE_POSITION_PREFIX = "_etm_position"
        private const val TEMPLATE_ATTRIBUTE_TITLE_PREFIX = "_etm_title"
        private const val TEMPLATE_ATTRIBUTE_TYPE_PREFIX = "_etm_type"
        const val TEMPLATE_ATTRIBUTE_TITLE_EXPIRATION = "@exp_date"
        private const val TEMPLATE_ATTRIBUTE_TYPE_PROTECTED = "Protected"
        private const val TEMPLATE_ATTRIBUTE_TYPE_INLINE = "Inline"
        private const val TEMPLATE_ATTRIBUTE_TYPE_MULTILINE = "Multiline"
        private const val TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME = "Date Time"
        private const val TEMPLATE_ATTRIBUTE_TYPE_DATE = "Date"
        private const val TEMPLATE_ATTRIBUTE_TYPE_TIME = "Time"
        private const val TEMPLATE_ATTRIBUTE_TYPE_LISTBOX = "Listbox"
        private const val TEMPLATE_ATTRIBUTE_TYPE_POPOUT = "Popout"
        private const val TEMPLATE_ATTRIBUTE_TYPE_RICH_TEXTBOX = "Rich Textbox"

        fun getDefaultTemplateGroupName(resources: Resources): String {
            return resources.getString(R.string.templates)
        }
    }
}