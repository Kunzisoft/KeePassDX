/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.keepassdroid.autofill;

public final class W3cHints {

    // Supported W3C autofill tokens (https://html.spec.whatwg.org/multipage/forms.html#autofill)
    public static final String HONORIFIC_PREFIX = "honorific-prefix";
    public static final String NAME = "name";
    public static final String GIVEN_NAME = "given-name";
    public static final String ADDITIONAL_NAME = "additional-name";
    public static final String FAMILY_NAME = "family-name";
    public static final String HONORIFIC_SUFFIX = "honorific-suffix";
    public static final String USERNAME = "username";
    public static final String NEW_PASSWORD = "new-password";
    public static final String CURRENT_PASSWORD = "current-password";
    public static final String ORGANIZATION_TITLE = "organization-title";
    public static final String ORGANIZATION = "organization";
    public static final String STREET_ADDRESS = "street-address";
    public static final String ADDRESS_LINE1 = "address-line1";
    public static final String ADDRESS_LINE2 = "address-line2";
    public static final String ADDRESS_LINE3 = "address-line3";
    public static final String ADDRESS_LEVEL4 = "address-level4";
    public static final String ADDRESS_LEVEL3 = "address-level3";
    public static final String ADDRESS_LEVEL2 = "address-level2";
    public static final String ADDRESS_LEVEL1 = "address-level1";
    public static final String COUNTRY = "country";
    public static final String COUNTRY_NAME = "country-name";
    public static final String POSTAL_CODE = "postal-code";
    public static final String CC_NAME = "cc-name";
    public static final String CC_GIVEN_NAME = "cc-given-name";
    public static final String CC_ADDITIONAL_NAME = "cc-additional-name";
    public static final String CC_FAMILY_NAME = "cc-family-name";
    public static final String CC_NUMBER = "cc-number";
    public static final String CC_EXPIRATION = "cc-exp";
    public static final String CC_EXPIRATION_MONTH = "cc-exp-month";
    public static final String CC_EXPIRATION_YEAR = "cc-exp-year";
    public static final String CC_CSC = "cc-csc";
    public static final String CC_TYPE = "cc-type";
    public static final String TRANSACTION_CURRENCY = "transaction-currency";
    public static final String TRANSACTION_AMOUNT = "transaction-amount";
    public static final String LANGUAGE = "language";
    public static final String BDAY = "bday";
    public static final String BDAY_DAY = "bday-day";
    public static final String BDAY_MONTH = "bday-month";
    public static final String BDAY_YEAR = "bday-year";
    public static final String SEX = "sex";
    public static final String URL = "url";
    public static final String PHOTO = "photo";
    // Optional W3C prefixes
    public static final String PREFIX_SECTION = "section-";
    public static final String SHIPPING = "shipping";
    public static final String BILLING = "billing";
    // W3C prefixes below...
    public static final String PREFIX_HOME = "home";
    public static final String PREFIX_WORK = "work";
    public static final String PREFIX_FAX = "fax";
    public static final String PREFIX_PAGER = "pager";
    // ... require those suffix
    public static final String TEL = "tel";
    public static final String TEL_COUNTRY_CODE = "tel-country-code";
    public static final String TEL_NATIONAL = "tel-national";
    public static final String TEL_AREA_CODE = "tel-area-code";
    public static final String TEL_LOCAL = "tel-local";
    public static final String TEL_LOCAL_PREFIX = "tel-local-prefix";
    public static final String TEL_LOCAL_SUFFIX = "tel-local-suffix";
    public static final String TEL_EXTENSION = "tel_extension";
    public static final String EMAIL = "email";
    public static final String IMPP = "impp";

    private W3cHints() {
    }
}