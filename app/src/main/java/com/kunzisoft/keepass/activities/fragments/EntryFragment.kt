package com.kunzisoft.keepass.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.model.EntryAttachmentState
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.model.StreamDirection
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.timeout.ClipboardHelper
import com.kunzisoft.keepass.utils.UuidUtil
import com.kunzisoft.keepass.view.EntryFieldView
import com.kunzisoft.keepass.viewmodels.EntryViewModel
import java.util.*

class EntryFragment: DatabaseFragment() {

    private lateinit var entryFieldsContainerView: View

    private lateinit var userNameFieldView: EntryFieldView
    private lateinit var passwordFieldView: EntryFieldView
    private lateinit var otpFieldView: EntryFieldView
    private lateinit var urlFieldView: EntryFieldView
    private lateinit var notesFieldView: EntryFieldView

    private lateinit var extraFieldsContainerView: View
    private lateinit var extraFieldsListView: ViewGroup

    private lateinit var expiresDateView: TextView
    private lateinit var creationDateView: TextView
    private lateinit var modificationDateView: TextView
    private lateinit var expiresImageView: ImageView

    private lateinit var attachmentsContainerView: View
    private lateinit var attachmentsListView: RecyclerView
    private var attachmentsAdapter: EntryAttachmentsItemsAdapter? = null

    private lateinit var uuidContainerView: View
    private lateinit var uuidView: TextView
    private lateinit var uuidReferenceView: TextView

    private var mFontInVisibility: Boolean = false
    private var mHideProtectedValue: Boolean = false
    private var mIsFirstTimeAskAllowCopyPasswordAndProtectedFields: Boolean = false
    private var mAllowCopyPasswordAndProtectedFields: Boolean = false

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

        entryFieldsContainerView = view.findViewById(R.id.entry_fields_container)
        entryFieldsContainerView.visibility = View.GONE

        userNameFieldView = view.findViewById(R.id.entry_user_name_field)
        userNameFieldView.setLabel(R.string.entry_user_name)

        passwordFieldView = view.findViewById(R.id.entry_password_field)
        passwordFieldView.setLabel(R.string.entry_password)

        otpFieldView = view.findViewById(R.id.entry_otp_field)
        otpFieldView.setLabel(R.string.entry_otp)

        urlFieldView = view.findViewById(R.id.entry_url_field)
        urlFieldView.setLabel(R.string.entry_url)
        urlFieldView.setLinkAll()

        notesFieldView = view.findViewById(R.id.entry_notes_field)
        notesFieldView.setLabel(R.string.entry_notes)
        notesFieldView.setAutoLink()

        extraFieldsContainerView = view.findViewById(R.id.extra_fields_container)
        extraFieldsListView = view.findViewById(R.id.extra_fields_list)

        attachmentsContainerView = view.findViewById(R.id.entry_attachments_container)
        attachmentsListView = view.findViewById(R.id.entry_attachments_list)
        attachmentsListView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = attachmentsAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        expiresDateView = view.findViewById(R.id.entry_expires_date)
        creationDateView = view.findViewById(R.id.entry_created)
        modificationDateView = view.findViewById(R.id.entry_modified)
        expiresImageView = view.findViewById(R.id.entry_expires_image)

        uuidContainerView = view.findViewById(R.id.entry_UUID_container)
        uuidContainerView.apply {
            visibility = if (PreferencesUtil.showUUID(context)) View.VISIBLE else View.GONE
        }
        uuidView = view.findViewById(R.id.entry_UUID)
        uuidReferenceView = view.findViewById(R.id.entry_UUID_reference)

        mEntryViewModel.entryInfo.observe(viewLifecycleOwner) { entryInfo ->
            assignEntryInfo(entryInfo)
        }
    }

    override fun onResume() {
        super.onResume()

        context?.let { context ->
            mFontInVisibility = PreferencesUtil.fieldFontIsInVisibility(context)
            mHideProtectedValue = PreferencesUtil.hideProtectedValue(context)
            mIsFirstTimeAskAllowCopyPasswordAndProtectedFields =
                PreferencesUtil.isFirstTimeAskAllowCopyPasswordAndProtectedFields(context)
            mAllowCopyPasswordAndProtectedFields =
                PreferencesUtil.allowCopyPasswordAndProtectedFields(context)
        }
    }

    fun firstEntryFieldCopyView(): View? {
        return try {
            when {
                userNameFieldView.isVisible && userNameFieldView.copyButtonView.isVisible -> userNameFieldView.copyButtonView
                passwordFieldView.isVisible && passwordFieldView.copyButtonView.isVisible -> passwordFieldView.copyButtonView
                otpFieldView.isVisible && otpFieldView.copyButtonView.isVisible -> otpFieldView.copyButtonView
                urlFieldView.isVisible && urlFieldView.copyButtonView.isVisible -> urlFieldView.copyButtonView
                notesFieldView.isVisible && notesFieldView.copyButtonView.isVisible -> notesFieldView.copyButtonView
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun launchEntryCopyEducationAction() {
        val appNameString = getString(R.string.app_name)
        mClipboardHelper?.timeoutCopyToClipboard(appNameString,
                getString(R.string.copy_field, appNameString))
    }

    private fun assignEntryInfo(entryInfo: EntryInfo?) {
        context?.let { context ->

            entryInfo?.username?.let { userName ->
                assignUserName(userName) {
                    mClipboardHelper?.timeoutCopyToClipboard(userName,
                            getString(R.string.copy_field,
                                    getString(R.string.entry_user_name)))
                }
            }

            val showWarningClipboardDialogOnClickListener = View.OnClickListener {
                showClipboardDialog(entryInfo)
            }
            val onPasswordCopyClickListener: View.OnClickListener? = if (mAllowCopyPasswordAndProtectedFields) {
                View.OnClickListener {
                    entryInfo?.password?.let { password ->
                        mClipboardHelper?.timeoutCopyToClipboard(password,
                                getString(R.string.copy_field,
                                        getString(R.string.entry_password)))
                    }
                }
            } else {
                // If dialog not already shown
                if (mIsFirstTimeAskAllowCopyPasswordAndProtectedFields) {
                    showWarningClipboardDialogOnClickListener
                } else {
                    null
                }
            }
            assignPassword(entryInfo?.password,
                    mAllowCopyPasswordAndProtectedFields,
                    onPasswordCopyClickListener)

            //Assign OTP field
            entryInfo?.otpModel?.let { otpModel ->
                val otpElement = OtpElement(otpModel)
                assignOtp(otpElement) {
                    mClipboardHelper?.timeoutCopyToClipboard(
                            otpElement.token,
                            getString(R.string.copy_field, getString(R.string.entry_otp))
                    )
                }
            }

            assignURL(entryInfo?.url)
            assignNotes(entryInfo?.notes)

            // Assign custom fields
            if (mDatabase?.allowEntryCustomFields() == true) {
                clearExtraFields()
                entryInfo?.customFields?.forEach { field ->
                    val label = field.name
                    // OTP field is already managed in dedicated view
                    if (label != OtpEntryFields.OTP_TOKEN_FIELD) {
                        val value = field.protectedValue
                        val allowCopyProtectedField = !value.isProtected || mAllowCopyPasswordAndProtectedFields
                        if (allowCopyProtectedField) {
                            addExtraField(label, value, allowCopyProtectedField) {
                                mClipboardHelper?.timeoutCopyToClipboard(
                                        value.toString(),
                                        getString(R.string.copy_field,
                                                TemplateField.getLocalizedName(context, field.name))
                                )
                            }
                        } else {
                            // If dialog not already shown
                            if (mIsFirstTimeAskAllowCopyPasswordAndProtectedFields) {
                                addExtraField(label, value, allowCopyProtectedField, showWarningClipboardDialogOnClickListener)
                            } else {
                                addExtraField(label, value, allowCopyProtectedField, null)
                            }
                        }
                    }
                }
            }

            setHiddenProtectedValue(mHideProtectedValue)

            // Manage attachments
            entryInfo?.attachments?.toSet()?.let { attachments ->
                assignAttachments(attachments)
            }

            // Assign dates
            assignCreationDate(entryInfo?.creationTime)
            assignModificationDate(entryInfo?.lastModificationTime)
            setExpires(entryInfo?.expires ?: false, entryInfo?.expiryTime)

            // Assign special data
            assignUUID(entryInfo?.id)
        }
    }

    private fun showClipboardDialog(entryInfo: EntryInfo?) {
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
                        dialog.dismiss()
                        assignEntryInfo(entryInfo)
                    }
                    setButton(AlertDialog.BUTTON_NEGATIVE, getText(R.string.disable)) { dialog, _ ->
                        PreferencesUtil.setAllowCopyPasswordAndProtectedFields(context, false)
                        dialog.dismiss()
                        assignEntryInfo(entryInfo)
                    }
                    show()
                }
        }
    }

    private fun assignUserName(userName: String?,
                               onClickListener: View.OnClickListener?) {
        userNameFieldView.apply {
            if (userName != null && userName.isNotEmpty()) {
                visibility = View.VISIBLE
                setValue(userName)
                applyFontVisibility(mFontInVisibility)
                showOrHideEntryFieldsContainer(false)
            } else {
                visibility = View.GONE
            }
            assignCopyButtonClickListener(onClickListener)
        }
    }

    private fun assignPassword(password: String?,
                       allowCopyPassword: Boolean,
                       onClickListener: View.OnClickListener?) {
        passwordFieldView.apply {
            if (password != null && password.isNotEmpty()) {
                visibility = View.VISIBLE
                setValue(password, true)
                applyFontVisibility(mFontInVisibility)
                activateCopyButton(allowCopyPassword)
                showOrHideEntryFieldsContainer(false)
            } else {
                visibility = View.GONE
            }
            assignCopyButtonClickListener(onClickListener)
        }
    }

    private fun assignOtp(otpElement: OtpElement?,
                          onClickListener: View.OnClickListener) {
        otpFieldView.removeCallbacks(mOtpRunnable)

        if (otpElement != null) {
            otpFieldView.visibility = View.VISIBLE

            if (otpElement.token.isEmpty()) {
                otpFieldView.setValue(R.string.error_invalid_OTP)
                otpFieldView.activateCopyButton(false)
                otpFieldView.assignCopyButtonClickListener(null)
            } else {
                otpFieldView.setLabel(otpElement.type.name)
                otpFieldView.setValue(otpElement.token)
                otpFieldView.assignCopyButtonClickListener(onClickListener)

                mOtpRunnable = Runnable {
                    if (otpElement.shouldRefreshToken()) {
                        otpFieldView.setValue(otpElement.token)
                    }
                    mEntryViewModel.onOtpElementUpdated(otpElement)
                    otpFieldView.postDelayed(mOtpRunnable, 1000)
                }
                mEntryViewModel.onOtpElementUpdated(otpElement)
                otpFieldView.post(mOtpRunnable)
            }
            showOrHideEntryFieldsContainer(false)
        } else {
            otpFieldView.visibility = View.GONE
        }
    }

    private fun assignURL(url: String?) {
        urlFieldView.apply {
            if (url != null && url.isNotEmpty()) {
                visibility = View.VISIBLE
                setValue(url)
                showOrHideEntryFieldsContainer(false)
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun assignNotes(notes: String?) {
        notesFieldView.apply {
            if (notes != null && notes.isNotEmpty()) {
                visibility = View.VISIBLE
                setValue(notes)
                applyFontVisibility(mFontInVisibility)
                showOrHideEntryFieldsContainer(false)
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun setExpires(isExpires: Boolean, expiryTime: DateInstant?) {
        expiresImageView.visibility = if (isExpires) View.VISIBLE else View.GONE
        expiresDateView.text = if (isExpires) {
            expiryTime?.getDateTimeString(resources)
        } else {
            resources.getString(R.string.never)
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

    private fun setHiddenProtectedValue(hiddenProtectedValue: Boolean) {
        passwordFieldView.hiddenProtectedValue = hiddenProtectedValue
        // Hidden style for custom fields
        extraFieldsListView.let {
            for (i in 0 until it.childCount) {
                val childCustomView = it.getChildAt(i)
                if (childCustomView is EntryFieldView)
                    childCustomView.hiddenProtectedValue = hiddenProtectedValue
            }
        }
    }

    private fun showOrHideEntryFieldsContainer(hide: Boolean) {
        entryFieldsContainerView.visibility = if (hide) View.GONE else View.VISIBLE
    }

    /* -------------
     * Extra Fields
     * -------------
     */

    private fun showOrHideExtraFieldsContainer(hide: Boolean) {
        extraFieldsContainerView.visibility = if (hide) View.GONE else View.VISIBLE
    }

    private fun addExtraField(title: String,
                              value: ProtectedString,
                              allowCopy: Boolean,
                              onCopyButtonClickListener: View.OnClickListener?) {
        context?.let { context ->
            extraFieldsListView.addView(EntryFieldView(context).apply {
                setLabel(TemplateField.getLocalizedName(context, title))
                setValue(value.toString(), value.isProtected)
                setAutoLink()
                activateCopyButton(allowCopy)
                assignCopyButtonClickListener(onCopyButtonClickListener)
                applyFontVisibility(mFontInVisibility)
            })

            showOrHideExtraFieldsContainer(false)
        }
    }

    private fun clearExtraFields() {
        extraFieldsListView.removeAllViews()
        showOrHideExtraFieldsContainer(true)
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

    companion object {

        fun getInstance(): EntryFragment {
            return EntryFragment().apply {
                arguments = Bundle()
            }
        }
    }
}
