/*
 * Copyright 2025 Jeremy Jamet / Kunzisoft.
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

import com.kunzisoft.keepass.database.element.Field
import com.kunzisoft.keepass.database.element.security.ProtectedString
import com.kunzisoft.keepass.database.element.template.TemplateField.LABEL_CVV
import com.kunzisoft.keepass.database.element.template.TemplateField.LABEL_HOLDER
import com.kunzisoft.keepass.database.element.template.TemplateField.LABEL_NUMBER

object CreditCardEntryFields {

    const val CREDIT_CARD_TAG = "Credit Card"

    /**
     * Parse fields of an entry to retrieve a Passkey
     */
    fun parseFields(getField: (id: String) -> CharArray?): CreditCard? {
        val cardHolderField = getField(LABEL_HOLDER)
        val cardNumberField = getField(LABEL_NUMBER)
        val cardCVVField = getField(LABEL_CVV)
        if (cardHolderField == null
            || cardNumberField == null)
            return null
        return CreditCard(
            cardholder = String(cardHolderField),
            number = cardNumberField,
            cvv = cardCVVField
        )
    }

    fun EntryInfo.setCreditCard(creditCard: CreditCard?) {
        if (creditCard != null) {
            tags.put(CREDIT_CARD_TAG)
            creditCard.cardholder?.let {
                addOrReplaceField(
                    Field(
                        LABEL_HOLDER,
                        ProtectedString(enableProtection = false, it)
                    )
                )
            }
            creditCard.number?.let {
                addOrReplaceField(
                    Field(
                        LABEL_NUMBER,
                        ProtectedString(enableProtection = false, it)
                    )
                )
            }
            creditCard.cvv?.let {
                addOrReplaceField(
                    Field(
                        LABEL_CVV,
                        ProtectedString(enableProtection = true, it)
                    )
                )
            }
        }
    }

    /**
     * Detect if the current field is a Credit Card field
     */
    fun Field.isCreditCard(): Boolean {
        return when(name) {
            CREDIT_CARD_TAG -> true
            LABEL_HOLDER -> true
            LABEL_NUMBER -> true
            LABEL_CVV -> true
            else -> false
        }
    }
}