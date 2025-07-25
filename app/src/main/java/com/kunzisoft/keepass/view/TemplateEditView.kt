package com.kunzisoft.keepass.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.IdRes
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.TemplateAttribute
import com.kunzisoft.keepass.database.element.template.TemplateAttributeAction
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.database.helper.isStandardPasswordName
import com.kunzisoft.keepass.model.DataDate
import com.kunzisoft.keepass.model.DataTime
import com.kunzisoft.keepass.otp.OtpEntryFields


class TemplateEditView @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyle: Int = 0)
    : TemplateAbstractView<TextEditFieldView, TextSelectFieldView, DateTimeEditFieldView>
        (context, attrs, defStyle) {

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
        refreshIcon()
    }

    fun setOnBackgroundColorClickListener(onClickListener: OnClickListener) {
        backgroundColorButton.setOnClickListener(onClickListener)
    }

    fun getBackgroundColor(): Int? {
        return mEntryInfo?.backgroundColor
    }

    fun setBackgroundColor(color: Int?) {
        applyBackgroundColor(color)
        mEntryInfo?.backgroundColor = color
    }

    private fun applyBackgroundColor(color: Int?) {
        if (color != null) {
            backgroundColorView.background.colorFilter = BlendModeColorFilterCompat
                .createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)
            backgroundColorView.visibility = View.VISIBLE
        } else {
            backgroundColorView.visibility = View.GONE
        }
    }

    fun setOnForegroundColorClickListener(onClickListener: OnClickListener) {
        foregroundColorButton.setOnClickListener(onClickListener)
    }

    fun getForegroundColor(): Int? {
        return mEntryInfo?.foregroundColor
    }

    fun setForegroundColor(color: Int?) {
        applyForegroundColor(color)
        mEntryInfo?.foregroundColor = color
    }

    private fun applyForegroundColor(color: Int?) {
        if (color != null) {
            foregroundColorView.background.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)
            foregroundColorView.visibility = View.VISIBLE
        } else {
            foregroundColorView.visibility = View.GONE
        }
    }

    override fun preProcessTemplate() {
        headerContainerView.isVisible = true
    }

    override fun buildLinearTextView(templateAttribute: TemplateAttribute,
                                     field: Field): TextEditFieldView? {
        return context?.let {
            (if (TemplateField.isStandardPasswordName(context, templateAttribute.label))
                PasswordTextEditFieldView(it)
            else TextEditFieldView(it)).apply {
                // hiddenProtectedValue (mHideProtectedValue) don't work with TextInputLayout
                setProtection(field.protectedValue.isProtected)
                default = templateAttribute.default
                setMaxChars(templateAttribute.options.getNumberChars())
                setMaxLines(templateAttribute.options.getNumberLines())
                setActionClick(templateAttribute, field, this)
                if (field.protectedValue.isProtected) {
                    textDirection = TEXT_DIRECTION_LTR
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                }
            }
        }
    }

    override fun buildListItemsView(templateAttribute: TemplateAttribute,
                                    field: Field): TextSelectFieldView? {
        return context?.let {
            TextSelectFieldView(it).apply {
                setItems(templateAttribute.options.getListItems())
                default = templateAttribute.default
                setActionClick(templateAttribute, field, this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                }
            }
        }
    }

    private fun setActionClick(templateAttribute: TemplateAttribute,
                               field: Field,
                               view: GenericTextFieldView) {
        view.apply {
            applyFontVisibility(mFontInVisibility)
            label = templateAttribute.alias
                ?: TemplateField.getLocalizedName(context, field.name)
            val fieldValue = field.protectedValue.stringValue
            value = if (fieldValue.isEmpty()) templateAttribute.default else fieldValue
            // TODO edition and password generator at same time
            when (templateAttribute.action) {
                TemplateAttributeAction.NONE -> {
                    setOnActionClickListener(null)
                }
                TemplateAttributeAction.CUSTOM_EDITION -> {
                    setOnActionClickListener({
                        mOnCustomEditionActionClickListener?.invoke(field)
                    }, R.drawable.ic_more_white_24dp)
                }
            }
            if (templateAttribute.options.isAssociatedWithPasswordGenerator()) {
                setOnActionClickListener({
                    mOnPasswordGenerationActionClickListener?.invoke(field)
                }, R.drawable.ic_random_white_24dp)
            }
        }
    }

    override fun buildDataTimeView(templateAttribute: TemplateAttribute,
                                  field: Field): DateTimeEditFieldView? {
        return context?.let {
            DateTimeEditFieldView(it).apply {
                label = TemplateField.getLocalizedName(context, field.name)
                val dateInstantType = templateAttribute.options.getDateFormat()
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
        return findViewWithTag<TextEditFieldView?>(FIELD_PASSWORD_TAG)?.getActionImageView()
    }

    fun setPasswordField(passwordField: Field) {
        val passwordView = getFieldViewById(passwordField.name.hashCode())
        if (passwordView is TextEditFieldView?) {
                passwordView?.value = passwordField.protectedValue.stringValue
        }
    }

    fun getPasswordField(): Field {
        val passwordView: TextEditFieldView? = templateContainerView.findViewWithTag(FIELD_PASSWORD_TAG)
        return Field(TemplateField.LABEL_PASSWORD, ProtectedString(true, passwordView?.value ?: ""))
    }

    private fun setCurrentDateTimeSelection(action: (dateInstant: DateInstant) -> DateInstant) {
        mTempDateTimeViewId?.let { viewId ->
            val dateTimeView = getFieldViewById(viewId)
            if (dateTimeView is DateTimeEditFieldView) {
                dateTimeView.dateTime = DateInstant(
                    action.invoke(dateTimeView.dateTime).instant,
                    dateTimeView.dateTime.type
                )
            }
        }
    }

    fun setCurrentDateTimeValue(date: DataDate) {
        // Save the date
        setCurrentDateTimeSelection { dateInstant ->
            dateInstant.setDate(date.year, date.month, date.day)
            if (dateInstant.type == DateInstant.Type.DATE_TIME) {
                // Trick to recall selection with time
                mOnDateInstantClickListener?.invoke(
                    DateInstant(dateInstant.instant, DateInstant.Type.TIME)
                )
            }
            dateInstant
        }
    }

    fun setCurrentTimeValue(time: DataTime) {
        setCurrentDateTimeSelection { dateInstant ->
            dateInstant.setTime(time.hour, time.minute)
            dateInstant
        }
    }

    override fun populateViewsWithEntryInfo(showEmptyFields: Boolean): List<ViewField> {
        refreshIcon()
        applyBackgroundColor(mEntryInfo?.backgroundColor)
        applyForegroundColor(mEntryInfo?.foregroundColor)
        return super.populateViewsWithEntryInfo(showEmptyFields)
    }

    override fun populateEntryInfoWithViews(templateFieldNotEmpty: Boolean,
                                            retrieveDefaultValues: Boolean) {
        super.populateEntryInfoWithViews(templateFieldNotEmpty, retrieveDefaultValues)
        mEntryInfo?.otpModel = OtpEntryFields.parseFields { key ->
            getCustomField(key).protectedValue.toString()
        }?.otpModel
    }

    override fun onRestoreEntryInstanceState(state: SavedState) {
        mTempDateTimeViewId = state.tempDateTimeViewId
    }

    override fun onSaveEntryInstanceState(savedState: SavedState) {
        savedState.tempDateTimeViewId = this.mTempDateTimeViewId
    }
}
