package com.kunzisoft.keepass.view

import android.content.Context
import android.graphics.Color
import com.google.android.material.textfield.TextInputLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.PwIcon
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.icons.IconDrawableFactory
import com.kunzisoft.keepass.icons.assignDatabaseIcon
import com.kunzisoft.keepass.icons.assignDefaultDatabaseIcon
import com.kunzisoft.keepass.model.Field

class EntryEditContentsView @JvmOverloads constructor(context: Context,
                                                      attrs: AttributeSet? = null,
                                                      defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private var fontInVisibility: Boolean = false

    private val entryTitleLayoutView: TextInputLayout
    private val entryTitleView: EditText
    private val entryPasswordLayoutView: TextInputLayout
    private val entryCommentView: EditText
    private val entryExtraFieldsContainer: ViewGroup

    private var iconColor: Int = 0

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_entry_edit_contents, this)

        entryTitleLayoutView = findViewById(R.id.entry_edit_container_title)
        entryTitleView = findViewById(R.id.entry_edit_title)
        entryPasswordLayoutView = findViewById(R.id.entry_edit_container_password)
        entryCommentView = findViewById(R.id.entry_edit_notes)
        entryExtraFieldsContainer = findViewById(R.id.entry_edit_advanced_container)

        // Retrieve the textColor to tint the icon
        val taIconColor = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.textColor))
        iconColor = taIconColor.getColor(0, Color.WHITE)
        taIconColor.recycle()
    }

    fun applyFontVisibilityToFields(fontInVisibility: Boolean) {
        this.fontInVisibility = fontInVisibility
    }

    var title: String
        get() {
            return entryTitleView.text.toString()
        }
        set(value) {
            entryTitleView.setText(value)
            if (fontInVisibility)
                entryTitleView.applyFontVisibility()
        }

    fun setDefaultIcon(iconFactory: IconDrawableFactory) {
    }

    fun setIcon(iconFactory: IconDrawableFactory, icon: PwIcon) {
    }

    fun setOnIconViewClickListener(clickListener: () -> Unit) {
    }

    fun setOnPasswordGeneratorClickListener(clickListener: () -> Unit) {
    }

    var notes: String
        get() {
            return entryCommentView.text.toString()
        }
        set(value) {
            entryCommentView.setText(value)
            if (fontInVisibility)
                entryCommentView.applyFontVisibility()
        }

    val customFields: MutableList<Field>
        get() {
            val customFieldsArray = ArrayList<Field>()
            // Add extra fields from views
            entryExtraFieldsContainer.let {
                for (i in 0 until it.childCount) {
                    val view = it.getChildAt(i) as EntryEditCustomField
                    val key = view.label
                    val value = view.value
                    val protect = view.isProtected
                    customFieldsArray.add(Field(key, ProtectedString(protect, value)))
                }
            }
            return customFieldsArray
        }

    /**
     * Add a new view to fill in the information of the customized field
     */
    fun addNewCustomField(name: String = "", value:ProtectedString = ProtectedString(false, "")) {
        val entryEditCustomField = EntryEditCustomField(context)
        entryEditCustomField.setData(name, value)
        entryEditCustomField.setFontVisibility(fontInVisibility)
        entryExtraFieldsContainer.addView(entryEditCustomField)
    }

    /**
     * Validate or not the entry form
     *
     * @return ErrorValidation An error with a message or a validation without message
     */
    fun isValid(): Boolean {
        var isValid = true

        // Require title
        if (entryTitleView.text.toString().isEmpty()) {
            entryTitleLayoutView.error = context.getString(R.string.error_title_required)
            isValid = false
        } else {
            entryTitleLayoutView.error = null
        }

        // Validate extra fields
        entryExtraFieldsContainer.let {
            for (i in 0 until it.childCount) {
                val entryEditCustomField = it.getChildAt(i) as EntryEditCustomField
                if (!entryEditCustomField.isValid()) {
                    isValid = false
                }
            }
        }
        return isValid
    }

}