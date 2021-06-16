package com.kunzisoft.keepass.activities.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.utils.UuidUtil
import com.kunzisoft.keepass.view.EntryFieldView
import com.kunzisoft.keepass.view.TemplateView
import com.kunzisoft.keepass.viewmodels.EntryViewModel
import java.util.*

class EntryFragment: DatabaseFragment() {

    private lateinit var templateView: TemplateView

    private lateinit var creationDateView: TextView
    private lateinit var modificationDateView: TextView

    private lateinit var attachmentsContainerView: View
    private lateinit var attachmentsListView: RecyclerView
    private var attachmentsAdapter: EntryAttachmentsItemsAdapter? = null

    private lateinit var uuidContainerView: View
    private lateinit var uuidView: TextView
    private lateinit var uuidReferenceView: TextView

    private var mOtpRunnable: Runnable? = null
    private var mClipboardHelper: ClipboardHelper? = null

    private val mEntryViewModel: EntryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.cloneInContext(contextThemed)
            .inflate(R.layout.fragment_entry, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        context?.let { context ->
            mClipboardHelper = ClipboardHelper(context)
            attachmentsAdapter = EntryAttachmentsItemsAdapter(context)
            attachmentsAdapter?.database = mDatabase
        }

        templateView = view.findViewById(R.id.entry_template)

        attachmentsContainerView = view.findViewById(R.id.entry_attachments_container)
        attachmentsListView = view.findViewById(R.id.entry_attachments_list)
        attachmentsListView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = attachmentsAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        creationDateView = view.findViewById(R.id.entry_created)
        modificationDateView = view.findViewById(R.id.entry_modified)

        uuidContainerView = view.findViewById(R.id.entry_UUID_container)
        uuidContainerView.apply {
            visibility = if (PreferencesUtil.showUUID(context)) View.VISIBLE else View.GONE
        }
        uuidView = view.findViewById(R.id.entry_UUID)
        uuidReferenceView = view.findViewById(R.id.entry_UUID_reference)

        mEntryViewModel.template.observe(viewLifecycleOwner) { template ->
            templateView.setTemplate(template)
        }

        mEntryViewModel.entryInfo.observe(viewLifecycleOwner) { entryInfo ->
            assignEntryInfo(entryInfo)
        }

        mEntryViewModel.onAttachmentAction.observe(viewLifecycleOwner) { entryAttachmentState ->
            entryAttachmentState?.let {
                if (it.streamDirection != StreamDirection.UPLOAD) {
                    putAttachment(it)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        loadTemplateSettings()
    }

    private fun loadTemplateSettings() {
        context?.let { context ->
            templateView.setFontInVisibility(PreferencesUtil.fieldFontIsInVisibility(context))
            templateView.setHideProtectedValue(PreferencesUtil.hideProtectedValue(context))
            templateView.setFirstTimeAskAllowCopyProtectedFields(PreferencesUtil.isFirstTimeAskAllowCopyProtectedFields(context))
            templateView.setAllowCopyProtectedFields(PreferencesUtil.allowCopyProtectedFields(context))
        }
    }

    private fun assignEntryInfo(entryInfo: EntryInfo?) {

        // Set copy buttons
        templateView.apply {
            setOnAskCopySafeClickListener {
                showClipboardDialog()
            }

            setOnCopyActionClickListener { field ->
                mClipboardHelper?.timeoutCopyToClipboard(
                    field.protectedValue.stringValue,
                    getString(
                        R.string.copy_field,
                        TemplateField.getLocalizedName(context, field.name)
                    )
                )
            }
        }

        // Populate entry views
        templateView.setEntryInfo(entryInfo)

        //Assign OTP field
        assignOtp(entryInfo)

        // Manage attachments
        entryInfo?.attachments?.toSet()?.let { attachments ->
            assignAttachments(attachments)
        }

        // Assign dates
        assignCreationDate(entryInfo?.creationTime)
        assignModificationDate(entryInfo?.lastModificationTime)

        // Assign special data
        assignUUID(entryInfo?.id)
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

    private fun assignOtp(entryInfo: EntryInfo?) {
        entryInfo?.otpModel?.let { otpModel ->
            val otpElement = OtpElement(otpModel)
            templateView.getOtpTokenView()?.let { otpFieldView ->
                otpFieldView.removeCallbacks(mOtpRunnable)
                if (otpElement.token.isEmpty()) {
                    otpFieldView.setLabel(R.string.entry_otp)
                    otpFieldView.setValue(R.string.error_invalid_OTP)
                    otpFieldView.setCopyButtonState(EntryFieldView.ButtonState.GONE)
                } else {
                    otpFieldView.label = otpElement.type.name
                    otpFieldView.value = otpElement.token
                    otpFieldView.setCopyButtonState(EntryFieldView.ButtonState.ACTIVATE)
                    otpFieldView.setCopyButtonClickListener {
                        mClipboardHelper?.timeoutCopyToClipboard(
                            otpElement.token,
                            getString(R.string.copy_field, getString(R.string.entry_otp))
                        )
                    }
                    mOtpRunnable = Runnable {
                        if (otpElement.shouldRefreshToken()) {
                            otpFieldView.value = otpElement.token
                        }
                        mEntryViewModel.onOtpElementUpdated(otpElement)
                        otpFieldView.postDelayed(mOtpRunnable, 1000)
                    }
                    mEntryViewModel.onOtpElementUpdated(otpElement)
                    otpFieldView.post(mOtpRunnable)
                }
            }
        }
    }

    private fun assignCreationDate(date: DateInstant?) {
        creationDateView.text = date?.getDateTimeString(resources)
    }

    private fun assignModificationDate(date: DateInstant?) {
        modificationDateView.text = date?.getDateTimeString(resources)
    }

    private fun assignUUID(uuid: UUID?) {
        uuidView.text = uuid?.toString()
        uuidReferenceView.text = UuidUtil.toHexString(uuid)
    }

    /* -------------
     * Attachments
     * -------------
     */

    private fun showAttachments(show: Boolean) {
        attachmentsContainerView.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun assignAttachments(attachments: Set<Attachment>) {
        showAttachments(attachments.isNotEmpty())
        attachmentsAdapter?.assignItems(attachments.map { EntryAttachmentState(it, StreamDirection.DOWNLOAD) })
        attachmentsAdapter?.onItemClickListener = { item ->
            mEntryViewModel.onAttachmentSelected(item.attachment)
        }
    }

    fun putAttachment(attachmentToDownload: EntryAttachmentState) {
        attachmentsAdapter?.putItem(attachmentToDownload)
    }

    /* -------------
     * Education
     * -------------
     */

    fun firstEntryFieldCopyView(): View? {
        return try {
            templateView.getActionImageView()
        } catch (e: Exception) {
            null
        }
    }

    fun launchEntryCopyEducationAction() {
        val appNameString = getString(R.string.app_name)
        mClipboardHelper?.timeoutCopyToClipboard(appNameString,
            getString(R.string.copy_field, appNameString))
    }

    companion object {

        fun getInstance(): EntryFragment {
            return EntryFragment().apply {
                arguments = Bundle()
            }
        }
    }
}
