package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.IdRes
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.*
import com.kunzisoft.keepass.database.element.template.TemplateEngine.Companion.addTemplateDecorator
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.KeyboardUtil.hideKeyboard
import com.kunzisoft.keepass.utils.readParcelableCompat


abstract class TemplateAbstractView<
        TEntryFieldView: GenericTextFieldView,
        TEntrySelectFieldView: GenericTextFieldView,
        TDateTimeView: GenericDateTimeFieldView> @JvmOverloads constructor(context: Context,
                                                                           attrs: AttributeSet? = null,
                                                                           defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private var mTemplate: Template? = null
    protected var mEntryInfo: EntryInfo? = null

    private var mViewFields = mutableListOf<ViewField>()

    protected var mFontInVisibility: Boolean = PreferencesUtil.fieldFontIsInVisibility(context)
    protected var mHideProtectedValue: Boolean = PreferencesUtil.hideProtectedValue(context)

    protected var headerContainerView: ViewGroup
    protected var entryIconView: ImageView
    protected var backgroundColorView: View
    protected var foregroundColorView: View
    protected var backgroundColorButton: ImageView
    protected var foregroundColorButton: ImageView
    private var titleContainerView: ViewGroup
    protected var templateContainerView: ViewGroup
    private var customFieldsContainerView: SectionView
    private var notReferencedFieldsContainerView: SectionView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_template, this)

        headerContainerView = findViewById(R.id.template_header_container)
        entryIconView = findViewById(R.id.template_icon_button)
        backgroundColorView = findViewById(R.id.template_background_color)
        foregroundColorView = findViewById(R.id.template_foreground_color)
        backgroundColorButton = findViewById(R.id.template_background_color_button)
        foregroundColorButton = findViewById(R.id.template_foreground_color_button)
        titleContainerView = findViewById(R.id.template_title_container)
        templateContainerView = findViewById(R.id.template_fields_container)
        // To fix card view margin below Marshmallow
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val paddingVertical = resources.getDimensionPixelSize(R.dimen.card_view_margin_vertical)
            val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.card_view_margin_horizontal)
            templateContainerView.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        }
        customFieldsContainerView = findViewById(R.id.custom_fields_container)
        notReferencedFieldsContainerView = findViewById(R.id.not_referenced_fields_container)
    }

    // To show icon image
    var populateIconMethod: ((ImageView, IconImage) -> Unit)? = null
        set(value) {
            field = value
            refreshIcon()
        }

    fun refreshIcon() {
        mEntryInfo?.icon?.let {
            populateIconMethod?.invoke(entryIconView, it)
        }
    }

    fun setTemplate(template: Template?) {
        if (mTemplate != template) {
            mTemplate = template
            applyTemplateParametersToEntry()
            if (mEntryInfo != null) {
                populateEntryInfoWithViews(templateFieldNotEmpty = true,
                                           retrieveDefaultValues = false)
            }
            buildTemplateAndPopulateInfo()
            clearFocus()
            hideKeyboard()
        }
    }

    private fun applyTemplateParametersToEntry() {
        // Change the entry icon by the template icon
        mTemplate?.icon?.let { templateIcon ->
            mEntryInfo?.icon = templateIcon
        }
        // Change the entry color by the template color
        mEntryInfo?.backgroundColor = mTemplate?.backgroundColor
        mEntryInfo?.foregroundColor = mTemplate?.foregroundColor
    }

    private fun buildTemplate() {
        // Retrieve preferences
        mHideProtectedValue = PreferencesUtil.hideProtectedValue(context)

        // Build each template section
        titleContainerView.removeAllViews()
        templateContainerView.removeAllViews()
        customFieldsContainerView.removeAllViews()
        notReferencedFieldsContainerView.removeAllViews()
        mViewFields.clear()

        mTemplate?.let { template ->

            preProcessTemplate()

            // Create title view
            val titleAttribute = TemplateAttribute(
                TemplateField.LABEL_TITLE,
                TemplateAttributeType.TEXT).apply {
                    default = template.title
            }
            val titleView = buildViewForTemplateField(
                titleAttribute,
                Field(
                    titleAttribute.label,
                    ProtectedString(titleAttribute.protected, "")
                ),
                FIELD_TITLE_TAG
            )
            titleContainerView.addView(titleView)

            // Build each section
            template.sections.forEach { templateSection ->

                val sectionView = SectionView(context, null, R.attr.cardViewStyle)

                // Build each attribute
                templateSection.attributes.forEach { templateAttribute ->
                    if (templateAttribute.label != TemplateField.LABEL_TITLE) {
                        val fieldTag: String =
                            getTagFromStandardTemplateAttribute(templateAttribute)

                        val attributeView = buildViewForTemplateField(
                            templateAttribute,
                            Field(
                                templateAttribute.label,
                                ProtectedString(templateAttribute.protected,
                                    templateAttribute.default)
                            ),
                            fieldTag
                        )
                        // Add created view to this parent
                        sectionView.addView(attributeView)
                    }
                }
                // Add build view to parent
                templateContainerView.addView(sectionView)
            }
        }
    }

    private fun getTagFromStandardTemplateAttribute(templateAttribute: TemplateAttribute): String {
        val fieldTag: String
        when {
            templateAttribute.label.equals(TemplateField.LABEL_TITLE, true) -> {
                fieldTag = FIELD_TITLE_TAG
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
            templateAttribute.label.equals(TemplateField.LABEL_EXPIRATION,true) -> {
                fieldTag = FIELD_EXPIRES_TAG
            }
            templateAttribute.label.equals(TemplateField.LABEL_NOTES, true) -> {
                fieldTag = FIELD_NOTES_TAG
            }
            else -> {
                fieldTag = FIELD_CUSTOM_TAG
            }
        }
        return fieldTag
    }

    abstract fun preProcessTemplate()

    /**
     * Not referenced fields are standard fields with content but not in template
     */
    private fun buildViewForNotReferencedField(field: Field, templateAttribute: TemplateAttribute): View? {
        val fieldTag: String = getTagFromStandardTemplateAttribute(templateAttribute)
        return buildViewForTemplateField(templateAttribute, field, fieldTag)
    }

    private fun buildViewForCustomField(field: Field): View? {
        val customFieldTemplateAttribute = TemplateAttribute(
            field.name,
            TemplateAttributeType.TEXT,
            field.protectedValue.isProtected,
            TemplateAttributeOption().apply {
                setNumberLines(20)
            },
            TemplateAttributeAction.CUSTOM_EDITION
        )
        return buildViewForTemplateField(customFieldTemplateAttribute, field, FIELD_CUSTOM_TAG)
    }

    private fun buildViewForTemplateField(templateAttribute: TemplateAttribute,
                                          field: Field,
                                          fieldTag: String): View? {
        // Build main view depending on type
        val itemView: View? = when (templateAttribute.type) {
            TemplateAttributeType.TEXT -> {
                buildLinearTextView(templateAttribute, field) as View?
            }
            TemplateAttributeType.LIST -> {
                buildListItemsView(templateAttribute, field) as View?
            }
            TemplateAttributeType.DATETIME -> {
                buildDataTimeView(templateAttribute, field) as View?
            }
            TemplateAttributeType.DIVIDER -> null
        }
        // Custom id defined by field name, use getViewByField(field: Field) to retrieve it
        itemView?.id = field.name.hashCode()
        itemView?.tag = fieldTag

        // Add new custom view id to the custom field list
        if (fieldTag == FIELD_CUSTOM_TAG) {
            val indexOldItem = getIndexViewFieldByName(field.name)
            if (indexOldItem >= 0)
                mViewFields.removeAt(indexOldItem)
            if (itemView?.id != null) {
                mViewFields.add(
                    ViewField(
                        itemView,
                        field
                    )
                )
            }
        }
        return itemView
    }

    protected abstract fun buildLinearTextView(templateAttribute: TemplateAttribute,
                                               field: Field): TEntryFieldView?

    protected abstract fun buildListItemsView(templateAttribute: TemplateAttribute,
                                              field: Field): TEntrySelectFieldView?

    protected abstract fun buildDataTimeView(templateAttribute: TemplateAttribute,
                                             field: Field): TDateTimeView?

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

    @Suppress("UNCHECKED_CAST")
    private fun populateEntryFieldView(fieldTag: String,
                                       templateAttribute: TemplateAttribute,
                                       entryInfoValue: String,
                                       showEmptyFields: Boolean) {
        try {
            var fieldView: TEntryFieldView? = findViewWithTag(fieldTag)
            if (!showEmptyFields && entryInfoValue.isEmpty()) {
                fieldView?.isFieldVisible = false
            } else if (fieldView == null && entryInfoValue.isNotEmpty()) {
                // Add new not referenced view if standard field not in template
                fieldView = buildViewForNotReferencedField(
                    Field(templateAttribute.label,
                        ProtectedString(templateAttribute.protected, "")),
                    templateAttribute
                ) as? TEntryFieldView?
                fieldView?.let {
                    addNotReferencedView(it as View)
                }
            }
            fieldView?.value = entryInfoValue
            fieldView?.applyFontVisibility(mFontInVisibility)
        } catch(e: Exception) {
            Log.e(TAG, "Unable to populate entry field view", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun populateDateTimeView(fieldTag: String,
                                     templateAttribute: TemplateAttribute,
                                     expires: Boolean,
                                     expiryTime: DateInstant,
                                     showEmptyFields: Boolean) {
        try {
            var fieldView: TDateTimeView? = findViewWithTag(fieldTag)
            if (!showEmptyFields && !expires) {
                fieldView?.isFieldVisible = false
            } else if (fieldView == null && expires) {
                fieldView = buildViewForNotReferencedField(
                        Field(templateAttribute.label,
                                ProtectedString(templateAttribute.protected, "")),
                        templateAttribute
                ) as? TDateTimeView?
                fieldView?.let {
                    addNotReferencedView(it as View)
                }
            }
            fieldView?.activation = expires
            fieldView?.dateTime = expiryTime
        } catch(e: Exception) {
            Log.e(TAG, "Unable to populate date time view", e)
        }
    }

    /**
     * Return empty custom fields
     */
    protected open fun populateViewsWithEntryInfo(showEmptyFields: Boolean): List<ViewField> {
        mEntryInfo?.let { entryInfo ->

            populateEntryFieldView(FIELD_TITLE_TAG,
                Template.TITLE_ATTRIBUTE,
                entryInfo.title,
                showEmptyFields)
            populateEntryFieldView(FIELD_USERNAME_TAG,
                Template.USERNAME_ATTRIBUTE,
                entryInfo.username,
                showEmptyFields)
            populateEntryFieldView(FIELD_PASSWORD_TAG,
                Template.PASSWORD_ATTRIBUTE,
                entryInfo.password,
                showEmptyFields)
            populateEntryFieldView(FIELD_URL_TAG,
                Template.URL_ATTRIBUTE,
                entryInfo.url,
                showEmptyFields)
            populateDateTimeView(FIELD_EXPIRES_TAG,
                Template.EXPIRATION_ATTRIBUTE,
                entryInfo.expires,
                entryInfo.expiryTime,
                showEmptyFields)
            populateEntryFieldView(FIELD_NOTES_TAG,
                Template.NOTES_ATTRIBUTE,
                entryInfo.notes,
                showEmptyFields)

            customFieldsContainerView.removeAllViews()
            val emptyCustomFields = mutableListOf<ViewField>().also { it.addAll(mViewFields) }
            entryInfo.customFields.forEach { customField ->
                val indexFieldViewId = getIndexViewFieldByName(customField.name)
                if (indexFieldViewId >= 0) {
                    // Template contains the custom view
                    val viewField = mViewFields[indexFieldViewId]
                    emptyCustomFields.remove(viewField)
                    viewField.view.let { customView ->
                            if (customView is GenericTextFieldView) {
                                customView.value = customField.protectedValue.stringValue
                                customView.applyFontVisibility(mFontInVisibility)
                            } else if (customView is GenericDateTimeFieldView) {
                                try {
                                    customView.activation = true
                                    customView.dateTime = DateInstant(customField
                                        .protectedValue.stringValue)
                                } catch (e: Exception) {
                                    customView.activation = false
                                    customView.dateTime = DateInstant.NEVER_EXPIRES
                                    Log.e(TAG, "unable to populate date time view", e)
                                }
                            }
                        }
                } else {
                    // If template view not found, create a new custom view
                    putCustomField(customField, false)
                }
            }
            return emptyCustomFields
        }
        return emptyList()
    }

    protected open fun populateEntryInfoWithViews(templateFieldNotEmpty: Boolean,
                                                  retrieveDefaultValues: Boolean) {
        if (mEntryInfo == null)
            mEntryInfo = EntryInfo()

        try {
            val titleView: TEntryFieldView? = findViewWithTag(FIELD_TITLE_TAG)
            titleView?.value?.let {
                mEntryInfo?.title = it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to populate title view", e)
        }

        try {
            val userNameView: TEntryFieldView? = findViewWithTag(FIELD_USERNAME_TAG)
            userNameView?.value?.let {
                mEntryInfo?.username = it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to populate username view", e)
        }

        try {
            val passwordView: TEntryFieldView? = findViewWithTag(FIELD_PASSWORD_TAG)
            passwordView?.value?.let {
                mEntryInfo?.password = it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to populate password view", e)
        }

        try {
            val urlView: TEntryFieldView? = findViewWithTag(FIELD_URL_TAG)
            urlView?.value?.let {
                mEntryInfo?.url = it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to populate url view", e)
        }

        try {
            val expirationView: TDateTimeView? = findViewWithTag(FIELD_EXPIRES_TAG)
            expirationView?.activation?.let {
                mEntryInfo?.expires = it
            }
            expirationView?.dateTime?.let {
                mEntryInfo?.expiryTime = it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to populate expiration view", e)
        }

        try {
            val notesView: TEntryFieldView? = findViewWithTag(FIELD_NOTES_TAG)
            notesView?.value?.let {
                mEntryInfo?.notes = it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to populate notes view", e)
        }

        retrieveCustomFieldsFromView(templateFieldNotEmpty, retrieveDefaultValues)
    }

    fun getEntryInfo(): EntryInfo {
        populateEntryInfoWithViews(templateFieldNotEmpty = true,
                                   retrieveDefaultValues = true)
        return mEntryInfo ?: EntryInfo()
    }

    fun reload() {
        buildTemplateAndPopulateInfo()
    }

    private fun buildTemplateAndPopulateInfo() {
        if (mTemplate != null && mEntryInfo != null) {
            buildTemplate()
            populateViewsWithEntryInfo(true)
        }
    }

    /* -------------
     * External value update
     * -------------
     */

    protected fun getFieldViewById(@IdRes viewId: Int): View? {
        return titleContainerView.findViewById(viewId)
            ?: templateContainerView.findViewById(viewId)
            ?: customFieldsContainerView.findViewById(viewId)
            ?: notReferencedFieldsContainerView.findViewById(viewId)
    }

    /* -------------
     * Custom Fields
     * -------------
     */

    protected data class ViewField(var view: View, var field: Field)

    private fun isStandardFieldName(name: String): Boolean {
        return TemplateField.isStandardFieldName(name)
    }

    protected fun getViewFieldByName(name: String): ViewField? {
        return mViewFields.find { it.field.name.equals(name, true) }
    }

    private fun getIndexViewFieldByName(name: String): Int {
        return mViewFields.indexOfFirst { it.field.name.equals(name, true) }
    }

    private fun retrieveCustomFieldsFromView(templateFieldNotEmpty: Boolean = false,
                                             retrieveDefaultValues: Boolean = false) {
        mEntryInfo?.customFields = mViewFields.mapNotNull {
            getCustomField(it.field.name, templateFieldNotEmpty, retrieveDefaultValues)
        }.toMutableList()
    }

    protected fun getCustomField(fieldName: String): Field {
        return getCustomField(fieldName,
            templateFieldNotEmpty = false,
            retrieveDefaultValues = false
        ) ?: Field(fieldName, ProtectedString(false))
    }

    private fun getCustomField(fieldName: String,
                               templateFieldNotEmpty: Boolean,
                               retrieveDefaultValues: Boolean): Field? {
        getViewFieldByName(fieldName)?.let { fieldId ->
            val editView: View = fieldId.view
            if (editView is GenericFieldView) {
                // Do not return field with a default value
                val defaultViewValue =
                    if (retrieveDefaultValues || editView.value != editView.default) {
                        editView.value
                    } else ""
                if (!templateFieldNotEmpty
                    || (editView.tag == FIELD_CUSTOM_TAG && defaultViewValue.isNotEmpty())) {
                    return Field(
                        fieldName,
                        ProtectedString(fieldId.field.protectedValue.isProtected, defaultViewValue)
                    )
                }
            }
        }
        return null
    }

    /**
     * Update a custom field or create a new one if doesn't exists, the old value is lost
     */
    private fun putCustomField(customField: Field, focus: Boolean): Boolean {
        if (mTemplate == TemplateEngine.CREATION
            && !TemplateEngine.containsTemplateDecorator(customField.name)) {
            customField.name = addTemplateDecorator(customField.name)
        }

        return if (!isStandardFieldName(customField.name)) {
            customFieldsContainerView.visibility = View.VISIBLE
            if (getIndexViewFieldByName(customField.name) >= 0) {
                // Update a custom field with a new value,
                // new field name must be the same as old field name
                replaceCustomField(customField, customField, false, focus)
            } else {
                val newCustomView = buildViewForCustomField(customField)
                newCustomView?.let {
                    customFieldsContainerView.addView(newCustomView)
                    val fieldId = ViewField(
                        newCustomView,
                        customField
                    )
                    val indexOldItem = getIndexViewFieldByName(fieldId.field.name)
                    if (indexOldItem >= 0)
                        mViewFields.removeAt(indexOldItem)
                    mViewFields.add(indexOldItem, fieldId)
                    if (focus)
                        newCustomView.requestFocus()
                }
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

    private fun replaceCustomField(oldField: Field,
                                   newField: Field,
                                   keepOldValue: Boolean,
                                   focus: Boolean): Boolean {
        if (!isStandardFieldName(newField.name)) {
            getViewFieldByName(oldField.name)?.view?.let { viewToReplace ->
                val oldValue = getCustomField(oldField.name).protectedValue.toString()

                val parentGroup = viewToReplace.parent as ViewGroup
                val indexInParent = parentGroup.indexOfChild(viewToReplace)
                parentGroup.removeView(viewToReplace)

                val newCustomFieldWithValue = if (keepOldValue)
                    Field(newField.name,
                        ProtectedString(newField.protectedValue.isProtected, oldValue)
                    )
                else
                    newField
                val oldPosition = getIndexViewFieldByName(oldField.name)
                if (oldPosition >= 0)
                    mViewFields.removeAt(oldPosition)

                val newCustomView = buildViewForCustomField(newCustomFieldWithValue)
                newCustomView?.let {
                    parentGroup.addView(newCustomView, indexInParent)
                    mViewFields.add(
                        oldPosition,
                        ViewField(
                            newCustomView,
                            newCustomFieldWithValue
                        )
                    )
                    if (focus)
                        newCustomView.requestFocus()
                }
                return true
            }
        }
        return false
    }

    /**
     * Update a custom field and keep the old value
     */
    fun replaceCustomField(oldField: Field, newField: Field): Boolean {
        val replace = replaceCustomField(oldField, newField, keepOldValue = true, focus = true)
        retrieveCustomFieldsFromView()
        return replace
    }

    fun removeCustomField(oldCustomField: Field) {
        val indexOldField = getIndexViewFieldByName(oldCustomField.name)
        if (indexOldField >= 0) {
            mViewFields[indexOldField].let { fieldView ->
                customFieldsContainerView.removeViewById(fieldView.view.id)
            }
            mViewFields.removeAt(indexOldField)
        }
        retrieveCustomFieldsFromView()
    }

    private fun addNotReferencedView(view: View) {
        notReferencedFieldsContainerView.addView(view)
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
        populateEntryInfoWithViews(templateFieldNotEmpty = false,
                                   retrieveDefaultValues = false)
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
            template = parcel.readParcelableCompat() ?: template
            entryInfo = parcel.readParcelableCompat() ?: entryInfo
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

        private val TAG = TemplateAbstractView::class.java.name
    }
}