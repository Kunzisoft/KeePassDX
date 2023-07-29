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
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.ReplaceFileDialogFragment
import com.kunzisoft.keepass.activities.dialogs.SetOTPDialogFragment
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.adapters.TagsProposalAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.template.Template
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.utils.getParcelableList
import com.kunzisoft.keepass.utils.putParcelableList
import com.kunzisoft.keepass.view.TagsCompletionView
import com.kunzisoft.keepass.view.TemplateEditView
import com.kunzisoft.keepass.view.collapse
import com.kunzisoft.keepass.view.expand
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.viewmodels.EntryEditViewModel
import com.tokenautocomplete.FilteredArrayAdapter


class EntryEditFragment: DatabaseFragment() {

    private val mEntryEditViewModel: EntryEditViewModel by activityViewModels()

    private lateinit var rootView: View
    private lateinit var templateView: TemplateEditView
    private lateinit var attachmentsContainerView: ViewGroup
    private lateinit var attachmentsListView: RecyclerView
    private var attachmentsAdapter: EntryAttachmentsItemsAdapter? = null
    private lateinit var tagsContainerView: TextInputLayout
    private lateinit var tagsCompletionView: TagsCompletionView
    private var tagsAdapter: FilteredArrayAdapter<String>? = null

    private var mTemplate: Template? = null
    private var mAllowMultipleAttachments: Boolean = false

    private var mIconColor: Int = 0

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        // Retrieve the textColor to tint the icon
        val taIconColor = context?.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        mIconColor = taIconColor?.getColor(0, Color.BLACK) ?: Color.BLACK
        taIconColor?.recycle()

        return inflater.inflate(R.layout.fragment_entry_edit, container, false)
    }

    override fun onViewCreated(view: View,
                               savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view
        // Hide only the first time
        if (savedInstanceState == null) {
            view.isVisible = false
        }
        templateView = view.findViewById(R.id.template_view)
        attachmentsContainerView = view.findViewById(R.id.entry_attachments_container)
        attachmentsListView = view.findViewById(R.id.entry_attachments_list)
        tagsContainerView = view.findViewById(R.id.entry_tags_label)
        tagsCompletionView = view.findViewById(R.id.entry_tags_completion_view)

        attachmentsAdapter = EntryAttachmentsItemsAdapter(requireContext())
        attachmentsListView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = attachmentsAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        templateView.apply {
            setOnIconClickListener {
                mEntryEditViewModel.requestIconSelection(templateView.getIcon())
            }
            setOnBackgroundColorClickListener {
                mEntryEditViewModel.requestBackgroundColorSelection(templateView.getBackgroundColor())
            }
            setOnForegroundColorClickListener {
                mEntryEditViewModel.requestForegroundColorSelection(templateView.getForegroundColor())
            }
            setOnCustomEditionActionClickListener { field ->
                mEntryEditViewModel.requestCustomFieldEdition(field)
            }
            setOnPasswordGenerationActionClickListener { field ->
                mEntryEditViewModel.requestPasswordSelection(field)
            }
            setOnDateInstantClickListener { dateInstant ->
                mEntryEditViewModel.requestDateTimeSelection(dateInstant)
            }
        }

        if (savedInstanceState != null) {
            val attachments: List<Attachment> =
                savedInstanceState.getParcelableList(ATTACHMENTS_TAG) ?: listOf()
            setAttachments(attachments)
        }

        mEntryEditViewModel.onTemplateChanged.observe(viewLifecycleOwner) { template ->
            this.mTemplate = template
            templateView.setTemplate(template)
        }

        mEntryEditViewModel.templatesEntry.observe(viewLifecycleOwner) { templateEntry ->
            if (templateEntry != null) {
                val selectedTemplate = if (mTemplate != null)
                    mTemplate
                else
                    templateEntry.defaultTemplate
                templateView.setTemplate(selectedTemplate)
                // Load entry info only the first time to keep change locally
                if (savedInstanceState == null) {
                    assignEntryInfo(templateEntry.entryInfo)
                }
                // To prevent flickering
                rootView.showByFading()
                // Apply timeout reset
                resetAppTimeoutWhenViewFocusedOrChanged(rootView)
            }
        }

        mEntryEditViewModel.requestEntryInfoUpdate.observe(viewLifecycleOwner) {
            val entryInfo = retrieveEntryInfo()
            mEntryEditViewModel.saveEntryInfo(it.database, it.entry, it.parent, entryInfo)
        }

        mEntryEditViewModel.onIconSelected.observe(viewLifecycleOwner) { iconImage ->
            templateView.setIcon(iconImage)
        }

        mEntryEditViewModel.onBackgroundColorSelected.observe(viewLifecycleOwner) { color ->
            templateView.setBackgroundColor(color)
        }

        mEntryEditViewModel.onForegroundColorSelected.observe(viewLifecycleOwner) { color ->
            templateView.setForegroundColor(color)
        }

        mEntryEditViewModel.onPasswordSelected.observe(viewLifecycleOwner) { passwordField ->
            templateView.setPasswordField(passwordField)
        }

        mEntryEditViewModel.onDateSelected.observe(viewLifecycleOwner) { viewModelDate ->
            // Save the date
            templateView.setCurrentDateTimeValue(viewModelDate)
        }

        mEntryEditViewModel.onTimeSelected.observe(viewLifecycleOwner) { viewModelTime ->
            // Save the time
            templateView.setCurrentTimeValue(viewModelTime)
        }

        mEntryEditViewModel.onCustomFieldEdited.observe(viewLifecycleOwner) { fieldAction ->
            val oldField = fieldAction.oldField
            val newField = fieldAction.newField
            // Field to add
            if (oldField == null) {
                newField?.let {
                    if (!templateView.putCustomField(it)) {
                        mEntryEditViewModel.showCustomFieldEditionError()
                    }
                }
            }
            // Field to replace
            oldField?.let {
                newField?.let {
                    if (!templateView.replaceCustomField(oldField, newField)) {
                        mEntryEditViewModel.showCustomFieldEditionError()
                    }
                }
            }
            // Field to remove
            if (newField == null) {
                oldField?.let {
                    templateView.removeCustomField(it)
                }
            }
        }

        mEntryEditViewModel.requestSetupOtp.observe(viewLifecycleOwner) {
            // Retrieve the current otpElement if exists
            // and open the dialog to set up the OTP
            SetOTPDialogFragment.build(templateView.getEntryInfo().otpModel)
                .show(parentFragmentManager, "addOTPDialog")
        }

        mEntryEditViewModel.onOtpCreated.observe(viewLifecycleOwner) {
            // Update the otp field with otpauth:// url
            templateView.putOtpElement(it)
        }

        mEntryEditViewModel.onBuildNewAttachment.observe(viewLifecycleOwner) {
            val attachmentToUploadUri = it.attachmentToUploadUri
            val fileName = it.fileName

            buildNewBinaryAttachment()?.let { binaryAttachment ->
                val entryAttachment = Attachment(fileName, binaryAttachment)
                // Ask to replace the current attachment
                if ((!mAllowMultipleAttachments
                            && containsAttachment()) ||
                    containsAttachment(EntryAttachmentState(entryAttachment, StreamDirection.UPLOAD))) {
                    ReplaceFileDialogFragment.build(attachmentToUploadUri, entryAttachment)
                        .show(parentFragmentManager, "replacementFileFragment")
                } else {
                    mEntryEditViewModel.startUploadAttachment(attachmentToUploadUri, entryAttachment)
                }
            }
        }

        mEntryEditViewModel.onAttachmentAction.observe(viewLifecycleOwner) { entryAttachmentState ->
            when (entryAttachmentState?.downloadState) {
                AttachmentState.START -> {
                    putAttachment(entryAttachmentState)
                    getAttachmentViewPosition(entryAttachmentState) { attachment, position ->
                        mEntryEditViewModel.binaryPreviewLoaded(attachment, position)
                    }
                }
                AttachmentState.IN_PROGRESS -> {
                    putAttachment(entryAttachmentState)
                }
                AttachmentState.COMPLETE -> {
                    putAttachment(entryAttachmentState) { entryAttachment ->
                        getAttachmentViewPosition(entryAttachment) { attachment, position ->
                            mEntryEditViewModel.binaryPreviewLoaded(attachment, position)
                        }
                    }
                    mEntryEditViewModel.onAttachmentAction(null)
                }
                AttachmentState.CANCELED,
                AttachmentState.ERROR -> {
                    removeAttachment(entryAttachmentState)
                    mEntryEditViewModel.onAttachmentAction(null)
                }
                else -> {}
            }
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {

        templateView.populateIconMethod = { imageView, icon ->
            database?.iconDrawableFactory?.assignDatabaseIcon(imageView, icon, mIconColor)
        }

        mAllowMultipleAttachments = database?.allowMultipleAttachments == true

        attachmentsAdapter?.database = database
        attachmentsAdapter?.onListSizeChangedListener = { previousSize, newSize ->
            if (previousSize > 0 && newSize == 0) {
                attachmentsContainerView.collapse(true)
            } else if (previousSize == 0 && newSize == 1) {
                attachmentsContainerView.expand(true)
            }
        }

        tagsAdapter = TagsProposalAdapter(requireContext(), database?.tagPool)
        tagsCompletionView.apply {
            threshold = 1
            setAdapter(tagsAdapter)
        }
        tagsContainerView.visibility = if (database?.allowTags() == true) View.VISIBLE else View.GONE
    }

    private fun assignEntryInfo(entryInfo: EntryInfo?) {
        // Populate entry views
        templateView.setEntryInfo(entryInfo)

        // Set Tags
        entryInfo?.tags?.let { tags ->
            tagsCompletionView.setText("")
            for (i in 0 until tags.size()) {
                tagsCompletionView.addObjectSync(tags.get(i))
            }
        }

        // Manage attachments
        setAttachments(entryInfo?.attachments ?: listOf())
    }

    private fun retrieveEntryInfo(): EntryInfo {
        val entryInfo = templateView.getEntryInfo()
        entryInfo.tags = tagsCompletionView.getTags()
        entryInfo.attachments = getAttachments().toMutableList()
        return entryInfo
    }

    /* -------------
     * Attachments
     * -------------
     */

    private fun getAttachments(): List<Attachment> {
        return attachmentsAdapter?.itemsList?.map { it.attachment } ?: listOf()
    }

    private fun setAttachments(attachments: List<Attachment>) {
        attachmentsContainerView.visibility = if (attachments.isEmpty()) View.GONE else View.VISIBLE
        attachmentsAdapter?.assignItems(attachments.map {
            EntryAttachmentState(it, StreamDirection.UPLOAD)
        })
        attachmentsAdapter?.onDeleteButtonClickListener = { item ->
            val attachment = item.attachment
            removeAttachment(EntryAttachmentState(attachment, StreamDirection.DOWNLOAD))
            mEntryEditViewModel.deleteAttachment(attachment)
        }
    }

    private fun containsAttachment(): Boolean {
        return attachmentsAdapter?.isEmpty() != true
    }

    private fun containsAttachment(attachment: EntryAttachmentState): Boolean {
        return attachmentsAdapter?.contains(attachment) ?: false
    }

    private fun putAttachment(attachment: EntryAttachmentState,
                              onPreviewLoaded: ((attachment: EntryAttachmentState) -> Unit)? = null) {
        // When only one attachment is allowed
        if (!mAllowMultipleAttachments
            && attachment.downloadState == AttachmentState.START) {
            attachmentsAdapter?.clear()
        }
        attachmentsContainerView.visibility = View.VISIBLE
        attachmentsAdapter?.putItem(attachment)
        attachmentsAdapter?.onBinaryPreviewLoaded = {
            onPreviewLoaded?.invoke(attachment)
        }
    }

    private fun removeAttachment(attachment: EntryAttachmentState) {
        attachmentsAdapter?.removeItem(attachment)
    }

    private fun getAttachmentViewPosition(attachment: EntryAttachmentState,
                                          position: (attachment: EntryAttachmentState, Float) -> Unit) {
        attachmentsListView.postDelayed({
            attachmentsAdapter?.indexOf(attachment)?.let { index ->
                position.invoke(attachment,
                    attachmentsContainerView.y
                        + attachmentsListView.y
                        + (attachmentsListView.getChildAt(index)?.y
                        ?: 0F)
                )
            }
        }, 250)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableList(ATTACHMENTS_TAG, getAttachments())
    }

    /* -------------
     * Education
     * -------------
     */

    fun getActionImageView(): View? {
        return templateView.getActionImageView()
    }

    fun launchGeneratePasswordEductionAction() {
        mEntryEditViewModel.requestPasswordSelection(templateView.getPasswordField())
    }

    companion object {
        private val TAG = EntryEditFragment::class.java.name

        private const val ATTACHMENTS_TAG = "ATTACHMENTS_TAG"
    }

}