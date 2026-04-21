package com.kunzisoft.keepass.model

import android.os.Parcelable
import com.kunzisoft.keepass.utils.clear
import kotlinx.parcelize.Parcelize

@Parcelize
data class PasswordInfo(
    val username: String,
    val password: CharArray,
    val appOrigin: AppOrigin?
): Parcelable {

    /**
     * Clear sensitive data
     */
    fun clear() {
        password.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PasswordInfo

        if (username != other.username) return false
        if (!password.contentEquals(other.password)) return false
        if (appOrigin != other.appOrigin) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + (appOrigin?.hashCode() ?: 0)
        return result
    }
}