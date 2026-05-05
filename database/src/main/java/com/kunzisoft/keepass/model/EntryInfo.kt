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
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.Tags
import com.kunzisoft.keepass.database.element.entry.AutoType
import com.kunzisoft.keepass.model.AppOriginEntryField.containsDomainOrApplicationId
import com.kunzisoft.keepass.model.AppOriginEntryField.setAppOrigin
import com.kunzisoft.keepass.model.AppOriginEntryField.setApplicationId
import com.kunzisoft.keepass.model.AppOriginEntryField.setWebDomain
import com.kunzisoft.keepass.model.CreditCardEntryFields.setCreditCard
import com.kunzisoft.keepass.model.PasskeyEntryFields.isPasskey
import com.kunzisoft.keepass.model.PasskeyEntryFields.setPasskey
import com.kunzisoft.keepass.otp.OtpElement
import com.kunzisoft.keepass.otp.OtpEntryFields.OTP_TOKEN_FIELD
import com.kunzisoft.keepass.otp.OtpEntryFields.isOTP
import com.kunzisoft.keepass.otp.OtpEntryFields.setOtp
import com.kunzisoft.keepass.utils.CharArrayUtil.clear
import com.kunzisoft.keepass.utils.readBooleanCompat
import com.kunzisoft.keepass.utils.readCharArrayCompat
import com.kunzisoft.keepass.utils.readListCompat
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.writeBooleanCompat
import com.kunzisoft.keepass.utils.writeCharArrayCompat
import java.util.Locale
import java.util.UUID

class EntryInfo : NodeInfo {

    var id: UUID = UUID.randomUUID()
    var username: String = ""
    var password: CharArray = charArrayOf()
    var url: String = ""
    var notes: String = ""
    var tags: Tags = Tags()
    var backgroundColor: Int? = null
    var foregroundColor: Int? = null
    var customFields: MutableList<Field> = mutableListOf()
    var attachments: MutableList<Attachment> = mutableListOf()
    var autoType: AutoType = AutoType()
    var otpModel: OtpModel? = null
    var creditCard: CreditCard? = null
    var passkey: Passkey? = null
    var appOrigin: AppOrigin? = null
    var isTemplate: Boolean = false

    constructor() : super()

    constructor(entryToCopy: EntryInfo) : super(entryToCopy) {
        this.id = entryToCopy.id
        this.username = entryToCopy.username
        this.password = entryToCopy.password.copyOf()
        this.url = entryToCopy.url
        this.notes = entryToCopy.notes
        this.tags = Tags(entryToCopy.tags)
        this.backgroundColor = entryToCopy.backgroundColor
        this.foregroundColor = entryToCopy.foregroundColor
        this.customFields = entryToCopy.customFields.map { Field(it) }.toMutableList()
        this.attachments = entryToCopy.attachments.toMutableList()
        this.autoType = AutoType(entryToCopy.autoType)
        this.otpModel = entryToCopy.otpModel?.let { OtpModel(it) }
        this.creditCard = entryToCopy.creditCard?.let { CreditCard(it) }
        this.passkey = entryToCopy.passkey?.let { Passkey(it) }
        this.appOrigin = entryToCopy.appOrigin?.let { AppOrigin(it) }
        this.isTemplate = entryToCopy.isTemplate
    }

    constructor(parcel: Parcel) : super(parcel) {
        id = parcel.readParcelableCompat<ParcelUuid>()?.uuid ?: id
        username = parcel.readString() ?: username
        password = parcel.readCharArrayCompat() ?: password
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
        creditCard = parcel.readParcelableCompat() ?: creditCard
        passkey = parcel.readParcelableCompat() ?: passkey
        appOrigin = parcel.readParcelableCompat() ?: appOrigin
        isTemplate = parcel.readBooleanCompat()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeParcelable(ParcelUuid(id), flags)
        parcel.writeString(username)
        parcel.writeCharArrayCompat(password)
        parcel.writeString(url)
        parcel.writeString(notes)
        parcel.writeParcelable(tags, flags)
        parcel.writeInt(backgroundColor ?: -1)
        parcel.writeInt(foregroundColor ?: -1)
        parcel.writeList(customFields)
        parcel.writeList(attachments)
        parcel.writeParcelable(autoType, flags)
        parcel.writeParcelable(otpModel, flags)
        parcel.writeParcelable(creditCard, flags)
        parcel.writeParcelable(passkey, flags)
        parcel.writeParcelable(appOrigin, flags)
        parcel.writeBooleanCompat(isTemplate)
    }

    fun clear() {
        password.clear()
        customFields.forEach { it.clear() }
        otpModel?.clear()
        creditCard?.clear()
        passkey?.clear()
    }

    fun containsOtpToken(): Boolean {
        return containsCustomField(OTP_TOKEN_FIELD)
    }

    fun getOtpToken(): CharArray? {
        return otpModel?.let {
            OtpElement(it).token
        }
    }

    fun getCustomFieldsForFilling(): List<Field> {
        return customFields.filter {
            !it.isOTP() && !it.isPasskey()
        }
    }

    fun containsCustomField(label: String): Boolean {
        return customFields.lastOrNull { it.name == label } != null
    }

    fun getGeneratedFieldValue(label: String): CharArray? {
        if (label == OTP_TOKEN_FIELD) {
            return getOtpToken()
        }
        return customFields.lastOrNull { it.name == label }?.protectedValue?.charArrayValue
    }

    /**
     * Add a field to the custom fields list, replace if name already exists
     */
    fun addOrReplaceField(field: Field) {
        customFields.lastOrNull { it.name == field.name }?.let {
            it.apply {
                protectedValue = field.protectedValue
            }
        } ?: customFields.add(field)
    }

    /**
     * Add a field to the custom fields list with a suffix position,
     * replace if name already exists
     */
    fun addOrReplaceFieldWithSuffix(field: Field, position: Int) {
        addOrReplaceField(Field(
            field.name + suffixFieldNamePosition(position),
            field.protectedValue)
        )
    }

    /**
     * Add a unique field to the list of custom fields with a suffix
     * if name already exists and value not the same
     * @param field the field to add
     * @param position the number to add to the suffix
     * @return the increment number and the custom field created
     */
    fun addUniqueField(field: Field, position: Int = 0): Pair<Int, Field> {
        val suffix = suffixFieldNamePosition(position)
        if (customFields.any { currentField -> currentField.name == field.name + suffix }) {
            val fieldFound = customFields.find {
                it.name == field.name + suffix
                        && it.protectedValue.charArrayValue
                            .contentEquals(field.protectedValue.charArrayValue)
            }
            return if (fieldFound != null) {
                Pair(position, fieldFound)
            } else {
                addUniqueField(field, position + 1)
            }
        } else {
            val field = Field(field.name + suffix, field.protectedValue)
            customFields.add(field)
            return Pair(position, field)
        }
    }

    /**
     * Capitalize and remove suffix of a title
     */
    fun String.toTitle(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    /**
     * True if this entry contains domain or applicationId,
     * OTP is ignored and considered not present
     */
    fun containsSearchInfo(searchInfo: SearchInfo): Boolean {
        return searchInfo.webDomain?.let { webDomain ->
            containsDomainOrApplicationId(webDomain)
        } ?: searchInfo.applicationId?.let { applicationId ->
            containsDomainOrApplicationId(applicationId)
        } ?: false
    }

    /**
     * Add searchInfo to current EntryInfo
     */
    private fun saveSearchInfo(database: Database?, searchInfo: SearchInfo) {
        searchInfo.otpString?.let { otpString ->
            setOtp(otpString)
        } ?: searchInfo.webDomain?.let { webDomain ->
            setWebDomain(
                webDomain,
                searchInfo.webScheme,
                database?.allowEntryCustomFields() == true
            )
        } ?: searchInfo.applicationId?.let { applicationId ->
            setApplicationId(applicationId)
        }
        if (title.isEmpty()) {
            title = searchInfo.toString().toTitle()
        }
    }

    /**
     * Add registerInfo to current EntryInfo,
     * return true if data has been overwritten
     */
    fun saveRegisterInfo(database: Database?, registerInfo: RegisterInfo): Boolean {
        saveSearchInfo(database, registerInfo.searchInfo)
        registerInfo.username?.let { username = it }
        registerInfo.password?.let { password = it }
        registerInfo.expiration?.let {
            expires = true
            expiryTime = it
        }
        setCreditCard(registerInfo.creditCard)
        val dataOverwrite: Boolean = setPasskey(registerInfo.passkey)
        saveAppOrigin(database, registerInfo.appOrigin)
        if (title.isEmpty()) {
            title = registerInfo.toString().toTitle()
        }
        return dataOverwrite
    }

    /**
     * Add AppOrigin
     */
    fun saveAppOrigin(database: Database?, appOrigin: AppOrigin?) {
        setAppOrigin(appOrigin, database?.allowEntryCustomFields() == true)
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
        if (!password.contentEquals(other.password)) return false
        if (url != other.url) return false
        if (notes != other.notes) return false
        if (tags != other.tags) return false
        if (backgroundColor != other.backgroundColor) return false
        if (foregroundColor != other.foregroundColor) return false
        if (customFields != other.customFields) return false
        if (attachments != other.attachments) return false
        if (autoType != other.autoType) return false
        if (otpModel != other.otpModel) return false
        if (creditCard != other.creditCard) return false
        if (passkey != other.passkey) return false
        if (appOrigin != other.appOrigin) return false
        if (isTemplate != other.isTemplate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        result = 31 * result + foregroundColor.hashCode()
        result = 31 * result + customFields.hashCode()
        result = 31 * result + attachments.hashCode()
        result = 31 * result + autoType.hashCode()
        result = 31 * result + (otpModel?.hashCode() ?: 0)
        result = 31 * result + (creditCard?.hashCode() ?: 0)
        result = 31 * result + (passkey?.hashCode() ?: 0)
        result = 31 * result + (appOrigin?.hashCode() ?: 0)
        result = 31 * result + isTemplate.hashCode()
        return result
    }


    companion object {

        /**
         * Create a field name suffix depending on the field position
         */
        fun suffixFieldNamePosition(position: Int): String {
            return if (position > 0) "_$position" else ""
        }

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
