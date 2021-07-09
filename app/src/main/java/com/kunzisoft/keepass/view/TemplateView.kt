package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.TemplateAttribute
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.model.OtpModel
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields.OTP_TOKEN_FIELD


class TemplateView @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyle: Int = 0)
    : TemplateAbstractView<TextFieldView, TextFieldView, DateTimeFieldView>
        (context, attrs, defStyle) {

    private var mOnAskCopySafeClickListener: (() -> Unit)? = null
    fun setOnAskCopySafeClickListener(listener: (() -> Unit)? = null) {
        this.mOnAskCopySafeClickListener = listener
    }
    private var mOnCopyActionClickListener: ((Field) -> Unit)? = null
    fun setOnCopyActionClickListener(listener: ((Field) -> Unit)? = null) {
        this.mOnCopyActionClickListener = listener
    }

    private var mFirstTimeAskAllowCopyProtectedFields: Boolean = false
    fun setFirstTimeAskAllowCopyProtectedFields(firstTimeAskAllowCopyProtectedFields : Boolean) {
        this.mFirstTimeAskAllowCopyProtectedFields = firstTimeAskAllowCopyProtectedFields
    }

    private var mAllowCopyProtectedFields: Boolean = false
    fun setAllowCopyProtectedFields(allowCopyProtectedFields : Boolean) {
        this.mAllowCopyProtectedFields = allowCopyProtectedFields
    }

    override fun preProcessTemplate() {
        headerContainerView.isVisible = false
    }

    override fun buildLinearTextView(templateAttribute: TemplateAttribute,
                                     field: Field): TextFieldView? {
        // Add an action icon if needed
        return context?.let {
            TextFieldView(it).apply {
                applyFontVisibility(mFontInVisibility)
                setProtection(field.protectedValue.isProtected, mHideProtectedValue)
                label = templateAttribute.alias
                        ?: TemplateField.getLocalizedName(context, field.name)
                setMaxChars(templateAttribute.options.getNumberChars())
                setMaxLines(templateAttribute.options.getNumberLines())
                // TODO Linkify
                value = field.protectedValue.stringValue

                if (field.protectedValue.isProtected) {
                    if (mFirstTimeAskAllowCopyProtectedFields) {
                        setCopyButtonState(TextFieldView.ButtonState.DEACTIVATE)
                        setCopyButtonClickListener {
                            mOnAskCopySafeClickListener?.invoke()
                        }
                    } else {
                        if (mAllowCopyProtectedFields) {
                            setCopyButtonState(TextFieldView.ButtonState.ACTIVATE)
                            setCopyButtonClickListener {
                                mOnCopyActionClickListener?.invoke(field)
                            }
                        } else {
                            setCopyButtonState(TextFieldView.ButtonState.GONE)
                            setCopyButtonClickListener(null)
                        }
                    }
                } else {
                    setCopyButtonState(TextFieldView.ButtonState.ACTIVATE)
                    setCopyButtonClickListener {
                        mOnCopyActionClickListener?.invoke(field)
                    }
                }
            }
        }
    }

    override fun buildListItemsView(
        templateAttribute: TemplateAttribute,
        field: Field
    ): TextFieldView? {
        // No special view for selection
        return buildLinearTextView(templateAttribute, field)
    }

    override fun buildDataTimeView(templateAttribute: TemplateAttribute,
                                   field: Field): DateTimeFieldView? {
        return context?.let {
            DateTimeFieldView(it).apply {
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
            }
        }
    }

    override fun getActionImageView(): View? {
        return findViewWithTag<TextFieldView?>(FIELD_PASSWORD_TAG)?.getCopyButtonView()
    }

    override fun populateViewsWithEntryInfo(showEmptyFields: Boolean): List<FieldId>  {
        val emptyCustomFields = super.populateViewsWithEntryInfo(false)

        // Hide empty custom fields
        emptyCustomFields.forEach { customFieldId ->
            templateContainerView.findViewById<View>(customFieldId.viewId)
                .isVisible = false
        }

        mEntryInfo?.let { entryInfo ->
            // Assign specific OTP dynamic view
            removeOtpRunnable()
            entryInfo.otpModel?.let {
                assignOtp(it)
            }
        }
        return emptyCustomFields
    }

    override fun getCustomField(fieldName: String, templateFieldNotEmpty: Boolean): Field? {
        customFieldIdByName(fieldName)?.let { fieldId ->
            val editView: View? = templateContainerView.findViewById(fieldId.viewId)
                ?: customFieldsContainerView.findViewById(fieldId.viewId)
            if (editView is GenericFieldView) {
                if (!templateFieldNotEmpty ||
                    (editView.tag == FIELD_CUSTOM_TAG
                            && editView.value.isNotEmpty()))
                    return Field(fieldName, ProtectedString(fieldId.protected, editView.value))
            }
        }
        return null
    }

    /*
     * OTP Runnable
     */

    private var mOtpRunnable: Runnable? = null
    private var mLastOtpTokenView: View? = null

    fun setOnOtpElementUpdated(listener: ((OtpElement?) -> Unit)?) {
        this.mOnOtpElementUpdated = listener
    }
    private var mOnOtpElementUpdated: ((OtpElement?) -> Unit)? = null

    private fun getOtpTokenView(): TextFieldView? {
        val indexFieldViewId = indexCustomFieldIdByName(OTP_TOKEN_FIELD)
        if (indexFieldViewId >= 0) {
            // Template contains the custom view
            val customFieldId = mCustomFieldIds[indexFieldViewId]
            return findViewById(customFieldId.viewId)
        }
        return null
    }

    private fun assignOtp(otpModel: OtpModel) {
        getOtpTokenView()?.apply {
            val otpElement = OtpElement(otpModel)
            if (otpElement.token.isEmpty()) {
                setLabel(R.string.entry_otp)
                setValue(R.string.error_invalid_OTP)
                setCopyButtonState(TextFieldView.ButtonState.GONE)
            } else {
                label = otpElement.type.name
                value = otpElement.token
                setCopyButtonState(TextFieldView.ButtonState.ACTIVATE)
                setCopyButtonClickListener {
                    mOnCopyActionClickListener?.invoke(Field(
                        otpElement.type.name,
                        ProtectedString(false, otpElement.token)))
                }
                mLastOtpTokenView = this
                mOtpRunnable = Runnable {
                    if (otpElement.shouldRefreshToken()) {
                        value = otpElement.token
                    }
                    if (mLastOtpTokenView == null) {
                        mOnOtpElementUpdated?.invoke(null)
                    } else {
                        mOnOtpElementUpdated?.invoke(otpElement)
                        postDelayed(mOtpRunnable, 1000)
                    }
                }
                mOnOtpElementUpdated?.invoke(otpElement)
                post(mOtpRunnable)
            }
        }
    }

    private fun removeOtpRunnable() {
        mLastOtpTokenView?.removeCallbacks(mOtpRunnable)
        mLastOtpTokenView = null
    }
}