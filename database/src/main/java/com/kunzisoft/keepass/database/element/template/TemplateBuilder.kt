/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
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
 */
package com.kunzisoft.keepass.database.element.template

import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import java.util.*

class TemplateBuilder {

    private val urlAttribute = TemplateAttribute(TemplateField.LABEL_URL, TemplateAttributeType.TEXT)
    private val usernameAttribute = TemplateAttribute(TemplateField.LABEL_USERNAME, TemplateAttributeType.TEXT)
    private val notesAttribute = TemplateAttribute(
        TemplateField.LABEL_NOTES,
        TemplateAttributeType.TEXT,
        false,
        TemplateAttributeOption().apply {
            setNumberLinesToMany()
        })
    private val holderAttribute = TemplateAttribute(TemplateField.LABEL_HOLDER, TemplateAttributeType.TEXT)
    private val numberAttribute = TemplateAttribute(TemplateField.LABEL_NUMBER, TemplateAttributeType.TEXT)
    private val cvvAttribute = TemplateAttribute(TemplateField.LABEL_CVV, TemplateAttributeType.TEXT, true)
    private val pinAttribute = TemplateAttribute(TemplateField.LABEL_PIN, TemplateAttributeType.TEXT, true)
    private val nameAttribute = TemplateAttribute(TemplateField.LABEL_NAME, TemplateAttributeType.TEXT)
    private val placeOfIssueAttribute = TemplateAttribute(TemplateField.LABEL_PLACE_OF_ISSUE, TemplateAttributeType.TEXT)
    private val dateOfIssueAttribute = TemplateAttribute(
        TemplateField.LABEL_DATE_OF_ISSUE,
        TemplateAttributeType.DATETIME,
        false,
        TemplateAttributeOption().apply {
            setDateFormatToDate()
        })
    private val expirationDateAttribute = TemplateAttribute(
        TemplateField.LABEL_EXPIRATION,
        TemplateAttributeType.DATETIME,
        false,
        TemplateAttributeOption().apply {
            setDateFormatToDate()
        })
    private val emailAddressAttribute = TemplateAttribute(TemplateField.LABEL_EMAIL_ADDRESS, TemplateAttributeType.TEXT)
    private val passwordAttribute = TemplateAttribute(TemplateField.LABEL_PASSWORD, TemplateAttributeType.TEXT, true)
    private val ssidAttribute = TemplateAttribute(TemplateField.LABEL_SSID, TemplateAttributeType.TEXT)
    private val wirelessTypeAttribute = TemplateAttribute(
        TemplateField.LABEL_TYPE,
        TemplateAttributeType.LIST,
        false,
        TemplateAttributeOption().apply{
            setListItems("WPA3", "WPA2", "WPA", "WEP")
            default = "WPA2"
        })
    private val tokenAttribute = TemplateAttribute(TemplateField.LABEL_TOKEN, TemplateAttributeType.TEXT)
    private val publicKeyAttribute = TemplateAttribute(TemplateField.LABEL_PUBLIC_KEY, TemplateAttributeType.TEXT)
    private val privateKeyAttribute = TemplateAttribute(TemplateField.LABEL_PRIVATE_KEY, TemplateAttributeType.TEXT, true)
    private val seedAttribute = TemplateAttribute(TemplateField.LABEL_SEED, TemplateAttributeType.TEXT, true)
    private val bicAttribute = TemplateAttribute(TemplateField.LABEL_BIC, TemplateAttributeType.TEXT)
    private val ibanAttribute = TemplateAttribute(TemplateField.LABEL_IBAN, TemplateAttributeType.TEXT)

    val email: Template
        get() {
            val sections = mutableListOf<TemplateSection>()
            val mainSection = TemplateSection(mutableListOf<TemplateAttribute>().apply {
                add(emailAddressAttribute)
                add(urlAttribute)
                add(passwordAttribute)
            })
            sections.add(mainSection)
            return Template(
                UUID.randomUUID(),
                TemplateField.LABEL_EMAIL,
                IconImage(IconImageStandard(IconImageStandard.EMAIL_ID)),
                sections)
        }

    val wifi: Template
        get() {
            val sections = mutableListOf<TemplateSection>()
            val mainSection = TemplateSection(mutableListOf<TemplateAttribute>().apply {
                add(ssidAttribute)
                add(passwordAttribute)
                add(wirelessTypeAttribute)
            })
            sections.add(mainSection)
            return Template(
                UUID.randomUUID(),
                TemplateField.LABEL_WIRELESS,
                IconImage(IconImageStandard(IconImageStandard.WIRELESS_ID)),
                sections)
        }

    val notes: Template
        get() {
            val sections = mutableListOf<TemplateSection>()
            val mainSection = TemplateSection(mutableListOf<TemplateAttribute>().apply {
                add(notesAttribute)
            })
            sections.add(mainSection)
            return Template(
                UUID.randomUUID(),
                TemplateField.LABEL_NOTES,
                IconImage(IconImageStandard(IconImageStandard.LIST_ID)),
                sections)
        }

    val idCard: Template
        get() {
            val sections = mutableListOf<TemplateSection>()
            val mainSection = TemplateSection(mutableListOf<TemplateAttribute>().apply {
                add(numberAttribute)
                add(nameAttribute)
                add(placeOfIssueAttribute)
                add(dateOfIssueAttribute)
                add(expirationDateAttribute)
            })
            sections.add(mainSection)
            return Template(
                UUID.randomUUID(),
                TemplateField.LABEL_ID_CARD,
                IconImage(IconImageStandard(IconImageStandard.ID_CARD_ID)),
                sections)
        }

    val creditCard: Template
        get() {
            val sections = mutableListOf<TemplateSection>()
            val mainSection = TemplateSection(mutableListOf<TemplateAttribute>().apply {
                add(numberAttribute)
                add(cvvAttribute)
                add(pinAttribute)
                add(holderAttribute)
                add(expirationDateAttribute)
            })
            sections.add(mainSection)
            return Template(
                UUID.randomUUID(),
                TemplateField.LABEL_DEBIT_CREDIT_CARD,
                IconImage(IconImageStandard(IconImageStandard.CREDIT_CARD_ID)),
                sections)
        }

    val bank: Template
        get() {
            val sections = mutableListOf<TemplateSection>()
            val mainSection = TemplateSection(mutableListOf<TemplateAttribute>().apply {
                add(nameAttribute)
                add(urlAttribute)
                add(usernameAttribute)
                add(passwordAttribute)
            })
            val ibanSection = TemplateSection(mutableListOf<TemplateAttribute>().apply {
                add(holderAttribute)
                add(bicAttribute)
                add(ibanAttribute)
            }, TemplateField.LABEL_ACCOUNT)
            sections.add(mainSection)
            sections.add(ibanSection)
            return Template(
                UUID.randomUUID(),
                TemplateField.LABEL_BANK,
                IconImage(IconImageStandard(IconImageStandard.DOLLAR_ID)),
                sections)
        }

    val cryptocurrency: Template
        get() {
            val sections = mutableListOf<TemplateSection>()
            val mainSection = TemplateSection(mutableListOf<TemplateAttribute>().apply {
                add(tokenAttribute)
                add(publicKeyAttribute)
                add(privateKeyAttribute)
                add(seedAttribute)
            })
            sections.add(mainSection)
            return Template(
                UUID.randomUUID(),
                TemplateField.LABEL_CRYPTOCURRENCY,
                IconImage(IconImageStandard(IconImageStandard.STAR_ID)),
                sections)
        }
}