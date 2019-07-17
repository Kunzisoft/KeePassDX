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
package com.kunzisoft.keepass.activities.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.*
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.password.PasswordGenerator
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.Util

class GeneratePasswordDialogFragment : DialogFragment() {

    private var mListener: GeneratePasswordListener? = null

    private var root: View? = null
    private var lengthTextView: EditText? = null
    private var passwordView: EditText? = null

    private var uppercaseBox: CompoundButton? = null
    private var lowercaseBox: CompoundButton? = null
    private var digitsBox: CompoundButton? = null
    private var minusBox: CompoundButton? = null
    private var underlineBox: CompoundButton? = null
    private var spaceBox: CompoundButton? = null
    private var specialsBox: CompoundButton? = null
    private var bracketsBox: CompoundButton? = null
    private var extendedBox: CompoundButton? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        try {
            mListener = context as GeneratePasswordListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(context?.toString()
                    + " must implement " + GeneratePasswordListener::class.java.name)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            val inflater = activity.layoutInflater
            root = inflater.inflate(R.layout.generate_password, null)

            passwordView = root?.findViewById(R.id.password)
            Util.applyFontVisibilityTo(context, passwordView)

            lengthTextView = root?.findViewById(R.id.length)

            uppercaseBox = root?.findViewById(R.id.cb_uppercase)
            lowercaseBox = root?.findViewById(R.id.cb_lowercase)
            digitsBox = root?.findViewById(R.id.cb_digits)
            minusBox = root?.findViewById(R.id.cb_minus)
            underlineBox = root?.findViewById(R.id.cb_underline)
            spaceBox = root?.findViewById(R.id.cb_space)
            specialsBox = root?.findViewById(R.id.cb_specials)
            bracketsBox = root?.findViewById(R.id.cb_brackets)
            extendedBox = root?.findViewById(R.id.cb_extended)

            assignDefaultCharacters()

            val seekBar = root?.findViewById<SeekBar>(R.id.seekbar_length)
            seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    lengthTextView?.setText(progress.toString())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })

            context?.let { context ->
                seekBar?.progress = PreferencesUtil.getDefaultPasswordLength(context)
            }

            root?.findViewById<Button>(R.id.generate_password_button)
                    ?.setOnClickListener { fillPassword() }

            builder.setView(root)
                    .setPositiveButton(R.string.accept) { _, _ ->
                        val bundle = Bundle()
                        bundle.putString(KEY_PASSWORD_ID, passwordView!!.text.toString())
                        mListener?.acceptPassword(bundle)

                        dismiss()
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                        val bundle = Bundle()
                        mListener?.cancelPassword(bundle)

                        dismiss()
                    }

            // Pre-populate a password to possibly save the user a few clicks
            fillPassword()

            return builder.create()
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun assignDefaultCharacters() {
        uppercaseBox?.isChecked = false
        lowercaseBox?.isChecked = false
        digitsBox?.isChecked = false
        minusBox?.isChecked = false
        underlineBox?.isChecked = false
        spaceBox?.isChecked = false
        specialsBox?.isChecked = false
        bracketsBox?.isChecked = false
        extendedBox?.isChecked = false

        context?.let { context ->
            PreferencesUtil.getDefaultPasswordCharacters(context)?.let { charSet ->
                for (passwordChar in charSet) {
                    when (passwordChar) {
                        getString(R.string.value_password_uppercase) -> uppercaseBox?.isChecked = true
                        getString(R.string.value_password_lowercase) -> lowercaseBox?.isChecked = true
                        getString(R.string.value_password_digits) -> digitsBox?.isChecked = true
                        getString(R.string.value_password_minus) -> minusBox?.isChecked = true
                        getString(R.string.value_password_underline) -> underlineBox?.isChecked = true
                        getString(R.string.value_password_space) -> spaceBox?.isChecked = true
                        getString(R.string.value_password_special) -> specialsBox?.isChecked = true
                        getString(R.string.value_password_brackets) -> bracketsBox?.isChecked = true
                        getString(R.string.value_password_extended) -> extendedBox?.isChecked = true
                    }
                }
            }
        }
    }

    private fun fillPassword() {
        root?.findViewById<EditText>(R.id.password)?.setText(generatePassword())
    }

    fun generatePassword(): String {
        var password = ""
        try {
            val length = Integer.valueOf(root?.findViewById<EditText>(R.id.length)?.text.toString())

            val generator = PasswordGenerator(activity)
            password = generator.generatePassword(length,
                    uppercaseBox?.isChecked == true,
                    lowercaseBox?.isChecked == true,
                    digitsBox?.isChecked == true,
                    minusBox?.isChecked == true,
                    underlineBox?.isChecked == true,
                    spaceBox?.isChecked == true,
                    specialsBox?.isChecked == true,
                    bracketsBox?.isChecked == true,
                    extendedBox?.isChecked == true)
        } catch (e: NumberFormatException) {
            Toast.makeText(context, R.string.error_wrong_length, Toast.LENGTH_LONG).show()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
        }

        return password
    }

    interface GeneratePasswordListener {
        fun acceptPassword(bundle: Bundle)
        fun cancelPassword(bundle: Bundle)
    }

    companion object {

        const val KEY_PASSWORD_ID = "KEY_PASSWORD_ID"
    }
}
