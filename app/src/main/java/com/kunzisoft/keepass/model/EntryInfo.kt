/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.model

import android.os.Parcel
import android.os.ParcelUuid
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.DateInstant
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.otp.OtpEntryFields.OTP_TOKEN_FIELD
import java.util.*

class EntryInfo : NodeInfo {

    var id: UUID = UUID.randomUUID()
    var username: String = ""
    var password: String = ""
    var url: String = ""
    var notes: String = ""
    var customFields: MutableList<Field> = mutableListOf()
    var attachments: MutableList<Attachment> = mutableListOf()
    var otpModel: OtpModel? = null
    var isTemplate: Boolean = false

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        id = parcel.readParcelable<ParcelUuid>(ParcelUuid::class.java.classLoader)?.uuid ?: id
        username = parcel.readString() ?: username
        password = parcel.readString() ?: password
        url = parcel.readString() ?: url
        notes = parcel.readString() ?: notes
        parcel.readList(customFields, Field::class.java.classLoader)
        parcel.readList(attachments, Attachment::class.java.classLoader)
        otpModel = parcel.readParcelable(OtpModel::class.java.classLoader) ?: otpModel
        isTemplate = parcel.readByte().toInt() != 0
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeParcelable(ParcelUuid(id), flags)
        parcel.writeString(username)
        parcel.writeString(password)
        parcel.writeString(url)
        parcel.writeString(notes)
        parcel.writeList(customFields)
        parcel.writeList(attachments)
        parcel.writeParcelable(otpModel, flags)
        parcel.writeByte((if (isTemplate) 1 else 0).toByte())
    }

    fun containsCustomFieldsProtected(): Boolean {
        return customFields.any { it.protectedValue.isProtected }
    }

    fun containsCustomFieldsNotProtected(): Boolean {
        return customFields.any { !it.protectedValue.isProtected }
    }

    fun containsCustomField(label: String): Boolean {
        return customFields.lastOrNull { it.name == label } != null
    }

    fun getGeneratedFieldValue(label: String): String {
        if (label == OTP_TOKEN_FIELD) {
            otpModel?.let {
                return OtpElement(it).token
            }
        }
        return customFields.lastOrNull { it.name == label }?.protectedValue?.toString() ?: ""
    }

    private fun addUniqueField(field: Field, number: Int = 0) {
        var sameName = false
        var sameValue = false
        val suffix = if (number > 0) "_$number" else ""
        customFields.forEach { currentField ->
            // Not write the same data again
            if (currentField.protectedValue.stringValue == field.protectedValue.stringValue) {
                sameValue = true
                return
            }
            // Same name but new value, create a new suffix
            if (currentField.name == field.name + suffix) {
                sameName = true
                addUniqueField(field, number + 1)
                return
            }
        }
        if (!sameName && !sameValue)
            (customFields as ArrayList<Field>).add(Field(field.name + suffix, field.protectedValue))
    }

    fun saveSearchInfo(database: Database?, searchInfo: SearchInfo) {
        searchInfo.otpString?.let { otpString ->
            // Replace the OTP field
            OtpEntryFields.parseOTPUri(otpString)?.let { otpElement ->
                if (title.isEmpty())
                    title = otpElement.issuer
                if (username.isEmpty())
                    username = otpElement.name
                // Add OTP field
                val mutableCustomFields = customFields as ArrayList<Field>
                val otpField = OtpEntryFields.buildOtpField(otpElement, null, null)
                if (mutableCustomFields.contains(otpField)) {
                    mutableCustomFields.remove(otpField)
                }
                mutableCustomFields.add(otpField)
            }
        } ?: searchInfo.webDomain?.let { webDomain ->
            // If unable to save web domain in custom field or URL not populated, save in URL
            val scheme = searchInfo.webScheme
            val webScheme = if (scheme.isNullOrEmpty()) "http" else scheme
            val webDomainToStore = "$webScheme://$webDomain"
            if (database?.allowEntryCustomFields() != true || url.isEmpty()) {
                url = webDomainToStore
            } else if (url != webDomainToStore) {
                // Save web domain in custom field
                addUniqueField(Field(WEB_DOMAIN_FIELD_NAME,
                        ProtectedString(false, webDomainToStore)),
                        1 // Start to one because URL is a standard field name
                )
            }
        } ?: run {
            // Save application id in custom field
            if (database?.allowEntryCustomFields() == true) {
                searchInfo.applicationId?.let { applicationId ->
                    addUniqueField(Field(APPLICATION_ID_FIELD_NAME,
                            ProtectedString(false, applicationId))
                    )
                }
            }
        }
    }

    fun saveRegisterInfo(database: Database?, registerInfo: RegisterInfo) {
        registerInfo.username?.let {
            username = it
        }
        registerInfo.password?.let {
            password = it
        }

        if (database?.allowEntryCustomFields() == true) {
            val creditCard: CreditCard? = registerInfo.creditCard

            creditCard?.let { cc ->
                cc.cardholder?.let {
                    val v = ProtectedString(false, it)
                    addUniqueField(Field(TemplateField.LABEL_HOLDER, v))
                }
                cc.expiration?.let {
                    expires = true
                    expiryTime = DateInstant(cc.expiration.millis)
                }
                cc.number?.let {
                    val v = ProtectedString(false, it)
                    addUniqueField(Field(TemplateField.LABEL_NUMBER, v))
                }
                cc.cvv?.let {
                    val v = ProtectedString(true, it)
                    addUniqueField(Field(TemplateField.LABEL_CVV, v))
                }
            }
        }
    }

    companion object {

        const val WEB_DOMAIN_FIELD_NAME = "URL"
        const val APPLICATION_ID_FIELD_NAME = "AndroidApp"

        @JvmField
        val CREATOR: Parcelable.Creator<EntryInfo> = object : Parcelable.Creator<EntryInfo> {
            override fun createFromParcel(parcel: Parcel): EntryInfo {
                return EntryInfo(parcel)
            }

            override fun newArray(size: Int): Array<EntryInfo?> {
                return arrayOfNulls(size)
            }
        }
    }
}
