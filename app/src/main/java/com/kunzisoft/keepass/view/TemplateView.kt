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
import com.kunzisoft.keepass.model.EntryInfo
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.settings.PreferencesUtil
import org.joda.time.DateTime


class TemplateView @JvmOverloads constructor(context: Context,
                                             attrs: AttributeSet? = null,
                                             defStyle: Int = 0)
    : FrameLayout(context, attrs, defStyle) {

    private var mTemplate: Template? = null
    private var mEntryInfo: EntryInfo? = null

    private var mCustomFieldIds = mutableListOf<FieldId>()

    private var mHideProtectedValue: Boolean = false
    private var mFontInVisibility: Boolean = false

    private var entryIconView: ImageView
    private var entryTitleView: EntryEditFieldView
    private var templateContainerView: ViewGroup
    private var customFieldsContainerView: SectionView

    // Current date time selection
    @IdRes
    private var mTempDateTimeViewId: Int? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        inflater?.inflate(R.layout.view_template, this)

        entryIconView = findViewById(R.id.entry_edit_icon_button)
        entryTitleView = findViewById(R.id.entry_edit_title)
        templateContainerView = findViewById(R.id.template_fields_container)
        // To fix card view margin in KitKat-
        val paddingVertical = resources.getDimensionPixelSize(R.dimen.card_view_margin_vertical)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.card_view_margin_horizontal)
            templateContainerView.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        }
        customFieldsContainerView = findViewById(R.id.custom_fields_container)

        buildTemplateAndPopulateInfo()
    }

    fun setOnIconClickListener(onClickListener: OnClickListener) {
        entryIconView.setOnClickListener(onClickListener)
    }

    private var mOnCustomEditionActionClickListener: ((Field) -> Unit)? = null
    fun setOnCustomEditionActionClickListener(listener: ((Field) -> Unit)?) {
        this.mOnCustomEditionActionClickListener = listener
    }

    private var mOnPasswordGenerationActionClickListener: ((Field) -> Unit)? = null
    fun setOnPasswordGenerationActionClickListener(listener: ((Field) -> Unit)?) {
        this.mOnPasswordGenerationActionClickListener = listener
    }

    private var mOnDateInstantClickListener: ((DateInstant) -> Unit)? = null
    fun setOnDateInstantClickListener(listener: ((DateInstant) -> Unit)?) {
        this.mOnDateInstantClickListener = listener
    }

    // To show icon image
    var populateIconMethod: ((ImageView, IconImage) -> Unit)? = null

    fun setTemplate(template: Template?) {
        mTemplate = template
        buildTemplateAndPopulateInfo()
    }

    fun buildTemplate() {
        // Retrieve preferences
        mHideProtectedValue = PreferencesUtil.hideProtectedValue(context)

        // Build each template section
        templateContainerView.removeAllViews()
        customFieldsContainerView.removeAllViews()
        mCustomFieldIds.clear()

        mTemplate?.sections?.forEach { templateSection ->

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

    private fun buildViewForCustomField(field: Field): View? {
        val customFieldTemplateAttribute = TemplateAttribute(
                field.name,
                TemplateAttributeType.INLINE,
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

    private fun buildLinearTextView(templateAttribute: TemplateAttribute,
                                    field: Field): View? {
        // Add an action icon if needed
        return context?.let {
            EntryEditFieldView(it).apply {
                label = TemplateField.getLocalizedName(context, field.name)
                setProtection(field.protectedValue.isProtected, mHideProtectedValue)
                setType(when (templateAttribute.type) {
                    TemplateAttributeType.MULTILINE -> EntryEditFieldView.TextType.MULTI_LINE
                    else -> EntryEditFieldView.TextType.NORMAL
                })
                value = field.protectedValue.stringValue
                when (templateAttribute.action) {
                    TemplateAttributeAction.NONE -> {
                        setOnActionClickListener(null)
                    }
                    TemplateAttributeAction.CUSTOM_EDITION -> {
                        setOnActionClickListener({
                            mOnCustomEditionActionClickListener?.invoke(field)
                        }, R.drawable.ic_more_white_24dp)
                    }
                    TemplateAttributeAction.PASSWORD_GENERATION -> {
                        setOnActionClickListener({
                            mOnPasswordGenerationActionClickListener?.invoke(field)
                        }, R.drawable.ic_generate_password_white_24dp)
                    }
                }
                templateAttribute.options.forEach { option ->
                    // TODO options
                }
                applyFontVisibility(mFontInVisibility)
            }
        }
    }

    private fun buildDataTimeView(templateAttribute: TemplateAttribute,
                                  field: Field): View? {
        return context?.let {
            DateTimeView(it).apply {
                label = TemplateField.getLocalizedName(context, field.name)
                try {
                    val value = field.protectedValue.toString()
                    activation = value.trim().isNotEmpty()
                    dateTime = DateInstant(value,
                            when (templateAttribute.type) {
                                TemplateAttributeType.DATE -> DateInstant.Type.DATE
                                TemplateAttributeType.TIME -> DateInstant.Type.TIME
                                else -> DateInstant.Type.DATE_TIME
                            })
                } catch (e: Exception) {
                    activation = false
                    dateTime = when (templateAttribute.type) {
                        TemplateAttributeType.DATE -> DateInstant.IN_ONE_MONTH_DATE
                        TemplateAttributeType.TIME -> DateInstant.IN_ONE_HOUR_TIME
                        else -> DateInstant.IN_ONE_MONTH_DATE_TIME
                    }
                }
                setOnDateClickListener = { dateInstant ->
                    mTempDateTimeViewId = id
                    mOnDateInstantClickListener?.invoke(dateInstant)
                }
            }
        }
    }

    fun getIcon(): IconImage {
        return mEntryInfo?.icon ?: IconImage()
    }

    fun setIcon(iconImage: IconImage) {
        mEntryInfo?.icon = iconImage
        populateIconMethod?.invoke(entryIconView, iconImage)
    }

    fun setPasswordValue(passwordField: Field) {
        val passwordView = getFieldViewById(passwordField.name.hashCode())
        if (passwordView is EntryEditFieldView?) {
            passwordView?.value = passwordField.protectedValue.stringValue
        }
    }

    fun setCurrentDateTimeValue(date: Date) {
        // Save the date
        setCurrentDateTimeSelection { instant ->
            val newDateInstant = DateInstant(DateTime(instant.date)
                    .withYear(date.year)
                    .withMonthOfYear(date.month + 1)
                    .withDayOfMonth(date.day)
                    .toDate(), instant.type)
            if (instant.type == DateInstant.Type.DATE_TIME) {
                val instantTime = DateInstant(instant.date, DateInstant.Type.TIME)
                // Trick to recall selection with time
                mOnDateInstantClickListener?.invoke(instantTime)
            }
            newDateInstant
        }
    }

    fun setCurrentTimeValue(time: Time) {
        setCurrentDateTimeSelection { instant ->
            DateInstant(DateTime(instant.date)
                    .withHourOfDay(time.hours)
                    .withMinuteOfHour(time.minutes)
                    .toDate(), instant.type)
        }
    }

    fun setEntryInfo(entryInfo: EntryInfo?) {
        mEntryInfo = entryInfo
        buildTemplateAndPopulateInfo()
    }

    private fun populateViewsWithEntryInfo() {
        mEntryInfo?.let { entryInfo ->
            setIcon(entryInfo.icon)

            entryTitleView.value = entryInfo.title

            val userNameView: EntryEditFieldView? =
                templateContainerView.findViewWithTag(FIELD_USERNAME_TAG)
            userNameView?.value = entryInfo.username

            val passwordView: EntryEditFieldView? =
                templateContainerView.findViewWithTag(FIELD_PASSWORD_TAG)
            passwordView?.value = entryInfo.password

            val urlView: EntryEditFieldView? = templateContainerView.findViewWithTag(FIELD_URL_TAG)
            urlView?.value = entryInfo.url

            val expirationView: DateTimeView? =
                templateContainerView.findViewWithTag(FIELD_EXPIRES_TAG)
            expirationView?.activation = entryInfo.expires
            expirationView?.dateTime = entryInfo.expiryTime

            val notesView: EntryEditFieldView? =
                templateContainerView.findViewWithTag(FIELD_NOTES_TAG)
            notesView?.value = entryInfo.notes

            customFieldsContainerView.removeAllViews()
            entryInfo.customFields.forEach { customField ->
                val indexFieldViewId = indexCustomFieldIdByName(customField.name)
                if (indexFieldViewId >= 0) {
                    // Template contains the custom view
                    val customFieldId = mCustomFieldIds[indexFieldViewId]
                    templateContainerView.findViewById<View>(customFieldId.viewId)
                        ?.let { customView ->
                            if (customView is EntryEditFieldView) {
                                customView.value = customField.protectedValue.stringValue
                            } else if (customView is DateTimeView) {
                                try {
                                    customView.dateTime =
                                        DateInstant(customField.protectedValue.stringValue)
                                } catch (e: Exception) {
                                    Log.e(TAG, "unable to populate date time view", e)
                                }
                            }
                        }
                } else {
                    // If template view not found, create a new custom view
                    putCustomField(customField, false)
                }
            }
        }
    }

    fun getEntryInfo(): EntryInfo {
        populateEntryInfoWithViews()
        return mEntryInfo ?: EntryInfo()
    }

    fun populateEntryInfoWithViews() {
        if (mEntryInfo == null)
            mEntryInfo = EntryInfo()

        // Icon already populate
        mEntryInfo?.title = entryTitleView.value

        val userNameView: EntryEditFieldView? = templateContainerView.findViewWithTag(FIELD_USERNAME_TAG)
        userNameView?.value?.let {
            mEntryInfo?.username = it
        }

        val passwordView: EntryEditFieldView? = templateContainerView.findViewWithTag(FIELD_PASSWORD_TAG)
        passwordView?.value?.let {
            mEntryInfo?.password = it
        }

        val urlView: EntryEditFieldView? = templateContainerView.findViewWithTag(FIELD_URL_TAG)
        urlView?.value?.let {
            mEntryInfo?.url = it
        }

        val expirationView: DateTimeView? = templateContainerView.findViewWithTag(FIELD_EXPIRES_TAG)
        expirationView?.activation?.let {
            mEntryInfo?.expires = it
        }
        expirationView?.dateTime?.let {
            mEntryInfo?.expiryTime = it
        }

        val notesView: EntryEditFieldView? = templateContainerView.findViewWithTag(FIELD_NOTES_TAG)
        notesView?.value?.let {
            mEntryInfo?.notes = it
        }

        retrieveCustomFieldsFromView(true)

        mEntryInfo?.otpModel = OtpEntryFields.parseFields { key ->
            getCustomField(key).protectedValue.toString()
        }?.otpModel
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

    private fun getFieldViewById(@IdRes viewId: Int): View? {
        return templateContainerView.findViewById(viewId)
                ?: customFieldsContainerView.findViewById(viewId)
    }

    private fun setCurrentDateTimeSelection(action: (dateInstant: DateInstant) -> DateInstant) {
        mTempDateTimeViewId?.let { viewId ->
            val dateTimeView = getFieldViewById(viewId)
            if (dateTimeView is DateTimeView) {
                dateTimeView.dateTime = DateInstant(
                        action.invoke(dateTimeView.dateTime).date,
                        dateTimeView.dateTime.type)
            }
        }
    }

    /* -------------
     * Custom Fields
     * -------------
     */

    private data class FieldId(var label: String, var viewId: Int, var protected: Boolean)

    private fun isStandardFieldName(name: String): Boolean {
        return TemplateField.isStandardFieldName(name)
    }

    private fun customFieldIdByName(name: String): FieldId? {
        return mCustomFieldIds.find { it.label.equals(name, true) }
    }

    private fun indexCustomFieldIdByName(name: String): Int {
        return mCustomFieldIds.indexOfFirst { it.label.equals(name, true) }
    }

    private fun retrieveCustomFieldsFromView(templateFieldNotEmpty: Boolean = false) {
        mEntryInfo?.customFields = mCustomFieldIds.mapNotNull {
            getCustomField(it.label, templateFieldNotEmpty)
        }.toMutableList()
    }

    private fun getCustomField(fieldName: String): Field {
        return getCustomField(fieldName, false)
                ?: Field(fieldName, ProtectedString(false, ""))
    }

    private fun getCustomField(fieldName: String, templateFieldNotEmpty: Boolean): Field? {
        customFieldIdByName(fieldName)?.let { fieldId ->
            val editView: View? = templateContainerView.findViewById(fieldId.viewId)
                    ?: customFieldsContainerView.findViewById(fieldId.viewId)
            if (editView is EntryEditFieldView) {
                if (!templateFieldNotEmpty ||
                        (editView.tag == FIELD_CUSTOM_TAG
                        && editView.value.isNotEmpty()))
                    return Field(fieldName, ProtectedString(fieldId.protected, editView.value))
            }
            if (editView is DateTimeView) {
                val value = if (editView.activation) editView.dateTime.toString() else ""
                if (!templateFieldNotEmpty ||
                        (editView.tag == FIELD_CUSTOM_TAG
                                && value.isNotEmpty()))
                    return Field(fieldName, ProtectedString(fieldId.protected, value))
            }
        }
        return null
    }

    /**
     * Update a custom field or create a new one if doesn't exists, the old value is lost
     */
    private fun putCustomField(customField: Field, focus: Boolean): Boolean {
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

    override fun onRestoreInstanceState(state: Parcelable?) {
        //begin boilerplate code so parent classes can restore state
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        } else {
            mTemplate = state.template
            mEntryInfo = state.entryInfo
            buildTemplateAndPopulateInfo()
            super.onRestoreInstanceState(state.superState)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superSave = super.onSaveInstanceState()
        val saveState = SavedState(superSave)
        saveState.template = this.mTemplate
        saveState.entryInfo = this.mEntryInfo
        return saveState
    }

    data class Date(val year: Int, val month: Int, val day: Int)
    data class Time(val hours: Int, val minutes: Int)

    internal class SavedState : BaseSavedState {
        var template: Template? = null
        var entryInfo: EntryInfo? = null

        constructor(superState: Parcelable?) : super(superState)

        private constructor(parcel: Parcel) : super(parcel) {
            template = parcel.readParcelable(Template::class.java.classLoader)
                ?: template
            entryInfo = parcel.readParcelable(EntryInfo::class.java.classLoader)
                ?: entryInfo
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(template, flags)
            out.writeParcelable(entryInfo, flags)
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
        private val TAG = TemplateView::class.java.name

        private const val FIELD_USERNAME_TAG = "FIELD_USERNAME_TAG"
        private const val FIELD_PASSWORD_TAG = "FIELD_PASSWORD_TAG"
        private const val FIELD_URL_TAG = "FIELD_URL_TAG"
        private const val FIELD_EXPIRES_TAG = "FIELD_EXPIRES_TAG"
        private const val FIELD_NOTES_TAG = "FIELD_NOTES_TAG"
        private const val FIELD_CUSTOM_TAG = "FIELD_CUSTOM_TAG"
    }
}