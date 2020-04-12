package com.kunzisoft.keepass.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.textfield.TextInputLayout
import com.kunzisoft.keepass.R

class KeyFileSelectionView @JvmOverloads constructor(context: Context,
                                                     attrs: AttributeSet? = null,
                                                     defStyle: Int = 0)
    : ConstraintLayout(context, attrs, defStyle) {

    private var mUri: Uri? = null

    private val keyFileNameInputLayout: TextInputLayout
    private val keyFileNameView: TextView
    private val keyFileOpenView: ImageView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_keyfile_selection, this)

        keyFileNameInputLayout = findViewById(R.id.input_entry_keyfile)
        keyFileNameView = findViewById(R.id.keyfile_name)
        keyFileOpenView = findViewById(R.id.keyfile_open_button)
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
            keyFileNameView.text = value?.let {
                DocumentFile.fromSingleUri(context, value)?.name ?: ""
            } ?: ""
        }
}