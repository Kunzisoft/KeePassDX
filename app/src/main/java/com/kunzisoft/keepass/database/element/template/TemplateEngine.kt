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
import java.util.*
import kotlin.collections.HashMap

abstract class TemplateEngine(private val mDatabase: DatabaseKDBX) {

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
                        templates.add(it)
                    }
                }
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

    protected fun getTemplateByCache(uuid: UUID): Template? {
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

    abstract fun getTemplate(entryKDBX: EntryKDBX): Template?

    abstract fun removeMetaTemplateRecognitionFromEntry(entry: EntryKDBX): EntryKDBX

    abstract fun addMetaTemplateRecognitionToEntry(template: Template, entry: EntryKDBX): EntryKDBX

    abstract fun decodeTemplateEntry(templateEntry: EntryKDBX): EntryKDBX

    abstract fun encodeTemplateEntry(templateEntry: EntryKDBX): EntryKDBX

    fun createTemplateEntry(template: Template): EntryKDBX {
        val newEntry = EntryKDBX().apply {
            nodeId = NodeIdUUID(template.uuid)
            title = template.title
            icon = template.icon
            template.sections.forEachIndexed { index, section ->
                section.attributes.forEach { attribute ->
                    if (index > 0) {
                        // Label is not important with section => [Section_X]: Divider
                        val sectionName = if (section.name.isEmpty())
                            "$SECTION_DECODED_TEMPLATE_PREFIX${index-1}"
                        else
                            section.name
                        putField("$PREFIX_DECODED_TEMPLATE$sectionName$SUFFIX_DECODED_TEMPLATE",
                            ProtectedString(false, TemplateAttributeType.DIVIDER.typeString))
                    }

                    putField(buildTemplateEntryField(attribute))
                }
            }
        }
        return encodeTemplateEntry(newEntry)
    }

    protected fun buildTemplateEntryField(attribute: TemplateAttribute): Field {
        val typeAndOptions = attribute.type.typeString +
                TemplateAttributeOption.getStringFromOptions(attribute.options)
        // PREFIX_DECODED_TEMPLATE to fix same label as standard fields
        return Field(PREFIX_DECODED_TEMPLATE
                + TemplateEngineCompatible.decodeTemplateAttribute(attribute.label)
                + SUFFIX_DECODED_TEMPLATE,
            ProtectedString(attribute.protected, typeAndOptions))
    }

    private fun buildTemplateSectionFromFields(fields: List<Field>): TemplateSection {
        val sectionAttributes = mutableListOf<TemplateAttribute>()
        fields.forEach { field ->
            sectionAttributes.add(TemplateAttribute(
                field.name.removePrefix(PREFIX_DECODED_TEMPLATE).removeSuffix(SUFFIX_DECODED_TEMPLATE),
                TemplateAttributeType.getFromString(field.protectedValue.stringValue),
                field.protectedValue.isProtected,
                TemplateAttributeOption.getOptionsFromString(field.protectedValue.stringValue))
            )
        }
        return TemplateSection(sectionAttributes)
    }

    private fun getTemplateFromTemplateEntry(templateEntry: EntryKDBX): Template? {

        val templateEntryDecoded = decodeTemplateEntry(templateEntry)
        val templateSections = mutableListOf<TemplateSection>()
        val sectionFields = mutableListOf<Field>()
        templateEntryDecoded.doForEachDecodedCustomField { field ->
            if (field.protectedValue.stringValue.contains(TemplateAttributeType.DIVIDER.typeString)) {
                templateSections.add(buildTemplateSectionFromFields(sectionFields))
                sectionFields.clear()
            } else {
                sectionFields.add(field)
            }
        }
        templateSections.add(buildTemplateSectionFromFields(sectionFields))

        // TODO Add decoder version
        return Template(templateEntry.id, templateEntry.title, templateEntry.icon, templateSections, 1)
    }

    companion object {

        private val TAG = TemplateEngine::class.java.name

        const val PREFIX_DECODED_TEMPLATE = "["
        const val SUFFIX_DECODED_TEMPLATE = "]"
        const val SECTION_DECODED_TEMPLATE_PREFIX = "Section "

        fun getDefaultTemplateGroupName(resources: Resources): String {
            return resources.getString(R.string.templates)
        }

        fun isTemplateNameAttribute(name: String): Boolean {
            return name.startsWith(PREFIX_DECODED_TEMPLATE)
                    && name.endsWith(SUFFIX_DECODED_TEMPLATE)
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