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
 */
package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.adapters.EntryAttachmentsItemsAdapter
import com.kunzisoft.keepass.adapters.EntryHistoryAdapter
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Entry
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.search.UuidUtil
import com.kunzisoft.keepass.model.EntryAttachment
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpType
import java.util.*


class EntryContentsView @JvmOverloads constructor(context: Context,
                                                  var attrs: AttributeSet? = null,
                                                  var defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private var fontInVisibility: Boolean = false

    private val userNameFieldView: EntryField
    private val passwordFieldView: EntryField
    private val otpFieldView: EntryField
    private val urlFieldView: EntryField
    private val notesFieldView: EntryField

    private var otpRunnable: Runnable? = null

    private val extraFieldsContainerView: View
    private val extraFieldsListView: ViewGroup

    private val creationDateView: TextView
    private val modificationDateView: TextView
    private val lastAccessDateView: TextView
    private val expiresImageView: ImageView
    private val expiresDateView: TextView

    private val attachmentsContainerView: View
    private val attachmentsListView: RecyclerView
    private val attachmentsAdapter = EntryAttachmentsItemsAdapter(context, false)

    private val historyContainerView: View
    private val historyListView: RecyclerView
    private val historyAdapter = EntryHistoryAdapter(context)

    private val uuidView: TextView
    private val uuidReferenceView: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_entry_contents, this)

        userNameFieldView = findViewById(R.id.entry_user_name_field)
        userNameFieldView.setLabel(R.string.entry_user_name)

        passwordFieldView = findViewById(R.id.entry_password_field)
        passwordFieldView.setLabel(R.string.entry_password)

        otpFieldView = findViewById(R.id.entry_otp_field)
        otpFieldView.setLabel(R.string.entry_otp)

        urlFieldView = findViewById(R.id.entry_url_field)
        urlFieldView.setLabel(R.string.entry_url)
        urlFieldView.setValueAutoLink(true)

        notesFieldView = findViewById(R.id.entry_notes_field)
        notesFieldView.setLabel(R.string.entry_notes)
        notesFieldView.setValueAutoLink(true)

        extraFieldsContainerView = findViewById(R.id.extra_fields_container)
        extraFieldsListView = findViewById(R.id.extra_fields_list)

        attachmentsContainerView = findViewById(R.id.entry_attachments_container)
        attachmentsListView = findViewById(R.id.entry_attachments_list)
        attachmentsListView?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = attachmentsAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        creationDateView = findViewById(R.id.entry_created)
        modificationDateView = findViewById(R.id.entry_modified)
        lastAccessDateView = findViewById(R.id.entry_accessed)
        expiresImageView = findViewById(R.id.entry_expires_image)
        expiresDateView = findViewById(R.id.entry_expires_date)

        historyContainerView = findViewById(R.id.entry_history_container)
        historyListView = findViewById(R.id.entry_history_list)
        historyListView?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
            adapter = historyAdapter
        }

        uuidView = findViewById(R.id.entry_UUID)
        uuidReferenceView = findViewById(R.id.entry_UUID_reference)
    }

    fun applyFontVisibilityToFields(fontInVisibility: Boolean) {
        this.fontInVisibility = fontInVisibility
    }

    fun assignUserName(userName: String?,
                       onClickListener: OnClickListener?) {
        userNameFieldView.apply {
            if (userName != null && userName.isNotEmpty()) {
                visibility = View.VISIBLE
                setValue(userName)
                applyFontVisibility(fontInVisibility)
            } else {
                visibility = View.GONE
            }
            assignCopyButtonClickListener(onClickListener)
        }
    }

    fun assignPassword(password: String?,
                       allowCopyPassword: Boolean,
                       onClickListener: OnClickListener?) {
        passwordFieldView.apply {
            if (password != null && password.isNotEmpty()) {
                visibility = View.VISIBLE
                setValue(password, true)
                applyFontVisibility(fontInVisibility)
                activateCopyButton(allowCopyPassword)
            }else {
                visibility = View.GONE
            }
            assignCopyButtonClickListener(onClickListener)
        }
    }

    fun assignOtp(otpElement: OtpElement?,
                  otpProgressView: ProgressBar?,
                  onClickListener: OnClickListener) {
        otpFieldView.removeCallbacks(otpRunnable)

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

                when (otpElement.type) {
                    // Only add token if HOTP
                    OtpType.HOTP -> {
                        otpProgressView?.visibility = View.GONE
                    }
                    // Refresh view if TOTP
                    OtpType.TOTP -> {
                        otpProgressView?.apply {
                            max = otpElement.period
                            progress = otpElement.secondsRemaining
                            visibility = View.VISIBLE
                        }
                        otpRunnable = Runnable {
                            if (otpElement.shouldRefreshToken()) {
                                otpFieldView.setValue(otpElement.token)
                            }
                            otpProgressView?.progress = otpElement.secondsRemaining
                            otpFieldView.postDelayed(otpRunnable, 1000)
                        }
                        otpFieldView.post(otpRunnable)
                    }
                }
            }
        } else {
            otpFieldView.visibility = View.GONE
            otpProgressView?.visibility = View.GONE
        }
    }

    fun assignURL(url: String?) {
        urlFieldView.apply {
            if (url != null && url.isNotEmpty()) {
                visibility = View.VISIBLE
                setValue(url)
            } else {
                visibility = View.GONE
            }
        }
    }

    fun assignNotes(notes: String?) {
        notesFieldView.apply {
            if (notes != null && notes.isNotEmpty()) {
                visibility = View.VISIBLE
                setValue(notes)
                applyFontVisibility(fontInVisibility)
            } else {
                visibility = View.GONE
            }
        }
    }

    fun assignCreationDate(date: DateInstant) {
        creationDateView.text = date.getDateTimeString(resources)
    }

    fun assignModificationDate(date: DateInstant) {
        modificationDateView.text = date.getDateTimeString(resources)
    }

    fun assignLastAccessDate(date: DateInstant) {
        lastAccessDateView.text = date.getDateTimeString(resources)
    }

    fun setExpires(isExpires: Boolean) {
        expiresImageView.visibility = if (isExpires) View.VISIBLE else View.GONE
    }

    fun assignExpiresDate(date: DateInstant) {
        assignExpiresDate(date.getDateTimeString(resources))
    }

    fun assignExpiresDate(constString: String) {
        expiresDateView.text = constString
    }

    fun assignUUID(uuid: UUID) {
        uuidView.text = uuid.toString()
        uuidReferenceView.text = UuidUtil.toHexString(uuid)
    }


    fun setHiddenProtectedValue(hiddenProtectedValue: Boolean) {
        passwordFieldView.hiddenProtectedValue = hiddenProtectedValue
        // Hidden style for custom fields
        extraFieldsListView.let {
            for (i in 0 until it.childCount) {
                val childCustomView = it.getChildAt(i)
                if (childCustomView is EntryField)
                    childCustomView.hiddenProtectedValue = hiddenProtectedValue
            }
        }
    }

    /* -------------
     * Extra Fields
     * -------------
     */

    private fun showOrHideExtraFieldsContainer(hide: Boolean) {
        extraFieldsContainerView.visibility = if (hide) View.GONE else View.VISIBLE
    }

    fun addExtraField(title: String,
                      value: ProtectedString,
                      allowCopy: Boolean,
                      onCopyButtonClickListener: OnClickListener?) {

        val entryCustomField: EntryField? = EntryField(context, attrs, defStyle)
        entryCustomField?.apply {
            setLabel(title)
            setValue(value.toString(), value.isProtected)
            setValueAutoLink(true)
            activateCopyButton(allowCopy)
            assignCopyButtonClickListener(onCopyButtonClickListener)
            applyFontVisibility(fontInVisibility)
        }
        entryCustomField?.let {
            extraFieldsListView.addView(it)
        }
        showOrHideExtraFieldsContainer(false)
    }

    fun clearExtraFields() {
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

    fun assignAttachments(attachments: ArrayList<EntryAttachment>,
                          onAttachmentClicked: (attachment: EntryAttachment)->Unit) {
        showAttachments(attachments.isNotEmpty())
        attachmentsAdapter.assignItems(attachments)
        attachmentsAdapter.onItemClickListener = { item ->
            onAttachmentClicked.invoke(item)
        }
    }

    fun updateAttachmentDownloadProgress(attachmentToDownload: EntryAttachment) {
        attachmentsAdapter.updateProgress(attachmentToDownload)
    }

    /* -------------
     * History
     * -------------
     */

    fun assignHistory(history: ArrayList<Entry>, action: (historyItem: Entry, position: Int)->Unit) {
        historyAdapter.clear()
        historyAdapter.entryHistoryList.addAll(history)
        historyAdapter.onItemClickListener = { item, position ->
            action.invoke(item, position)
        }
        historyContainerView.visibility = if (historyAdapter.entryHistoryList.isEmpty())
            View.GONE
        else
            View.VISIBLE
        historyAdapter.notifyDataSetChanged()
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }
}
