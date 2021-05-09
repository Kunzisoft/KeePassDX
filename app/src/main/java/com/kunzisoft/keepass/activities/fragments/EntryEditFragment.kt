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
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
import com.kunzisoft.keepass.database.element.template.Template.CREATOR.STANDARD_EXPIRES
import com.kunzisoft.keepass.database.element.template.Template.CREATOR.STANDARD_NOTES
import com.kunzisoft.keepass.database.element.template.Template.CREATOR.STANDARD_PASSWORD
import com.kunzisoft.keepass.database.element.template.Template.CREATOR.STANDARD_TITLE
import com.kunzisoft.keepass.database.element.template.Template.CREATOR.STANDARD_URL
import com.kunzisoft.keepass.database.element.template.Template.CREATOR.STANDARD_USERNAME
import com.kunzisoft.keepass.database.element.template.TemplateAttribute
import com.kunzisoft.keepass.database.element.template.TemplateAttribute.CREATOR.OPTION_EDITION
import com.kunzisoft.keepass.database.element.template.TemplateAttribute.CREATOR.OPTION_PASSWORD_GENERATOR
import com.kunzisoft.keepass.database.element.template.TemplateType
import com.kunzisoft.keepass.education.EntryEditActivityEducation
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.*
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.ExpirationView
import com.kunzisoft.keepass.view.applyFontVisibility
import com.kunzisoft.keepass.view.collapse
import com.kunzisoft.keepass.view.expand
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

class EntryEditFragment : StylishFragment() {

    private var mTemplate: Template? = null

    private var mInflater: LayoutInflater? = null

    private lateinit var entryIconView: ImageView
    private lateinit var entryTitleView: TextView

    private lateinit var templateContainerView: ViewGroup

    private var customFieldsContainerView: ViewGroup? = null

    private lateinit var attachmentsContainerView: View
    private lateinit var attachmentsListView: RecyclerView
    private lateinit var attachmentsAdapter: EntryAttachmentsItemsAdapter

    private var fontInVisibility: Boolean = false
    private var iconColor: Int = 0

    var drawFactory: IconDrawableFactory? = null
    var setOnDateClickListener: (() -> Unit)? = null
    var setOnPasswordGeneratorClickListener: View.OnClickListener? = null
    var setOnIconViewClickListener: ((IconImage) -> Unit)? = null
    var mOnEditCustomField: ((Field) -> Unit)? = null
    var setOnRemoveAttachment: ((Attachment) -> Unit)? = null

    // Elements to modify the current entry
    private var mEntryInfo = EntryInfo()
    private var mLastFocusedEditField: FocusedEditField? = null
    private var mExtraViewToRequestFocus: EditText? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val rootView = inflater.cloneInContext(contextThemed)
                .inflate(R.layout.fragment_entry_edit_contents, container, false)

        mInflater = inflater

        fontInVisibility = PreferencesUtil.fieldFontIsInVisibility(requireContext())

        entryIconView = rootView.findViewById(R.id.entry_edit_icon_button)
        entryIconView.setOnClickListener {
            setOnIconViewClickListener?.invoke(mEntryInfo.icon)
        }

        entryTitleView = rootView.findViewById(R.id.entry_edit_title)

        templateContainerView = rootView.findViewById(R.id.template_fields_container)

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
            setOnRemoveAttachment?.invoke(attachment)
        }

        return rootView
    }

    override fun onDetach() {
        super.onDetach()

        drawFactory = null
        setOnDateClickListener = null
        setOnPasswordGeneratorClickListener = null
        setOnIconViewClickListener = null
        setOnRemoveAttachment = null
        mOnEditCustomField = null
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
        val customFieldsNotConsumed = ArrayList(mEntryInfo.customFields)

        context?.let { context ->
            // Set info in view
            setIcon(mEntryInfo.icon)
            entryTitleView.text = mEntryInfo.title

            // Build each template section
            templateContainerView.removeAllViews()
            mCustomFields.clear()

            mTemplate?.sections?.forEach { templateSection ->

                val sectionView: View? = mInflater?.inflate(R.layout.view_template_section, templateContainerView, false)
                val sectionContainerView = sectionView?.findViewById<ViewGroup>(R.id.template_section_container)
                val sectionListView = sectionView?.findViewById<ViewGroup>(R.id.template_section_container_list)
                sectionView?.id = ViewCompat.generateViewId()
                sectionContainerView?.id = ViewCompat.generateViewId()
                sectionListView?.id = ViewCompat.generateViewId()

                customFieldsContainerView = null

                // Build each attribute
                templateSection.attributes.forEach { templateAttribute ->
                    var fieldTag = FIELD_CUSTOM_TAG

                    val fieldValue = when (templateAttribute.label.toLowerCase(Locale.ENGLISH)) {
                        STANDARD_TITLE -> {
                            throw Exception("title cannot be in template attribute")
                        }
                        STANDARD_USERNAME -> {
                            fieldTag = FIELD_USERNAME_TAG
                            mEntryInfo.username
                        }
                        STANDARD_PASSWORD -> {
                            fieldTag = FIELD_PASSWORD_TAG
                            mEntryInfo.password
                        }
                        STANDARD_URL -> {
                            fieldTag = FIELD_URL_TAG
                            mEntryInfo.url
                        }
                        STANDARD_EXPIRES -> {
                            fieldTag = FIELD_EXPIRES_TAG
                            mEntryInfo.expiryTime
                                    .getDateTimeString(context.resources)
                            // TODO Tests
                        }
                        STANDARD_NOTES -> {
                            fieldTag = FIELD_NOTES_TAG
                            mEntryInfo.notes
                        }
                        else -> {
                            // Retrieve custom field value if exists to populate template
                            val index = customFieldsNotConsumed.indexOfFirst { field ->
                                field.name.equals(templateAttribute.label, true)
                            }
                            if (index != -1) {
                                val templateCustomField = customFieldsNotConsumed.removeAt(index)
                                templateCustomField.protectedValue.toString()
                            } else {
                                ""
                            }
                            // TODO Other type
                        }
                    }

                    val templateField = Field(templateAttribute.label,
                            ProtectedString(templateAttribute.protected, fieldValue))
                    val attributeView = buildView(sectionListView,
                            templateAttribute,
                            templateField,
                            fieldTag)
                    // Add created view to this parent
                    sectionListView?.addView(attributeView)
                }

                // Add standard fields not in template

                // Add custom fields not in template
                if (templateSection.dynamic) {
                    customFieldsNotConsumed.forEach { customDynamicField ->
                        val fieldView = buildViewForCustomField(sectionListView, customDynamicField)
                        sectionListView?.addView(fieldView)
                    }
                    customFieldsContainerView = sectionListView
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

                // Add build view to parent
                templateContainerView.addView(sectionView)
            }
        }
    }

    private fun buildViewForCustomField(rootView: ViewGroup?,
                                        field: Field): View? {
        val customFieldTemplateAttribute = TemplateAttribute(
                field.name,
                TemplateType.INLINE,
                field.protectedValue.isProtected,
                ArrayList<String>().apply { add(OPTION_EDITION) })
        return buildView(rootView, customFieldTemplateAttribute, field, FIELD_CUSTOM_TAG)
    }

    private fun buildView(rootView: ViewGroup?,
                          templateAttribute: TemplateAttribute,
                          field: Field,
                          fieldTag: String): View? {
        // Build main view depending on type
        val itemView: View? =
                when (templateAttribute.type) {
                    TemplateType.INLINE,
                    TemplateType.URL,
                    TemplateType.MULTILINE -> {
                        // Add an action icon if needed
                        if (templateAttribute.containsActionOption()) {
                            mInflater?.inflate(R.layout.view_entry_edit_field_action, rootView, false)
                        } else {
                            mInflater?.inflate(R.layout.view_entry_edit_field, rootView, false)
                        }
                    }
                    TemplateType.DATE,
                    TemplateType.TIME,
                    TemplateType.DATETIME -> {
                        ExpirationView(requireContext())
                    }
                    TemplateType.LISTBOX -> TODO()
                    TemplateType.POPOUT -> TODO()
                    TemplateType.RICH_TEXTBOX -> TODO()

                    //TODO Password
                }

        // Value View
        val fieldTextView: TextInputEditText? = itemView?.findViewById(R.id.edit_field_text)
        when (templateAttribute.type) {
            TemplateType.INLINE -> {
                fieldTextView?.inputType?.let {
                    fieldTextView.inputType = it or EditorInfo.TYPE_TEXT_VARIATION_NORMAL
                }
                fieldTextView?.maxLines = 1
            }
            TemplateType.URL -> {
                fieldTextView?.inputType?.let {
                    fieldTextView.inputType = it or EditorInfo.TYPE_TEXT_VARIATION_URI
                }
                fieldTextView?.maxLines = 1
            }
            TemplateType.MULTILINE -> {
                fieldTextView?.inputType?.let {
                    fieldTextView.inputType = it or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
                }
                fieldTextView?.maxEms = 40
                fieldTextView?.maxLines = 40
            }
            else -> {}
        }
        if (field.protectedValue.isProtected) {
            fieldTextView?.inputType?.let {
                fieldTextView.inputType = it or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
            }
        }
        fieldTextView?.setText(field.protectedValue.toString())
        if (fontInVisibility)
            fieldTextView?.applyFontVisibility()
        // To retrieve each fields
        fieldTextView?.id = ViewCompat.generateViewId()
        fieldTextView?.tag = fieldTag

        if (mLastFocusedEditField?.field == field) {
            mExtraViewToRequestFocus = fieldTextView
        }

        // Label view
        val fieldTextLayout: TextInputLayout? = itemView?.findViewById(R.id.edit_field_text_layout)
        if (field.protectedValue.isProtected) {
            fieldTextLayout?.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }
        fieldTextLayout?.hint = TemplatesCustomFields.getLocalizedName(context, field.name)

        // Add action button and attach listener
        val fieldButtonView: ImageView? = itemView?.findViewById(R.id.edit_field_action_button)
        templateAttribute.options.forEach { option ->
            when (option) {
                OPTION_EDITION -> {
                    fieldButtonView?.setOnClickListener {
                        mOnEditCustomField?.invoke(field)
                    }
                }
                OPTION_PASSWORD_GENERATOR -> {
                    fieldButtonView?.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                            R.drawable.ic_generate_password_white_24dp))
                    fieldButtonView?.setOnClickListener(setOnPasswordGeneratorClickListener) // TODO generic
                }
            }
        }

        // Generate unique id for each view to prevent bugs
        fieldTextLayout?.id = ViewCompat.generateViewId()
        fieldButtonView?.id = ViewCompat.generateViewId()
        itemView?.id = ViewCompat.generateViewId()

        if (fieldTag == FIELD_CUSTOM_TAG) {
            mCustomFields[field.name] = FieldId(itemView!!.id, field.protectedValue.isProtected)
        }
        return itemView
    }

    private fun populateEntryWithViews() {
        // Icon already populate
        mEntryInfo.title = entryTitleView.text.toString()

        val userNameView: EditText? = templateContainerView.findViewWithTag(FIELD_USERNAME_TAG)
        userNameView?.text?.toString()?.let {
            mEntryInfo.username = it
        }

        val passwordView: EditText? = templateContainerView.findViewWithTag(FIELD_PASSWORD_TAG)
        passwordView?.text?.toString()?.let {
            mEntryInfo.password = it
        }

        val urlView: EditText? = templateContainerView.findViewWithTag(FIELD_URL_TAG)
        urlView?.text?.toString()?.let {
            mEntryInfo.url = it
        }

        mEntryInfo.expiryTime = expiryTime

        val notesView: EditText? = templateContainerView.findViewWithTag(FIELD_NOTES_TAG)
        notesView?.text?.toString()?.let {
            mEntryInfo.notes = it
        }

        mEntryInfo.customFields = getCustomFields()
        mEntryInfo.otpModel = OtpEntryFields.parseFields { key ->
            getCustomFields().firstOrNull { it.name == key }?.protectedValue?.toString()
        }?.otpModel

        mEntryInfo.attachments = getAttachments()
    }

    fun setIcon(iconImage: IconImage) {
        mEntryInfo.icon = iconImage
        drawFactory?.assignDatabaseIcon(entryIconView, iconImage, iconColor)
    }

    fun setPassword(password: String) {
        val passwordView: EditText? = templateContainerView.findViewWithTag(FIELD_PASSWORD_TAG)
        passwordView?.setText(password)
    }

    var expiryTime: DateInstant
        get() {
            val expirationView: ExpirationView? = templateContainerView.findViewWithTag(FIELD_EXPIRES_TAG)
            return expirationView?.expiryTime ?: DateInstant()
        }
        set(value) {
            val expirationView: ExpirationView? = templateContainerView.findViewWithTag(FIELD_EXPIRES_TAG)
            expirationView?.expiryTime = value
        }

    /* -------------
     * Extra Fields
     * -------------
     */

    private var mCustomFields = LinkedHashMap<String, FieldId>() // <label>, <viewId>
    private data class FieldId(var viewId: Int, var protected: Boolean)

    private fun isStandardFieldName(name: String): Boolean {
        return Template.isStandardFieldName(name)
    }

    private fun containsCustomFieldName(name: String): Boolean {
        return mCustomFields.keys.firstOrNull { it.equals(name, true) } != null
    }

    private fun getCustomField(fieldName: String): Field? {
        var editView: EditText?
        mCustomFields[fieldName]?.let { fieldId ->
            editView = templateContainerView
                    .findViewById<View?>(fieldId.viewId)
                    ?.findViewWithTag(FIELD_CUSTOM_TAG)
            return Field(fieldName, ProtectedString(fieldId.protected, editView?.text?.toString() ?: ""))
        } // TODO other view type
        return null
    }

    fun getCustomFields(): List<Field> {
        // TODO focus ?
        return mCustomFields.map {
            getCustomField(it.key)!!
        }
    }

    /**
     * Update a custom field or create a new one if doesn't exists, the old value is lost
     */
    fun putCustomField(customField: Field): Boolean {
        return if (!isStandardFieldName(customField.name)) {
            customFieldsContainerView?.visibility = View.VISIBLE
            if (containsCustomFieldName(customField.name)) {
                replaceCustomField(customField, customField)
            } else {
                val newCustomView = buildViewForCustomField(customFieldsContainerView, customField)
                customFieldsContainerView?.addView(newCustomView)
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
                        val oldValue = getCustomField(oldField.name)?.protectedValue?.toString() ?: ""

                        val parentGroup = viewToReplace.parent as ViewGroup
                        val indexInParent = parentGroup.indexOfChild(viewToReplace)
                        parentGroup.removeView(viewToReplace)

                        val newCustomFieldWithValue = Field(newField.name,
                                ProtectedString(newField.protectedValue.isProtected, oldValue))
                        mCustomFields.remove(oldField.name)

                        val newCustomView = buildViewForCustomField(parentGroup, newCustomFieldWithValue)
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
            customFieldsContainerView?.findViewById<View>(viewId)?.let { viewToRemove ->
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