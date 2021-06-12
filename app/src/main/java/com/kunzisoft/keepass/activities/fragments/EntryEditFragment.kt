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

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.dialogs.SetOTPDialogFragment
import com.kunzisoft.keepass.activities.lock.resetAppTimeoutWhenViewFocusedOrChanged
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.education.EntryEditActivityEducation
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.model.AttachmentState
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.view.TemplateView
import com.kunzisoft.keepass.view.collapse
import com.kunzisoft.keepass.view.expand
import com.kunzisoft.keepass.viewmodels.EntryEditViewModel

class EntryEditFragment: DatabaseFragment(), SetOTPDialogFragment.CreateOtpListener {

    private var iconColor: Int = 0

    var drawFactory: IconDrawableFactory? = null

    private val mEntryEditViewModel: EntryEditViewModel by activityViewModels()

    private lateinit var templateView: TemplateView
    private lateinit var attachmentsContainerView: ViewGroup
    private lateinit var attachmentsListView: RecyclerView
    private var attachmentsAdapter: EntryAttachmentsItemsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        // TODO mTempDateTimeViewId = savedInstanceState.getInt(KEY_SELECTION_DATE_TIME_ID)

        val rootView = inflater.cloneInContext(contextThemed)
                .inflate(R.layout.fragment_entry_edit, container, false)

        templateView = rootView.findViewById(R.id.template_view)
        templateView.populateIconMethod = { imageView, icon ->
            drawFactory?.assignDatabaseIcon(imageView, icon, iconColor)
        }
        attachmentsContainerView = rootView.findViewById(R.id.entry_attachments_container)
        attachmentsListView = rootView.findViewById(R.id.entry_attachments_list)

        rootView.resetAppTimeoutWhenViewFocusedOrChanged(requireContext(), mDatabase)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the textColor to tint the icon
        val taIconColor = contextThemed?.theme?.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        iconColor = taIconColor?.getColor(0, Color.WHITE) ?: Color.WHITE
        taIconColor?.recycle()

        templateView.apply {
            setOnIconClickListener {
                mEntryEditViewModel.requestIconSelection(templateView.getIcon())
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

        attachmentsAdapter = EntryAttachmentsItemsAdapter(requireContext())
        attachmentsAdapter?.database = mDatabase
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

        mEntryEditViewModel.onTemplateChanged.observe(viewLifecycleOwner) { template ->
            templateView.buildTemplate(template)
        }

        mEntryEditViewModel.entryInfoLoaded.observe(viewLifecycleOwner) { entryInfo ->
            templateView.setEntryInfo(entryInfo)
            assignAttachments(entryInfo.attachments, StreamDirection.UPLOAD) { attachment ->
                removeAttachment(EntryAttachmentState(attachment, StreamDirection.DOWNLOAD))
                mEntryEditViewModel.deleteAttachment(attachment)
            }
        }

        mEntryEditViewModel.requestEntryInfoUpdate.observe(viewLifecycleOwner) {
            mEntryEditViewModel.saveEntryInfo(retrieveEntryInfo())
        }

        mEntryEditViewModel.onIconSelected.observe(viewLifecycleOwner) { iconImage ->
            templateView.setIcon(iconImage)
        }

        mEntryEditViewModel.onPasswordSelected.observe(viewLifecycleOwner) { passwordField ->
            templateView.setPasswordValue(passwordField)
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

        mEntryEditViewModel.onAttachmentAction.observe(viewLifecycleOwner) { entryAttachmentState ->
            when (entryAttachmentState?.downloadState) {
                AttachmentState.START -> {
                    putAttachment(entryAttachmentState)
                    getAttachmentViewPosition(entryAttachmentState) {
                        mEntryEditViewModel.binaryPreviewLoaded(entryAttachmentState, it)
                    }
                }
                AttachmentState.IN_PROGRESS -> {
                    putAttachment(entryAttachmentState)
                }
                AttachmentState.COMPLETE -> {
                    putAttachment(entryAttachmentState) {
                        getAttachmentViewPosition(entryAttachmentState) {
                            mEntryEditViewModel.binaryPreviewLoaded(entryAttachmentState, it)
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

    override fun onResume() {
        super.onResume()

        // TODO fontInVisibility = PreferencesUtil.fieldFontIsInVisibility(requireContext())
    }

    override fun onPause() {
        super.onPause()
        mEntryEditViewModel.updateEntryInfo(templateView.getEntryInfo())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        drawFactory = mDatabase?.iconDrawableFactory
    }

    override fun onDetach() {
        super.onDetach()

        drawFactory = null
    }

    private fun retrieveEntryInfo(): EntryInfo {
        val entryInfo = templateView.getEntryInfo()
        entryInfo.attachments = getAttachments()
        return entryInfo
    }

    fun generatePasswordEducationPerformed(entryEditActivityEducation: EntryEditActivityEducation): Boolean {
        /* TODO
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
        */
        return false
    }

    /* -------------
     * OTP
     * -------------
     */

    fun setupOtp() {
        // Retrieve the current otpElement if exists
        // and open the dialog to set up the OTP
        /*
        SetOTPDialogFragment.build(mEntryInfo.otpModel)
                .show(parentFragmentManager, "addOTPDialog")
                TODO OTP
         */
    }

    override fun onOtpCreated(otpElement: OtpElement) {
        // Update the otp field with otpauth:// url
        /*
        TODO OTP
        val otpField = OtpEntryFields.buildOtpField(otpElement, mEntryInfo.title, mEntryInfo.username)
        putCustomField(Field(otpField.name, otpField.protectedValue))
        */
    }

    /* -------------
     * Attachments
     * -------------
     */

    private fun getAttachments(): List<Attachment> {
        return attachmentsAdapter?.itemsList?.map { it.attachment } ?: listOf()
    }

    private fun assignAttachments(attachments: List<Attachment>,
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

    private fun putAttachment(attachment: EntryAttachmentState,
                              onPreviewLoaded: (() -> Unit)? = null) {
        // When only one attachment is allowed
        if (mDatabase?.allowMultipleAttachments == false) {
            clearAttachments()
        }
        attachmentsContainerView.visibility = View.VISIBLE
        attachmentsAdapter?.putItem(attachment)
        attachmentsAdapter?.onBinaryPreviewLoaded = {
            onPreviewLoaded?.invoke()
        }
    }

    private fun removeAttachment(attachment: EntryAttachmentState) {
        attachmentsAdapter?.removeItem(attachment)
    }

    private fun clearAttachments() {
        attachmentsAdapter?.clear()
    }

    private fun getAttachmentViewPosition(attachment: EntryAttachmentState, position: (Float) -> Unit) {
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
        //mEntryEditViewModel.loadEntryInfo(retrieveEntryInfo())

        super.onSaveInstanceState(outState)
    }

    companion object {
        private val TAG = EntryEditFragment::class.java.name
    }

}