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
 *
 */
package com.kunzisoft.keepass.tasks

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.kunzisoft.keepass.R

open class ProgressTaskDialogFragment : DialogFragment(), ProgressTaskUpdater {

    @StringRes
    private var title = UNDEFINED
    @StringRes
    private var message = UNDEFINED
    @StringRes
    private var warning = UNDEFINED

    private var titleView: TextView? = null
    private var messageView: TextView? = null
    private var warningView: TextView? = null
    private var progressView: ProgressBar? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = it.layoutInflater

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            @SuppressLint("InflateParams")
            val root = inflater.inflate(R.layout.fragment_progress, null)
            builder.setView(root)

            titleView = root.findViewById(R.id.progress_dialog_title)
            messageView = root.findViewById(R.id.progress_dialog_message)
            warningView = root.findViewById(R.id.progress_dialog_warning)
            progressView = root.findViewById(R.id.progress_dialog_bar)

            updateTitle(title)
            updateMessage(message)
            updateWarning(warning)

            isCancelable = false

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    fun setTitle(@StringRes titleId: Int) {
        this.title = titleId
    }

    private fun updateView(textView: TextView?, @StringRes resId: Int) {
        activity?.runOnUiThread {
            if (resId == UNDEFINED) {
                textView?.visibility = View.GONE
            } else {
                textView?.setText(resId)
                textView?.visibility = View.VISIBLE
            }
        }
    }

    fun updateTitle(@StringRes resId: Int) {
        this.title = resId
        updateView(titleView, title)
    }

    override fun updateMessage(@StringRes resId: Int) {
        this.message = resId
        updateView(messageView, message)
    }

    fun updateWarning(@StringRes resId: Int) {
        this.warning = resId
        updateView(warningView, warning)
    }

    companion object {

        private const val PROGRESS_TASK_DIALOG_TAG = "progressDialogFragment"

        const val UNDEFINED = -1

        fun build(@StringRes titleId: Int? = null,
                  @StringRes messageId: Int? = null,
                  @StringRes warningId: Int? = null): ProgressTaskDialogFragment {
            // Create an instance of the dialog fragment and show it
            val dialog = ProgressTaskDialogFragment()
            titleId?.let {
                dialog.updateTitle(it)
            }
            messageId?.let {
                dialog.updateMessage(it)
            }
            warningId?.let {
                dialog.updateWarning(it)
            }
            return dialog
        }

        fun start(activity: FragmentActivity,
                  dialog: ProgressTaskDialogFragment) {
            dialog.show(activity.supportFragmentManager, PROGRESS_TASK_DIALOG_TAG)
        }

        private fun retrieveProgressDialog(activity: FragmentActivity): ProgressTaskDialogFragment? {
            return activity.supportFragmentManager
                    .findFragmentByTag(PROGRESS_TASK_DIALOG_TAG) as ProgressTaskDialogFragment?
        }

        private fun retrieveAndShowProgressDialog(activity: FragmentActivity): ProgressTaskDialogFragment {
            var dialog = retrieveProgressDialog(activity)
            if (dialog == null) {
                dialog = build()
                start(activity, build())
            }
            return dialog
        }

        fun stop(activity: FragmentActivity) {
            retrieveProgressDialog(activity)?.dismissAllowingStateLoss()
        }

        fun updateTitle(activity: FragmentActivity, titleId: Int) {
            retrieveAndShowProgressDialog(activity).updateTitle(titleId)
        }

        fun updateMessage(activity: FragmentActivity, messageId: Int) {
            retrieveAndShowProgressDialog(activity).updateMessage(messageId)
        }

        fun updateWarning(activity: FragmentActivity, warningId: Int) {
            retrieveAndShowProgressDialog(activity).updateWarning(warningId)
        }
    }
}
