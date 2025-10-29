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
package com.kunzisoft.keepass.tasks

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kunzisoft.keepass.R
import kotlinx.coroutines.launch

open class ProgressTaskDialogFragment : DialogFragment() {

    private var titleView: TextView? = null
    private var messageView: TextView? = null
    private var warningView: TextView? = null
    private var cancelButton: Button? = null
    private var progressView: ProgressBar? = null

    private val progressTaskViewModel: ProgressTaskViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        try {
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
                cancelButton = root.findViewById(R.id.progress_dialog_cancel)
                progressView = root.findViewById(R.id.progress_dialog_bar)

                isCancelable = false

                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        progressTaskViewModel.progressTaskState.collect { state ->
                            when (state) {
                                is ProgressTaskViewModel.ProgressTaskState.Show -> {
                                    val value = state.value
                                    updateView(
                                        titleView,
                                        value.titleId?.let { title ->
                                            getString(title)
                                        })
                                    updateView(
                                        messageView,
                                        value.messageId?.let { message ->
                                            getString(message)
                                        })
                                    updateView(
                                        warningView,
                                        value.warningId?.let { warning ->
                                            getString(warning)
                                        })
                                    cancelButton?.apply {
                                        isVisible = value.cancelable != null
                                        setOnClickListener {
                                            value.cancelable?.invoke()
                                        }
                                    }
                                }
                                else -> {
                                    // Nothing here, this fragment is stopped externally
                                }
                            }
                        }
                    }
                }

                return builder.create()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to create progress dialog", e)
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun updateView(textView: TextView?, value: String?) {
        if (value == null) {
            textView?.visibility = View.GONE
        } else {
            textView?.text = value
            textView?.visibility = View.VISIBLE
        }
    }

    companion object {
        private val TAG = ProgressTaskDialogFragment::class.java.simpleName
        const val PROGRESS_TASK_DIALOG_TAG = "progressDialogFragment"
    }
}
