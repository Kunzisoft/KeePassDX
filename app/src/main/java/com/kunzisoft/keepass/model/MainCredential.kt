package com.kunzisoft.keepass.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class MainCredential(var masterPassword: String? = null, var keyFileUri: Uri? = null): Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readParcelable(Uri::class.java.classLoader)) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(masterPassword)
        parcel.writeParcelable(keyFileUri, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MainCredential> {
        override fun createFromParcel(parcel: Parcel): MainCredential {
            return MainCredential(parcel)
        }

        override fun newArray(size: Int): Array<MainCredential?> {
            return arrayOfNulls(size)
        }
    }
}
