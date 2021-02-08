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
package com.kunzisoft.keepass.activities

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.GeneratePasswordDialogFragment
import com.kunzisoft.keepass.activities.lock.resetAppTimeoutWhenViewFocusedOrChanged
import com.kunzisoft.keepass.activities.stylish.StylishFragment
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.education.EntryEditActivityEducation
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.model.*
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.view.applyFontVisibility
import com.kunzisoft.keepass.view.collapse
import com.kunzisoft.keepass.view.expand

class EntryEditFragment: StylishFragment() {

    private lateinit var entryTitleLayoutView: TextInputLayout
    private lateinit var entryTitleView: EditText
    private lateinit var entryIconView: ImageView
    private lateinit var entryUserNameView: EditText
    private lateinit var entryUrlView: EditText
    private lateinit var entryPasswordLayoutView: TextInputLayout
    private lateinit var entryPasswordView: EditText
    private lateinit var entryPasswordGeneratorView: View
    private lateinit var entryExpiresCheckBox: CompoundButton
    private lateinit var entryExpiresTextView: TextView
    private lateinit var entryNotesView: EditText
    private lateinit var extraFieldsContainerView: View
    private lateinit var extraFieldsListView: ViewGroup
    private lateinit var attachmentsContainerView: View
    private lateinit var attachmentsListView: RecyclerView

    private lateinit var attachmentsAdapter: EntryAttachmentsItemsAdapter

    private var fontInVisibility: Boolean = false
    private var iconColor: Int = 0
    private var expiresInstant: DateInstant = DateInstant.IN_ONE_MONTH

    var drawFactory: IconDrawableFactory? = null
    var setOnDateClickListener: View.OnClickListener? = null
    var setOnPasswordGeneratorClickListener: View.OnClickListener? = null
    var setOnIconViewClickListener: View.OnClickListener? = null
    var setOnEditCustomField: ((Field) -> Unit)? = null
    var setOnRemoveAttachment: ((Attachment) -> Unit)? = null

    // Elements to modify the current entry
    private var mEntryInfo = EntryInfo()
    private var mBinaryCipherKey: Database.LoadedKey? = null
    private var mLastFocusedEditField: FocusedEditField? = null
    private var mExtraViewToRequestFocus: EditText? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val rootView = inflater.cloneInContext(contextThemed)
            .inflate(R.layout.fragment_entry_edit_contents, container, false)

        fontInVisibility = PreferencesUtil.fieldFontIsInVisibility(requireContext())

        entryTitleLayoutView = rootView.findViewById(R.id.entry_edit_container_title)
        entryTitleView = rootView.findViewById(R.id.entry_edit_title)
        entryIconView = rootView.findViewById(R.id.entry_edit_icon_button)
        entryIconView.setOnClickListener {
            setOnIconViewClickListener?.onClick(it)
        }

        entryUserNameView = rootView.findViewById(R.id.entry_edit_user_name)
        entryUrlView = rootView.findViewById(R.id.entry_edit_url)
        entryPasswordLayoutView = rootView.findViewById(R.id.entry_edit_container_password)
        entryPasswordView = rootView.findViewById(R.id.entry_edit_password)
        entryPasswordGeneratorView = rootView.findViewById(R.id.entry_edit_password_generator_button)
        entryPasswordGeneratorView.setOnClickListener {
            setOnPasswordGeneratorClickListener?.onClick(it)
        }
        entryExpiresCheckBox = rootView.findViewById(R.id.entry_edit_expires_checkbox)
        entryExpiresTextView = rootView.findViewById(R.id.entry_edit_expires_text)
        entryExpiresTextView.setOnClickListener {
            if (entryExpiresCheckBox.isChecked)
                setOnDateClickListener?.onClick(it)
        }

        entryNotesView = rootView.findViewById(R.id.entry_edit_notes)

        extraFieldsContainerView = rootView.findViewById(R.id.extra_fields_container)
        extraFieldsListView = rootView.findViewById(R.id.extra_fields_list)

        attachmentsContainerView = rootView.findViewById(R.id.entry_attachments_container)
        attachmentsListView = rootView.findViewById(R.id.entry_attachments_list)
        attachmentsAdapter = EntryAttachmentsItemsAdapter(requireContext())
        attachmentsAdapter.binaryCipherKey = arguments?.getSerializable(KEY_BINARY_CIPHER_KEY) as? Database.LoadedKey?
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

        entryExpiresCheckBox.setOnCheckedChangeListener { _, _ ->
            assignExpiresDateText()
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
            mLastFocusedEditField = savedInstanceState.getParcelable(KEY_LAST_FOCUSED_FIELD) ?: mLastFocusedEditField
        }

        populateViewsWithEntry()

        return rootView
    }

    override fun onDetach() {
        super.onDetach()

        drawFactory = null
        setOnDateClickListener = null
        setOnPasswordGeneratorClickListener = null
        setOnIconViewClickListener = null
        setOnRemoveAttachment = null
        setOnEditCustomField = null
    }

    fun getEntryInfo(): EntryInfo? {
        populateEntryWithViews()
        return mEntryInfo
    }

    fun generatePasswordEducationPerformed(entryEditActivityEducation: EntryEditActivityEducation): Boolean {
        return entryEditActivityEducation.checkAndPerformedGeneratePasswordEducation(
                entryPasswordGeneratorView,
                {
                    GeneratePasswordDialogFragment().show(parentFragmentManager, "PasswordGeneratorFragment")
                },
                {
                    try {
                        (activity as? EntryEditActivity?)?.performedNextEducation(entryEditActivityEducation)
                    } catch (ignore: Exception) {}
                }
        )
    }

    private fun populateViewsWithEntry() {
        // Set info in view
        icon = mEntryInfo.icon
        title = mEntryInfo.title
        username = mEntryInfo.username
        url = mEntryInfo.url
        password = mEntryInfo.password
        expires = mEntryInfo.expires
        expiryTime = mEntryInfo.expiryTime
        notes = mEntryInfo.notes
        assignExtraFields(mEntryInfo.customFields) { fields ->
            setOnEditCustomField?.invoke(fields)
        }
        assignAttachments(mEntryInfo.attachments, StreamDirection.UPLOAD) { attachment ->
            setOnRemoveAttachment?.invoke(attachment)
        }
    }

    private fun populateEntryWithViews() {
        // Icon already populate
        mEntryInfo.title = title
        mEntryInfo.username = username
        mEntryInfo.url = url
        mEntryInfo.password = password
        mEntryInfo.expires = expires
        mEntryInfo.expiryTime = expiryTime
        mEntryInfo.notes = notes
        mEntryInfo.customFields = getExtraFields()
        mEntryInfo.otpModel = OtpEntryFields.parseFields { key ->
            getExtraFields().firstOrNull { it.name == key }?.protectedValue?.toString()
        }?.otpModel
        mEntryInfo.attachments = getAttachments()
    }

    var title: String
        get() {
            return entryTitleView.text.toString()
        }
        set(value) {
            entryTitleView.setText(value)
            if (fontInVisibility)
                entryTitleView.applyFontVisibility()
        }

    var icon: IconImage
        get() {
            return mEntryInfo.icon
        }
        set(value) {
            mEntryInfo.icon = value
            drawFactory?.let { drawFactory ->
                entryIconView.assignDatabaseIcon(drawFactory, value, iconColor)
            }
        }

    var username: String
        get() {
            return entryUserNameView.text.toString()
        }
        set(value) {
            entryUserNameView.setText(value)
            if (fontInVisibility)
                entryUserNameView.applyFontVisibility()
        }

    var url: String
        get() {
            return entryUrlView.text.toString()
        }
        set(value) {
            entryUrlView.setText(value)
            if (fontInVisibility)
                entryUrlView.applyFontVisibility()
        }

    var password: String
        get() {
            return entryPasswordView.text.toString()
        }
        set(value) {
            entryPasswordView.setText(value)
            if (fontInVisibility) {
                entryPasswordView.applyFontVisibility()
            }
        }

    private fun assignExpiresDateText() {
        entryExpiresTextView.text = if (entryExpiresCheckBox.isChecked) {
            entryExpiresTextView.setOnClickListener(setOnDateClickListener)
            expiresInstant.getDateTimeString(resources)
        } else {
            entryExpiresTextView.setOnClickListener(null)
            resources.getString(R.string.never)
        }
        if (fontInVisibility)
            entryExpiresTextView.applyFontVisibility()
    }

    var expires: Boolean
        get() {
            return entryExpiresCheckBox.isChecked
        }
        set(value) {
            if (!value) {
                expiresInstant = DateInstant.IN_ONE_MONTH
            }
            entryExpiresCheckBox.isChecked = value
            assignExpiresDateText()
        }

    var expiryTime: DateInstant
        get() {
            return if (expires)
                expiresInstant
            else
                DateInstant.NEVER_EXPIRE
        }
        set(value) {
            if (expires)
                expiresInstant = value
            assignExpiresDateText()
        }

    var notes: String
        get() {
            return entryNotesView.text.toString()
        }
        set(value) {
            entryNotesView.setText(value)
            if (fontInVisibility)
                entryNotesView.applyFontVisibility()
        }

    /* -------------
     * Extra Fields
     * -------------
     */

    private var mExtraFieldsList: MutableList<Field> = ArrayList()
    private var mOnEditButtonClickListener: ((item: Field)->Unit)? = null

    private fun buildViewFromField(extraField: Field): View? {
        val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val itemView: View? = inflater?.inflate(R.layout.item_entry_edit_extra_field, extraFieldsListView, false)
        itemView?.id = View.NO_ID

        val extraFieldValueContainer: TextInputLayout? = itemView?.findViewById(R.id.entry_extra_field_value_container)
        extraFieldValueContainer?.isPasswordVisibilityToggleEnabled = extraField.protectedValue.isProtected
        extraFieldValueContainer?.hint = extraField.name
        extraFieldValueContainer?.id = View.NO_ID

        val extraFieldValue: TextInputEditText? = itemView?.findViewById(R.id.entry_extra_field_value)
        extraFieldValue?.apply {
            if (extraField.protectedValue.isProtected) {
                inputType = extraFieldValue.inputType or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
            }
            setText(extraField.protectedValue.toString())
            if (fontInVisibility)
                applyFontVisibility()
        }
        extraFieldValue?.id = View.NO_ID
        extraFieldValue?.tag = "FIELD_VALUE_TAG"
        if (mLastFocusedEditField?.field == extraField) {
            mExtraViewToRequestFocus = extraFieldValue
        }

        val extraFieldEditButton: View? = itemView?.findViewById(R.id.entry_extra_field_edit)
        extraFieldEditButton?.setOnClickListener {
            mOnEditButtonClickListener?.invoke(extraField)
        }
        extraFieldEditButton?.id = View.NO_ID

        return itemView
    }

    fun getExtraFields(): List<Field> {
        mLastFocusedEditField = null
        for (index in 0 until extraFieldsListView.childCount) {
            val extraFieldValue: EditText = extraFieldsListView.getChildAt(index)
                    .findViewWithTag("FIELD_VALUE_TAG")
            val extraField = mExtraFieldsList[index]
            extraField.protectedValue.stringValue = extraFieldValue.text?.toString() ?: ""
            if (extraFieldValue.isFocused) {
                mLastFocusedEditField = FocusedEditField().apply {
                    field = extraField
                    cursorSelectionStart = extraFieldValue.selectionStart
                    cursorSelectionEnd = extraFieldValue.selectionEnd
                }
            }
        }
        return mExtraFieldsList
    }

    /**
     * Remove all children and add new views for each field
     */
    fun assignExtraFields(fields: List<Field>,
                          onEditButtonClickListener: ((item: Field)->Unit)?) {
        extraFieldsContainerView.visibility = if (fields.isEmpty()) View.GONE else View.VISIBLE
        // Reinit focused field
        mExtraFieldsList.clear()
        mExtraFieldsList.addAll(fields)
        extraFieldsListView.removeAllViews()
        fields.forEach {
            extraFieldsListView.addView(buildViewFromField(it))
        }
        // Request last focus
        mLastFocusedEditField?.let { focusField ->
            mExtraViewToRequestFocus?.apply {
                requestFocus()
                setSelection(focusField.cursorSelectionStart,
                                focusField.cursorSelectionEnd)
            }
        }
        mLastFocusedEditField = null
        mOnEditButtonClickListener = onEditButtonClickListener
    }

    /**
     * Update an extra field or create a new one if doesn't exists, the old value is lost
     */
    fun putExtraField(extraField: Field) {
        extraFieldsContainerView.visibility = View.VISIBLE
        val oldField = mExtraFieldsList.firstOrNull { it.name == extraField.name }
        oldField?.let {
            val index = mExtraFieldsList.indexOf(oldField)
            mExtraFieldsList.removeAt(index)
            mExtraFieldsList.add(index, extraField)
            extraFieldsListView.removeViewAt(index)
            val newView = buildViewFromField(extraField)
            extraFieldsListView.addView(newView, index)
            newView?.requestFocus()
        } ?: kotlin.run {
            mExtraFieldsList.add(extraField)
            val newView = buildViewFromField(extraField)
            extraFieldsListView.addView(newView)
            newView?.requestFocus()
        }
    }

    /**
     * Update an extra field and keep the old value
     */
    fun replaceExtraField(oldExtraField: Field, newExtraField: Field) {
        extraFieldsContainerView.visibility = View.VISIBLE
        val index = mExtraFieldsList.indexOf(oldExtraField)
        val oldValueEditText: EditText = extraFieldsListView.getChildAt(index)
                .findViewWithTag("FIELD_VALUE_TAG")
        val oldValue = oldValueEditText.text.toString()
        val newExtraFieldWithOldValue = Field(newExtraField).apply {
            this.protectedValue.stringValue = oldValue
        }
        mExtraFieldsList.removeAt(index)
        mExtraFieldsList.add(index, newExtraFieldWithOldValue)
        extraFieldsListView.removeViewAt(index)
        val newView = buildViewFromField(newExtraFieldWithOldValue)
        extraFieldsListView.addView(newView, index)
        newView?.requestFocus()
    }

    fun removeExtraField(oldExtraField: Field) {
        val previousSize = mExtraFieldsList.size
        val index = mExtraFieldsList.indexOf(oldExtraField)
        extraFieldsListView.getChildAt(index)?.let {
            it.collapse(true) {
                mExtraFieldsList.removeAt(index)
                extraFieldsListView.removeViewAt(index)
                val newSize = mExtraFieldsList.size

                if (previousSize > 0 && newSize == 0) {
                    extraFieldsContainerView.collapse(true)
                } else if (previousSize == 0 && newSize == 1) {
                    extraFieldsContainerView.expand(true)
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
                          onDeleteItem: (attachment: Attachment)->Unit) {
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
                      onPreviewLoaded: (()-> Unit)? = null) {
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
        outState.putSerializable(KEY_BINARY_CIPHER_KEY, mBinaryCipherKey)
        outState.putParcelable(KEY_LAST_FOCUSED_FIELD, mLastFocusedEditField)

        super.onSaveInstanceState(outState)
    }

    companion object {
        const val KEY_TEMP_ENTRY_INFO = "KEY_TEMP_ENTRY_INFO"
        const val KEY_BINARY_CIPHER_KEY = "KEY_BINARY_CIPHER_KEY"
        const val KEY_LAST_FOCUSED_FIELD = "KEY_LAST_FOCUSED_FIELD"

        fun getInstance(entryInfo: EntryInfo?,
                        loadedKey: Database.LoadedKey?): EntryEditFragment {
            return EntryEditFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(KEY_TEMP_ENTRY_INFO, entryInfo)
                    putSerializable(KEY_BINARY_CIPHER_KEY, loadedKey)
                }
            }
        }
    }

}