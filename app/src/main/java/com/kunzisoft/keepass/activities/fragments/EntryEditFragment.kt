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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.SetOTPDialogFragment
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.adapters.TagsProposalAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.FieldProtection
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.view.TagsCompletionView
import com.kunzisoft.keepass.view.TemplateEditView
import com.kunzisoft.keepass.view.collapse
import com.kunzisoft.keepass.view.expand
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.viewmodels.AttachmentsViewModel
import com.kunzisoft.keepass.viewmodels.EntryEditViewModel
import com.kunzisoft.keepass.viewmodels.NodeEditViewModel
import com.tokenautocomplete.FilteredArrayAdapter
import kotlinx.coroutines.launch


class EntryEditFragment: DatabaseFragment() {

    private val mEntryEditViewModel: EntryEditViewModel by activityViewModels()
    private val mAttachmentsViewModel: AttachmentsViewModel by activityViewModels()

    private lateinit var rootView: View
    private lateinit var templateView: TemplateEditView
    private lateinit var attachmentsContainerView: ViewGroup
    private lateinit var attachmentsListView: RecyclerView
    private var attachmentsAdapter: EntryAttachmentsItemsAdapter? = null
    private lateinit var tagsContainerView: TextInputLayout
    private lateinit var tagsCompletionView: TagsCompletionView
    private var tagsAdapter: FilteredArrayAdapter<String>? = null
    private var mAllowMultipleAttachments: Boolean = false

    private var mIconColor: Int = 0

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        // Retrieve the textColor to tint the icon
        context?.obtainStyledAttributes(intArrayOf(android.R.attr.textColor)).also { taIconColor ->
            mIconColor = taIconColor?.getColor(0, Color.BLACK) ?: Color.BLACK
        }?.recycle()

        return inflater.inflate(R.layout.fragment_entry_edit, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
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
        attachmentsAdapter?.onDeleteButtonClickListener = { item ->
            mAttachmentsViewModel.deleteAttachment(item)
        }
        attachmentsAdapter?.onListSizeChangedListener = { previousSize, newSize ->
            if (previousSize > 0 && newSize == 0) {
                attachmentsContainerView.collapse(true)
            } else if (previousSize == 0 && newSize == 1) {
                attachmentsContainerView.expand(true)
            } else {
                attachmentsContainerView.isVisible = newSize != 0
            }
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
            setOnChangeFieldProtectionClickListener { fieldProtection ->
                mEntryEditViewModel.requestChangeFieldProtection(fieldProtection)
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

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mEntryEditViewModel.entryEditUIState.collect { uiState ->
                        // To prevent flickering
                        if (uiState.loaded)
                            rootView.showByFading()
                        // Apply timeout reset
                        resetAppTimeoutWhenViewFocusedOrChanged(rootView)
                    }
                }
                launch {
                    mAttachmentsViewModel.attachmentsUIState.collect { state ->
                        attachmentsAdapter?.assignItems(state.attachments)
                    }
                }
                launch {
                    mEntryEditViewModel.entryEditEvents.collect { event ->
                        when (event) {
                            is EntryEditViewModel.EntryEditEvent.EntryLoaded -> {
                                // Load entry info only the first time to keep change locally
                                assignEntryInfo(event.entryInfo)
                            }
                            is EntryEditViewModel.EntryEditEvent.OnTemplateChanged -> {
                                templateView.setTemplate(event.template)
                            }
                            is EntryEditViewModel.EntryEditEvent.OnPasswordSelected -> {
                                templateView.setPasswordField(event.field)
                            }
                            is EntryEditViewModel.EntryEditEvent.OnCustomFieldEdited -> {
                                val oldField = event.oldField
                                val newField = event.newField
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
                            is EntryEditViewModel.EntryEditEvent.RequestSetupOTP -> {
                                // Retrieve the current otpElement if exists
                                // and open the dialog to set up the OTP
                                SetOTPDialogFragment.build(templateView.getEntryInfo().otpModel)
                                    .show(parentFragmentManager, "addOTPDialog")
                            }
                            is EntryEditViewModel.EntryEditEvent.OnOtpCreated -> {
                                // Update the otp field with otpauth:// url
                                templateView.putOtpElement(event.otpElement)
                            }
                            is EntryEditViewModel.EntryEditEvent.OnFieldProtectionUpdated -> {
                                updateFieldProtection(event.fieldProtection)
                            }
                            is EntryEditViewModel.EntryEditEvent.RetrieveEntryInfoForClosing -> {
                                mEntryEditViewModel.askToCloseEntry(
                                    currentEntryInfo = retrieveEntryInfo(),
                                    closeType = event.closeType
                                )
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    mEntryEditViewModel.nodeEditEvents.collect { event ->
                        when(event) {
                            is NodeEditViewModel.NodeEditEvent.OnIconSelected -> {
                                templateView.setIcon(event.icon)
                            }
                            is NodeEditViewModel.NodeEditEvent.RequestColorSelection -> {}
                            is NodeEditViewModel.NodeEditEvent.RequestIconSelection -> {}
                            is NodeEditViewModel.NodeEditEvent.OnBackgroundColorSelected -> {
                                templateView.setBackgroundColor(event.color)
                            }
                            is NodeEditViewModel.NodeEditEvent.OnForegroundColorSelected -> {
                                templateView.setForegroundColor(event.color)
                            }
                            is NodeEditViewModel.NodeEditEvent.RequestDateTimeSelection -> {}
                            is NodeEditViewModel.NodeEditEvent.OnDateSelected -> {
                                // Save the date
                                templateView.setCurrentDateTimeValue(event.date)
                            }
                            is NodeEditViewModel.NodeEditEvent.OnTimeSelected -> {
                                // Save the time
                                templateView.setCurrentTimeValue(event.time)
                            }
                        }
                    }
                }
                launch {
                    mAttachmentsViewModel.attachmentEvents.collect { event ->
                        when (event) {
                            is AttachmentsViewModel.AttachmentEvent.OnBuildNewAttachment -> {
                                mDatabaseViewModel.buildNewBinaryAttachment()?.let { binaryAttachment ->
                                    mAttachmentsViewModel.onNewBinaryAttachmentBuilt(
                                        attachment = Attachment(event.fileName, binaryAttachment),
                                        allowMultipleAttachment = mAllowMultipleAttachments,
                                        attachmentToUploadUri = event.attachmentToUploadUri
                                    )
                                }
                            }
                            is AttachmentsViewModel.AttachmentEvent.OnEntryReadyForSave -> {
                                mEntryEditViewModel.saveEntryInfo(event.entryInfo)
                            }
                            is AttachmentsViewModel.AttachmentEvent.HighlightAttachment -> {
                                getAttachmentViewPosition(event.attachment) { _, position ->
                                    mEntryEditViewModel.scrollTo(position)
                                }
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    mEntryEditViewModel.onEntryValidationRequested.collect {
                        // Delete temp attachment if not completely downloaded
                        mAttachmentsViewModel.removeTempAttachmentsNotCompleted(
                            database = mDatabase,
                            entryInfo = retrieveEntryInfo()
                        )
                    }
                }
            }
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {

        templateView.populateIconMethod = { imageView, icon ->
            database.iconDrawableFactory.assignDatabaseIcon(imageView, icon, mIconColor)
        }

        mAllowMultipleAttachments = database.allowMultipleAttachments == true

        attachmentsAdapter?.binaryCache = database.binaryCache

        tagsAdapter = TagsProposalAdapter(
            requireContext(),
            database.tagPoolWithoutHistory
        )
        tagsCompletionView.apply {
            threshold = 1
            setAdapter(tagsAdapter)
        }
        tagsContainerView.visibility = if (database.allowTags()) View.VISIBLE else View.GONE
    }

    private fun assignEntryInfo(entryInfo: EntryInfo?) {
        // Populate entry views
        templateView.setEntryInfo(entryInfo)

        // Set Tags
        entryInfo?.tags?.let { tags ->
            tagsCompletionView.setText("")
            for (i in 0 until tags.size()) {
                tagsCompletionView.addObjectSync(tags.get(i).name)
            }
        }

        // Add attachments
        val attachments = entryInfo?.attachments
        attachmentsContainerView.isVisible = attachments?.isNotEmpty() ?: false
        mAttachmentsViewModel.setAttachments(
            attachments = attachments ?: listOf(),
            direction = StreamDirection.UPLOAD
        )
    }

    private fun retrieveEntryInfo(): EntryInfo {
        val entryInfo = templateView.getEntryInfo()
        entryInfo.tags = tagsCompletionView.getTags()
        entryInfo.attachments = mAttachmentsViewModel.getAttachments().toMutableList()
        return entryInfo
    }

    private fun updateFieldProtection(fieldProtection: FieldProtection) {
        templateView.setFieldProtection(fieldProtection)
    }

    /* -------------
     * Attachments
     * -------------
     */

    private fun getAttachmentViewPosition(
        attachment: EntryAttachmentState,
        position: (attachment: EntryAttachmentState, Float) -> Unit
    ) {
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
    }

}