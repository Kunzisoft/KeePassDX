package com.kunzisoft.keepass.database.element.template
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
import android.content.res.Resources
import android.graphics.Color
import android.util.Log
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.database.DatabaseKDBX
import com.kunzisoft.keepass.database.element.entry.EntryKDBX
import com.kunzisoft.keepass.database.element.group.GroupKDBX
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.security.ProtectedString
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
                    getTemplateFromTemplateEntry(templateEntry).let {
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
        return Template(CREATION)
    }

    fun createNewTemplatesGroup(templatesGroupName: String): GroupKDBX {
        val newTemplatesGroup = mDatabase.createGroup().apply {
            title = templatesGroupName
            icon.standard = mDatabase.getStandardIcon(IconImageStandard.BUILD_ID)
            enableAutoType = false
            enableSearching = false
            isExpanded = false
        }
        mDatabase.addGroupTo(newTemplatesGroup, mDatabase.rootGroup)
        // Build default templates
        getDefaults().forEach { defaultTemplate ->
            createTemplateEntry(defaultTemplate).also {
                mDatabase.addEntryTo(it, newTemplatesGroup)
            }
        }
        return newTemplatesGroup
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
                    getTemplateFromTemplateEntry(templateEntry).let { newTemplate ->
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

    abstract fun getVersion(): Int

    abstract fun getTemplate(entryKDBX: EntryKDBX): Template?

    abstract fun removeMetaTemplateRecognitionFromEntry(entry: EntryKDBX): EntryKDBX

    abstract fun addMetaTemplateRecognitionToEntry(template: Template, entry: EntryKDBX): EntryKDBX

    abstract fun buildTemplateEntryField(attribute: TemplateAttribute): Field

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
                        putField(Field(addTemplateDecorator(sectionName),
                            ProtectedString(false, TemplateAttributeType.DIVIDER.typeString))
                        )
                    }

                    putField(buildTemplateEntryField(attribute))
                }
            }
        }
        return encodeTemplateEntry(newEntry)
    }

    private fun buildTemplateSectionFromFields(fields: List<Field>): TemplateSection {
        val sectionAttributes = mutableListOf<TemplateAttribute>()
        fields.forEach { field ->
            sectionAttributes.add(TemplateAttribute(
                removeTemplateDecorator(field.name),
                TemplateAttributeType.getFromString(field.protectedValue.stringValue),
                field.protectedValue.isProtected,
                TemplateAttributeOption.getOptionsFromString(field.protectedValue.stringValue))
            )
        }
        return TemplateSection(sectionAttributes)
    }

    private fun getTemplateFromTemplateEntry(templateEntry: EntryKDBX): Template {

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

        var backgroundColor: Int? = null
        templateEntry.backgroundColor.let {
            try {
                backgroundColor = Color.parseColor(it)
            } catch (e: Exception) {}
        }
        var foregroundColor: Int? = null
        templateEntry.foregroundColor.let {
            try {
                foregroundColor = Color.parseColor(it)
            } catch (e: Exception) {}
        }

        return Template(
            templateEntry.id,
            templateEntry.title,
            templateEntry.icon,
            backgroundColor,
            foregroundColor,
            templateSections,
            getVersion()
        )
    }

    companion object {

        private val TAG = TemplateEngine::class.java.name

        private const val PREFIX_DECODED_TEMPLATE = "["
        private const val SUFFIX_DECODED_TEMPLATE = "]"
        private const val SECTION_DECODED_TEMPLATE_PREFIX = "Section "

        val CREATION: Template
            get() {
                val sections = mutableListOf<TemplateSection>()
                val mainSection = TemplateSection(mutableListOf<TemplateAttribute>().apply {
                    // Dynamic part
                })
                sections.add(mainSection)
                return Template(UUID(0, 1),
                    TemplateField.LABEL_TEMPLATE,
                    IconImage(IconImageStandard(IconImageStandard.BUILD_ID)),
                    sections)
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

        fun containsTemplateDecorator(name: String): Boolean {
            return name.startsWith(PREFIX_DECODED_TEMPLATE)
                    && name.endsWith(SUFFIX_DECODED_TEMPLATE)
        }

        fun addTemplateDecorator(name: String): String {
            return "$PREFIX_DECODED_TEMPLATE${name}$SUFFIX_DECODED_TEMPLATE"
        }

        fun removeTemplateDecorator(name: String): String {
            return name
                .removePrefix(PREFIX_DECODED_TEMPLATE)
                .removeSuffix(SUFFIX_DECODED_TEMPLATE)
        }
    }
}
