package com.kunzisoft.keepass.model

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.settings.PreferencesUtil
import com.kunzisoft.keepass.utils.ObjectNameResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.publicsuffixlist.PublicSuffixList

class SearchInfo : ObjectNameResource, Parcelable {
    var manualSelection: Boolean = false
    var applicationId: String? = null
        set(value) {
            field = when {
                value == null -> null
                Regex(APPLICATION_ID_REGEX).matches(value) -> value
                else -> null
            }
        }
    // A web domain can also containing an IP
    var webDomain: String? = null
        set(value) {
            field = when {
                value == null -> null
                Regex(WEB_DOMAIN_REGEX).matches(value) -> value
                Regex(WEB_IP_REGEX).matches(value) -> value
                else -> null
            }
        }
    var webScheme: String? = null
        get() {
            return if (webDomain == null) null else field
        }
    var otpString: String? = null

    constructor()

    constructor(toCopy: SearchInfo?) {
        manualSelection = toCopy?.manualSelection ?: manualSelection
        applicationId = toCopy?.applicationId
        webDomain = toCopy?.webDomain
        webScheme = toCopy?.webScheme
        otpString = toCopy?.otpString
    }

    private constructor(parcel: Parcel) {
        manualSelection = parcel.readByte().toInt() != 0
        val readAppId = parcel.readString()
        applicationId =  if (readAppId.isNullOrEmpty()) null else readAppId
        val readDomain = parcel.readString()
        webDomain = if (readDomain.isNullOrEmpty()) null else readDomain
        val readScheme = parcel.readString()
        webScheme = if (readScheme.isNullOrEmpty()) null else readScheme
        val readOtp = parcel.readString()
        otpString = if (readOtp.isNullOrEmpty()) null else readOtp
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte((if (manualSelection) 1 else 0).toByte())
        parcel.writeString(applicationId ?: "")
        parcel.writeString(webDomain ?: "")
        parcel.writeString(webScheme ?: "")
        parcel.writeString(otpString ?: "")
    }

    override fun getName(resources: Resources): String {
        otpString?.let { otpString ->
            OtpEntryFields.parseOTPUri(otpString)?.let { otpElement ->
                return "${otpElement.type} (${Uri.decode(otpElement.issuer)}:${Uri.decode(otpElement.name)})"
            }
        }
        return toString()
    }

    fun containsOnlyNullValues(): Boolean {
        return applicationId == null
                && webDomain == null
                && webScheme == null
                && otpString == null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchInfo) return false

        if (manualSelection != other.manualSelection) return false
        if (applicationId != other.applicationId) return false
        if (webDomain != other.webDomain) return false
        if (webScheme != other.webScheme) return false
        if (otpString != other.otpString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = manualSelection.hashCode()
        result = 31 * result + (applicationId?.hashCode() ?: 0)
        result = 31 * result + (webDomain?.hashCode() ?: 0)
        result = 31 * result + (webScheme?.hashCode() ?: 0)
        result = 31 * result + (otpString?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return otpString ?: webDomain ?: applicationId ?: ""
    }

    companion object {
        // https://gist.github.com/rishabhmhjn/8663966
        const val APPLICATION_ID_REGEX = "^(?:[a-zA-Z]+(?:\\d*[a-zA-Z_]*)*)(?:\\.[a-zA-Z]+(?:\\d*[a-zA-Z_]*)*)+\$"
        const val WEB_DOMAIN_REGEX = "^(?!://)([a-zA-Z0-9-_]+\\.)*[a-zA-Z0-9][a-zA-Z0-9-_]+\\.[a-zA-Z]{2,11}?\$"
        const val WEB_IP_REGEX = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\$"

        @JvmField
        val CREATOR: Parcelable.Creator<SearchInfo> = object : Parcelable.Creator<SearchInfo> {
            override fun createFromParcel(parcel: Parcel): SearchInfo {
                return SearchInfo(parcel)
            }

            override fun newArray(size: Int): Array<SearchInfo?> {
                return arrayOfNulls(size)
            }
        }

        /**
         * Get the concrete web domain AKA without sub domain if needed
         */
        fun getConcreteWebDomain(context: Context,
                                 webDomain: String?,
                                 concreteWebDomain: (String?) -> Unit) {
            CoroutineScope(Dispatchers.Main).launch {
                if (webDomain != null) {
                    // Warning, web domain can contains IP, don't crop in this case
                    if (PreferencesUtil.searchSubdomains(context)
                            || Regex(WEB_IP_REGEX).matches(webDomain)) {
                        concreteWebDomain.invoke(webDomain)
                    } else {
                        val publicSuffixList = PublicSuffixList(context)
                        concreteWebDomain.invoke(publicSuffixList
                                .getPublicSuffixPlusOne(webDomain).await())
                    }
                } else {
                    concreteWebDomain.invoke(null)
                }
            }
        }
    }
}