/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
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
 *
 */
package com.kunzisoft.keepass.activities.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.lock.resetAppTimeoutWhenViewFocusedOrChanged
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.database.element.template.TemplateAttribute
import com.kunzisoft.keepass.database.element.template.TemplateAttributeAction
import com.kunzisoft.keepass.database.element.template.TemplateAttributeType
import com.kunzisoft.keepass.education.EntryEditActivityEducation
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.*
import com.kunzisoft.keepass.model.TemplatesCustomFields.STANDARD_EXPIRATION
import com.kunzisoft.keepass.model.TemplatesCustomFields.STANDARD_NOTES
import com.kunzisoft.keepass.model.TemplatesCustomFields.STANDARD_PASSWORD
import com.kunzisoft.keepass.model.TemplatesCustomFields.STANDARD_TITLE
import com.kunzisoft.keepass.model.TemplatesCustomFields.STANDARD_URL
import com.kunzisoft.keepass.model.TemplatesCustomFields.STANDARD_USERNAME
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

class EntryEditFragment : StylishFragment() {

    private var mTemplate: Template? = null

    private var mInflater: LayoutInflater? = null

    private lateinit var entryIconView: ImageView
    private lateinit var entryTitleView: EntryEditFieldView
    private lateinit var templateContainerView: ViewGroup
    private lateinit var customFieldsContainerView: SectionView

    private lateinit var attachmentsContainerView: View
    private lateinit var attachmentsListView: RecyclerView
    private lateinit var attachmentsAdapter: EntryAttachmentsItemsAdapter

    private var fontInVisibility: Boolean = false
    private var iconColor: Int = 0

    var drawFactory: IconDrawableFactory? = null
    var onIconClickListener: ((IconImage) -> Unit)? = null
    var onPasswordGeneratorClickListener: ((Field) -> Unit)? = null
    var onDateTimeClickListener: ((DateInstant) -> Unit)? = null
    var onEditCustomFieldClickListener: ((Field) -> Unit)? = null
    var onRemoveAttachment: ((Attachment) -> Unit)? = null

    // Elements to modify the current entry
    private var mEntryInfo = EntryInfo()
    private var mLastFocusedEditField: FocusedEditField? = null
    private var mExtraViewToRequestFocus: EditText? = null

    // Current date time selection
    private var mTempDateTimeView: DateTimeView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val rootView = inflater.cloneInContext(contextThemed)
                .inflate(R.layout.fragment_entry_edit_contents, container, false)

        mInflater = inflater

        fontInVisibility = PreferencesUtil.fieldFontIsInVisibility(requireContext())

        entryIconView = rootView.findViewById(R.id.entry_edit_icon_button)
        entryIconView.setOnClickListener {
            onIconClickListener?.invoke(mEntryInfo.icon)
        }

        entryTitleView = rootView.findViewById(R.id.entry_edit_title)
        templateContainerView = rootView.findViewById(R.id.template_fields_container)
        customFieldsContainerView = rootView.findViewById(R.id.custom_fields_container)

        attachmentsContainerView = rootView.findViewById(R.id.entry_attachments_container)
        attachmentsListView = rootView.findViewById(R.id.entry_attachments_list)
        attachmentsAdapter = EntryAttachmentsItemsAdapter(requireContext())
        // TODO retrieve current database with its unique key
        attachmentsAdapter.database = Database.getInstance()
        //attachmentsAdapter.database = arguments?.getInt(KEY_DATABASE)
        attachmentsAdapter.onListSizeChangedListener = { previousSize, newSize ->
            if (previousSize > 0 && newSize == 0) {
                attachmentsContainerView.collapse(true)
            } else if (previousSize == 0 && newSize == 1) {
                attachmentsContainerView.expand(true)
            }
        }
        attachmentsListView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = attachmentsAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        // Retrieve the textColor to tint the icon
        val taIconColor = contextThemed?.theme?.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        iconColor = taIconColor?.getColor(0, Color.WHITE) ?: Color.WHITE
        taIconColor?.recycle()

        rootView?.resetAppTimeoutWhenViewFocusedOrChanged(requireContext())

        // Retrieve the new entry after an orientation change
        if (arguments?.containsKey(KEY_TEMP_ENTRY_INFO) == true)
            mEntryInfo = arguments?.getParcelable(KEY_TEMP_ENTRY_INFO) ?: mEntryInfo
        else if (savedInstanceState?.containsKey(KEY_TEMP_ENTRY_INFO) == true) {
            mEntryInfo = savedInstanceState.getParcelable(KEY_TEMP_ENTRY_INFO) ?: mEntryInfo
        }

        if (savedInstanceState?.containsKey(KEY_LAST_FOCUSED_FIELD) == true) {
            mLastFocusedEditField = savedInstanceState.getParcelable(KEY_LAST_FOCUSED_FIELD)
                    ?: mLastFocusedEditField
        }

        populateViewsWithEntry()
        assignAttachments(mEntryInfo.attachments, StreamDirection.UPLOAD) { attachment ->
            onRemoveAttachment?.invoke(attachment)
        }

        return rootView
    }

    override fun onDetach() {
        super.onDetach()

        drawFactory = null
        onDateTimeClickListener = null
        onPasswordGeneratorClickListener = null
        onIconClickListener = null
        onRemoveAttachment = null
        onEditCustomFieldClickListener = null
    }

    fun getEntryInfo(): EntryInfo {
        populateEntryWithViews()
        return mEntryInfo
    }

    fun generatePasswordEducationPerformed(entryEditActivityEducation: EntryEditActivityEducation): Boolean {
        return false // TODO education
        /*
        return entryEditActivityEducation.checkAndPerformedGeneratePasswordEducation(
                entryPasswordGeneratorView,
                {
                    GeneratePasswordDialogFragment().show(parentFragmentManager, "PasswordGeneratorFragment")
                },
                {
                    try {
                        (activity as? EntryEditActivity?)?.performedNextEducation(entryEditActivityEducation)
                    } catch (ignore: Exception) {
                    }
                }
        )
         */
    }

    fun assignTemplate(template: Template) {
        this.mTemplate = template
        populateViewsWithEntry()
    }

    private fun populateViewsWithEntry() {
        context?.let { context ->

            // Set info in view
            setIcon(mEntryInfo.icon)
            entryTitleView.apply {
                label = getString(R.string.entry_title)
                setValue(mEntryInfo.title, EntryEditFieldView.TextType.NORMAL)
            }

            // Build each template section
            templateContainerView.removeAllViews()
            customFieldsContainerView.clear()
            mCustomFields.clear()

            val customFieldsNotConsumed = ArrayList(mEntryInfo.customFields)
            mTemplate?.sections?.forEach { templateSection ->

                val sectionView = SectionView(context)

                // Build each attribute
                templateSection.attributes.forEach { templateAttribute ->
                    val fieldTag: String
                    val fieldValue: String

                    when (templateAttribute.label.toLowerCase(Locale.ENGLISH)) {
                        STANDARD_TITLE -> {
                            throw Exception("title cannot be in template attribute")
                        }
                        STANDARD_USERNAME -> {
                            fieldTag = FIELD_USERNAME_TAG
                            fieldValue = mEntryInfo.username
                        }
                        STANDARD_PASSWORD -> {
                            fieldTag = FIELD_PASSWORD_TAG
                            fieldValue = mEntryInfo.password
                        }
                        STANDARD_URL -> {
                            fieldTag = FIELD_URL_TAG
                            fieldValue = mEntryInfo.url
                        }
                        STANDARD_EXPIRATION -> {
                            fieldTag = FIELD_EXPIRES_TAG
                            fieldValue = mEntryInfo.getExpiresStringValue()
                        }
                        STANDARD_NOTES -> {
                            fieldTag = FIELD_NOTES_TAG
                            fieldValue = mEntryInfo.notes
                        }
                        else -> {
                            fieldTag = FIELD_CUSTOM_TAG
                            // Retrieve custom field value if exists to populate template
                            val index = customFieldsNotConsumed.indexOfFirst { field ->
                                field.name.equals(templateAttribute.label, true)
                            }
                            fieldValue = if (index != -1) {
                                val templateCustomField = customFieldsNotConsumed.removeAt(index)
                                templateCustomField.protectedValue.toString()
                            } else {
                                ""
                            }
                        }
                    }

                    val attributeView = buildViewForTemplateField(
                            templateAttribute,
                            Field(templateAttribute.label,
                                    ProtectedString(templateAttribute.protected, fieldValue)),
                            fieldTag)

                    // Add created view to this parent
                    sectionView.addAttributeView(attributeView)
                }

                // Add build view to parent
                templateContainerView.addView(sectionView)
            }

            // Add custom fields not in template
            customFieldsNotConsumed.forEach { customDynamicField ->
                val fieldView = buildViewForCustomField(customDynamicField)
                customFieldsContainerView.addAttributeView(fieldView)
            }

            /*
            // Request last focus
            mLastFocusedEditField?.let { focusField ->
                mExtraViewToRequestFocus?.apply {
                    requestFocus()
                    setSelection(focusField.cursorSelectionStart,
                            focusField.cursorSelectionEnd)
                }
            }
            mLastFocusedEditField = null
             */
        }
    }

    private fun buildViewForCustomField(field: Field): View? {
        val customFieldTemplateAttribute = TemplateAttribute(
                field.name,
                TemplateAttributeType.INLINE,
                field.protectedValue.isProtected,
                TemplateAttributeAction.CUSTOM_EDITION)
        return buildViewForTemplateField(customFieldTemplateAttribute, field, FIELD_CUSTOM_TAG)
    }

    private fun buildViewForTemplateField(templateAttribute: TemplateAttribute,
                                          field: Field,
                                          fieldTag: String): View? {
        // Build main view depending on type
        val itemView: View? = when (templateAttribute.type) {
                    TemplateAttributeType.INLINE,
                    TemplateAttributeType.URL,
                    TemplateAttributeType.MULTILINE -> {
                        buildLinearTextView(templateAttribute, field)
                    }
                    TemplateAttributeType.DATE,
                    TemplateAttributeType.TIME,
                    TemplateAttributeType.DATETIME -> {
                        buildDataTimeView(templateAttribute, field)
                    }
                    TemplateAttributeType.LISTBOX -> TODO()
                    TemplateAttributeType.POPOUT -> TODO()
                    TemplateAttributeType.RICH_TEXTBOX -> TODO()

                    //TODO Password
                }
        itemView?.id = ViewCompat.generateViewId()
        itemView?.tag = fieldTag

        // Add new custom view id to the custom field list
        if (fieldTag == FIELD_CUSTOM_TAG) {
            mCustomFields[field.name] = FieldId(itemView!!.id, field.protectedValue.isProtected)
        }
        return itemView
    }

    private fun buildLinearTextView(templateAttribute: TemplateAttribute,
                                    field: Field): View? {
        // Add an action icon if needed
        return context?.let {
            EntryEditFieldView(it).apply {
                label = TemplatesCustomFields.getLocalizedName(context, field.name)
                setProtection(field.protectedValue.isProtected)
                setValue(field.protectedValue.toString(), when (templateAttribute.type) {
                    TemplateAttributeType.MULTILINE -> EntryEditFieldView.TextType.MULTI_LINE
                    else -> EntryEditFieldView.TextType.NORMAL
                })
                when (templateAttribute.action) {
                    TemplateAttributeAction.NONE -> {
                        setOnActionClickListener(null)
                    }
                    TemplateAttributeAction.CUSTOM_EDITION -> {
                        setOnActionClickListener({
                            onEditCustomFieldClickListener?.invoke(field)
                        }, R.drawable.ic_more_white_24dp)
                    }
                    TemplateAttributeAction.PASSWORD_GENERATION -> {
                        setOnActionClickListener({
                            onPasswordGeneratorClickListener?.invoke(field)
                        }, R.drawable.ic_generate_password_white_24dp)
                    }
                }
                templateAttribute.options.forEach { option ->
                    // TODO options
                }
                if (mLastFocusedEditField?.field == field) {
                    // TODO mExtraViewToRequestFocus = fieldTextView
                }
                applyFontVisibility(fontInVisibility)
            }
        }
    }

    private fun buildDataTimeView(templateAttribute: TemplateAttribute,
                                  field: Field): View? {
        return context?.let {
            DateTimeView(it).apply {
                label = TemplatesCustomFields.getLocalizedName(context, field.name)
                try {
                    val value = field.protectedValue.toString()
                    activation = value.trim().isNotEmpty()
                    dateTime = DateInstant(value,
                            when (templateAttribute.type) {
                                TemplateAttributeType.DATE -> DateInstant.Type.DATE
                                TemplateAttributeType.TIME -> DateInstant.Type.TIME
                                else -> DateInstant.Type.DATE_TIME
                            })
                } catch (e: Exception) {
                    activation = false
                    dateTime = when (templateAttribute.type) {
                                TemplateAttributeType.DATE -> DateInstant.IN_ONE_MONTH_DATE
                                TemplateAttributeType.TIME -> DateInstant.IN_ONE_HOUR_TIME
                                else -> DateInstant.IN_ONE_MONTH_DATE_TIME
                            }
                }
                setOnDateClickListener = { dateInstant ->
                    mTempDateTimeView = this
                    onDateTimeClickListener?.invoke(dateInstant)
                }
            }
        }
    }

    private fun populateEntryWithViews() {
        // Icon already populate
        mEntryInfo.title = entryTitleView.value

        val userNameView: EntryEditFieldView? = templateContainerView.findViewWithTag(FIELD_USERNAME_TAG)
        userNameView?.value?.let {
            mEntryInfo.username = it
        }

        val passwordView: EntryEditFieldView? = templateContainerView.findViewWithTag(FIELD_PASSWORD_TAG)
        passwordView?.value?.let {
            mEntryInfo.password = it
        }

        val urlView: EntryEditFieldView? = templateContainerView.findViewWithTag(FIELD_URL_TAG)
        urlView?.value?.let {
            mEntryInfo.url = it
        }

        val expirationView: DateTimeView? = templateContainerView.findViewWithTag(FIELD_EXPIRES_TAG)
        expirationView?.activation?.let {
            mEntryInfo.expires = it
        }
        expirationView?.dateTime?.let {
            mEntryInfo.expiryTime = it
        }

        val notesView: EntryEditFieldView? = templateContainerView.findViewWithTag(FIELD_NOTES_TAG)
        notesView?.value?.let {
            mEntryInfo.notes = it
        }

        mEntryInfo.customFields = getCustomFields()
        mEntryInfo.otpModel = OtpEntryFields.parseFields { key ->
            getCustomField(key).protectedValue.toString()
        }?.otpModel

        mEntryInfo.attachments = getAttachments()
    }

    fun setIcon(iconImage: IconImage) {
        mEntryInfo.icon = iconImage
        drawFactory?.assignDatabaseIcon(entryIconView, iconImage, iconColor)
    }

    fun setPassword(password: String) {
        mEntryInfo.password = password
        val passwordView: EntryEditFieldView? = templateContainerView.findViewWithTag(FIELD_PASSWORD_TAG)
        passwordView?.value = password
    }

    /* -------------
     * Date Time selection
     * -------------
     */

    fun setCurrentDateTimeSelection(expiration: DateInstant) {
        // TODO fix orientation change
        mTempDateTimeView?.dateTime = expiration
    }

    fun getCurrentDateTimeSelection(): DateInstant? {
        return mTempDateTimeView?.dateTime
    }

    /* -------------
     * Extra Fields
     * -------------
     */

    private var mCustomFields = LinkedHashMap<String, FieldId>() // <label>, <viewId>
    private data class FieldId(var viewId: Int, var protected: Boolean)

    private fun isStandardFieldName(name: String): Boolean {
        return TemplatesCustomFields.isStandardFieldName(name)
    }

    private fun containsCustomFieldName(name: String): Boolean {
        return mCustomFields.keys.firstOrNull { it.equals(name, true) } != null
    }

    private fun getCustomField(fieldName: String): Field {
        mCustomFields[fieldName]?.let { fieldId ->
            val editView: View? = templateContainerView.findViewById(fieldId.viewId)
                    ?: customFieldsContainerView.findViewById(fieldId.viewId)
            if (editView is EntryEditFieldView) {
                return Field(fieldName, ProtectedString(fieldId.protected, editView.value))
            }
            if (editView is DateTimeView) {
                val value = if (editView.activation) editView.dateTime.toString() else ""
                return Field(fieldName, ProtectedString(fieldId.protected, value))
            }
        } // TODO other view type
        return Field(fieldName, ProtectedString(false, ""))
    }

    private fun getCustomFields(): List<Field> {
        // TODO focus ?
        return mCustomFields.map {
            getCustomField(it.key)
        }
    }

    /**
     * Update a custom field or create a new one if doesn't exists, the old value is lost
     */
    fun putCustomField(customField: Field): Boolean {
        return if (!isStandardFieldName(customField.name)) {
            customFieldsContainerView.visibility = View.VISIBLE
            if (containsCustomFieldName(customField.name)) {
                replaceCustomField(customField, customField)
            } else {
                val newCustomView = buildViewForCustomField(customField)
                customFieldsContainerView.addView(newCustomView)
                mCustomFields[customField.name] = FieldId(newCustomView!!.id, customField.protectedValue.isProtected)
                newCustomView.requestFocus()
                true
            }
        } else {
            false
        }
    }

    /**
     * Update a custom field and keep the old value
     */
    fun replaceCustomField(oldField: Field, newField: Field): Boolean {
        if (!isStandardFieldName(newField.name)) {
            if (containsCustomFieldName(oldField.name)) {
                mCustomFields[oldField.name]?.viewId?.let { viewId ->
                    templateContainerView.findViewById<View>(viewId)?.let { viewToReplace ->
                        val oldValue = getCustomField(oldField.name).protectedValue.toString()

                        val parentGroup = viewToReplace.parent as ViewGroup
                        val indexInParent = parentGroup.indexOfChild(viewToReplace)
                        parentGroup.removeView(viewToReplace)

                        val newCustomFieldWithValue = Field(newField.name,
                                ProtectedString(newField.protectedValue.isProtected, oldValue))
                        mCustomFields.remove(oldField.name)

                        val newCustomView = buildViewForCustomField(newCustomFieldWithValue)
                        parentGroup.addView(newCustomView, indexInParent)
                        mCustomFields[newCustomFieldWithValue.name] = FieldId(newCustomView!!.id,
                                newCustomFieldWithValue.protectedValue.isProtected)
                        newCustomView.requestFocus()
                        return true
                    }
                }
            }
        }
        return false
    }

    fun removeCustomField(oldCustomField: Field) {
        val previousSize = mCustomFields.size
        mCustomFields[oldCustomField.name]?.viewId?.let { viewId ->
            customFieldsContainerView.findViewById<View>(viewId)?.let { viewToRemove ->
                viewToRemove.collapse(true) {
                    mCustomFields.remove(oldCustomField.name)

                    // TODO collapse empty section
                    /*
                    val newSize = mCustomFields.size
                    if (previousSize > 0 && newSize == 0) {
                        extraFieldsContainerView.collapse(true)
                    } else if (previousSize == 0 && newSize == 1) {
                        extraFieldsContainerView.expand(true)
                    }
                    */
                }
            }
        }
    }

    /* -------------
     * Attachments
     * -------------
     */

    fun getAttachments(): List<Attachment> {
        return attachmentsAdapter.itemsList.map { it.attachment }
    }

    fun assignAttachments(attachments: List<Attachment>,
                          streamDirection: StreamDirection,
                          onDeleteItem: (attachment: Attachment) -> Unit) {
        attachmentsContainerView.visibility = if (attachments.isEmpty()) View.GONE else View.VISIBLE
        attachmentsAdapter.assignItems(attachments.map { EntryAttachmentState(it, streamDirection) })
        attachmentsAdapter.onDeleteButtonClickListener = { item ->
            onDeleteItem.invoke(item.attachment)
        }
    }

    fun containsAttachment(): Boolean {
        return !attachmentsAdapter.isEmpty()
    }

    fun containsAttachment(attachment: EntryAttachmentState): Boolean {
        return attachmentsAdapter.contains(attachment)
    }

    fun putAttachment(attachment: EntryAttachmentState,
                      onPreviewLoaded: (() -> Unit)? = null) {
        attachmentsContainerView.visibility = View.VISIBLE
        attachmentsAdapter.putItem(attachment)
        attachmentsAdapter.onBinaryPreviewLoaded = {
            onPreviewLoaded?.invoke()
        }
    }

    fun removeAttachment(attachment: EntryAttachmentState) {
        attachmentsAdapter.removeItem(attachment)
    }

    fun clearAttachments() {
        attachmentsAdapter.clear()
    }

    fun getAttachmentViewPosition(attachment: EntryAttachmentState, position: (Float) -> Unit) {
        attachmentsListView.postDelayed({
            position.invoke(attachmentsContainerView.y
                    + attachmentsListView.y
                    + (attachmentsListView.getChildAt(attachmentsAdapter.indexOf(attachment))?.y
                    ?: 0F)
            )
        }, 250)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        populateEntryWithViews()
        outState.putParcelable(KEY_TEMP_ENTRY_INFO, mEntryInfo)
        outState.putParcelable(KEY_LAST_FOCUSED_FIELD, mLastFocusedEditField)

        super.onSaveInstanceState(outState)
    }

    companion object {
        const val KEY_TEMP_ENTRY_INFO = "KEY_TEMP_ENTRY_INFO"
        const val KEY_DATABASE = "KEY_DATABASE"
        const val KEY_LAST_FOCUSED_FIELD = "KEY_LAST_FOCUSED_FIELD"

        private const val FIELD_USERNAME_TAG = "FIELD_USERNAME_TAG"
        private const val FIELD_PASSWORD_TAG = "FIELD_PASSWORD_TAG"
        private const val FIELD_URL_TAG = "FIELD_URL_TAG"
        private const val FIELD_EXPIRES_TAG = "FIELD_EXPIRES_TAG"
        private const val FIELD_NOTES_TAG = "FIELD_NOTES_TAG"
        private const val FIELD_CUSTOM_TAG = "FIELD_CUSTOM_TAG"

        fun getInstance(entryInfo: EntryInfo?): EntryEditFragment {
                        //database: Database?): EntryEditFragment {
            return EntryEditFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_TEMP_ENTRY_INFO, entryInfo)
                    // TODO Unique database key database.key
                    putInt(KEY_DATABASE, 0)
                }
            }
        }
    }

}