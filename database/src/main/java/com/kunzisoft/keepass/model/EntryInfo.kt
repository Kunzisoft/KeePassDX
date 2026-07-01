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
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.EntryId
import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.entry.AutoType
import com.kunzisoft.keepass.database.element.node.NodeIdUUID
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.Template
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
import com.kunzisoft.keepass.utils.readCharArrayCompat
import com.kunzisoft.keepass.utils.readListCompat
import com.kunzisoft.keepass.utils.readParcelableCompat
import com.kunzisoft.keepass.utils.writeCharArrayCompat
import java.util.Locale

/**
 * Data class representing information about an entry in the database.
 * This class is used for UI representation and data transfer.
 */
open class EntryInfo : NodeInfo {

    override var nodeId: EntryId = NodeIdUUID()
    var username: String = ""
    var password: CharArray = charArrayOf()
    var url: String = ""
    var notes: String = ""
    var backgroundColor: Int? = null
    var foregroundColor: Int? = null
    var customFields: MutableList<Field> = mutableListOf()
    var attachments: MutableList<Attachment> = mutableListOf()
    var autoType: AutoType = AutoType()
    var otpModel: OtpModel? = null
    var creditCard: CreditCard? = null
    var passkey: Passkey? = null
    var appOrigin: AppOrigin? = null
    var template: Template = Template.STANDARD

    /**
     * Default constructor.
     */
    constructor() : super()

    /**
     * Copy constructor.
     * @param entryToCopy The entry info to copy.
     */
    constructor(entryToCopy: EntryInfo) : super(entryToCopy) {
        this.nodeId = entryToCopy.nodeId
        this.username = entryToCopy.username
        this.password = entryToCopy.password.copyOf()
        this.url = entryToCopy.url
        this.notes = entryToCopy.notes
        this.backgroundColor = entryToCopy.backgroundColor
        this.foregroundColor = entryToCopy.foregroundColor
        this.customFields = entryToCopy.customFields.map { Field(it) }.toMutableList()
        this.attachments = entryToCopy.attachments.toMutableList()
        this.autoType = AutoType(entryToCopy.autoType)
        this.otpModel = entryToCopy.otpModel?.let { OtpModel(it) }
        this.creditCard = entryToCopy.creditCard?.let { CreditCard(it) }
        this.passkey = entryToCopy.passkey?.let { Passkey(it) }
        this.appOrigin = entryToCopy.appOrigin?.let { AppOrigin(it) }
        this.template = entryToCopy.template
    }

    /**
     * Parcel constructor.
     * @param parcel The parcel to read from.
     */
    constructor(parcel: Parcel) : super(parcel) {
        nodeId = parcel.readParcelableCompat<EntryId>() ?: nodeId
        username = parcel.readString() ?: username
        password = parcel.readCharArrayCompat() ?: password
        url = parcel.readString() ?: url
        notes = parcel.readString() ?: notes
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
        template = parcel.readParcelableCompat() ?: template
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeParcelable(nodeId, flags)
        parcel.writeString(username)
        parcel.writeCharArrayCompat(password)
        parcel.writeString(url)
        parcel.writeString(notes)
        parcel.writeInt(backgroundColor ?: -1)
        parcel.writeInt(foregroundColor ?: -1)
        parcel.writeList(customFields)
        parcel.writeList(attachments)
        parcel.writeParcelable(autoType, flags)
        parcel.writeParcelable(otpModel, flags)
        parcel.writeParcelable(creditCard, flags)
        parcel.writeParcelable(passkey, flags)
        parcel.writeParcelable(appOrigin, flags)
        parcel.writeParcelable(template, flags)
    }

    /**
     * Clear sensitive data from memory.
     */
    fun clear() {
        password.clear()
        customFields.forEach { it.clear() }
        otpModel?.clear()
        creditCard?.clear()
        passkey?.clear()
    }

    /**
     * Check if the entry contains an OTP token.
     * @return True if it contains an OTP token, false otherwise.
     */
    fun containsOtpToken(): Boolean {
        return containsCustomField(OTP_TOKEN_FIELD)
    }

    /**
     * Get the OTP token.
     * @return The OTP token if present, null otherwise.
     */
    fun getOtpToken(): CharArray? {
        return otpModel?.let {
            OtpElement(it).token
        }
    }

    /**
     * Get all fields suitable for content provider
     */
    override fun getFieldsForContentProvider(): List<Field> {
        return mutableListOf<Field>().apply {
            addAll(super.getFieldsForContentProvider())
            add(Field(FIELD_ENTRY_USERNAME, ProtectedString(
                enableProtection = false,
                value = username.toCharArray())
            ))
            add(Field(FIELD_ENTRY_PASSWORD, ProtectedString(
                enableProtection = true,
                value = password.copyOf())
            ))
            add(Field(FIELD_ENTRY_URL, ProtectedString(
                enableProtection = false,
                value = url.toCharArray())
            ))
            add(Field(FIELD_ENTRY_NOTES, ProtectedString(
                enableProtection = false,
                value = notes.toCharArray())
            ))
            addAll(getCustomFieldsForFilling())
        }
    }

    /**
     * Get custom fields suitable for auto-filling.
     * @return List of fields excluding OTP and Passkey fields.
     */
    fun getCustomFieldsForFilling(): List<Field> {
        return customFields.filter {
            !it.isOTP() && !it.isPasskey()
        }
    }

    /**
     * Check if a custom field with the specified label exists.
     * @param label The label to search for.
     * @return True if found, false otherwise.
     */
    fun containsCustomField(label: String): Boolean {
        return customFields.lastOrNull { it.name == label } != null
    }

    /**
     * Get the generated value of a field by its label.
     * @param label The label of the field.
     * @return The field value as CharArray, or null if not found.
     */
    fun getGeneratedFieldValue(label: String): CharArray? {
        if (label == OTP_TOKEN_FIELD) {
            return getOtpToken()
        }
        return customFields.lastOrNull { it.name == label }?.protectedValue?.charArrayValue
    }

    /**
     * Add a field to the custom fields list, replace if name already exists.
     * @param field The field to add or replace.
     */
    fun addOrReplaceField(field: Field) {
        customFields.lastOrNull { it.name == field.name }?.let {
            it.apply {
                protectedValue = field.protectedValue
            }
        } ?: customFields.add(field)
    }

    /**
     * Add a field to the custom fields list with a suffix position, replace if name already exists.
     * @param field The field to add.
     * @param position The position to use for the suffix.
     */
    fun addOrReplaceFieldWithSuffix(field: Field, position: Int) {
        addOrReplaceField(Field(
            field.name + suffixFieldNamePosition(position),
            field.protectedValue)
        )
    }

    /**
     * Add a unique field to the list of custom fields with a suffix if name already exists and value is not the same.
     * @param field the field to add.
     * @param position the number to add to the suffix.
     * @return the increment number and the custom field created.
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
     * Capitalize and remove suffix of a title.
     * @receiver The string to format.
     * @return The formatted title string.
     */
    fun String.toTitle(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    /**
     * True if this entry contains domain or applicationId,
     * OTP is ignored and considered not present.
     * @param searchInfo Info to search for.
     * @return True if it contains search info, false otherwise.
     */
    fun containsSearchInfo(searchInfo: SearchInfo): Boolean {
        return searchInfo.webDomain?.let { webDomain ->
            containsDomainOrApplicationId(webDomain)
        } ?: searchInfo.applicationId?.let { applicationId ->
            containsDomainOrApplicationId(applicationId)
        } ?: false
    }

    /**
     * Check if it's allowed to save search info to this entry.
     * @param searchInfo Search info to check.
     * @return True if allowed, false otherwise.
     */
    fun allowedToSaveSearchInfo(
        searchInfo: SearchInfo?
    ): Boolean {
        if (searchInfo == null || searchInfo.toString().isEmpty())
            return false
        return !(this.containsSearchInfo(searchInfo))
    }

    /**
     * True if this entry contains any attachment.
     * @return True if attachments list is not empty.
     */
    fun containsAttachment(): Boolean {
        return attachments.isNotEmpty()
    }

    /**
     * Add searchInfo to current EntryInfo.
     * @param searchInfo Search info to save.
     * @param customFieldsAllowed True if custom fields are allowed.
     */
    private fun saveSearchInfo(searchInfo: SearchInfo, customFieldsAllowed: Boolean) {
        searchInfo.otpString?.let { otpString ->
            setOtp(otpString)
        } ?: searchInfo.webDomain?.let { webDomain ->
            setWebDomain(
                webDomain,
                searchInfo.webScheme,
                customFieldsAllowed
            )
        } ?: searchInfo.applicationId?.let { applicationId ->
            setApplicationId(applicationId)
        }
        if (title.isEmpty()) {
            title = searchInfo.toString().toTitle()
        }
    }

    /**
     * Add registerInfo to current EntryInfo, return true if data has been overwritten.
     * @param registerInfo Register info to save.
     * @param customFieldsAllowed True if custom fields are allowed.
     * @return True if data was overwritten, false otherwise.
     */
    fun saveRegisterInfo(registerInfo: RegisterInfo, customFieldsAllowed: Boolean): Boolean {
        saveSearchInfo(registerInfo.searchInfo, customFieldsAllowed)
        registerInfo.username?.let { username = it }
        registerInfo.password?.let { password = it }
        registerInfo.expiration?.let {
            expires = true
            expiryTime = it
        }
        setCreditCard(registerInfo.creditCard)
        val dataOverwrite: Boolean = setPasskey(registerInfo.passkey)
        saveAppOrigin(registerInfo.appOrigin, customFieldsAllowed)
        if (title.isEmpty()) {
            title = registerInfo.toString().toTitle()
        }
        return dataOverwrite
    }

    /**
     * Add AppOrigin.
     * @param appOrigin App origin to save.
     * @param customFieldsAllowed True if custom fields are allowed.
     */
    fun saveAppOrigin(appOrigin: AppOrigin?, customFieldsAllowed: Boolean) {
        setAppOrigin(appOrigin, customFieldsAllowed)
    }

    /**
     * Get a visual title for the entry, fallback to URL, username or ID.
     * @return The best visual title string.
     */
    fun getVisualTitle(): String {
        return title.ifEmpty {
            url.ifEmpty {
                username.ifEmpty { nodeId.toString() }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntryInfo) return false
        if (!super.equals(other)) return false

        if (nodeId != other.nodeId) return false
        if (username != other.username) return false
        if (!password.contentEquals(other.password)) return false
        if (url != other.url) return false
        if (notes != other.notes) return false
        if (backgroundColor != other.backgroundColor) return false
        if (foregroundColor != other.foregroundColor) return false
        if (customFields != other.customFields) return false
        if (attachments != other.attachments) return false
        if (autoType != other.autoType) return false
        if (otpModel != other.otpModel) return false
        if (creditCard != other.creditCard) return false
        if (passkey != other.passkey) return false
        if (appOrigin != other.appOrigin) return false
        if (template != other.template) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + nodeId.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + password.contentHashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        result = 31 * result + foregroundColor.hashCode()
        result = 31 * result + customFields.hashCode()
        result = 31 * result + attachments.hashCode()
        result = 31 * result + autoType.hashCode()
        result = 31 * result + (otpModel?.hashCode() ?: 0)
        result = 31 * result + (creditCard?.hashCode() ?: 0)
        result = 31 * result + (passkey?.hashCode() ?: 0)
        result = 31 * result + (appOrigin?.hashCode() ?: 0)
        result = 31 * result + template.hashCode()
        return result
    }


    companion object {

        /**
         * Create a field name suffix depending on the field position
         */
        fun suffixFieldNamePosition(position: Int): String {
            return if (position > 0) "_$position" else ""
        }

        private const val FIELD_ENTRY_USERNAME = "username"
        private const val FIELD_ENTRY_PASSWORD = "password"
        private const val FIELD_ENTRY_URL = "url"
        private const val FIELD_ENTRY_NOTES = "notes"

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
