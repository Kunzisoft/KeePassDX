package com.kunzisoft.keepass.utils

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import com.kunzisoft.keepass.database.element.DateInstant
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

object TimeUtil {

    fun DateInstant.getDateTimeString(resources: Resources): String {
        val locale = ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.ROOT
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

    fun DateInstant.getDateTimeStringShort(resources: Resources): String {
        val locale = ConfigurationCompat.getLocales(resources.configuration)[0] ?: Locale.ROOT
        val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", locale)

        return when (type) {
            DateInstant.Type.DATE -> DateFormat.getDateInstance(
                DateFormat.MEDIUM,
                locale
            ).format(date)
            DateInstant.Type.TIME -> DateFormat.getTimeInstance(
                DateFormat.SHORT,
                locale
            ).format(date)
            else -> dateFormat.format(date)
        }
    }
}