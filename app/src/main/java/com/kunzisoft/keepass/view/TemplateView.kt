package com.kunzisoft.keepass.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.template.TemplateAttribute
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.database.helper.getLocalizedName
import com.kunzisoft.keepass.database.helper.isOtpLabel
import com.kunzisoft.keepass.database.helper.isPasskeyLabel
import com.kunzisoft.keepass.database.helper.isStandardPasswordName
import com.kunzisoft.keepass.model.FieldProtection
import com.kunzisoft.keepass.model.OtpModel
import com.kunzisoft.keepass.model.Passkey
import com.kunzisoft.keepass.model.PasskeyEntryFields.PASSKEY_FIELD
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields.OTP_TOKEN_FIELD


class TemplateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TemplateAbstractView<TextFieldView, TextFieldView, DateTimeFieldView>
        (context, attrs, defStyle) {

    private var mOnChangeFieldProtectionClickListener: ((FieldProtection) -> Unit)? = null
    fun setOnChangeFieldProtectionClickListener(listener: ((FieldProtection) -> Unit)?) {
        this.mOnChangeFieldProtectionClickListener = listener
    }

    private var mOnAskCopySafeClickListener: (() -> Unit)? = null
    fun setOnAskCopySafeClickListener(listener: (() -> Unit)? = null) {
        this.mOnAskCopySafeClickListener = listener
    }
    private var mOnCopyActionClickListener: ((FieldProtection) -> Unit)? = null
    fun setOnCopyActionClickListener(listener: ((FieldProtection) -> Unit)? = null) {
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

    override fun buildLinearTextView(
        templateAttribute: TemplateAttribute,
        field: Field
    ): TextFieldView? {
        // Add an action icon if needed
        return context?.let {
            var buildTextField = TextFieldView(it)
            var needUserVerificationToReveal = true
            when {
                TemplateField.isStandardPasswordName(context, templateAttribute.label) -> {
                    buildTextField = PasswordTextFieldView(it)
                }
                TemplateField.isOtpLabel(context, templateAttribute.label) -> {
                    buildTextField = OtpTextFieldView(it)
                    needUserVerificationToReveal = false
                }
                TemplateField.isPasskeyLabel(context, templateAttribute.label) -> {
                    buildTextField = PasskeyTextFieldView(it)
                }
            }
            buildTextField.apply {
                applyFontVisibility(mFontInVisibility)
                onRevealChanged = {
                    mOnChangeFieldProtectionClickListener?.invoke(
                        FieldProtection(field, isRevealed(), needUserVerificationToReveal)
                    )
                }
                setProtection(
                    isProtected = field.protectedValue.isProtected,
                    isRevealedByDefault = mRevealedFields.contains(field.name),
                    needUserVerificationToReveal = needUserVerificationToReveal
                )
                // Trick to bypass the onSaveInstanceState in rebuild child
                onSaveInstanceState = {
                    saveUnprotectedFieldState(field, isRevealed())
                }
                label = templateAttribute.alias
                        ?: TemplateField.getLocalizedName(context, field.name)
                setMaxChars(templateAttribute.options.getNumberChars())
                // TODO Linkify
                value = field.protectedValue.charArrayValue
                // Here the value is often empty

                if (field.protectedValue.isProtected) {
                    textDirection = TEXT_DIRECTION_LTR
                    if (mFirstTimeAskAllowCopyProtectedFields) {
                        setCopyButtonState(TextFieldView.ButtonState.DEACTIVATE)
                        setCopyButtonClickListener { _ ->
                            mOnAskCopySafeClickListener?.invoke()
                        }
                    } else {
                        if (mAllowCopyProtectedFields) {
                            setCopyButtonState(TextFieldView.ButtonState.ACTIVATE)
                            setCopyButtonClickListener { fieldProtection ->
                                mOnCopyActionClickListener?.invoke(fieldProtection)
                            }
                        } else {
                            setCopyButtonState(TextFieldView.ButtonState.GONE)
                            setCopyButtonClickListener(null)
                        }
                    }
                } else {
                    setCopyButtonState(TextFieldView.ButtonState.ACTIVATE)
                    setCopyButtonClickListener { fieldProtection ->
                        mOnCopyActionClickListener?.invoke(fieldProtection)
                    }
                }
                mFields[field.name] = this
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
                type = templateAttribute.options.getDateFormat()
                isExpirable = templateAttribute.options.getExpirable()
                try {
                    val value = field.protectedValue.toString().trim()
                    activation = value.isNotEmpty()
                } catch (_: Exception) {
                    activation = false
                }
            }
        }
    }

    override fun getActionImageView(): View? {
        return findViewWithTag<TextFieldView?>(FIELD_PASSWORD_TAG)?.getCopyButtonView()
    }

    override fun populateViewsWithEntryInfo(showEmptyFields: Boolean): List<ViewField>  {
        val emptyCustomFields = super.populateViewsWithEntryInfo(false)
        // Hide empty custom fields
        emptyCustomFields.forEach { customFieldId ->
            customFieldId.view.isVisible = false
        }
        // Assign specific OTP dynamic view
        assignOtp(mEntryInfo?.otpModel)
        assignPasskey(mEntryInfo?.passkey)
        return emptyCustomFields
    }

    private fun getOtpTokenView(): OtpTextFieldView? {
        getViewFieldByName(OTP_TOKEN_FIELD)?.let { viewField ->
            val view = viewField.view
            if (view is OtpTextFieldView)
                return view
        }
        return null
    }

    fun setOtpUpdatedListener(listener: ((OtpElement?) -> Unit)?) {
        getOtpTokenView()?.setOnOtpUpdatedListener(listener)
    }

    private fun assignOtp(otpModel: OtpModel?) {
        otpModel?.let {
            getOtpTokenView()?.apply {
                setOtpModel(otpModel)
            }
        }
    }

    private fun getPasskeyView(): PasskeyTextFieldView? {
        getViewFieldByName(PASSKEY_FIELD)?.let { viewField ->
            val view = viewField.view
            if (view is PasskeyTextFieldView)
                return view
        }
        return null
    }

    private fun assignPasskey(passkey: Passkey?) {
        passkey?.let {
            getPasskeyView()?.apply {
                relyingParty = passkey.relyingParty
                username = passkey.username
            }
        }
    }
}
