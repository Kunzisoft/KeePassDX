package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.BuildConfig
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpTokenType
import com.kunzisoft.keepass.otp.OtpType
import com.kunzisoft.keepass.otp.TokenCalculator

class SetOTPDialogFragment : DialogFragment() {

    var createOTPElementListener: ((OtpElement) -> Unit)? = null

    var mOtpElement: OtpElement = OtpElement()

    var otpTypeSpinner: Spinner? = null
    var otpTokenTypeSpinner: Spinner? = null
    var otpSecretTextView: EditText? = null
    var otpPeriodContainer: View? = null
    var otpPeriodTextView: EditText? = null
    var otpCounterContainer: View? = null
    var otpCounterTextView: EditText? = null
    var otpDigitsTextView: EditText? = null
    var otpAlgorithmSpinner: Spinner? = null

    var totpTokenTypeAdapter: ArrayAdapter<OtpTokenType>? = null
    var hotpTokenTypeAdapter: ArrayAdapter<OtpTokenType>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val root = activity.layoutInflater.inflate(R.layout.fragment_set_otp, null)
            otpTypeSpinner = root.findViewById(R.id.setup_otp_type)
            otpTokenTypeSpinner = root.findViewById(R.id.setup_otp_token_type)
            otpSecretTextView = root.findViewById(R.id.setup_otp_secret)
            otpPeriodContainer= root.findViewById(R.id.setup_otp_period_title)
            otpPeriodTextView = root.findViewById(R.id.setup_otp_period)
            otpCounterContainer= root.findViewById(R.id.setup_otp_counter_title)
            otpCounterTextView = root.findViewById(R.id.setup_otp_counter)
            otpDigitsTextView = root.findViewById(R.id.setup_otp_digits)
            otpAlgorithmSpinner = root.findViewById(R.id.setup_otp_algorithm)

            context?.let { context ->


                // Otp Token type selection
                val hotpTokenTypeArray = OtpTokenType.getHotpTokenTypeValues()
                hotpTokenTypeAdapter = ArrayAdapter(context,
                        android.R.layout.simple_spinner_item, hotpTokenTypeArray).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                val totpTokenTypeArray = OtpTokenType.getTotpTokenTypeValues(BuildConfig.CLOSED_STORE)
                totpTokenTypeAdapter = ArrayAdapter(context,
                        android.R.layout.simple_spinner_item, totpTokenTypeArray).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                otpTokenTypeSpinner?.apply {
                    adapter = totpTokenTypeAdapter
                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            when (adapter) {
                                hotpTokenTypeAdapter -> {
                                    switchTokenType(OtpTokenType.RFC4226)
                                }
                                totpTokenTypeAdapter -> {
                                    switchTokenType(OtpTokenType.RFC6238)
                                }
                            }
                        }

                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            (parent?.selectedItem as OtpTokenType?)?.let {
                                switchTokenType(it)
                            }
                        }
                    }
                }

                // HOTP / TOTP Type selection
                val otpTypeArray = OtpType.values()
                otpTypeSpinner?.apply {
                    adapter = ArrayAdapter<OtpType>(context,
                            android.R.layout.simple_spinner_item, otpTypeArray).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            mOtpElement.type = null
                        }

                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            (parent?.selectedItem as OtpType?)?.let {
                                switchType(it)
                            }
                        }
                    }

                    setSelection(1)
                }

                // OTP Algorithm
                val otpAlgorithmArray = TokenCalculator.HashAlgorithm.values()
                otpAlgorithmSpinner?.apply {
                    adapter = ArrayAdapter<TokenCalculator.HashAlgorithm>(context,
                            android.R.layout.simple_spinner_item, otpAlgorithmArray).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }

                    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            mOtpElement.algorithm = TokenCalculator.HashAlgorithm.SHA1
                        }

                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            mOtpElement.algorithm = parent?.selectedItem as TokenCalculator.HashAlgorithm
                        }
                    }
                }
            }

            val builder = AlertDialog.Builder(activity)
            builder.apply {
                setTitle(R.string.entry_setup_otp)
                setView(root)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            // Set secret in OtpElement
                            otpSecretTextView?.text?.toString()?.let {
                                mOtpElement.setBase32Secret(it)
                            }
                            // Set counter in OtpElement
                            otpCounterTextView?.text?.toString()?.let {
                                mOtpElement.counter = it.toInt()
                            }
                            // Set period in OtpElement
                            otpPeriodTextView?.text?.toString()?.let {
                                mOtpElement.period = it.toInt()
                            }
                            createOTPElementListener?.invoke(mOtpElement)
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                        }
            }

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun switchType(otpType: OtpType) {
        mOtpElement.type = otpType

        when (otpType) {
            OtpType.HOTP -> {
                otpPeriodContainer?.visibility = View.GONE
                otpCounterContainer?.visibility = View.VISIBLE
                otpTokenTypeSpinner?.adapter = hotpTokenTypeAdapter
            }
            OtpType.TOTP -> {
                otpPeriodContainer?.visibility = View.VISIBLE
                otpCounterContainer?.visibility = View.GONE
                otpTokenTypeSpinner?.adapter = totpTokenTypeAdapter
            }
        }

        upgradeParameters()

    }

    private fun switchTokenType(otpTokenType: OtpTokenType) {
        mOtpElement.tokenType = otpTokenType

        upgradeParameters()
    }

    private fun upgradeParameters() {
        otpCounterTextView?.setText(mOtpElement.counter.toString())
        otpPeriodTextView?.setText(mOtpElement.period.toString())
        otpDigitsTextView?.setText(mOtpElement.digits.toString())
    }
}