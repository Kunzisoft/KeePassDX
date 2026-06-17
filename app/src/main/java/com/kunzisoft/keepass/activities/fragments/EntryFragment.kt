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

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.FieldProtection
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.TimeUtil.getDateTimeString
import com.kunzisoft.keepass.view.TemplateView
import com.kunzisoft.keepass.view.collapse
import com.kunzisoft.keepass.view.expand
import com.kunzisoft.keepass.view.hideByFading
import com.kunzisoft.keepass.view.showByFading
import com.kunzisoft.keepass.viewmodels.AttachmentsViewModel
import com.kunzisoft.keepass.viewmodels.EntryViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class EntryFragment: DatabaseFragment() {

    private lateinit var rootView: View
    private lateinit var mainSection: View
    private lateinit var advancedSection: View

    private lateinit var templateView: TemplateView

    private lateinit var creationDateView: TextView
    private lateinit var modificationDateView: TextView

    private lateinit var attachmentsContainerView: View
    private lateinit var attachmentsListView: RecyclerView
    private var attachmentsAdapter: EntryAttachmentsItemsAdapter? = null

    private lateinit var customDataView: TextView

    private lateinit var uuidContainerView: View
    private lateinit var uuidReferenceView: TextView

    private val mEntryViewModel: EntryViewModel by activityViewModels()
    private val mAttachmentsViewModel: AttachmentsViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_entry, container, false)
    }
    
    override fun onViewCreated(view: View,
                               savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view
        // Hide only the first time
        if (savedInstanceState == null) {
            view.isVisible = false
        }

        mainSection = view.findViewById(R.id.entry_section_main)
        advancedSection = view.findViewById(R.id.entry_section_advanced)

        templateView = view.findViewById(R.id.entry_template)
        loadTemplateSettings()

        templateView.apply {
            // Set copy buttons
            setOnChangeFieldProtectionClickListener { fieldProtection ->
                mEntryViewModel.requestChangeFieldProtection(fieldProtection)
            }
            setOnAskCopySafeClickListener {
                showClipboardDialog()
            }
            setOnCopyActionClickListener { fieldProtection ->
                mEntryViewModel.requestCopyField(fieldProtection)
            }
            // OTP timer updated
            onOtpUpdatedListener = mEntryViewModel::onOtpElementUpdated
        }

        attachmentsContainerView = view.findViewById(R.id.entry_attachments_container)
        attachmentsListView = view.findViewById(R.id.entry_attachments_list)
        attachmentsListView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        creationDateView = view.findViewById(R.id.entry_created)
        modificationDateView = view.findViewById(R.id.entry_modified)

        // TODO Custom data
        // customDataView = view.findViewById(R.id.entry_custom_data)

        uuidContainerView = view.findViewById(R.id.entry_UUID_container)
        uuidContainerView.apply {
            visibility = if (PreferencesUtil.showUUID(context)) View.VISIBLE else View.GONE
        }
        uuidReferenceView = view.findViewById(R.id.entry_UUID_reference)

        context?.let { context ->
            attachmentsAdapter = EntryAttachmentsItemsAdapter(context)
            attachmentsAdapter?.onItemClickListener = { item ->
                mEntryViewModel.onAttachmentSelected(item.attachment)
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
            attachmentsListView.adapter = attachmentsAdapter
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mEntryViewModel.entryUIState
                        .map { it.entryInfo }
                        .distinctUntilChanged()
                        .collect { entryInfo ->
                            entryInfo?.let {
                                assignEntryInfo(it)
                                // Smooth appearing
                                rootView.showByFading()
                            }
                        }
                }
                launch {
                    mAttachmentsViewModel.attachmentsUIState.collect { state ->
                        attachmentsAdapter?.assignItems(state.attachments)
                    }
                }
                launch {
                    mEntryViewModel.entryEvents.collect { event ->
                        when (event) {
                            is EntryViewModel.EntryEvent.EntryLoaded -> {
                                resetAppTimeoutWhenViewFocusedOrChanged(rootView)
                            }
                            is EntryViewModel.EntryEvent.SectionSelected -> {
                                when (event.section) {
                                    EntryViewModel.EntrySection.MAIN -> {
                                        mainSection.showByFading()
                                        advancedSection.hideByFading()
                                    }
                                    EntryViewModel.EntrySection.ADVANCED -> {
                                        mainSection.hideByFading()
                                        advancedSection.showByFading()
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    mEntryViewModel.onFieldProtectionUpdated.collect { fieldProtection ->
                        updateField(fieldProtection)
                    }
                }
            }
        }
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase) {
        attachmentsAdapter?.binaryCache = database.binaryCache
    }

    private fun loadTemplateSettings() {
        context?.let { context ->
            templateView.setFirstTimeAskAllowCopyProtectedFields(PreferencesUtil.isFirstTimeAskAllowCopyProtectedFields(context))
            templateView.setAllowCopyProtectedFields(PreferencesUtil.allowCopyProtectedFields(context))
        }
    }

    private fun assignEntryInfo(entryInfo: EntryInfo) {
        // Set template
        templateView.setTemplate(entryInfo.template)

        // Populate entry views
        templateView.setEntryInfo(entryInfo)

        // Assign attachments
        val attachments = entryInfo.attachments
        attachmentsContainerView.isVisible = attachments.isNotEmpty()
        mAttachmentsViewModel.setAttachments(
            attachments = attachments,
            direction = StreamDirection.DOWNLOAD
        )

        // Assign dates
        creationDateView.text = entryInfo.creationTime.getDateTimeString(resources)
        modificationDateView.text = entryInfo.lastModificationTime.getDateTimeString(resources)

        // TODO Custom data
        // customDataView.text = entryInfo.customData.toString()

        // Assign special data
        uuidReferenceView.text = entryInfo.nodeId.toString()
    }

    fun updateField(field: FieldProtection) {
        templateView.setFieldProtection(field)
    }

    private fun showClipboardDialog() {
        context?.let {
            AlertDialog.Builder(it)
                .setMessage(
                    getString(R.string.allow_copy_password_warning) +
                            "\n\n" +
                            getString(R.string.clipboard_warning)
                )
                .create().apply {
                    setButton(AlertDialog.BUTTON_POSITIVE, getText(R.string.enable)) { dialog, _ ->
                        PreferencesUtil.setAllowCopyPasswordAndProtectedFields(context, true)
                        finishDialog(dialog)
                    }
                    setButton(AlertDialog.BUTTON_NEGATIVE, getText(R.string.disable)) { dialog, _ ->
                        PreferencesUtil.setAllowCopyPasswordAndProtectedFields(context, false)
                        finishDialog(dialog)
                    }
                    show()
                }
        }
    }

    private fun finishDialog(dialog: DialogInterface) {
        dialog.dismiss()
        loadTemplateSettings()
        templateView.reload()
    }

    /* -------------
     * Education
     * -------------
     */

    fun firstEntryFieldCopyView(): View? {
        return try {
            templateView.getActionImageView()
        } catch (_: Exception) {
            null
        }
    }

    fun launchEntryCopyEducationAction() {
        val appNameString = getString(R.string.app_name)
        mEntryViewModel.copyToClipboard(appNameString)
    }

    companion object {

        fun getInstance(): EntryFragment {
            return EntryFragment().apply {
                arguments = Bundle()
            }
        }
    }
}
