package com.kunzisoft.keepass.database.element.template

import android.content.res.Resources
import android.util.Log
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.utils.UuidUtil
import java.util.*
import kotlin.collections.HashMap

class TemplateEngine(private val databaseKDBX: DatabaseKDBX) {

    private val mCacheTemplates = HashMap<UUID, Template>()

    fun getTemplates(): List<Template> {
        val templates = mutableListOf<Template>()
        try {
            val templateGroup = databaseKDBX.getTemplatesGroup()
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

    fun createNewTemplatesGroup(resources: Resources): GroupKDBX {
        return databaseKDBX.createGroup().apply {
            title = getDefaultTemplateGroupName(resources)
            icon.standard = databaseKDBX.getStandardIcon(IconImageStandard.FOLDER_ID)
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
                databaseKDBX.getEntryById(uuid)?.let { templateEntry ->
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

    private fun getTemplateFromTemplateEntry(templateEntry: EntryKDBX): Template? {
        var templateVersion: Int? = null
        val attributes = HashMap<String, TemplateAttributePosition>()
        templateEntry.doForEachDecodedCustomField { key, value ->
            when {
                key.equals(TEMPLATE_LABEL_VERSION, true) -> {
                    try {
                        templateVersion = value.stringValue.toInt()
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template version", e)
                    }
                }
                key.startsWith(TEMPLATE_ATTRIBUTE_POSITION_PREFIX) -> {
                    try {
                        val attributeName = key.substring(TEMPLATE_ATTRIBUTE_POSITION_PREFIX.length)
                        val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                        attribute.position = value.stringValue.toInt()
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template position", e)
                    }
                }
                key.startsWith(TEMPLATE_ATTRIBUTE_TITLE_PREFIX) -> {
                    try {
                        val attributeName = key.substring(TEMPLATE_ATTRIBUTE_TITLE_PREFIX.length)
                        val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                        var referenceLabel = value.stringValue
                        if (referenceLabel.equals(TEMPLATE_ATTRIBUTE_TITLE_EXPIRATION, true)) {
                            referenceLabel = TemplateField.STANDARD_EXPIRATION
                        }
                        attribute.attribute.label = referenceLabel
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template position", e)
                    }
                }
                key.startsWith(TEMPLATE_ATTRIBUTE_TYPE_PREFIX) -> {
                    try {
                        val attributeName = key.substring(TEMPLATE_ATTRIBUTE_TYPE_PREFIX.length)
                        val attribute = getOrRetrieveAttributeFromName(attributes, attributeName)
                        val attributeValue = value.stringValue
                        if (attributeValue.contains(TEMPLATE_ATTRIBUTE_TYPE_PROTECTED, true)) {
                            attribute.attribute.protected = true
                        }
                        when {
                            attributeValue.contains(TEMPLATE_ATTRIBUTE_TYPE_INLINE, true) ||
                                    attributeValue.contains(TEMPLATE_ATTRIBUTE_TYPE_POPOUT, true) -> {
                                attribute.attribute.type = TemplateAttributeType.INLINE
                            }
                            attributeValue.contains(TEMPLATE_ATTRIBUTE_TYPE_MULTILINE, true) ||
                                    attributeValue.contains(TEMPLATE_ATTRIBUTE_TYPE_RICH_TEXTBOX, true) -> {
                                attribute.attribute.type = TemplateAttributeType.MULTILINE
                            }
                            attributeValue.contains(TEMPLATE_ATTRIBUTE_TYPE_DATE_TIME, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATETIME
                            }
                            attributeValue.contains(TEMPLATE_ATTRIBUTE_TYPE_DATE, true) -> {
                                attribute.attribute.type = TemplateAttributeType.DATE
                            }
                            attributeValue.contains(TEMPLATE_ATTRIBUTE_TYPE_TIME, true) -> {
                                attribute.attribute.type = TemplateAttributeType.TIME
                            }
                            attributeValue.contains(TEMPLATE_ATTRIBUTE_TYPE_LISTBOX, true) -> {
                                // TODO List box
                                attribute.attribute.type = TemplateAttributeType.INLINE
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to retrieve template position", e)
                    }
                }
                // TODO section
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

    companion object {
        private data class TemplateAttributePosition(var position: Int, var attribute: TemplateAttribute)

        private val TAG = TemplateEngine::class.java.name
        private const val TEMPLATE_LABEL_VERSION = "_etm_template"
        const val TEMPLATE_ENTRY_UUID = "_etm_template_uuid"
        private const val TEMPLATE_ATTRIBUTE_POSITION_PREFIX = "_etm_position"
        private const val TEMPLATE_ATTRIBUTE_TITLE_PREFIX = "_etm_title"
        private const val TEMPLATE_ATTRIBUTE_TITLE_EXPIRATION = "@exp_date"
        private const val TEMPLATE_ATTRIBUTE_TYPE_PREFIX = "_etm_type"
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