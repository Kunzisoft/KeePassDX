package com.kunzisoft.keepass.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.readEnum
import com.kunzisoft.keepass.utils.writeEnum

class CipherDecryptDatabase(): Parcelable {

    var databaseUri: Uri? = null
    var credentialStorage: com.kunzisoft.keepass.model.CredentialStorage =
        com.kunzisoft.keepass.model.CredentialStorage.Companion.DEFAULT
    var decryptedValue: ByteArray = byteArrayOf()

    constructor(parcel: Parcel): this() {
        databaseUri = parcel.readParcelable(Uri::class.java.classLoader)
        credentialStorage = parcel.readEnum<com.kunzisoft.keepass.model.CredentialStorage>() ?: credentialStorage
        decryptedValue = ByteArray(parcel.readInt())
        parcel.readByteArray(decryptedValue)
    }

    fun replaceContent(copy: CipherDecryptDatabase) {
        this.decryptedValue = copy.decryptedValue
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(databaseUri, flags)
        parcel.writeEnum(credentialStorage)
        parcel.writeInt(decryptedValue.size)
        parcel.writeByteArray(decryptedValue)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CipherDecryptDatabase> {
        override fun createFromParcel(parcel: Parcel): CipherDecryptDatabase {
            return CipherDecryptDatabase(parcel)
        }

        override fun newArray(size: Int): Array<CipherDecryptDatabase?> {
            return arrayOfNulls(size)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CipherDecryptDatabase

        if (databaseUri != other.databaseUri) return false

        return true
    }

    override fun hashCode(): Int {
        return databaseUri.hashCode()
    }
}
