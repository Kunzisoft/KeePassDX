/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kunzisoft.keepass.activities.view

import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.utils.Util
import java.text.DateFormat
import java.util.*

class EntryContentsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private var fontInVisibility: Boolean = false
    private val colorAccent: Int

    private var userNameContainerView: View? = null
    private var userNameView: TextView? = null
    private var userNameActionView: ImageView? = null

    private var passwordContainerView: View? = null
    private var passwordView: TextView? = null
    private var passwordActionView: ImageView? = null

    private var urlContainerView: View? = null
    private var urlView: TextView? = null

    private var commentContainerView: View? = null
    private var commentView: TextView? = null

    private var extrasView: ViewGroup? = null

    private val dateFormat: DateFormat
    private val timeFormat: DateFormat

    private var creationDateView: TextView? = null
    private var modificationDateView: TextView? = null
    private var lastAccessDateView: TextView? = null
    private var expiresDateView: TextView? = null

    val isUserNamePresent: Boolean
        get() = userNameContainerView!!.visibility == View.VISIBLE

    val isPasswordPresent: Boolean
        get() = passwordContainerView!!.visibility == View.VISIBLE

    init {

        fontInVisibility = false

        dateFormat = android.text.format.DateFormat.getDateFormat(context)
        timeFormat = android.text.format.DateFormat.getTimeFormat(context)

        inflate(context)

        val attrColorAccent = intArrayOf(R.attr.colorAccentCompat)
        val taColorAccent = context.theme.obtainStyledAttributes(attrColorAccent)
        colorAccent = taColorAccent.getColor(0, Color.BLACK)
        taColorAccent.recycle()
    }

    private fun inflate(context: Context) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.entry_view_contents, this)

        userNameContainerView = findViewById(R.id.entry_user_name_container)
        userNameView = findViewById(R.id.entry_user_name)
        userNameActionView = findViewById(R.id.entry_user_name_action_image)

        passwordContainerView = findViewById(R.id.entry_password_container)
        passwordView = findViewById(R.id.entry_password)
        passwordActionView = findViewById(R.id.entry_password_action_image)

        urlContainerView = findViewById(R.id.entry_url_container)
        urlView = findViewById(R.id.entry_url)

        commentContainerView = findViewById(R.id.entry_notes_container)
        commentView = findViewById(R.id.entry_notes)

        extrasView = findViewById(R.id.extra_strings)

        creationDateView = findViewById(R.id.entry_created)
        modificationDateView = findViewById(R.id.entry_modified)
        lastAccessDateView = findViewById(R.id.entry_accessed)
        expiresDateView = findViewById(R.id.entry_expires)
    }

    fun applyFontVisibilityToFields(fontInVisibility: Boolean) {
        this.fontInVisibility = fontInVisibility
    }

    fun assignUserName(userName: String?) {
        if (userName != null && userName.isNotEmpty()) {
            userNameContainerView?.visibility = View.VISIBLE
            userNameView?.apply {
                text = userName
                if (fontInVisibility)
                    Util.applyFontVisibilityTo(context, this)
            }
        } else {
            userNameContainerView?.visibility = View.GONE
        }
    }

    fun assignUserNameCopyListener(onClickListener: OnClickListener) {
        userNameActionView?.setOnClickListener(onClickListener)
    }

    fun assignPassword(password: String?, allowCopyPassword: Boolean) {
        if (password != null && password.isNotEmpty()) {
            passwordContainerView?.visibility = View.VISIBLE
            passwordView?.apply {
                text = password
                if (fontInVisibility)
                    Util.applyFontVisibilityTo(context, this)
            }
            if (!allowCopyPassword) {
                passwordActionView?.setColorFilter(ContextCompat.getColor(context, R.color.grey_dark))
            } else {
                passwordActionView?.setColorFilter(colorAccent)
            }
        } else {
            passwordContainerView?.visibility = View.GONE
        }
    }

    fun assignPasswordCopyListener(onClickListener: OnClickListener?) {
        if (onClickListener == null)
            isClickable = false
        passwordActionView?.setOnClickListener(onClickListener)
    }

    fun atLeastOneFieldProtectedPresent(): Boolean {
        extrasView?.let {
            for (i in 0 until it.childCount) {
                val childCustomView = it.getChildAt(i)
                if (childCustomView is EntryCustomFieldProtected)
                    return true
            }
        }
        return false
    }

    fun setHiddenPasswordStyle(hiddenStyle: Boolean) {
        if (!hiddenStyle) {
            passwordView?.transformationMethod = null
        } else {
            passwordView?.transformationMethod = PasswordTransformationMethod.getInstance()
        }
        // Hidden style for custom fields
        extrasView?.let {
            for (i in 0 until it.childCount) {
                val childCustomView = it.getChildAt(i)
                if (childCustomView is EntryCustomFieldProtected)
                    childCustomView.setHiddenPasswordStyle(hiddenStyle)
            }
        }
    }

    fun assignURL(url: String?) {
        if (url != null && url.isNotEmpty()) {
            urlContainerView?.visibility = View.VISIBLE
            urlView?.text = url
        } else {
            urlContainerView?.visibility = View.GONE
        }
    }

    fun assignComment(comment: String?) {
        if (comment != null && comment.isNotEmpty()) {
            commentContainerView?.visibility = View.VISIBLE
            commentView?.apply {
                text = comment
                if (fontInVisibility)
                    Util.applyFontVisibilityTo(context, this)
            }

        } else {
            commentContainerView?.visibility = View.GONE
        }
    }

    fun addExtraField(title: String, value: ProtectedString, showAction: Boolean, onActionClickListener: OnClickListener) {
        val entryCustomField: EntryCustomField
        if (value.isProtected)
            entryCustomField = EntryCustomFieldProtected(context, null, title, value, showAction, onActionClickListener)
        else
            entryCustomField = EntryCustomField(context, null, title, value, showAction, onActionClickListener)
        entryCustomField.applyFontVisibility(fontInVisibility)
        extrasView?.addView(entryCustomField)
    }

    fun clearExtraFields() {
        extrasView?.removeAllViews()
    }

    private fun getDateTime(date: Date): String {
        return dateFormat.format(date) + " " + timeFormat.format(date)
    }

    fun assignCreationDate(date: Date) {
        creationDateView?.text = getDateTime(date)
    }

    fun assignModificationDate(date: Date) {
        modificationDateView?.text = getDateTime(date)
    }

    fun assignLastAccessDate(date: Date) {
        lastAccessDateView?.text = getDateTime(date)
    }

    fun assignExpiresDate(date: Date) {
        expiresDateView?.text = getDateTime(date)
    }

    fun assignExpiresDate(constString: String) {
        expiresDateView?.text = constString
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }
}
