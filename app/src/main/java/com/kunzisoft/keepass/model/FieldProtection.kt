package com.kunzisoft.keepass.model

import android.os.Parcelable
import com.kunzisoft.keepass.database.element.Field
import kotlinx.parcelize.Parcelize

/**
 * Wrapped class for field to manage the current protection state in views
 */
@Parcelize
data class FieldProtection(
    val field: Field,
    var isCurrentlyProtected: Boolean
): Parcelable