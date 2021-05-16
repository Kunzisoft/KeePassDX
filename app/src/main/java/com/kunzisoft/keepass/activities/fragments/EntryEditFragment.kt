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
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.EntryEditActivity
import com.kunzisoft.keepass.activities.dialogs.GeneratePasswordDialogFragment
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
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

class EntryEditFragment : StylishFragment() {

    private var mTemplate: Template = Template.STANDARD

    private var mInflater: LayoutInflater? = null

    private lateinit var entryIconView: ImageView
    private lateinit var entryTitleView: EntryEditFieldView
    private lateinit var templateContainerView: ViewGroup
    private lateinit var customFieldsContainerView: SectionView

    private lateinit var attachmentsContainerView: View
    private lateinit var attachmentsListView: RecyclerView
    private lateinit var attachmentsAdapter: EntryAttachmentsItemsAdapter

    private var fontInVisibility: Boolean = false
    private var mHideProtectedValue: Boolean = false
    private var iconColor: Int = 0

    var drawFactory: IconDrawableFactory? = null
    var onIconClickListener: ((IconImage) -> Unit)? = null
    var onPasswordGeneratorClickListener: ((Field) -> Unit)? = null
    var onDateTimeClickListener: ((DateInstant) -> Unit)? = null
    var onEditCustomFieldClickListener: ((Field) -> Unit)? = null
    var onRemoveAttachment: ((Attachment) -> Unit)? = null

    // Elements to modify the current entry
    private var mEntryInfo = EntryInfo()

    private var mCustomFields = LinkedHashMap<String, FieldId>() // <label>, <viewId>
    // Current date time selection
    @IdRes
    private var mTempDateTimeViewId: Int? = null

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

        if (savedInstanceState?.containsKey(KEY_TEMPLATE) == true) {
            mTemplate = savedInstanceState.getParcelable(KEY_TEMPLATE) ?: mTemplate
        }
        if (savedInstanceState?.containsKey(KEY_SELECTION_DATE_TIME_ID) == true) {
            mTempDateTimeViewId = savedInstanceState.getInt(KEY_SELECTION_DATE_TIME_ID)
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
        val generatePasswordView = templateContainerView
                .findViewWithTag<EntryEditFieldView?>(FIELD_PASSWORD_TAG)
                ?.getActionImageView()
        return if (generatePasswordView != null) {
            entryEditActivityEducation.checkAndPerformedGeneratePasswordEducation(
                    generatePasswordView,
                    {
                        GeneratePasswordDialogFragment
                                .getInstance(Field(STANDARD_PASSWORD, ProtectedString(true, mEntryInfo.password)))
                                .show(parentFragmentManager, "PasswordGeneratorFragment")
                    },
                    {
                        try {
                            (activity as? EntryEditActivity?)
                                    ?.performedNextEducation(entryEditActivityEducation)
                        } catch (ignore: Exception) {
                        }
                    }
            )
        } else {
            false
        }
    }

    fun getTemplate(): Template {
        return mTemplate
    }

    fun assignTemplate(template: Template) {
        this.mTemplate = template
        populateViewsWithEntry()
    }

    private fun populateViewsWithEntry() {
        activity?.let { context ->
            // Retrieve preferences
            mHideProtectedValue = PreferencesUtil.hideProtectedValue(context)

            // Build each template section
            templateContainerView.removeAllViews()
            customFieldsContainerView.removeAllViews()
            mCustomFields.clear()

            // Set info in view
            setIcon(mEntryInfo.icon)
            entryTitleView.apply {
                label = getString(R.string.entry_title)
                setValue(mEntryInfo.title, EntryEditFieldView.TextType.NORMAL)
            }

            val customFieldsNotConsumed = ArrayList(mEntryInfo.customFields)
            mTemplate.sections.forEach { templateSection ->

                val sectionView = SectionView(context, null, R.attr.cardViewStyle)
                // Add build view to parent
                templateContainerView.addView(sectionView)

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
                    sectionView.addView(attributeView)
                }
            }

            // Add custom fields not in template
            customFieldsNotConsumed.forEach { customDynamicField ->
                val fieldView = buildViewForCustomField(customDynamicField)
                customFieldsContainerView.addView(fieldView)
            }
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
                }
        // Custom id defined by field name, use getViewByField(field: Field) to retrieve it
        itemView?.id = field.name.hashCode()
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
                setProtection(field.protectedValue.isProtected, mHideProtectedValue)
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
                    mTempDateTimeViewId = id
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

    /* -------------
     * External value update
     * -------------
     */

    private fun getFieldViewByField(field: Field): View? {
        return getFieldViewById(field.name.hashCode())
    }

    private fun getFieldViewById(@IdRes viewId: Int): View? {
        return templateContainerView.findViewById(viewId)
                ?: customFieldsContainerView.findViewById(viewId)
    }

    fun setIcon(iconImage: IconImage) {
        mEntryInfo.icon = iconImage
        drawFactory?.assignDatabaseIcon(entryIconView, iconImage, iconColor)
    }

    fun setPassword(passwordField: Field) {
        val passwordValue = passwordField.protectedValue.stringValue
        mEntryInfo.password = passwordValue
        val passwordView = getFieldViewByField(passwordField)
        if (passwordView is EntryEditFieldView?) {
            passwordView?.value = passwordValue
        }
    }

    private fun setCurrentDateTimeSelection(action: (dateInstant: DateInstant) -> DateInstant) {
        mTempDateTimeViewId?.let { viewId ->
            val dateTimeView = getFieldViewById(viewId)
            if (dateTimeView is DateTimeView) {
                dateTimeView.dateTime = DateInstant(
                        action.invoke(dateTimeView.dateTime).date,
                        dateTimeView.dateTime.type)
            }
        }
    }

    fun setDate(year: Int, month: Int, day: Int) {
        // Save the date
        setCurrentDateTimeSelection { instant ->
            val newDateInstant = DateInstant(DateTime(instant.date)
                        .withYear(year)
                        .withMonthOfYear(month + 1)
                        .withDayOfMonth(day)
                        .toDate(), instant.type)
            if (instant.type == DateInstant.Type.DATE_TIME) {
                val instantTime = DateInstant(instant.date, DateInstant.Type.TIME)
                // Trick to recall selection with time
                onDateTimeClickListener?.invoke(instantTime)
            }
            newDateInstant
        }
    }

    fun setTime(hours: Int, minutes: Int) {
        // Save the time
        setCurrentDateTimeSelection { instant ->
            DateInstant(DateTime(instant.date)
                    .withHourOfDay(hours)
                    .withMinuteOfHour(minutes)
                    .toDate(), instant.type)
        }
    }

    /* -------------
     * Custom Fields
     * -------------
     */

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
            mCustomFields[oldField.name]?.viewId?.let { viewId ->
                customFieldsContainerView.findViewById<View>(viewId)?.let { viewToReplace ->
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
        return false
    }

    fun removeCustomField(oldCustomField: Field) {
        mCustomFields[oldCustomField.name]?.viewId?.let { viewId ->
            customFieldsContainerView.removeViewById(viewId)
            mCustomFields.remove(oldCustomField.name)
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
        outState.putParcelable(KEY_TEMPLATE, mTemplate)
        mTempDateTimeViewId?.let {
            outState.putInt(KEY_SELECTION_DATE_TIME_ID, it)
        }

        super.onSaveInstanceState(outState)
    }

    companion object {
        const val KEY_TEMP_ENTRY_INFO = "KEY_TEMP_ENTRY_INFO"
        const val KEY_TEMPLATE = "KEY_TEMPLATE"
        const val KEY_DATABASE = "KEY_DATABASE"
        const val KEY_SELECTION_DATE_TIME_ID = "KEY_SELECTION_DATE_TIME_ID"

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