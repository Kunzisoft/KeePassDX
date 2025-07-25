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

    private val urlAttribute = TemplateAttribute(
        label = TemplateField.LABEL_URL,
        type = TemplateAttributeType.TEXT
    )
    private val usernameAttribute = TemplateAttribute(
        label = TemplateField.LABEL_USERNAME,
        type = TemplateAttributeType.TEXT
    )
    private val notesAttribute = TemplateAttribute(
        label = TemplateField.LABEL_NOTES,
        type = TemplateAttributeType.TEXT,
        protected = false,
        options = TemplateAttributeOption().apply {
            setNumberLinesToMany()
        }
    )
    private val holderAttribute = TemplateAttribute(
        label = TemplateField.LABEL_HOLDER,
        type = TemplateAttributeType.TEXT
    )
    private val numberAttribute = TemplateAttribute(
        label = TemplateField.LABEL_NUMBER,
        type = TemplateAttributeType.TEXT
    )
    private val cvvAttribute = TemplateAttribute(
        label = TemplateField.LABEL_CVV,
        type = TemplateAttributeType.TEXT,
        protected = true
    )
    private val pinAttribute = TemplateAttribute(
        label = TemplateField.LABEL_PIN,
        type = TemplateAttributeType.TEXT,
        protected = true
    )
    private val nameAttribute = TemplateAttribute(
        label = TemplateField.LABEL_NAME,
        type = TemplateAttributeType.TEXT
    )
    private val placeOfIssueAttribute = TemplateAttribute(
        label = TemplateField.LABEL_PLACE_OF_ISSUE,
        type = TemplateAttributeType.TEXT
    )
    private val dateOfIssueAttribute = TemplateAttribute(
        label = TemplateField.LABEL_DATE_OF_ISSUE,
        type = TemplateAttributeType.DATETIME,
        protected = false,
        options = TemplateAttributeOption().apply {
            setDateFormatToDate()
        }
    )
    private val expirationDateAttribute = TemplateAttribute(
        label = TemplateField.LABEL_EXPIRATION,
        type = TemplateAttributeType.DATETIME,
        protected = false,
        options = TemplateAttributeOption().apply {
            setDateFormatToDate()
        }
    )
    private val emailAddressAttribute = TemplateAttribute(
        label = TemplateField.LABEL_USERNAME,
        type = TemplateAttributeType.TEXT,
        options = TemplateAttributeOption().apply {
            alias = TemplateField.LABEL_EMAIL_ADDRESS
        }
    )
    private val passwordAttribute = TemplateAttribute(
        label = TemplateField.LABEL_PASSWORD,
        type = TemplateAttributeType.TEXT,
        protected = true
    )
    private val ssidAttribute = TemplateAttribute(
        label = TemplateField.LABEL_SSID,
        type = TemplateAttributeType.TEXT
    )
    private val wirelessTypeAttribute = TemplateAttribute(
        label = TemplateField.LABEL_TYPE,
        type = TemplateAttributeType.LIST,
        protected = false,
        options = TemplateAttributeOption().apply{
            setListItems("WPA3", "WPA2", "WPA", "WEP")
            default = "WPA2"
        }
    )
    private val tokenAttribute = TemplateAttribute(
        label = TemplateField.LABEL_TOKEN,
        type = TemplateAttributeType.TEXT
    )
    private val publicKeyAttribute = TemplateAttribute(
        label = TemplateField.LABEL_PUBLIC_KEY,
        type = TemplateAttributeType.TEXT
    )
    private val privateKeyAttribute = TemplateAttribute(
        label = TemplateField.LABEL_PRIVATE_KEY,
        type = TemplateAttributeType.TEXT,
        protected = true
    )
    private val seedAttribute = TemplateAttribute(
        label = TemplateField.LABEL_SEED,
        type = TemplateAttributeType.TEXT,
        protected = true
    )
    private val bicAttribute = TemplateAttribute(
        label = TemplateField.LABEL_BIC,
        type = TemplateAttributeType.TEXT
    )
    private val ibanAttribute = TemplateAttribute(
        label = TemplateField.LABEL_IBAN,
        type = TemplateAttributeType.TEXT
    )

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