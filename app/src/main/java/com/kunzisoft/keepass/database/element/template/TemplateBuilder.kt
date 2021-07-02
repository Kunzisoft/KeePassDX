package com.kunzisoft.keepass.database.element.template

import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import java.util.*

class TemplateBuilder(labelBuilder: (plainLabel: String) -> String) {

    private val urlAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_URL), TemplateAttributeType.INLINE)
    private val usernameAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_USERNAME), TemplateAttributeType.INLINE)
    private val notesPlainAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_NOTES), TemplateAttributeType.MULTILINE)
    private val holderAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_HOLDER), TemplateAttributeType.INLINE)
    private val numberAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_NUMBER), TemplateAttributeType.INLINE)
    private val cvvAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_CVV), TemplateAttributeType.INLINE, true)
    private val pinAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_PIN), TemplateAttributeType.INLINE, true)
    private val nameAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_NAME), TemplateAttributeType.INLINE)
    private val placeOfIssueAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_PLACE_OF_ISSUE), TemplateAttributeType.INLINE)
    private val dateOfIssueAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_DATE_OF_ISSUE), TemplateAttributeType.DATE)
    private val expirationDateAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_EXPIRATION), TemplateAttributeType.DATE)
    private val emailAddressAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_EMAIL_ADDRESS), TemplateAttributeType.INLINE)
    private val passwordAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_PASSWORD), TemplateAttributeType.INLINE, true)
    private val ssidAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_SSID), TemplateAttributeType.INLINE)
    private val tokenAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_TOKEN), TemplateAttributeType.INLINE)
    private val publicKeyAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_PUBLIC_KEY), TemplateAttributeType.INLINE)
    private val privateKeyAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_PRIVATE_KEY), TemplateAttributeType.INLINE, true)
    private val seedAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_SEED), TemplateAttributeType.INLINE, true)
    private val bicAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_BIC), TemplateAttributeType.INLINE)
    private val ibanAttribute = TemplateAttribute(labelBuilder(TemplateField.LABEL_IBAN), TemplateAttributeType.INLINE)

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
                add(notesPlainAttribute)
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
            })
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