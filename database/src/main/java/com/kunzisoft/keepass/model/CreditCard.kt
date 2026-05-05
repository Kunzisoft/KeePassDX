package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.CharArrayUtil.clear
import com.kunzisoft.keepass.utils.readCharArrayCompat
import com.kunzisoft.keepass.utils.writeCharArrayCompat

data class CreditCard(
    val cardholder: String?,
    val number: CharArray?,
    val cvv: CharArray?
) : Parcelable {

    constructor(creditCard: CreditCard) : this(
        creditCard.cardholder,
        creditCard.number?.copyOf(),
        creditCard.cvv?.copyOf()
    )

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readCharArrayCompat(),
            parcel.readCharArrayCompat()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(cardholder)
        parcel.writeCharArrayCompat(number)
        parcel.writeCharArrayCompat(cvv)
    }

    fun clear() {
        number?.clear()
        cvv?.clear()
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CreditCard> {
        override fun createFromParcel(parcel: Parcel): CreditCard {
            return CreditCard(parcel)
        }

        override fun newArray(size: Int): Array<CreditCard?> {
            return arrayOfNulls(size)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CreditCard

        if (cardholder != other.cardholder) return false
        if (!number.contentEquals(other.number)) return false
        if (!cvv.contentEquals(other.cvv)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cardholder?.hashCode() ?: 0
        result = 31 * result + (number?.contentHashCode() ?: 0)
        result = 31 * result + (cvv?.contentHashCode() ?: 0)
        return result
    }
}
