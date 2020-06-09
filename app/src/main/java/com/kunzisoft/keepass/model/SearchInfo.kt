package com.kunzisoft.keepass.model

import android.content.res.Resources
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.ObjectNameResource

class SearchInfo : ObjectNameResource, Parcelable {

    var applicationId: String? = null
    var webDomain: String? = null

    var genericInfo: String? = null

    constructor()

    private constructor(parcel: Parcel) {
        val readAppId = parcel.readString()
        applicationId =  if (readAppId.isNullOrEmpty()) null else readAppId
        val readDomain = parcel.readString()
        webDomain = if (readDomain.isNullOrEmpty()) null else readDomain
        val readGeneric = parcel.readString()
        genericInfo = if (readGeneric.isNullOrEmpty()) null else readGeneric
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(applicationId ?: "")
        parcel.writeString(webDomain ?: "")
        parcel.writeString(genericInfo ?: "")
    }

    override fun getName(resources: Resources): String {
        return applicationId ?: webDomain ?: genericInfo ?: ""
    }

    fun isNull(): Boolean {
        return applicationId == null && webDomain == null && genericInfo == null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchInfo

        if (applicationId != other.applicationId) return false
        if (webDomain != other.webDomain) return false
        if (genericInfo != other.genericInfo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = applicationId?.hashCode() ?: 0
        result = 31 * result + (webDomain?.hashCode() ?: 0)
        result = 31 * result + (genericInfo?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return applicationId ?: webDomain ?: genericInfo ?: ""
    }

    companion object {

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