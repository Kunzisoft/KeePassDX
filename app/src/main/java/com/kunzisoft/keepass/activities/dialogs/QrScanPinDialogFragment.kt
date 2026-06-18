package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.qrshare.QrShareAttemptTracker
import com.kunzisoft.keepass.qrshare.QrSharePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.AEADBadTagException

class QrScanPinDialogFragment : DialogFragment() {

    private var qrContent: String? = null
    private var ciphertext: ByteArray? = null

    private var pinLayout: TextInputLayout? = null
    private var pinInput: TextInputEditText? = null
    private var attemptsText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        qrContent = arguments?.getString(ARG_QR_CONTENT)
        qrContent?.let { ciphertext = QrSharePayload.extractCiphertext(it) }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_qr_scan_pin, null)
        pinLayout = view.findViewById(R.id.qr_scan_pin_layout)
        pinInput = view.findViewById(R.id.qr_scan_pin_input)
        attemptsText = view.findViewById(R.id.qr_scan_attempts_text)

        val ct = ciphertext
        if (ct != null && QrShareAttemptTracker.isBlocked(requireContext(), ct)) {
            showBlocked()
        }

        pinInput?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                attemptDecrypt()
                true
            } else false
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.qr_scan_dialog_title)
            .setView(view)
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        attemptDecrypt()
                    }
                }
            }
    }

    private fun attemptDecrypt() {
        val ct = ciphertext ?: run {
            showError(getString(R.string.qr_scan_invalid))
            return
        }
        if (QrShareAttemptTracker.isBlocked(requireContext(), ct)) {
            showBlocked()
            return
        }
        val pin = pinInput?.text?.toString()?.toCharArray() ?: return
        pinInput?.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { QrSharePayload.decode(qrContent ?: "", pin) }
            }
            pin.fill('0')
            pinInput?.isEnabled = true

            result.fold(
                onSuccess = { entryInfo ->
                    val bundle = bundleOf(KEY_ENTRY_INFO to entryInfo)
                    parentFragmentManager.setFragmentResult(REQUEST_KEY, bundle)
                    dismiss()
                },
                onFailure = { error ->
                    when (error) {
                        is QrSharePayload.ExpiredException -> showError(getString(R.string.qr_scan_expired))
                        is IllegalArgumentException -> showError(getString(R.string.qr_scan_invalid))
                        is AEADBadTagException -> {
                            val failed = QrShareAttemptTracker.recordFailedAttempt(requireContext(), ct)
                            if (failed >= QrShareAttemptTracker.MAX_ATTEMPTS) {
                                showBlocked()
                            } else {
                                val remaining = QrShareAttemptTracker.MAX_ATTEMPTS - failed
                                showError(getString(R.string.qr_scan_wrong_pin, remaining))
                            }
                        }
                        else -> showError(getString(R.string.qr_scan_invalid))
                    }
                }
            )
        }
    }

    private fun showError(message: String) {
        attemptsText?.text = message
        attemptsText?.visibility = View.VISIBLE
    }

    private fun showBlocked() {
        pinInput?.isEnabled = false
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        showError(getString(R.string.qr_scan_blocked))
    }

    companion object {
        const val TAG = "QrScanPinDialogFragment"
        const val REQUEST_KEY = "qr_scan_pin_result"
        const val KEY_ENTRY_INFO = "entry_info"
        private const val ARG_QR_CONTENT = "qr_content"

        fun newInstance(qrContent: String) = QrScanPinDialogFragment().apply {
            arguments = bundleOf(ARG_QR_CONTENT to qrContent)
        }
    }
}
