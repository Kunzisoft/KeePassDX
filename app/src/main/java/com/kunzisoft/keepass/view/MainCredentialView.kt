/*
 * Copyright 2022 Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.view

import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.helpers.ExternalFileHelper
import com.kunzisoft.keepass.activities.helpers.setOpenDocumentClickListener
import com.kunzisoft.keepass.model.MainCredential
import com.kunzisoft.keepass.model.CredentialStorage

class MainCredentialView @JvmOverloads constructor(context: Context,
                                                   attrs: AttributeSet? = null,
                                                   defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private var passwordView: EditText
    private var keyFileSelectionView: KeyFileSelectionView
    private var checkboxPasswordView: CompoundButton
    private var checkboxKeyFileView: CompoundButton

    var onPasswordChecked: (CompoundButton.OnCheckedChangeListener)? = null
    var onValidateListener: (() -> Unit)? = null

    private var mCredentialStorage: CredentialStorage = CredentialStorage.PASSWORD

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_credentials, this)

        passwordView = findViewById(R.id.password)
        keyFileSelectionView = findViewById(R.id.keyfile_selection)
        checkboxPasswordView = findViewById(R.id.password_checkbox)
        checkboxKeyFileView = findViewById(R.id.keyfile_checkox)

        val onEditorActionListener = object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onValidateListener?.invoke()
                    return true
                }
                return false
            }
        }

        passwordView.setOnEditorActionListener(onEditorActionListener)
        passwordView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun afterTextChanged(editable: Editable) {
                if (editable.toString().isNotEmpty() && !checkboxPasswordView.isChecked)
                    checkboxPasswordView.isChecked = true
            }
        })
        passwordView.setOnKeyListener { _, _, keyEvent ->
            var handled = false
            if (keyEvent.action == KeyEvent.ACTION_DOWN
                && keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                onValidateListener?.invoke()
                handled = true
            }
            handled
        }

        checkboxPasswordView.setOnCheckedChangeListener { view, checked ->
            onPasswordChecked?.onCheckedChanged(view, checked)
        }
    }

    fun setOpenKeyfileClickListener(externalFileHelper: ExternalFileHelper?) {
        keyFileSelectionView.setOpenDocumentClickListener(externalFileHelper)
    }

    fun populatePasswordTextView(text: String?) {
        if (text == null || text.isEmpty()) {
            passwordView.setText("")
            if (checkboxPasswordView.isChecked)
                checkboxPasswordView.isChecked = false
        } else {
            passwordView.setText(text)
            if (checkboxPasswordView.isChecked)
                checkboxPasswordView.isChecked = true
        }
    }

    fun populateKeyFileTextView(uri: Uri?) {
        if (uri == null || uri.toString().isEmpty()) {
            keyFileSelectionView.uri = null
            if (checkboxKeyFileView.isChecked)
                checkboxKeyFileView.isChecked = false
        } else {
            keyFileSelectionView.uri = uri
            if (!checkboxKeyFileView.isChecked)
                checkboxKeyFileView.isChecked = true
        }
    }

    fun isFill(): Boolean {
        return checkboxPasswordView.isChecked || checkboxKeyFileView.isChecked
    }

    fun getMainCredential(): MainCredential {
        return MainCredential().apply {
            this.masterPassword = if (checkboxPasswordView.isChecked)
                passwordView.text?.toString() else null
            this.keyFileUri = if (checkboxKeyFileView.isChecked)
                keyFileSelectionView.uri else null
        }
    }

    fun changeConditionToStoreCredential(credentialStorage: CredentialStorage) {
        this.mCredentialStorage = credentialStorage
    }

    fun conditionToStoreCredential(): Boolean {
        // TODO HARDWARE_KEY
        return when (mCredentialStorage) {
            CredentialStorage.PASSWORD -> checkboxPasswordView.isChecked
            CredentialStorage.KEY_FILE -> checkboxPasswordView.isChecked
            CredentialStorage.HARDWARE_KEY -> false
        }
    }

    /**
     * Return content of the store credential view allowed,
     * String? for password
     *
     */
    fun retrieveCredentialForStorage(listener: CredentialStorageListener): ByteArray? {
        return when (mCredentialStorage) {
            CredentialStorage.PASSWORD -> listener.passwordToStore(passwordView.text?.toString())
            CredentialStorage.KEY_FILE -> listener.keyfileToStore(keyFileSelectionView.uri)
            CredentialStorage.HARDWARE_KEY -> listener.hardwareKeyToStore()
        }
    }

    interface CredentialStorageListener {
        fun passwordToStore(password: String?): ByteArray?
        fun keyfileToStore(keyfile: Uri?): ByteArray?
        fun hardwareKeyToStore(): ByteArray?
    }

    fun requestPasswordFocus() {
        passwordView.requestFocusFromTouch()
    }

    // Auto select the password field and open keyboard
    fun focusPasswordFieldAndOpenKeyboard() {
        passwordView.postDelayed({
            passwordView.requestFocusFromTouch()
            val inputMethodManager = context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as? InputMethodManager?
            inputMethodManager?.showSoftInput(passwordView, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val saveState = SavedState(superState)
        saveState.mCredentialStorage = this.mCredentialStorage
        return saveState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        this.mCredentialStorage = state.mCredentialStorage ?: CredentialStorage.DEFAULT
    }

    internal class SavedState : BaseSavedState {
        var mCredentialStorage: CredentialStorage? = null

        constructor(superState: Parcelable?) : super(superState) {}

        private constructor(parcel: Parcel) : super(parcel) {
            mCredentialStorage = CredentialStorage.getFromOrdinal(parcel.readInt())
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(mCredentialStorage?.ordinal ?: 0)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}