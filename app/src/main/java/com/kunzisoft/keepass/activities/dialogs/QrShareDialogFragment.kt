package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.qrshare.QrSharePayload
import com.kunzisoft.keepass.utils.getParcelableCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QrShareDialogFragment : DialogFragment() {

    private var entryInfo: EntryInfo? = null

    private var stepPinView: View? = null
    private var stepDisplayView: View? = null
    private var pinInputLayout: TextInputLayout? = null
    private var pinInput: TextInputEditText? = null
    private var pinConfirmLayout: TextInputLayout? = null
    private var pinConfirmInput: TextInputEditText? = null
    private var pinWarning: TextView? = null
    private var ttlGroup: RadioGroup? = null
    private var progressBar: ProgressBar? = null
    private var qrImage: ImageView? = null
    private var expiryText: TextView? = null

    private var countDownTimer: CountDownTimer? = null
    private var generateButton: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entryInfo = arguments?.getParcelableCompat(ARG_ENTRY_INFO)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_qr_share, null)
        bindViews(view)
        setupPinListeners()

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.qr_share_dialog_title)
            .setView(view)
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .setPositiveButton(R.string.qr_share_generate, null)
            .create()

        dialog.setOnShowListener {
            generateButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            generateButton?.setOnClickListener { onGenerateClicked(dialog) }
        }
        return dialog
    }

    private fun bindViews(view: View) {
        stepPinView = view.findViewById(R.id.qr_share_step_pin)
        stepDisplayView = view.findViewById(R.id.qr_share_step_display)
        pinInputLayout = view.findViewById(R.id.qr_share_pin_layout)
        pinInput = view.findViewById(R.id.qr_share_pin_input)
        pinConfirmLayout = view.findViewById(R.id.qr_share_pin_confirm_layout)
        pinConfirmInput = view.findViewById(R.id.qr_share_pin_confirm_input)
        pinWarning = view.findViewById(R.id.qr_share_pin_warning)
        ttlGroup = view.findViewById(R.id.qr_share_ttl_group)
        progressBar = view.findViewById(R.id.qr_share_progress)
        qrImage = view.findViewById(R.id.qr_share_image)
        expiryText = view.findViewById(R.id.qr_share_expiry_text)
    }

    private fun setupPinListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePinRealtime()
            }
        }
        pinInput?.addTextChangedListener(watcher)
        pinConfirmInput?.addTextChangedListener(watcher)
    }

    private fun validatePinRealtime() {
        val pin = pinInput?.text?.toString() ?: return
        if (pin.length in 1..5) {
            pinInputLayout?.error = getString(R.string.qr_share_pin_too_short)
        } else {
            pinInputLayout?.error = null
        }
        pinWarning?.visibility = if (pin.length in 6..8 && pin.all { it.isDigit() }) View.VISIBLE else View.GONE
        pinConfirmLayout?.error = null
    }

    private fun onGenerateClicked(dialog: AlertDialog) {
        val pin = pinInput?.text?.toString() ?: return
        val confirm = pinConfirmInput?.text?.toString() ?: return

        if (pin.length < 6) {
            pinInputLayout?.error = getString(R.string.qr_share_pin_too_short)
            return
        }
        if (pin != confirm) {
            pinConfirmLayout?.error = getString(R.string.qr_share_pin_mismatch)
            return
        }

        val ttlMs = when (ttlGroup?.checkedRadioButtonId) {
            R.id.qr_share_ttl_30s -> 30_000L
            R.id.qr_share_ttl_5min -> 300_000L
            else -> 60_000L
        }

        showQrDisplay(dialog)
        generateQr(pin.toCharArray(), ttlMs)
        pin.forEach { }
    }

    private fun showQrDisplay(dialog: AlertDialog) {
        stepPinView?.visibility = View.GONE
        stepDisplayView?.visibility = View.VISIBLE
        progressBar?.visibility = View.VISIBLE
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.visibility = View.GONE
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setText(android.R.string.ok)
    }

    private fun generateQr(pin: CharArray, ttlMs: Long) {
        val entry = entryInfo ?: return
        lifecycleScope.launch {
            val qrContent = withContext(Dispatchers.IO) {
                runCatching { QrSharePayload.encode(entry, pin, ttlMs) }.getOrNull()
            }
            pin.fill('0')
            progressBar?.visibility = View.GONE
            if (qrContent != null) {
                val bitmap = withContext(Dispatchers.IO) {
                    runCatching {
                        BarcodeEncoder().encodeBitmap(qrContent, BarcodeFormat.QR_CODE, 600, 600)
                    }.getOrNull()
                }
                qrImage?.setImageBitmap(bitmap)
                qrImage?.visibility = View.VISIBLE
                startCountdown(ttlMs)
            } else {
                expiryText?.text = getString(R.string.qr_share_expired)
            }
        }
    }

    private fun startCountdown(durationMs: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val s = millisUntilFinished / 1000
                val formatted = if (s >= 60) "%d:%02d".format(s / 60, s % 60) else "${s}s"
                expiryText?.text = getString(R.string.qr_share_expires_in, formatted)
            }
            override fun onFinish() {
                qrImage?.setImageBitmap(null)
                qrImage?.visibility = View.GONE
                expiryText?.text = getString(R.string.qr_share_expired)
            }
        }.start()
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        countDownTimer = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "QrShareDialogFragment"
        private const val ARG_ENTRY_INFO = "entry_info"

        fun newInstance(entryInfo: EntryInfo) = QrShareDialogFragment().apply {
            arguments = bundleOf(ARG_ENTRY_INFO to entryInfo)
        }
    }
}
