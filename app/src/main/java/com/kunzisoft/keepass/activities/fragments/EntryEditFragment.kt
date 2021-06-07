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
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.EntryEditActivity
import com.kunzisoft.keepass.activities.dialogs.GeneratePasswordDialogFragment
import com.kunzisoft.keepass.activities.lock.resetAppTimeoutWhenViewFocusedOrChanged
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.*
import com.kunzisoft.keepass.database.element.template.TemplateField.LABEL_EXPIRATION
import com.kunzisoft.keepass.database.element.template.TemplateField.LABEL_NOTES
import com.kunzisoft.keepass.database.element.template.TemplateField.LABEL_PASSWORD
import com.kunzisoft.keepass.database.element.template.TemplateField.LABEL_TITLE
import com.kunzisoft.keepass.database.element.template.TemplateField.LABEL_URL
import com.kunzisoft.keepass.database.element.template.TemplateField.LABEL_USERNAME
import com.kunzisoft.keepass.education.EntryEditActivityEducation
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.*
import com.kunzisoft.keepass.viewmodels.EntryEditViewModel
import org.joda.time.DateTime

class EntryEditFragment: DatabaseFragment() {

    private lateinit var rootView: View
    private lateinit var entryIconView: ImageView
    private lateinit var entryTitleView: EntryEditFieldView
    private lateinit var templateContainerView: ViewGroup
    private lateinit var customFieldsContainerView: SectionView

    private lateinit var attachmentsContainerView: ViewGroup
    private lateinit var attachmentsListView: RecyclerView
    private var attachmentsAdapter: EntryAttachmentsItemsAdapter? = null

    private var fontInVisibility: Boolean = false
    private var mHideProtectedValue: Boolean = false
    private var iconColor: Int = 0

    var drawFactory: IconDrawableFactory? = null
    var onIconClickListener: ((IconImage) -> Unit)? = null
    var onPasswordGeneratorClickListener: ((Field) -> Unit)? = null
    var onDateTimeClickListener: ((DateInstant) -> Unit)? = null
    var onEditCustomFieldClickListener: ((Field) -> Unit)? = null
    var onRemoveAttachment: ((Attachment) -> Unit)? = null

    private val mEntryEditViewModel: EntryEditViewModel by activityViewModels()
    // Elements to modify the current entry
    private var mEntryInfo = EntryInfo()
    private var mTemplate = Template.STANDARD

    private var mCustomFieldIds = mutableListOf<FieldId>()
    // Current date time selection
    @IdRes
    private var mTempDateTimeViewId: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        rootView = inflater.cloneInContext(contextThemed)
                .inflate(R.layout.fragment_entry_edit, container, false)

        fontInVisibility = PreferencesUtil.fieldFontIsInVisibility(requireContext())

        entryIconView = rootView.findViewById(R.id.entry_edit_icon_button)
        entryIconView.setOnClickListener {
            onIconClickListener?.invoke(mEntryInfo.icon)
        }
        entryTitleView = rootView.findViewById(R.id.entry_edit_title)
        templateContainerView = rootView.findViewById(R.id.template_fields_container)
        // To fix card view margin in KitKat-
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val paddingVertical = resources.getDimensionPixelSize(R.dimen.card_view_margin_vertical)
            val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.card_view_margin_horizontal)
            templateContainerView.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        }
        customFieldsContainerView = rootView.findViewById(R.id.custom_fields_container)

        attachmentsContainerView = rootView.findViewById(R.id.entry_attachments_container)
        attachmentsListView = rootView.findViewById(R.id.entry_attachments_list)
        attachmentsAdapter = EntryAttachmentsItemsAdapter(requireContext())
        attachmentsAdapter?.database = mDatabase
        //attachmentsAdapter.database = arguments?.getInt(KEY_DATABASE)
        attachmentsAdapter?.onListSizeChangedListener = { previousSize, newSize ->
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

        rootView.resetAppTimeoutWhenViewFocusedOrChanged(requireContext(), mDatabase)

        if (savedInstanceState?.containsKey(KEY_SELECTION_DATE_TIME_ID) == true) {
            mTempDateTimeViewId = savedInstanceState.getInt(KEY_SELECTION_DATE_TIME_ID)
        }

        mEntryEditViewModel.entryInfo.observe(viewLifecycleOwner) { entryInfo ->
            mEntryInfo = entryInfo
            populateViewsWithEntry()
        }

        mEntryEditViewModel.template.observe(viewLifecycleOwner) { template ->
            mTemplate = template
            populateViewsWithEntry()
            rootView.showByFading()
        }

        mEntryEditViewModel.requestSaveEntry.observe(viewLifecycleOwner) {
            populateEntryWithViews()
            mEntryEditViewModel.setResponseSaveEntry(mEntryInfo)
        }

        assignAttachments(mEntryInfo.attachments, StreamDirection.UPLOAD) { attachment ->
            onRemoveAttachment?.invoke(attachment)
        }

        rootView.showByFading()
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

    fun generatePasswordEducationPerformed(entryEditActivityEducation: EntryEditActivityEducation): Boolean {
        val generatePasswordView = templateContainerView
                .findViewWithTag<EntryEditFieldView?>(FIELD_PASSWORD_TAG)
                ?.getActionImageView()
        return if (generatePasswordView != null) {
            entryEditActivityEducation.checkAndPerformedGeneratePasswordEducation(
                    generatePasswordView,
                    {
                        GeneratePasswordDialogFragment
                                .getInstance(Field(LABEL_PASSWORD, ProtectedString(true, mEntryInfo.password)))
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

    private fun populateViewsWithEntry() {
        activity?.let { context ->
            // Retrieve preferences
            mHideProtectedValue = PreferencesUtil.hideProtectedValue(context)

            // Build each template section
            templateContainerView.removeAllViews()
            customFieldsContainerView.removeAllViews()
            mCustomFieldIds.clear()

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

                    when {
                        templateAttribute.label.equals(LABEL_TITLE, true) -> {
                            throw Exception("title cannot be in template attribute")
                        }
                        templateAttribute.label.equals(LABEL_USERNAME, true) -> {
                            fieldTag = FIELD_USERNAME_TAG
                            fieldValue = mEntryInfo.username
                        }
                        templateAttribute.label.equals(LABEL_PASSWORD, true) -> {
                            fieldTag = FIELD_PASSWORD_TAG
                            fieldValue = mEntryInfo.password
                        }
                        templateAttribute.label.equals(LABEL_URL, true) -> {
                            fieldTag = FIELD_URL_TAG
                            fieldValue = mEntryInfo.url
                        }
                        templateAttribute.label.equals(LABEL_EXPIRATION, true) -> {
                            fieldTag = FIELD_EXPIRES_TAG
                            fieldValue = mEntryInfo.getExpiresStringValue()
                        }
                        templateAttribute.label.equals(LABEL_NOTES, true) -> {
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
                field.protectedValue.stringValue,
                TemplateAttributeAction.CUSTOM_EDITION)
        return buildViewForTemplateField(customFieldTemplateAttribute, field, FIELD_CUSTOM_TAG)
    }

    private fun buildViewForTemplateField(templateAttribute: TemplateAttribute,
                                          field: Field,
                                          fieldTag: String): View? {
        // Build main view depending on type
        val itemView: View? = when (templateAttribute.type) {
                    TemplateAttributeType.INLINE,
                    TemplateAttributeType.MULTILINE -> {
                        buildLinearTextView(templateAttribute, field)
                    }
                    TemplateAttributeType.DATE,
                    TemplateAttributeType.TIME,
                    TemplateAttributeType.DATETIME -> {
                        buildDataTimeView(templateAttribute, field)
                    }
                }
        // Custom id defined by field name, use getViewByField(field: Field) to retrieve it
        itemView?.id = field.name.hashCode()
        itemView?.tag = fieldTag

        // Add new custom view id to the custom field list
        if (fieldTag == FIELD_CUSTOM_TAG) {
            val indexOldItem = indexCustomFieldIdByName(field.name)
            if (indexOldItem > 0)
                mCustomFieldIds.removeAt(indexOldItem)
            mCustomFieldIds.add(FieldId(field.name, itemView!!.id, field.protectedValue.isProtected))
        }
        return itemView
    }

    private fun buildLinearTextView(templateAttribute: TemplateAttribute,
                                    field: Field): View? {
        // Add an action icon if needed
        return context?.let {
            EntryEditFieldView(it).apply {
                label = TemplateField.getLocalizedName(context, field.name)
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
                label = TemplateField.getLocalizedName(context, field.name)
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

        mEntryInfo.customFields = mCustomFieldIds.map {
            getCustomField(it.label)
        }

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

    private data class FieldId(var label: String, var viewId: Int, var protected: Boolean)

    private fun isStandardFieldName(name: String): Boolean {
        return TemplateField.isStandardFieldName(name)
    }

    private fun customFieldIdByName(name: String): FieldId? {
        return mCustomFieldIds.find { it.label.equals(name, true) }
    }

    private fun indexCustomFieldIdByName(name: String): Int {
        return mCustomFieldIds.indexOfFirst { it.label.equals(name, true) }
    }

    private fun getCustomField(fieldName: String): Field {
        customFieldIdByName(fieldName)?.let { fieldId ->
            val editView: View? = templateContainerView.findViewById(fieldId.viewId)
                    ?: customFieldsContainerView.findViewById(fieldId.viewId)
            if (editView is EntryEditFieldView) {
                return Field(fieldName, ProtectedString(fieldId.protected, editView.value))
            }
            if (editView is DateTimeView) {
                val value = if (editView.activation) editView.dateTime.toString() else ""
                return Field(fieldName, ProtectedString(fieldId.protected, value))
            }
        }
        return Field(fieldName, ProtectedString(false, ""))
    }

    /**
     * Update a custom field or create a new one if doesn't exists, the old value is lost
     */
    fun putCustomField(customField: Field): Boolean {
        return if (!isStandardFieldName(customField.name)) {
            customFieldsContainerView.visibility = View.VISIBLE
            if (indexCustomFieldIdByName(customField.name) >= 0) {
                replaceCustomField(customField, customField)
            } else {
                val newCustomView = buildViewForCustomField(customField)
                customFieldsContainerView.addView(newCustomView)
                mCustomFieldIds.add(FieldId(customField.name,
                        newCustomView!!.id,
                        customField.protectedValue.isProtected))
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
            customFieldIdByName(oldField.name)?.viewId?.let { viewId ->
                customFieldsContainerView.findViewById<View>(viewId)?.let { viewToReplace ->
                    val oldValue = getCustomField(oldField.name).protectedValue.toString()

                    val parentGroup = viewToReplace.parent as ViewGroup
                    val indexInParent = parentGroup.indexOfChild(viewToReplace)
                    parentGroup.removeView(viewToReplace)

                    val newCustomFieldWithValue = Field(newField.name,
                            ProtectedString(newField.protectedValue.isProtected, oldValue))
                    val oldPosition = indexCustomFieldIdByName(oldField.name)
                    if (oldPosition > 0)
                        mCustomFieldIds.removeAt(oldPosition)

                    val newCustomView = buildViewForCustomField(newCustomFieldWithValue)
                    parentGroup.addView(newCustomView, indexInParent)
                    mCustomFieldIds.add(oldPosition, FieldId(newCustomFieldWithValue.name,
                            newCustomView!!.id,
                            newCustomFieldWithValue.protectedValue.isProtected))
                    newCustomView.requestFocus()
                    return true
                }
            }
        }
        return false
    }

    fun removeCustomField(oldCustomField: Field) {
        val indexOldField = indexCustomFieldIdByName(oldCustomField.name)
        if (indexOldField > 0) {
            mCustomFieldIds[indexOldField].viewId.let { viewId ->
                customFieldsContainerView.removeViewById(viewId)
            }
            mCustomFieldIds.removeAt(indexOldField)
        }
    }

    /* -------------
     * Attachments
     * -------------
     */

    fun getAttachments(): List<Attachment> {
        return attachmentsAdapter?.itemsList?.map { it.attachment } ?: listOf()
    }

    fun assignAttachments(attachments: List<Attachment>,
                          streamDirection: StreamDirection,
                          onDeleteItem: (attachment: Attachment) -> Unit) {
        attachmentsContainerView.visibility = if (attachments.isEmpty()) View.GONE else View.VISIBLE
        attachmentsAdapter?.assignItems(attachments.map { EntryAttachmentState(it, streamDirection) })
        attachmentsAdapter?.onDeleteButtonClickListener = { item ->
            onDeleteItem.invoke(item.attachment)
        }
    }

    fun containsAttachment(): Boolean {
        return attachmentsAdapter?.isEmpty() != true
    }

    fun containsAttachment(attachment: EntryAttachmentState): Boolean {
        return attachmentsAdapter?.contains(attachment) ?: false
    }

    fun putAttachment(attachment: EntryAttachmentState,
                      onPreviewLoaded: (() -> Unit)? = null) {
        attachmentsContainerView.visibility = View.VISIBLE
        attachmentsAdapter?.putItem(attachment)
        attachmentsAdapter?.onBinaryPreviewLoaded = {
            onPreviewLoaded?.invoke()
        }
    }

    fun removeAttachment(attachment: EntryAttachmentState) {
        attachmentsAdapter?.removeItem(attachment)
    }

    fun clearAttachments() {
        attachmentsAdapter?.clear()
    }

    fun getAttachmentViewPosition(attachment: EntryAttachmentState, position: (Float) -> Unit) {
        attachmentsListView.postDelayed({
            attachmentsAdapter?.indexOf(attachment)?.let { index ->
                position.invoke(attachmentsContainerView.y
                        + attachmentsListView.y
                        + (attachmentsListView.getChildAt(index)?.y
                        ?: 0F)
                )
            }
        }, 250)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        populateEntryWithViews()
        mTempDateTimeViewId?.let {
            outState.putInt(KEY_SELECTION_DATE_TIME_ID, it)
        }

        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_SELECTION_DATE_TIME_ID = "KEY_SELECTION_DATE_TIME_ID"

        private const val FIELD_USERNAME_TAG = "FIELD_USERNAME_TAG"
        private const val FIELD_PASSWORD_TAG = "FIELD_PASSWORD_TAG"
        private const val FIELD_URL_TAG = "FIELD_URL_TAG"
        private const val FIELD_EXPIRES_TAG = "FIELD_EXPIRES_TAG"
        private const val FIELD_NOTES_TAG = "FIELD_NOTES_TAG"
        private const val FIELD_CUSTOM_TAG = "FIELD_CUSTOM_TAG"

        fun getInstance(): EntryEditFragment {
            return EntryEditFragment().apply {
                arguments = Bundle().apply {}
            }
        }
    }

}