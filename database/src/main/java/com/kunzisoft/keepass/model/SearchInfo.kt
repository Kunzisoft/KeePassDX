package com.kunzisoft.keepass.model

import android.content.res.Resources
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.utils.ObjectNameResource
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.readListCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat
import com.kunzisoft.keepass.utils.writeListCompat

class SearchInfo : ObjectNameResource, Parcelable {
    var manualSelection: Boolean = false

    var isTagSearch: Boolean = false
    var tags: List<String> = listOf()

    var applicationId: String? = null
        set(value) {
            field = when {
                value == null -> null
                Regex(APPLICATION_ID_REGEX).matches(value) -> value
                else -> null
            }
        }
    // A web domain can also include an IP
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
    var relyingParty: String? = null
    var credentialIds: List<String> = listOf()
    var otpString: String? = null

    constructor()

    constructor(toCopy: SearchInfo?) {
        manualSelection = toCopy?.manualSelection ?: manualSelection
        isTagSearch = toCopy?.isTagSearch ?: isTagSearch
        tags = toCopy?.tags ?: listOf()
        applicationId = toCopy?.applicationId
        webDomain = toCopy?.webDomain
        webScheme = toCopy?.webScheme
        relyingParty = toCopy?.relyingParty
        credentialIds = toCopy?.credentialIds ?: listOf()
        otpString = toCopy?.otpString
    }

    private constructor(parcel: Parcel) {
        manualSelection = parcel.readBooleanCompat()
        isTagSearch = parcel.readBooleanCompat()
        tags = parcel.readListCompat()
        val readAppId = parcel.readString()
        applicationId = if (readAppId.isNullOrEmpty()) null else readAppId
        val readDomain = parcel.readString()
        webDomain = if (readDomain.isNullOrEmpty()) null else readDomain
        val readScheme = parcel.readString()
        webScheme = if (readScheme.isNullOrEmpty()) null else readScheme
        val readRelyingParty = parcel.readString()
        relyingParty = if (readRelyingParty.isNullOrEmpty()) null else readRelyingParty
        val readCredentialIdList = mutableListOf<String>()
        parcel.readStringList(readCredentialIdList)
        credentialIds = readCredentialIdList.toList()
        val readOtp = parcel.readString()
        otpString = if (readOtp.isNullOrEmpty()) null else readOtp
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeBooleanCompat(manualSelection)
        parcel.writeBooleanCompat(isTagSearch)
        parcel.writeListCompat(tags)
        parcel.writeString(applicationId ?: "")
        parcel.writeString(webDomain ?: "")
        parcel.writeString(webScheme ?: "")
        parcel.writeString(relyingParty ?: "")
        parcel.writeStringList(credentialIds)
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
        return tags.isEmpty()
                && applicationId == null
                && webDomain == null
                && webScheme == null
                && relyingParty == null
                && credentialIds.isEmpty()
                && otpString == null
    }

    var isAppIdSearch: Boolean = false
        get() = applicationId != null
        private set

    var isDomainSearch: Boolean = false
        get() = webDomain != null
        private set

    var isPasskeySearch: Boolean = false
        get() = relyingParty != null
        private set

    var isOTPSearch: Boolean = false
        get() = otpString != null
        private set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchInfo) return false

        if (manualSelection != other.manualSelection) return false
        if (isTagSearch != other.isTagSearch) return false
        if (tags != other.tags) return false
        if (applicationId != other.applicationId) return false
        if (webDomain != other.webDomain) return false
        if (webScheme != other.webScheme) return false
        if (relyingParty != other.relyingParty) return false
        if (credentialIds != other.credentialIds) return false
        if (otpString != other.otpString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = manualSelection.hashCode()
        result = 31 * result + (isTagSearch.hashCode())
        result = 31 * result + (tags.hashCode())
        result = 31 * result + (applicationId?.hashCode() ?: 0)
        result = 31 * result + (webDomain?.hashCode() ?: 0)
        result = 31 * result + (webScheme?.hashCode() ?: 0)
        result = 31 * result + (relyingParty?.hashCode() ?: 0)
        result = 31 * result + (credentialIds.hashCode())
        result = 31 * result + (otpString?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return otpString ?: webDomain ?: applicationId ?: relyingParty ?: if (tags.isEmpty()) "" else tags.toString()
    }

    fun optionsString(): List<String> {
        return if (isPasskeySearch && credentialIds.isNotEmpty()) credentialIds else listOf()
    }

    fun toRegisterInfo(): RegisterInfo {
        return RegisterInfo(this)
    }

    companion object {
        // https://gist.github.com/rishabhmhjn/8663966
        const val APPLICATION_ID_REGEX = "^(?:[a-zA-Z]+(?:\\d*[a-zA-Z_]*)*)(?:\\.[a-zA-Z]+(?:\\d*[a-zA-Z_]*)*)+\$"
        const val WEB_DOMAIN_REGEX = "^(?!://)([a-zA-Z0-9-_]+\\.)*[a-zA-Z]{2,11}?\$"
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
    }
}
