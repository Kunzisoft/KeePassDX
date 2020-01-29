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
package com.kunzisoft.keepass.activities.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.OtpModel
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpElement.Companion.MAX_HOTP_COUNTER
import com.kunzisoft.keepass.otp.OtpElement.Companion.MAX_OTP_DIGITS
import com.kunzisoft.keepass.otp.OtpElement.Companion.MAX_TOTP_PERIOD
import com.kunzisoft.keepass.otp.OtpElement.Companion.MIN_HOTP_COUNTER
import com.kunzisoft.keepass.otp.OtpElement.Companion.MIN_OTP_DIGITS
import com.kunzisoft.keepass.otp.OtpElement.Companion.MIN_TOTP_PERIOD
import com.kunzisoft.keepass.otp.OtpTokenType
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.otp.TokenCalculator
import java.util.*

class SetOTPDialogFragment : DialogFragment() {

    private var mCreateOTPElementListener: CreateOtpListener? = null

    private var mOtpElement: OtpElement = OtpElement()

    private var otpTypeSpinner: Spinner? = null
    private var otpTokenTypeSpinner: Spinner? = null
    private var otpSecretContainer: TextInputLayout? = null
    private var otpSecretTextView: EditText? = null
    private var otpPeriodContainer: TextInputLayout? = null
    private var otpPeriodTextView: EditText? = null
    private var otpCounterContainer: TextInputLayout? = null
    private var otpCounterTextView: EditText? = null
    private var otpDigitsContainer: TextInputLayout? = null
    private var otpDigitsTextView: EditText? = null
    private var otpAlgorithmSpinner: Spinner? = null

    private var otpTypeAdapter: ArrayAdapter<OtpType>? = null
    private var otpTokenTypeAdapter: ArrayAdapter<OtpTokenType>? = null
    private var totpTokenTypeAdapter: ArrayAdapter<OtpTokenType>? = null
    private var hotpTokenTypeAdapter: ArrayAdapter<OtpTokenType>? = null
    private var otpAlgorithmAdapter: ArrayAdapter<TokenCalculator.HashAlgorithm>? = null

    private var mManualEvent = false
    private var mOnFocusChangeListener = View.OnFocusChangeListener { _, isFocus ->
        if (!isFocus)
            mManualEvent = true
    }
    private var mOnTouchListener = View.OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mManualEvent = true
            }
        }
        false
    }

    private var mSecretWellFormed = false
    private var mCounterWellFormed = true
    private var mPeriodWellFormed = true
    private var mDigitsWellFormed = true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mCreateOTPElementListener = context as CreateOtpListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(context.toString()
                    + " must implement " + CreateOtpListener::class.java.name)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        // Retrieve OTP model from instance state
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_OTP)) {
                savedInstanceState.getParcelable<OtpModel>(KEY_OTP)?.let { otpModel ->
                    mOtpElement = OtpElement(otpModel)
                }
            }
        } else {
            arguments?.apply {
                if (containsKey(KEY_OTP)) {
                    getParcelable<OtpModel?>(KEY_OTP)?.let { otpModel ->
                        mOtpElement = OtpElement(otpModel)
                    }
                }
            }
        }

        activity?.let { activity ->
            val root = activity.layoutInflater.inflate(R.layout.fragment_set_otp, null) as ViewGroup?
            otpTypeSpinner = root?.findViewById(R.id.setup_otp_type)
            otpTokenTypeSpinner = root?.findViewById(R.id.setup_otp_token_type)
            otpSecretContainer = root?.findViewById(R.id.setup_otp_secret_label)
            otpSecretTextView = root?.findViewById(R.id.setup_otp_secret)
            otpAlgorithmSpinner = root?.findViewById(R.id.setup_otp_algorithm)
            otpPeriodContainer= root?.findViewById(R.id.setup_otp_period_label)
            otpPeriodTextView = root?.findViewById(R.id.setup_otp_period)
            otpCounterContainer= root?.findViewById(R.id.setup_otp_counter_label)
            otpCounterTextView = root?.findViewById(R.id.setup_otp_counter)
            otpDigitsContainer = root?.findViewById(R.id.setup_otp_digits_label)
            otpDigitsTextView = root?.findViewById(R.id.setup_otp_digits)

            // To fix init element
            // With tab keyboard selection
            otpSecretTextView?.onFocusChangeListener = mOnFocusChangeListener
            // With finger selection
            otpTypeSpinner?.setOnTouchListener(mOnTouchListener)
            otpTokenTypeSpinner?.setOnTouchListener(mOnTouchListener)
            otpSecretTextView?.setOnTouchListener(mOnTouchListener)
            otpAlgorithmSpinner?.setOnTouchListener(mOnTouchListener)
            otpPeriodTextView?.setOnTouchListener(mOnTouchListener)
            otpCounterTextView?.setOnTouchListener(mOnTouchListener)
            otpDigitsTextView?.setOnTouchListener(mOnTouchListener)


            // HOTP / TOTP Type selection
            val otpTypeArray = OtpType.values()
            otpTypeAdapter = ArrayAdapter<OtpType>(activity,
                    android.R.layout.simple_spinner_item, otpTypeArray).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            otpTypeSpinner?.adapter = otpTypeAdapter

            // Otp Token type selection
            val hotpTokenTypeArray = OtpTokenType.getHotpTokenTypeValues()
            hotpTokenTypeAdapter = ArrayAdapter(activity,
                    android.R.layout.simple_spinner_item, hotpTokenTypeArray).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            // Proprietary only on closed and full version
            val totpTokenTypeArray = OtpTokenType.getTotpTokenTypeValues(
                    BuildConfig.CLOSED_STORE && BuildConfig.FULL_VERSION)
            totpTokenTypeAdapter = ArrayAdapter(activity,
                    android.R.layout.simple_spinner_item, totpTokenTypeArray).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            otpTokenTypeAdapter = hotpTokenTypeAdapter
            otpTokenTypeSpinner?.adapter = otpTokenTypeAdapter

            // OTP Algorithm
            val otpAlgorithmArray = TokenCalculator.HashAlgorithm.values()
            otpAlgorithmAdapter = ArrayAdapter<TokenCalculator.HashAlgorithm>(activity,
                    android.R.layout.simple_spinner_item, otpAlgorithmArray).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            otpAlgorithmSpinner?.adapter = otpAlgorithmAdapter

            // Set the default value of OTP element
            upgradeType()
            upgradeTokenType()
            upgradeParameters()

            attachListeners()

            val builder = AlertDialog.Builder(activity)
            builder.apply {
                setTitle(R.string.entry_setup_otp)
                setView(root)
                        .setPositiveButton(android.R.string.ok) {_, _ -> }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                        }
            }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        (dialog as AlertDialog).getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
            if (mSecretWellFormed
                    && mCounterWellFormed
                    && mPeriodWellFormed
                    && mDigitsWellFormed) {
                mCreateOTPElementListener?.onOtpCreated(mOtpElement)
                dismiss()
            }
        }
    }

    private fun attachListeners() {
        // Set Type listener
        otpTypeSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (mManualEvent) {
                    (parent?.selectedItem as OtpType?)?.let {
                        mOtpElement.type = it
                        upgradeTokenType()
                    }
                }
            }
        }

        // Set type token listener
        otpTokenTypeSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (mManualEvent) {
                    (parent?.selectedItem as OtpTokenType?)?.let {
                        mOtpElement.tokenType = it
                        upgradeParameters()
                    }
                }
            }
        }

        // Set algorithm spinner
        otpAlgorithmSpinner?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (mManualEvent) {
                    (parent?.selectedItem as TokenCalculator.HashAlgorithm?)?.let {
                        mOtpElement.algorithm = it
                    }
                }
            }
        }

        // Set secret in OtpElement
        otpSecretTextView?.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let { userString ->
                    try {
                        mOtpElement.setBase32Secret(userString.toUpperCase(Locale.ENGLISH))
                        otpSecretContainer?.error = null
                    } catch (exception: Exception) {
                        otpSecretContainer?.error = getString(R.string.error_otp_secret_key)
                    }
                    mSecretWellFormed = otpSecretContainer?.error == null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Set counter in OtpElement
        otpCounterTextView?.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (mManualEvent) {
                    s?.toString()?.toLongOrNull()?.let {
                        try {
                            mOtpElement.counter = it
                            otpCounterContainer?.error = null
                        } catch (exception: Exception) {
                            otpCounterContainer?.error = getString(R.string.error_otp_counter,
                                    MIN_HOTP_COUNTER, MAX_HOTP_COUNTER)
                        }
                        mCounterWellFormed = otpCounterContainer?.error == null
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Set period in OtpElement
        otpPeriodTextView?.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (mManualEvent) {
                    s?.toString()?.toIntOrNull()?.let {
                        try {
                            mOtpElement.period = it
                            otpPeriodContainer?.error = null
                        } catch (exception: Exception) {
                            otpPeriodContainer?.error = getString(R.string.error_otp_period,
                                    MIN_TOTP_PERIOD, MAX_TOTP_PERIOD)
                        }
                        mPeriodWellFormed = otpPeriodContainer?.error == null
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Set digits in OtpElement
        otpDigitsTextView?.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (mManualEvent) {
                    s?.toString()?.toIntOrNull()?.let {
                        try {
                            mOtpElement.digits = it
                            otpDigitsContainer?.error = null
                        } catch (exception: Exception) {
                            otpDigitsContainer?.error = getString(R.string.error_otp_digits,
                                    MIN_OTP_DIGITS, MAX_OTP_DIGITS)
                        }
                        mDigitsWellFormed = otpDigitsContainer?.error == null
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun upgradeType() {
        otpTypeSpinner?.setSelection(OtpType.values().indexOf(mOtpElement.type))
    }

    private fun upgradeTokenType() {
        when (mOtpElement.type) {
            OtpType.HOTP -> {
                otpPeriodContainer?.visibility = View.GONE
                otpCounterContainer?.visibility = View.VISIBLE
                otpTokenTypeSpinner?.adapter = hotpTokenTypeAdapter
                otpTokenTypeSpinner?.setSelection(OtpTokenType
                        .getHotpTokenTypeValues().indexOf(mOtpElement.tokenType))
            }
            OtpType.TOTP -> {
                otpPeriodContainer?.visibility = View.VISIBLE
                otpCounterContainer?.visibility = View.GONE
                otpTokenTypeSpinner?.adapter = totpTokenTypeAdapter
                otpTokenTypeSpinner?.setSelection(OtpTokenType
                        .getTotpTokenTypeValues().indexOf(mOtpElement.tokenType))
            }
        }
    }

    private fun upgradeParameters() {
        otpAlgorithmSpinner?.setSelection(TokenCalculator.HashAlgorithm.values()
                .indexOf(mOtpElement.algorithm))
        otpSecretTextView?.apply {
            setText(mOtpElement.getBase32Secret())
            // Cursor at end
            setSelection(this.text.length)
        }
        otpCounterTextView?.setText(mOtpElement.counter.toString())
        otpPeriodTextView?.setText(mOtpElement.period.toString())
        otpDigitsTextView?.setText(mOtpElement.digits.toString())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_OTP, mOtpElement.otpModel)
    }

    interface CreateOtpListener {
        fun onOtpCreated(otpElement: OtpElement)
    }

    companion object {

        private const val KEY_OTP = "KEY_OTP"

        fun build(otpModel: OtpModel? = null): SetOTPDialogFragment {
            return SetOTPDialogFragment().apply {
                if (otpModel != null) {
                    arguments = Bundle().apply {
                        putParcelable(KEY_OTP, otpModel)
                    }
                }
            }
        }
    }
}