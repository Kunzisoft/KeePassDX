package com.kunzisoft.keepass.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.credentialprovider.magikeyboard.MagikeyboardService
import com.kunzisoft.keepass.qrshare.QrShareAttemptTracker
import com.kunzisoft.keepass.utils.RETURN_TO_MAGIKEYBOARD_ACTION
import com.kunzisoft.keepass.qrshare.QrSharePayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.AEADBadTagException

class QrScanFromKeyboardActivity : AppCompatActivity() {

    private var pendingQrContent: String? = null
    private var ciphertext: ByteArray? = null

    private var pinLayout: TextInputLayout? = null
    private var pinInput: TextInputEditText? = null
    private var attemptsText: TextView? = null
    private var okButton: MaterialButton? = null

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        val content = result.contents
        if (content != null && content.startsWith(QrSharePayload.QR_PREFIX)) {
            pendingQrContent = content
            ciphertext = QrSharePayload.extractCiphertext(content)
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MagikeyboardService.pinEntryActive = true
        setContentView(R.layout.activity_qr_scan_keyboard)

        pinLayout = findViewById(R.id.qr_scan_pin_layout)
        pinInput = findViewById(R.id.qr_scan_pin_input)
        attemptsText = findViewById(R.id.qr_scan_attempts_text)
        okButton = findViewById(R.id.qr_scan_ok)

        pinInput?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                attemptDecrypt(); true
            } else false
        }

        okButton?.setOnClickListener { attemptDecrypt() }
        findViewById<MaterialButton>(R.id.qr_scan_cancel).setOnClickListener { finish() }

        if (savedInstanceState == null) {
            scanLauncher.launch(ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt(getString(R.string.qr_scan_prompt))
                setBeepEnabled(false)
                setBarcodeImageEnabled(false)
                setOrientationLocked(true)
            })
        }
    }

    override fun onDestroy() {
        MagikeyboardService.pinEntryActive = false
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val ct = ciphertext
        if (ct != null && QrShareAttemptTracker.isBlocked(this, ct)) {
            showError(getString(R.string.qr_scan_blocked))
            okButton?.isEnabled = false
        }
    }

    private fun attemptDecrypt() {
        val content = pendingQrContent ?: return
        val ct = ciphertext ?: return
        if (QrShareAttemptTracker.isBlocked(this, ct)) {
            showError(getString(R.string.qr_scan_blocked))
            return
        }
        val pin = pinInput?.text?.toString()?.toCharArray() ?: return
        if (pin.isEmpty()) return

        pinInput?.isEnabled = false
        okButton?.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { QrSharePayload.decode(content, pin) }
            }
            pin.fill('0')
            pinInput?.isEnabled = true
            okButton?.isEnabled = true

            result.fold(
                onSuccess = { entryInfo ->
                    MagikeyboardService.pinEntryActive = false
                    MagikeyboardService.setScannedQrEntry(entryInfo)
                    sendBroadcast(Intent(RETURN_TO_MAGIKEYBOARD_ACTION))
                    finish()
                },
                onFailure = { error ->
                    when (error) {
                        is QrSharePayload.ExpiredException ->
                            showError(getString(R.string.qr_scan_expired))
                        is IllegalArgumentException ->
                            showError(getString(R.string.qr_scan_invalid))
                        is AEADBadTagException -> {
                            val failed = QrShareAttemptTracker.recordFailedAttempt(this@QrScanFromKeyboardActivity, ct)
                            if (failed >= QrShareAttemptTracker.MAX_ATTEMPTS) {
                                showError(getString(R.string.qr_scan_blocked))
                                okButton?.isEnabled = false
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
}
