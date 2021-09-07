package com.kunzisoft.keepass.view

import android.view.View
import androidx.annotation.DrawableRes

interface GenericTextFieldView: GenericFieldView {
    fun applyFontVisibility(fontInVisibility: Boolean)
    fun setOnActionClickListener(onActionClickListener: View.OnClickListener? = null,
                                 @DrawableRes actionImageId: Int? = null)
}