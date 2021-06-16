package com.kunzisoft.keepass.view

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.IdRes
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.*
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.settings.PreferencesUtil


abstract class TemplateAbstractView @JvmOverloads constructor(context: Context,
                                                              attrs: AttributeSet? = null,
                                                              defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private var mTemplate: Template? = null
    protected var mEntryInfo: EntryInfo? = null

    protected var mCustomFieldIds = mutableListOf<FieldId>()

    protected var mFontInVisibility: Boolean = false
    protected var mHideProtectedValue: Boolean = false

    protected var headerContainerView: ViewGroup
    protected var entryIconView: ImageView
    protected var templateContainerView: ViewGroup
    protected var customFieldsContainerView: SectionView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_template, this)

        headerContainerView = findViewById(R.id.entry_edit_header_container)
        entryIconView = findViewById(R.id.entry_edit_icon_button)
        templateContainerView = findViewById(R.id.template_fields_container)
        // To fix card view margin in KitKat-
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val paddingVertical = resources.getDimensionPixelSize(R.dimen.card_view_margin_vertical)
            val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.card_view_margin_horizontal)
            templateContainerView.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        }
        customFieldsContainerView = findViewById(R.id.custom_fields_container)
    }

    // To show icon image
    var populateIconMethod: ((ImageView, IconImage) -> Unit)? = null

    fun setTemplate(template: Template?) {
        if (mTemplate != template) {
            mTemplate = template
            if (mEntryInfo != null) {
                populateEntryInfoWithViews(true)
            }
            buildTemplateAndPopulateInfo()
            clearFocus()
            (context.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager?)
                ?.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    fun buildTemplate() {
        // Retrieve preferences
        mHideProtectedValue = PreferencesUtil.hideProtectedValue(context)

        // Build each template section
        templateContainerView.removeAllViews()
        customFieldsContainerView.removeAllViews()
        mCustomFieldIds.clear()

        mTemplate?.let { template ->

            buildHeader()

            template.sections.forEach { templateSection ->

                val sectionView = SectionView(context, null, R.attr.cardViewStyle)
                // Add build view to parent
                templateContainerView.addView(sectionView)

                // Build each attribute
                templateSection.attributes.forEach { templateAttribute ->
                    val fieldTag: String
                    when {
                        templateAttribute.label.equals(TemplateField.LABEL_TITLE, true) -> {
                            throw Exception("title cannot be in template attribute")
                        }
                        templateAttribute.label.equals(TemplateField.LABEL_USERNAME, true) -> {
                            fieldTag = FIELD_USERNAME_TAG
                        }
                        templateAttribute.label.equals(TemplateField.LABEL_PASSWORD, true) -> {
                            fieldTag = FIELD_PASSWORD_TAG
                        }
                        templateAttribute.label.equals(TemplateField.LABEL_URL, true) -> {
                            fieldTag = FIELD_URL_TAG
                        }
                        templateAttribute.label.equals(
                            TemplateField.LABEL_EXPIRATION,
                            true
                        ) -> {
                            fieldTag = FIELD_EXPIRES_TAG
                        }
                        templateAttribute.label.equals(TemplateField.LABEL_NOTES, true) -> {
                            fieldTag = FIELD_NOTES_TAG
                        }
                        else -> {
                            fieldTag = FIELD_CUSTOM_TAG
                        }
                    }

                    val attributeView = buildViewForTemplateField(
                        templateAttribute,
                        Field(
                            templateAttribute.label,
                            ProtectedString(templateAttribute.protected, "")
                        ),
                        fieldTag
                    )
                    // Add created view to this parent
                    sectionView.addView(attributeView)
                }
            }
        }
    }

    abstract fun buildHeader()

    private fun buildViewForCustomField(field: Field): View? {
        val customFieldTemplateAttribute = TemplateAttribute(
            field.name,
            TemplateAttributeType.MULTILINE,
            field.protectedValue.isProtected,
            field.protectedValue.stringValue,
            TemplateAttributeAction.CUSTOM_EDITION)
        return buildViewForTemplateField(customFieldTemplateAttribute, field, FIELD_CUSTOM_TAG)
    }

    private fun buildViewForTemplateField(templateAttribute: TemplateAttribute,
                                          field: Field,
                                          fieldTag: String): View? {
        // Build main view depending on type
        val itemView: View? = when (templateAttribute.type) {
            TemplateAttributeType.INLINE,
            TemplateAttributeType.SMALL_MULTILINE,
            TemplateAttributeType.MULTILINE -> {
                buildLinearTextView(templateAttribute, field)
            }
            TemplateAttributeType.DATE,
            TemplateAttributeType.TIME,
            TemplateAttributeType.DATETIME -> {
                buildDataTimeView(templateAttribute, field)
            }
        }
        // Custom id defined by field name, use getViewByField(field: Field) to retrieve it
        itemView?.id = field.name.hashCode()
        itemView?.tag = fieldTag

        // Add new custom view id to the custom field list
        if (fieldTag == FIELD_CUSTOM_TAG) {
            val indexOldItem = indexCustomFieldIdByName(field.name)
            if (indexOldItem >= 0)
                mCustomFieldIds.removeAt(indexOldItem)
            mCustomFieldIds.add(FieldId(field.name, itemView!!.id, field.protectedValue.isProtected))
        }
        return itemView
    }

    protected abstract fun buildLinearTextView(templateAttribute: TemplateAttribute,
                                    field: Field): View?

    protected abstract fun buildDataTimeView(templateAttribute: TemplateAttribute,
                                  field: Field): View?

    abstract fun getActionImageView(): View?

    fun setFontInVisibility(fontInVisibility: Boolean) {
        this.mFontInVisibility = fontInVisibility
    }

    fun setHideProtectedValue(hideProtectedValue: Boolean) {
        this.mHideProtectedValue = hideProtectedValue
    }

    fun setEntryInfo(entryInfo: EntryInfo?) {
        mEntryInfo = entryInfo
        buildTemplateAndPopulateInfo()
    }

    protected abstract fun populateViewsWithEntryInfo()

    fun getEntryInfo(): EntryInfo {
        populateEntryInfoWithViews(true)
        return mEntryInfo ?: EntryInfo()
    }

    abstract fun populateEntryInfoWithViews(templateFieldNotEmpty: Boolean)

    fun reload() {
        buildTemplateAndPopulateInfo()
    }

    private fun buildTemplateAndPopulateInfo() {
        if (mTemplate != null && mEntryInfo != null) {
            buildTemplate()
            populateViewsWithEntryInfo()
        }
    }

    /* -------------
     * External value update
     * -------------
     */

    protected fun getFieldViewById(@IdRes viewId: Int): View? {
        return templateContainerView.findViewById(viewId)
            ?: customFieldsContainerView.findViewById(viewId)
    }

    /* -------------
     * Custom Fields
     * -------------
     */

    protected data class FieldId(var label: String, var viewId: Int, var protected: Boolean)

    private fun isStandardFieldName(name: String): Boolean {
        return TemplateField.isStandardFieldName(name)
    }

    protected fun customFieldIdByName(name: String): FieldId? {
        return mCustomFieldIds.find { it.label.equals(name, true) }
    }

    protected fun indexCustomFieldIdByName(name: String): Int {
        return mCustomFieldIds.indexOfFirst { it.label.equals(name, true) }
    }

    protected fun retrieveCustomFieldsFromView(templateFieldNotEmpty: Boolean = false) {
        mEntryInfo?.customFields = mCustomFieldIds.mapNotNull {
            getCustomField(it.label, templateFieldNotEmpty)
        }.toMutableList()
    }

    protected fun getCustomField(fieldName: String): Field {
        return getCustomField(fieldName, false)
            ?: Field(fieldName, ProtectedString(false, ""))
    }

    protected abstract fun getCustomField(fieldName: String, templateFieldNotEmpty: Boolean): Field?

    /**
     * Update a custom field or create a new one if doesn't exists, the old value is lost
     */
    protected fun putCustomField(customField: Field, focus: Boolean): Boolean {
        return if (!isStandardFieldName(customField.name)) {
            customFieldsContainerView.visibility = View.VISIBLE
            if (indexCustomFieldIdByName(customField.name) >= 0) {
                replaceCustomField(customField, customField, focus)
            } else {
                val newCustomView = buildViewForCustomField(customField)
                customFieldsContainerView.addView(newCustomView)
                val fieldId = FieldId(customField.name,
                    newCustomView!!.id,
                    customField.protectedValue.isProtected)
                val indexOldItem = indexCustomFieldIdByName(fieldId.label)
                if (indexOldItem >= 0)
                    mCustomFieldIds.removeAt(indexOldItem)
                mCustomFieldIds.add(indexOldItem, fieldId)
                if (focus)
                    newCustomView.requestFocus()
                true
            }
        } else {
            false
        }
    }

    fun putCustomField(customField: Field): Boolean {
        val put = putCustomField(customField, true)
        retrieveCustomFieldsFromView()
        return put
    }

    /**
     * Update a custom field and keep the old value
     */
    private fun replaceCustomField(oldField: Field, newField: Field, focus: Boolean): Boolean {
        if (!isStandardFieldName(newField.name)) {
            customFieldIdByName(oldField.name)?.viewId?.let { viewId ->
                customFieldsContainerView.findViewById<View>(viewId)?.let { viewToReplace ->
                    val oldValue = getCustomField(oldField.name).protectedValue.toString()

                    val parentGroup = viewToReplace.parent as ViewGroup
                    val indexInParent = parentGroup.indexOfChild(viewToReplace)
                    parentGroup.removeView(viewToReplace)

                    val newCustomFieldWithValue = Field(newField.name,
                        ProtectedString(newField.protectedValue.isProtected, oldValue))
                    val oldPosition = indexCustomFieldIdByName(oldField.name)
                    if (oldPosition >= 0)
                        mCustomFieldIds.removeAt(oldPosition)

                    val newCustomView = buildViewForCustomField(newCustomFieldWithValue)
                    parentGroup.addView(newCustomView, indexInParent)
                    mCustomFieldIds.add(oldPosition, FieldId(newCustomFieldWithValue.name,
                        newCustomView!!.id,
                        newCustomFieldWithValue.protectedValue.isProtected))
                    if (focus)
                        newCustomView.requestFocus()
                    return true
                }
            }
        }
        return false
    }

    fun replaceCustomField(oldField: Field, newField: Field): Boolean {
        val replace = replaceCustomField(oldField, newField, true)
        retrieveCustomFieldsFromView()
        return replace
    }

    fun removeCustomField(oldCustomField: Field) {
        val indexOldField = indexCustomFieldIdByName(oldCustomField.name)
        if (indexOldField >= 0) {
            mCustomFieldIds[indexOldField].viewId.let { viewId ->
                customFieldsContainerView.removeViewById(viewId)
            }
            mCustomFieldIds.removeAt(indexOldField)
        }
        retrieveCustomFieldsFromView()
    }

    fun putOtpElement(otpElement: OtpElement) {
        val otpField = OtpEntryFields.buildOtpField(otpElement,
            mEntryInfo?.title, mEntryInfo?.username)
        putCustomField(Field(otpField.name, otpField.protectedValue))
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        //begin boilerplate code so parent classes can restore state
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        } else {
            mTemplate = state.template
            mEntryInfo = state.entryInfo
            onRestoreEntryInstanceState(state)
            buildTemplateAndPopulateInfo()
            super.onRestoreInstanceState(state.superState)
        }
    }

    protected open fun onRestoreEntryInstanceState(state: SavedState) {}

    override fun onSaveInstanceState(): Parcelable {
        val superSave = super.onSaveInstanceState()
        val saveState = SavedState(superSave)
        populateEntryInfoWithViews(false)
        saveState.template = this.mTemplate
        saveState.entryInfo = this.mEntryInfo
        onSaveEntryInstanceState(saveState)
        return saveState
    }

    protected open fun onSaveEntryInstanceState(savedState: SavedState) {}

    protected class SavedState : BaseSavedState {
        var template: Template? = null
        var entryInfo: EntryInfo? = null
        // TODO Move
        var tempDateTimeViewId: Int? = null

        constructor(superState: Parcelable?) : super(superState)

        private constructor(parcel: Parcel) : super(parcel) {
            template = parcel.readParcelable(Template::class.java.classLoader)
                ?: template
            entryInfo = parcel.readParcelable(EntryInfo::class.java.classLoader)
                ?: entryInfo
            val dateTimeViewId = parcel.readInt()
            if (dateTimeViewId != -1)
                tempDateTimeViewId = dateTimeViewId
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(template, flags)
            out.writeParcelable(entryInfo, flags)
            out.writeInt(tempDateTimeViewId ?: -1)
        }

        companion object {
            //required field that makes Parcelables from a Parcel
            @JvmField val CREATOR = object : Creator<SavedState?> {
                override fun createFromParcel(parcel: Parcel): SavedState {
                    return SavedState(parcel)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        const val FIELD_TITLE_TAG = "FIELD_TITLE_TAG"
        const val FIELD_USERNAME_TAG = "FIELD_USERNAME_TAG"
        const val FIELD_PASSWORD_TAG = "FIELD_PASSWORD_TAG"
        const val FIELD_URL_TAG = "FIELD_URL_TAG"
        const val FIELD_EXPIRES_TAG = "FIELD_EXPIRES_TAG"
        const val FIELD_NOTES_TAG = "FIELD_NOTES_TAG"
        const val FIELD_CUSTOM_TAG = "FIELD_CUSTOM_TAG"
    }
}