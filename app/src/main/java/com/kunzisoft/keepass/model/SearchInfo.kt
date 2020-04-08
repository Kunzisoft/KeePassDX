package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable

class SearchInfo : Parcelable {

    var applicationId: String? = null
    var webDomain: String? = null

    constructor()

    private constructor(parcel: Parcel) {
        val readAppId = parcel.readString()
        applicationId =  if (readAppId.isNullOrEmpty()) null else readAppId
        val readDomain = parcel.readString()
        webDomain = if (readDomain.isNullOrEmpty()) null else readDomain
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(applicationId ?: "")
        parcel.writeString(webDomain ?: "")
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