package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.model.CreditCard
import com.kunzisoft.keepass.model.TemplatesCustomFields.buildAllFields
import com.kunzisoft.keepass.model.Field
import java.util.*

class CreditCardDetailsDialogFragment : DialogFragment() {
    private var mCreditCard: CreditCard? = null
    private var entryCCFieldListener: EntryCCFieldListener? = null

    private var mCcCardholderName: EditText? = null
    private var mCcCardNumber: EditText? = null
    private var mCcSecurityCode: EditText? = null

    private var mCcExpirationMonthSpinner: Spinner? = null
    private var mCcExpirationYearSpinner: Spinner? = null

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
        val year = mCcExpirationYearSpinner?.selectedItem?.toString()?.substring(2,4) ?: ""

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

            root?.run {
                mCcCardholderName = findViewById(R.id.creditCardholderNameField)
                mCcCardNumber = findViewById(R.id.creditCardNumberField)
                mCcSecurityCode = findViewById(R.id.creditCardSecurityCode)
                mCcExpirationMonthSpinner = findViewById(R.id.expirationMonth)
                mCcExpirationYearSpinner = findViewById(R.id.expirationYear)

                mCreditCard?.cardholder?.let {
                    mCcCardholderName?.setText(it)
                }
                mCreditCard?.number?.let {
                    mCcCardNumber?.setText(it)
                }
                mCreditCard?.cvv?.let {
                    mCcSecurityCode?.setText(it)
                }
            }

            val months = arrayOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")
            mCcExpirationMonthSpinner?.let { spinner ->
                spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
                mCreditCard?.let { cc ->
                    spinner.setSelection(getIndex(spinner, cc.getExpirationMonth()))
                }
            }

            val years = arrayOfNulls<String>(5)
            val year = Calendar.getInstance()[Calendar.YEAR]
            for (i in years.indices) {
                years[i] = (year + i).toString()
            }
            mCcExpirationYearSpinner?.let { spinner ->
                spinner.adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, years)
                mCreditCard?.let { cc ->
                    spinner.setSelection(getIndex(spinner, "20" + cc.getExpirationYear()))
                }
            }

            val builder = AlertDialog.Builder(activity)

            builder.setView(root).setTitle(R.string.entry_setup_credit_card)
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
            if (spinner.getItemAtPosition(i).toString() == value) {
                return i
            }
        }
        return 0
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