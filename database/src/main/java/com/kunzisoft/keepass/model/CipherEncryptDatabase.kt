package com.kunzisoft.keepass.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.writeEnum

class CipherEncryptDatabase(): Parcelable {

    var databaseUri: Uri? = null
    var credentialStorage: com.kunzisoft.keepass.model.CredentialStorage =
        com.kunzisoft.keepass.model.CredentialStorage.Companion.DEFAULT
    var encryptedValue: ByteArray = byteArrayOf()
    var specParameters: ByteArray = byteArrayOf()

    constructor(parcel: Parcel): this() {
        databaseUri = parcel.readParcelable(Uri::class.java.classLoader)
        credentialStorage = parcel.readEnum<com.kunzisoft.keepass.model.CredentialStorage>() ?: credentialStorage
        encryptedValue = ByteArray(parcel.readInt())
        parcel.readByteArray(encryptedValue)
        specParameters = ByteArray(parcel.readInt())
        parcel.readByteArray(specParameters)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(databaseUri, flags)
        parcel.writeEnum(credentialStorage)
        parcel.writeInt(encryptedValue.size)
        parcel.writeByteArray(encryptedValue)
        parcel.writeInt(specParameters.size)
        parcel.writeByteArray(specParameters)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CipherEncryptDatabase> {
        override fun createFromParcel(parcel: Parcel): CipherEncryptDatabase {
            return CipherEncryptDatabase(parcel)
        }

        override fun newArray(size: Int): Array<CipherEncryptDatabase?> {
            return arrayOfNulls(size)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CipherEncryptDatabase

        if (databaseUri != other.databaseUri) return false

        return true
    }

    override fun hashCode(): Int {
        return databaseUri.hashCode()
    }
}
