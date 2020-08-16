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
import com.kunzisoft.keepass.adapters.EntryAttachmentsAdapter
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.icons.assignDefaultDatabaseIcon
import com.kunzisoft.keepass.model.EntryAttachment
import com.kunzisoft.keepass.model.Field
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
    private val entryExtraFieldsContainerParent: ViewGroup
    private val entryExtraFieldsContainer: ViewGroup
    private val attachmentsContainerView: View
    private val attachmentsListView: RecyclerView

    private val attachmentsAdapter = EntryAttachmentsAdapter(context, true)

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
        entryExtraFieldsContainerParent = findViewById(R.id.extra_fields_container_parent)
        entryExtraFieldsContainer = findViewById(R.id.extra_fields_container)
        attachmentsContainerView = findViewById(R.id.entry_attachments_container)
        attachmentsListView = findViewById(R.id.entry_attachments_list)
        attachmentsListView?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
            adapter = attachmentsAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        entryExpiresCheckBox.setOnCheckedChangeListener { _, _ ->
            assignExpiresDateText()
        }

        entryExtraFieldsContainer.setOnHierarchyChangeListener(object: OnHierarchyChangeListener {
            override fun onChildViewRemoved(parent: View?, child: View?) {}

            override fun onChildViewAdded(parent: View?, child: View?) {
                parent?.let {
                    if ((parent as ViewGroup).childCount == 1)
                        parent.expand()
                }
            }
        })

        // Retrieve the textColor to tint the icon
        val taIconColor = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        iconColor = taIconColor.getColor(0, Color.WHITE)
        taIconColor.recycle()
    }

    fun applyFontVisibilityToFields(fontInVisibility: Boolean) {
        this.fontInVisibility = fontInVisibility
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

    val customFields: MutableList<Field>
        get() {
            val customFieldsArray = ArrayList<Field>()
            // Add extra fields from views
            entryExtraFieldsContainer.let {
                try {
                    for (i in 0 until it.childCount) {
                        val view = it.getChildAt(i) as EntryEditExtraField
                        customFieldsArray.add(view.extraField)
                    }
                } catch (exception: Exception) {
                    // Extra field container contains another type of view
                }
            }
            return customFieldsArray
        }

    private fun getExtraFieldByLabel(label: String): EntryEditExtraField? {
        for (i in 0..entryExtraFieldsContainer.childCount) {
            try {
                val extraFieldView = entryExtraFieldsContainer.getChildAt(i) as EntryEditExtraField?
                if (extraFieldView?.extraField?.name == label) {
                    return extraFieldView
                }
            } catch(e: Exception) {
                // Simply ignore when child view is not a custom field
            }
        }
        return null
    }

    private fun showOrHideExtraFieldsContainer(hide: Boolean, animate: Boolean = false) {
        entryExtraFieldsContainerParent.apply {
            if (!animate)
                visibility = if (hide) View.GONE else View.VISIBLE
            else
                if (hide) collapse(true) else expand(true)
        }
    }

    private fun buildNewEntryEditExtraField(): EntryEditExtraField {
        return EntryEditExtraField(context).apply {
            applyFontVisibility(fontInVisibility)
            onViewDeletedListener = {
                // remove callback
                if (entryExtraFieldsContainer.childCount <= 1) {
                    showOrHideExtraFieldsContainer(hide = true, animate = true)
                }
            }
        }
    }

    /**
     * Remove all children and add new views for each field
     */
    fun assignExtraFields(fields: List<Field>) {
        showOrHideExtraFieldsContainer(fields.isEmpty())
        entryExtraFieldsContainer.removeAllViews()
        fields.forEach { extraField ->
            entryExtraFieldsContainer.post {
                entryExtraFieldsContainer.addView(buildNewEntryEditExtraField().apply {
                    this.extraField = extraField
                })
            }
        }
    }

    /**
     * Update an extra field or create a new one if doesn't exists
     */
    fun putExtraField(extraField: Field)
            : EntryEditExtraField {
        showOrHideExtraFieldsContainer(false)
        var extraFieldView = getExtraFieldByLabel(extraField.name)
        // Create new view if not exists
        if (extraFieldView == null) {
            extraFieldView = buildNewEntryEditExtraField()
            // No need animation because of scroll
            entryExtraFieldsContainer.post {
                entryExtraFieldsContainer.addView(extraFieldView)
            }
        }
        extraFieldView.extraField = extraField
        return extraFieldView
    }

    /* -------------
     * Attachments
     * -------------
     */

    private fun showOrHideAttachmentsContainer(hide: Boolean, animate: Boolean = false) {
        attachmentsContainerView.apply {
            if (!animate)
                visibility = if (hide) View.GONE else View.VISIBLE
            else
                if (hide) collapse(true) else expand(true)
        }
    }

    fun assignAttachments(attachments: java.util.ArrayList<EntryAttachment>,
                          onDeleteItem: (attachment: EntryAttachment)->Unit) {
        showOrHideAttachmentsContainer(attachments.isEmpty())
        attachmentsAdapter.assignAttachments(attachments)
        attachmentsAdapter.onDeleteButtonClickListener = { item ->
            onDeleteItem.invoke(item)
        }
        attachmentsAdapter.onItemDeletedListener = { _, lastOne ->
            if (lastOne)
                showOrHideAttachmentsContainer(hide = true, animate = true)
        }
    }

    /**
     * Validate or not the entry form
     *
     * @return ErrorValidation An error with a message or a validation without message
     */
    fun isValid(): Boolean {
        // Validate extra fields
        entryExtraFieldsContainer.let {
            try {
                val customFieldLabelSet = HashSet<String>()
                for (i in 0 until it.childCount) {
                    val entryEditCustomField = it.getChildAt(i) as EntryEditExtraField
                    if (customFieldLabelSet.contains(entryEditCustomField.extraField.name)) {
                        entryEditCustomField.setError(R.string.error_label_exists)
                        return false
                    }
                    customFieldLabelSet.add(entryEditCustomField.extraField.name)
                    if (!entryEditCustomField.isValid()) {
                        return false
                    }
                }
            } catch (exception: Exception) {
                return false
            }
        }
        return true
    }

}