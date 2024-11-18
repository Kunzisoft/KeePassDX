package com.kunzisoft.keepass.utils

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.model.DataDate
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object TimeUtil {

    fun DateInstant.getDateTimeString(resources: Resources): String {
        val locale = ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.ROOT
        val date = instant.toDate()
        return when (type) {
            DateInstant.Type.DATE -> DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                locale)
                .format(date)
            DateInstant.Type.TIME -> DateFormat.getTimeInstance(
                DateFormat.SHORT,
                locale)
                .format(date)
            else -> DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT,
                locale)
                .format(date)
        }
    }

    // https://github.com/material-components/material-components-android/issues/882#issuecomment-1111374962
    // To fix UTC time in date picker
    fun datePickerToDataDate(millis: Long): DataDate {
        val selectedUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        selectedUtc.timeInMillis = millis
        val selectedLocal = Calendar.getInstance()
        selectedLocal.clear()
        selectedLocal.set(
            selectedUtc.get(Calendar.YEAR),
            selectedUtc.get(Calendar.MONTH),
            selectedUtc.get(Calendar.DAY_OF_MONTH))
        return DataDate(
            selectedLocal.get(Calendar.YEAR),
            selectedLocal.get(Calendar.MONTH) + 1,
            selectedLocal.get(Calendar.DAY_OF_MONTH),
        )
    }
}