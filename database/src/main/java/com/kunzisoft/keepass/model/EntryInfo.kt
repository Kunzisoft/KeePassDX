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
import com.kunzisoft.keepass.database.element.*
import com.kunzisoft.keepass.database.element.entry.AutoType
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.TemplateField
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields
import com.kunzisoft.keepass.otp.OtpEntryFields.OTP_TOKEN_FIELD
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.readListCompat
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat
import java.util.*

class EntryInfo : NodeInfo {

    var id: UUID = UUID.randomUUID()
    var username: String = ""
    var password: String = ""
    var url: String = ""
    var notes: String = ""
    var tags: Tags = Tags()
    var backgroundColor: Int? = null
    var foregroundColor: Int? = null
    var customFields: MutableList<Field> = mutableListOf()
    var attachments: MutableList<Attachment> = mutableListOf()
    var autoType: AutoType = AutoType()
    var otpModel: OtpModel? = null
    var isTemplate: Boolean = false

    constructor() : super()

    constructor(parcel: Parcel) : super(parcel) {
        id = parcel.readParcelableCompat<ParcelUuid>()?.uuid ?: id
        username = parcel.readString() ?: username
        password = parcel.readString() ?: password
        url = parcel.readString() ?: url
        notes = parcel.readString() ?: notes
        tags = parcel.readParcelableCompat() ?: tags
        val readBgColor = parcel.readInt()
        backgroundColor = if (readBgColor == -1) null else readBgColor
        val readFgColor = parcel.readInt()
        foregroundColor = if (readFgColor == -1) null else readFgColor
        parcel.readListCompat(customFields)
        parcel.readListCompat(attachments)
        autoType = parcel.readParcelableCompat() ?: autoType
        otpModel = parcel.readParcelableCompat() ?: otpModel
        isTemplate = parcel.readBooleanCompat()
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
        parcel.writeParcelable(tags, flags)
        parcel.writeInt(backgroundColor ?: -1)
        parcel.writeInt(foregroundColor ?: -1)
        parcel.writeList(customFields)
        parcel.writeList(attachments)
        parcel.writeParcelable(autoType, flags)
        parcel.writeParcelable(otpModel, flags)
        parcel.writeBooleanCompat(isTemplate)
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

    // Return true if modified
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

    private fun containsDomainOrApplicationId(search: String): Boolean {
        if (url.contains(search))
            return true
        return customFields.find {
            it.protectedValue.stringValue.contains(search)
        } != null
    }

    /**
     * Add searchInfo to current EntryInfo, return true if new data, false if no modification
     */
    fun saveSearchInfo(database: Database?, searchInfo: SearchInfo): Boolean {
        var modification = false
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
                modification = true
            }
        } ?: searchInfo.webDomain?.let { webDomain ->
            // If unable to save web domain in custom field or URL not populated, save in URL
            val scheme = searchInfo.webScheme
            val webScheme = if (scheme.isNullOrEmpty()) "https" else scheme
            val webDomainToStore = "$webScheme://$webDomain"
            if (!containsDomainOrApplicationId(webDomain)) {
                if (database?.allowEntryCustomFields() != true || url.isEmpty()) {
                    url = webDomainToStore
                } else {
                    // Save web domain in custom field
                    addUniqueField(
                        Field(
                            WEB_DOMAIN_FIELD_NAME,
                            ProtectedString(false, webDomainToStore)
                        ),
                        1 // Start to one because URL is a standard field name
                    )
                }
                modification = true
            }
        } ?: searchInfo.applicationId?.let { applicationId ->
            // Save application id in custom field
            if (database?.allowEntryCustomFields() == true) {
                if (!containsDomainOrApplicationId(applicationId)) {
                    addUniqueField(
                        Field(
                            APPLICATION_ID_FIELD_NAME,
                            ProtectedString(false, applicationId)
                        )
                    )
                    modification = true
                }
            }
        }
        if (title.isEmpty()) {
            title = searchInfoToTitle(searchInfo)
        }
        return modification
    }

    /**
     * Capitalize and remove suffix of web domain to create a title
     */
    private fun searchInfoToTitle(searchInfo: SearchInfo): String {
        val webDomain = searchInfo.webDomain
        return webDomain?.substring(0, webDomain.lastIndexOf('.'))?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } ?: searchInfo.toString()
    }

    fun saveRegisterInfo(database: Database?, registerInfo: RegisterInfo) {
        registerInfo.searchInfo.let {
            title = searchInfoToTitle(it)
        }
        registerInfo.username?.let {
            username = it
        }
        registerInfo.password?.let {
            password = it
        }

        if (database?.allowEntryCustomFields() == true) {
            val creditCard: CreditCard? = registerInfo.creditCard
            creditCard?.cardholder?.let {
                addUniqueField(Field(TemplateField.LABEL_HOLDER, ProtectedString(false, it)))
            }
            creditCard?.expiration?.let {
                expires = true
                expiryTime = DateInstant(creditCard.expiration.millis)
            }
            creditCard?.number?.let {
                addUniqueField(Field(TemplateField.LABEL_NUMBER, ProtectedString(false, it)))
            }
            creditCard?.cvv?.let {
                addUniqueField(Field(TemplateField.LABEL_CVV, ProtectedString(true, it)))
            }
        }
    }

    fun getVisualTitle(): String {
        return title.ifEmpty {
            url.ifEmpty {
                username.ifEmpty { id.toString() }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntryInfo) return false
        if (!super.equals(other)) return false

        if (id != other.id) return false
        if (username != other.username) return false
        if (password != other.password) return false
        if (url != other.url) return false
        if (notes != other.notes) return false
        if (tags != other.tags) return false
        if (backgroundColor != other.backgroundColor) return false
        if (foregroundColor != other.foregroundColor) return false
        if (customFields != other.customFields) return false
        if (attachments != other.attachments) return false
        if (autoType != other.autoType) return false
        if (otpModel != other.otpModel) return false
        if (isTemplate != other.isTemplate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        result = 31 * result + foregroundColor.hashCode()
        result = 31 * result + customFields.hashCode()
        result = 31 * result + attachments.hashCode()
        result = 31 * result + autoType.hashCode()
        result = 31 * result + (otpModel?.hashCode() ?: 0)
        result = 31 * result + isTemplate.hashCode()
        return result
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
