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
package com.kunzisoft.keepass.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.adapters.EntryExtraFieldsItemsAdapter
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.icons.assignDefaultDatabaseIcon
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.Field
import com.kunzisoft.keepass.model.FocusedEditField
import com.kunzisoft.keepass.model.StreamDirection
import org.joda.time.Duration
import org.joda.time.Instant

class EntryEditContentsView @JvmOverloads constructor(context: Context,
                                                      attrs: AttributeSet? = null,
                                                      defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private var fontInVisibility: Boolean = false

    private val entryTitleLayoutView: TextInputLayout
    private val entryTitleView: EditText
    private val entryIconView: ImageView
    private val entryUserNameView: EditText
    private val entryUrlView: EditText
    private val entryPasswordLayoutView: TextInputLayout
    private val entryPasswordView: EditText
    val entryPasswordGeneratorView: View
    private val entryExpiresCheckBox: CompoundButton
    private val entryExpiresTextView: TextView
    private val entryNotesView: EditText
    private val extraFieldsContainerView: ViewGroup
    private val extraFieldsListView: RecyclerView
    private val attachmentsContainerView: View
    private val attachmentsListView: RecyclerView

    private val extraFieldsAdapter = EntryExtraFieldsItemsAdapter(context)
    private val attachmentsAdapter = EntryAttachmentsItemsAdapter(context)

    private var iconColor: Int = 0
    private var expiresInstant: DateInstant = DateInstant(Instant.now().plus(Duration.standardDays(30)).toDate())

    var onDateClickListener: OnClickListener? = null
        set(value) {
            field = value
            if (entryExpiresCheckBox.isChecked)
                entryExpiresTextView.setOnClickListener(value)
            else
                entryExpiresTextView.setOnClickListener(null)
        }

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_entry_edit_contents, this)

        entryTitleLayoutView = findViewById(R.id.entry_edit_container_title)
        entryTitleView = findViewById(R.id.entry_edit_title)
        entryIconView = findViewById(R.id.entry_edit_icon_button)
        entryUserNameView = findViewById(R.id.entry_edit_user_name)
        entryUrlView = findViewById(R.id.entry_edit_url)
        entryPasswordLayoutView = findViewById(R.id.entry_edit_container_password)
        entryPasswordView = findViewById(R.id.entry_edit_password)
        entryPasswordGeneratorView = findViewById(R.id.entry_edit_password_generator_button)
        entryExpiresCheckBox = findViewById(R.id.entry_edit_expires_checkbox)
        entryExpiresTextView = findViewById(R.id.entry_edit_expires_text)
        entryNotesView = findViewById(R.id.entry_edit_notes)

        extraFieldsContainerView = findViewById(R.id.extra_fields_container)
        extraFieldsListView = findViewById(R.id.extra_fields_list)
        // To hide or not the container
        extraFieldsAdapter.onListSizeChangedListener = { previousSize, newSize ->
            if (previousSize > 0 && newSize == 0) {
                extraFieldsContainerView.collapse(true)
            } else if (previousSize == 0 && newSize == 1) {
                extraFieldsContainerView.expand(true)
            }
        }
        extraFieldsListView?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = extraFieldsAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        attachmentsContainerView = findViewById(R.id.entry_attachments_container)
        attachmentsListView = findViewById(R.id.entry_attachments_list)
        attachmentsAdapter.onListSizeChangedListener = { previousSize, newSize ->
            if (previousSize > 0 && newSize == 0) {
                attachmentsContainerView.collapse(true)
            } else if (previousSize == 0 && newSize == 1) {
                attachmentsContainerView.expand(true)
            }
        }
        attachmentsListView?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = attachmentsAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        entryExpiresCheckBox.setOnCheckedChangeListener { _, _ ->
            assignExpiresDateText()
        }

        // Retrieve the textColor to tint the icon
        val taIconColor = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        iconColor = taIconColor.getColor(0, Color.WHITE)
        taIconColor.recycle()
    }

    fun applyFontVisibilityToFields(fontInVisibility: Boolean) {
        this.fontInVisibility = fontInVisibility
        this.extraFieldsAdapter.applyFontVisibility = fontInVisibility
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

    fun setDefaultIcon(iconFactory: IconDrawableFactory) {
        entryIconView.assignDefaultDatabaseIcon(iconFactory, iconColor)
    }

    fun setIcon(iconFactory: IconDrawableFactory, icon: IconImage) {
        entryIconView.assignDatabaseIcon(iconFactory, icon, iconColor)
    }

    fun setOnIconViewClickListener(clickListener: () -> Unit) {
        entryIconView.setOnClickListener { clickListener.invoke() }
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
            entryExpiresTextView.setOnClickListener(onDateClickListener)
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
            entryExpiresCheckBox.isChecked = value
            assignExpiresDateText()
        }

    var expiresDate: DateInstant
        get() {
            return expiresInstant
        }
        set(value) {
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

    fun getExtraFields(): List<Field> {
        return extraFieldsAdapter.itemsList
    }

    fun getExtraFieldFocused(): FocusedEditField {
        // To keep focused after an orientation change
        return extraFieldsAdapter.getFocusedField()
    }

    /**
     * Remove all children and add new views for each field
     */
    fun assignExtraFields(fields: List<Field>, focusedExtraField: FocusedEditField? = null) {
        extraFieldsContainerView.visibility = if (fields.isEmpty()) View.GONE else View.VISIBLE
        // Reinit focused field
        extraFieldsAdapter.assignItems(fields, focusedExtraField)
    }

    /**
     * Update an extra field or create a new one if doesn't exists
     */
    fun putExtraField(extraField: Field) {
        extraFieldsContainerView.visibility = View.VISIBLE
        val oldField = extraFieldsAdapter.itemsList.firstOrNull { it.name == extraField.name }
        oldField?.let {
            if (extraField.protectedValue.stringValue.isEmpty())
                extraField.protectedValue.stringValue = it.protectedValue.stringValue
        }
        extraFieldsAdapter.putItem(extraField)
    }

    /* -------------
     * Attachments
     * -------------
     */

    fun getAttachments(): List<Attachment> {
        return attachmentsAdapter.itemsList.map { it.attachment }
    }

    fun assignAttachments(attachments: ArrayList<Attachment>,
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

    fun putAttachment(attachment: EntryAttachmentState) {
        attachmentsContainerView.visibility = View.VISIBLE
        attachmentsAdapter.putItem(attachment)
    }

    fun removeAttachment(attachment: EntryAttachmentState) {
        attachmentsAdapter.removeItem(attachment)
    }

    fun clearAttachments() {
        attachmentsAdapter.clear()
    }

    /**
     * Validate or not the entry form
     *
     * @return ErrorValidation An error with a message or a validation without message
     */
    fun isValid(): Boolean {
        // TODO
        return true
    }

}