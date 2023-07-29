package com.kunzisoft.keepass.view

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.UriUtil.getDocumentFile


class KeyFileSelectionView @JvmOverloads constructor(context: Context,
                                                     attrs: AttributeSet? = null,
                                                     defStyle: Int = 0)
    : ConstraintLayout(context, attrs, defStyle) {

    private var mUri: Uri? = null

    private val keyFileNameInputLayout: TextInputLayout
    private val keyFileNameView: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_keyfile_selection, this)

        keyFileNameInputLayout = findViewById(R.id.input_entry_keyfile)
        keyFileNameView = findViewById(R.id.keyfile_name)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        keyFileNameView.setOnClickListener(l)
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        super.setOnLongClickListener(l)
        keyFileNameView.setOnLongClickListener(l)
    }

    var error: CharSequence?
        get() = keyFileNameInputLayout.error
        set(value) {
            keyFileNameInputLayout.error = value
        }

    var uri: Uri?
        get() {
            return mUri
        }
        set(value) {
            mUri = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                value?.let { keyFileUri ->
                    val mimeType = context?.contentResolver?.getType(keyFileUri)
                    error = if (mimeType?.contains("image") == true) {
                        context.getString(R.string.warning_keyfile_integrity)
                    } else {
                        null
                    }
                }
            }
            keyFileNameView.text = value?.let {
                value.getDocumentFile(context)?.name ?: value.path
            } ?: ""
        }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val saveState = SavedState(superState)
        saveState.mUri = this.mUri
        return saveState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        this.mUri = state.mUri
    }

    internal class SavedState : BaseSavedState {
        var mUri: Uri? = null

        constructor(superState: Parcelable?) : super(superState) {}

        private constructor(parcel: Parcel) : super(parcel) {
            mUri = parcel.readParcelableCompat()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(mUri, flags)
        }

        companion object CREATOR : Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}