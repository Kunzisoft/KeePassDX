package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.Field
import com.kunzisoft.keepass.model.CreditCardCustomFields.buildAllFields
import com.kunzisoft.keepass.model.CreditCard

class CreditCardDetailsDialogFragment : DialogFragment() {
    private var mCreditCard: CreditCard? = null
    private var entryCCFieldListener: EntryCCFieldListener? = null

    private var mCcCardholderName: EditText? = null
    private var mCcCardNumber: EditText? = null
    private var mCcSecurityCode: EditText? = null

    private var mCcExpirationMonthSpinner: Spinner? = null
    private var mCcExpirationYearSpinner: Spinner? = null

    private var mCcCardNumberWellFormed: Boolean = false
    private var mCcSecurityCodeWellFormed: Boolean = false

    private var mPositiveButton: Button? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            entryCCFieldListener = context as EntryCCFieldListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(context.toString()
                    + " must implement " + EntryCCFieldListener::class.java.name)
        }
    }

    override fun onDetach() {
        entryCCFieldListener = null
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()

        // To prevent auto dismiss
        val d = dialog as AlertDialog?
        if (d != null) {
            mPositiveButton = d.getButton(Dialog.BUTTON_POSITIVE) as Button
            mPositiveButton?.run {
                isEnabled = mCcSecurityCodeWellFormed && mCcCardNumberWellFormed
                attachListeners()
                setOnClickListener {
                    submitDialog()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_CREDIT_CARD, mCreditCard)
    }

    private fun submitDialog() {
        val ccNumber = mCcCardNumber?.text?.toString() ?: ""

        val month = mCcExpirationMonthSpinner?.selectedItem?.toString() ?: ""
        val year = mCcExpirationYearSpinner?.selectedItem?.toString() ?: ""

        val cvv = mCcSecurityCode?.text?.toString() ?: ""
        val ccName = mCcCardholderName?.text?.toString() ?: ""

        entryCCFieldListener?.onNewCCFieldsApproved(buildAllFields(ccName, ccNumber, month + year, cvv))

        (dialog as AlertDialog?)?.dismiss()
    }

    interface EntryCCFieldListener {
        fun onNewCCFieldsApproved(ccFields: ArrayList<Field>)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Retrieve credit card details if available
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_CREDIT_CARD)) {
                mCreditCard = savedInstanceState.getParcelable(KEY_CREDIT_CARD)
            }
        } else {
            arguments?.apply {
                if (containsKey(KEY_CREDIT_CARD)) {
                    mCreditCard = getParcelable(KEY_CREDIT_CARD)
                }
            }
        }

        activity?.let { activity ->
            val root = activity.layoutInflater.inflate(R.layout.entry_cc_details_dialog, null)

            mCcCardholderName = root?.findViewById(R.id.creditCardholderNameField)

            mCcExpirationMonthSpinner = root?.findViewById(R.id.expirationMonth)
            mCcExpirationYearSpinner = root?.findViewById(R.id.expirationYear)

            mCcCardNumber = root?.findViewById(R.id.creditCardNumberField)
            mCcSecurityCode = root?.findViewById(R.id.creditCardSecurityCode)

            mCreditCard?.let  {
                mCcCardholderName!!.setText(it.cardholder)
                mCcCardNumberWellFormed = true
                mCcCardNumber!!.setText(it.number)
                mCcSecurityCodeWellFormed = true
                mCcSecurityCode!!.setText(it.cvv)
            }

            val monthAdapter = ArrayAdapter.createFromResource(requireContext(),
                    R.array.month_array, android.R.layout.simple_spinner_item)
            monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mCcExpirationMonthSpinner!!.adapter = monthAdapter

            mCreditCard?.let { mCcExpirationMonthSpinner!!.setSelection(
                    getIndex(mCcExpirationMonthSpinner!!, it.getExpirationMonth()) ) }

            val yearAdapter = ArrayAdapter.createFromResource(requireContext(),
                    R.array.year_array, android.R.layout.simple_spinner_item)
            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mCcExpirationYearSpinner!!.adapter = yearAdapter

            mCreditCard?.let { mCcExpirationYearSpinner!!.setSelection(
                    getIndex(mCcExpirationYearSpinner!!, it.getExpirationYear()) ) }

            val builder = AlertDialog.Builder(activity)

            builder.setView(root).setTitle(R.string.entry_setup_cc)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }

            val dialogCreated = builder.create()

            dialogCreated.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

            return dialogCreated
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun getIndex(spinner: Spinner, value: String?): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString().equals(value, ignoreCase = true)) {
                return i
            }
        }
        return 0
    }

    private fun attachListeners() {
        mCcCardNumber?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val userString = s?.toString()
                mCcCardNumberWellFormed = userString?.length == 16
                mPositiveButton?.run {
                    isEnabled = mCcSecurityCodeWellFormed && mCcCardNumberWellFormed
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        mCcSecurityCode?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val userString = s?.toString()
                mCcSecurityCodeWellFormed = (userString?.length == 3 || userString?.length == 4)
                mPositiveButton?.run {
                    isEnabled = mCcSecurityCodeWellFormed && mCcCardNumberWellFormed
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }

    companion object {

        private const val KEY_CREDIT_CARD = "KEY_CREDIT_CARD"

        fun build(creditCard: CreditCard? = null): CreditCardDetailsDialogFragment {
            return CreditCardDetailsDialogFragment().apply {
                if (creditCard != null) {
                    arguments = Bundle().apply {
                        putParcelable(KEY_CREDIT_CARD, creditCard)
                    }
                }
            }
        }
    }

}