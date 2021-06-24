package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.TemplateAttribute
import com.kunzisoft.keepass.database.element.template.TemplateAttributeAction
import com.kunzisoft.keepass.database.element.template.TemplateAttributeType
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.otp.OtpEntryFields
import org.joda.time.DateTime


class TemplateEditView @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyle: Int = 0)
    : TemplateAbstractView<EntryEditFieldView, DateTimeEditView>(context, attrs, defStyle) {

    // Current date time selection
    @IdRes
    private var mTempDateTimeViewId: Int? = null

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

    fun setOnIconClickListener(onClickListener: OnClickListener) {
        entryIconView.setOnClickListener(onClickListener)
    }

    fun getIcon(): IconImage {
        return mEntryInfo?.icon ?: IconImage()
    }

    fun setIcon(iconImage: IconImage) {
        mEntryInfo?.icon = iconImage
        populateIconMethod?.invoke(entryIconView, iconImage)
    }

    override fun preProcessTemplate() {
        headerContainerView.isVisible = true
    }

    override fun buildLinearTextView(templateAttribute: TemplateAttribute,
                                     field: Field): EntryEditFieldView? {
        // Add an action icon if needed
        return context?.let {
            EntryEditFieldView(it).apply {
                applyFontVisibility(mFontInVisibility)
                setProtection(field.protectedValue.isProtected, mHideProtectedValue)
                label = TemplateField.getLocalizedName(context, field.name)
                setType(when (templateAttribute.type) {
                    TemplateAttributeType.SMALL_MULTILINE -> TextType.SMALL_MULTI_LINE
                    TemplateAttributeType.MULTILINE -> TextType.MULTI_LINE
                    else -> TextType.NORMAL
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
            }
        }
    }

    override fun buildDataTimeView(templateAttribute: TemplateAttribute,
                                  field: Field): DateTimeEditView? {
        return context?.let {
            DateTimeEditView(it).apply {
                label = TemplateField.getLocalizedName(context, field.name)
                val dateInstantType = dateInstantTypeFromTemplateAttributeType(templateAttribute.type)
                try {
                    val value = field.protectedValue.toString().trim()
                    type = dateInstantType
                    activation = value.isNotEmpty()
                } catch (e: Exception) {
                    type = dateInstantType
                    activation = false
                }
                setOnDateClickListener = { dateInstant ->
                    mTempDateTimeViewId = id
                    mOnDateInstantClickListener?.invoke(dateInstant)
                }
            }
        }
    }

    override fun getActionImageView(): View? {
        return findViewWithTag<EntryEditFieldView?>(FIELD_PASSWORD_TAG)?.getActionImageView()
    }

    fun setPasswordField(passwordField: Field) {
        val passwordView = getFieldViewById(passwordField.name.hashCode())
        if (passwordView is EntryEditFieldView?) {
                passwordView?.value = passwordField.protectedValue.stringValue
        }
    }

    fun getPasswordField(): Field {
        val passwordView: EntryEditFieldView? = templateContainerView.findViewWithTag(FIELD_PASSWORD_TAG)
        return Field(TemplateField.LABEL_PASSWORD, ProtectedString(true, passwordView?.value ?: ""))
    }

    private fun setCurrentDateTimeSelection(action: (dateInstant: DateInstant) -> DateInstant) {
        mTempDateTimeViewId?.let { viewId ->
            val dateTimeView = getFieldViewById(viewId)
            if (dateTimeView is DateTimeEditView) {
                dateTimeView.dateTime = DateInstant(
                    action.invoke(dateTimeView.dateTime).date,
                    dateTimeView.dateTime.type)
            }
        }
    }

    fun setCurrentDateTimeValue(date: DataDate) {
        // Save the date
        setCurrentDateTimeSelection { instant ->
            val newDateInstant = DateInstant(
                DateTime(instant.date)
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

    fun setCurrentTimeValue(time: DataTime) {
        setCurrentDateTimeSelection { instant ->
            DateInstant(
                DateTime(instant.date)
                .withHourOfDay(time.hours)
                .withMinuteOfHour(time.minutes)
                .toDate(), instant.type)
        }
    }

    override fun populateViewsWithEntryInfo(showEmptyFields: Boolean): List<FieldId> {
        mEntryInfo?.let { entryInfo ->
            setIcon(entryInfo.icon)
        }
        return super.populateViewsWithEntryInfo(showEmptyFields)
    }

    override fun populateEntryInfoWithViews(templateFieldNotEmpty: Boolean) {
        super.populateEntryInfoWithViews(templateFieldNotEmpty)
        mEntryInfo?.otpModel = OtpEntryFields.parseFields { key ->
            getCustomField(key).protectedValue.toString()
        }?.otpModel
    }

    override fun getCustomField(fieldName: String, templateFieldNotEmpty: Boolean): Field? {
        customFieldIdByName(fieldName)?.let { fieldId ->
            val editView: View? = templateContainerView.findViewById(fieldId.viewId)
                ?: customFieldsContainerView.findViewById(fieldId.viewId)
            if (editView is EntryEditFieldView) {
                if (!templateFieldNotEmpty ||
                    (editView.tag == FIELD_CUSTOM_TAG
                            && editView.value.isNotEmpty()))
                    return Field(fieldName, ProtectedString(fieldId.protected, editView.value))
            }
            if (editView is DateTimeEditView) {
                val value = if (editView.activation) editView.dateTime.toString() else ""
                if (!templateFieldNotEmpty ||
                    (editView.tag == FIELD_CUSTOM_TAG
                            && value.isNotEmpty()))
                    return Field(fieldName, ProtectedString(fieldId.protected, value))
            }
        }
        return null
    }

    override fun onRestoreEntryInstanceState(state: SavedState) {
        mTempDateTimeViewId = state.tempDateTimeViewId
    }

    override fun onSaveEntryInstanceState(savedState: SavedState) {
        savedState.tempDateTimeViewId = this.mTempDateTimeViewId
    }
}