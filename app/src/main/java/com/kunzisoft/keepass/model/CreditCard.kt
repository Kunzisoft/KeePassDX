package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.Parcelable

class CreditCard() : Parcelable {
    var cardholder: String = "";
    var number: String = "";
    var expiration: String = "";
    var cvv: String = "";

    constructor(ccFields: List<Field>?) : this() {
        ccFields?.let {
            for (field in it) {
                when (field.name) {
                    CreditCardCustomFields.CC_CARDHOLDER_FIELD_NAME ->
                        this.cardholder = field.protectedValue.stringValue
                    CreditCardCustomFields.CC_NUMBER_FIELD_NAME ->
                        this.number = field.protectedValue.stringValue
                    CreditCardCustomFields.CC_EXP_FIELD_NAME ->
                        this.expiration = field.protectedValue.stringValue
                    CreditCardCustomFields.CC_CVV_FIELD_NAME ->
                        this.cvv = field.protectedValue.stringValue
                }
            }
        }
    }

    constructor(parcel: Parcel) : this() {
        cardholder = parcel.readString() ?: cardholder
        number = parcel.readString() ?: number
        expiration = parcel.readString() ?: expiration
        cvv = parcel.readString() ?: cvv
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(cardholder)
        parcel.writeString(number)
        parcel.writeString(expiration)
        parcel.writeString(cvv)
    }

    fun getExpirationMonth(): String {
        return if (expiration.length == 4) {
            expiration.substring(0, 2)
        } else {
            ""
        }
    }

    fun getExpirationYear(): String {
        return if (expiration.length == 4) {
            expiration.substring(2, 4)
        } else {
            ""
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CreditCard

        if (cardholder != other.cardholder) return false
        if (number != other.number) return false
        if (expiration != other.expiration) return false
        if (cvv != other.cvv) return false

        return true
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun hashCode(): Int {
        var result = cardholder.hashCode()
        result = 31 * result + number.hashCode()
        result = 31 * result + expiration.hashCode()
        result = 31 * result + cvv.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<CreditCard> {
        override fun createFromParcel(parcel: Parcel): CreditCard {
            return CreditCard(parcel)
        }

        override fun newArray(size: Int): Array<CreditCard?> {
            return arrayOfNulls(size)
        }
    }
}
