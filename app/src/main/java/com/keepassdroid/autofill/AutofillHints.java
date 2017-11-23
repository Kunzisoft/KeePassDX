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

import android.os.Build;
import android.service.autofill.SaveInfo;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;

import com.google.common.collect.ImmutableMap;
import com.keepassdroid.model.FilledAutofillField;
import com.keepassdroid.model.FilledAutofillFieldCollection;

import java.util.Calendar;

@RequiresApi(api = Build.VERSION_CODES.O)
public final class AutofillHints {
    public static final int PARTITION_OTHER = 0;
    public static final int PARTITION_ADDRESS = 1;
    public static final int PARTITION_EMAIL = 2;
    public static final int PARTITION_CREDIT_CARD = 3;
    public static final int[] PARTITIONS = {
            PARTITION_OTHER, PARTITION_ADDRESS, PARTITION_EMAIL, PARTITION_CREDIT_CARD
    };
    /* TODO: finish building fake data for all hints. */
    private static final ImmutableMap<String, AutofillHintProperties> sValidHints =
            new ImmutableMap.Builder<String, AutofillHintProperties>()
                    .put(View.AUTOFILL_HINT_EMAIL_ADDRESS, new AutofillHintProperties(
                            View.AUTOFILL_HINT_EMAIL_ADDRESS, SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS,
                            PARTITION_EMAIL,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(View.AUTOFILL_HINT_EMAIL_ADDRESS);
                                filledAutofillField.setTextValue("email" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(View.AUTOFILL_HINT_NAME, new AutofillHintProperties(
                            View.AUTOFILL_HINT_NAME, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(View.AUTOFILL_HINT_NAME);
                                filledAutofillField.setTextValue("name" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(View.AUTOFILL_HINT_USERNAME, new AutofillHintProperties(
                            View.AUTOFILL_HINT_USERNAME, SaveInfo.SAVE_DATA_TYPE_USERNAME,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(View.AUTOFILL_HINT_USERNAME);
                                filledAutofillField.setTextValue("login" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(View.AUTOFILL_HINT_PASSWORD, new AutofillHintProperties(
                            View.AUTOFILL_HINT_PASSWORD, SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(View.AUTOFILL_HINT_PASSWORD);
                                filledAutofillField.setTextValue("login" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(View.AUTOFILL_HINT_PHONE, new AutofillHintProperties(
                            View.AUTOFILL_HINT_PHONE, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(View.AUTOFILL_HINT_PHONE);
                                filledAutofillField.setTextValue("" + seed + "2345678910");
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(View.AUTOFILL_HINT_POSTAL_ADDRESS, new AutofillHintProperties(
                            View.AUTOFILL_HINT_POSTAL_ADDRESS, SaveInfo.SAVE_DATA_TYPE_ADDRESS,
                            PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(View.AUTOFILL_HINT_POSTAL_ADDRESS);
                                filledAutofillField.setTextValue(
                                        "" + seed + " Fake Ln, Fake, FA, FAA 10001");
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(View.AUTOFILL_HINT_POSTAL_CODE, new AutofillHintProperties(
                            View.AUTOFILL_HINT_POSTAL_CODE, SaveInfo.SAVE_DATA_TYPE_ADDRESS,
                            PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(View.AUTOFILL_HINT_POSTAL_CODE);
                                filledAutofillField.setTextValue("1000" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER, new AutofillHintProperties(
                            View.AUTOFILL_HINT_CREDIT_CARD_NUMBER,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD,
                            PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField = new FilledAutofillField(
                                        View.AUTOFILL_HINT_CREDIT_CARD_NUMBER);
                                filledAutofillField.setTextValue("" + seed + "234567");
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE, new AutofillHintProperties(
                            View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD,
                            PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField = new FilledAutofillField(
                                        View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE);
                                filledAutofillField.setTextValue("" + seed + seed + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE,
                            new AutofillHintProperties(
                                    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE,
                                    SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD, PARTITION_CREDIT_CARD,
                                    (seed) -> {
                                        FilledAutofillField filledAutofillField = new FilledAutofillField(
                                                View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE);
                                        Calendar calendar = Calendar.getInstance();
                                        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + seed);
                                        filledAutofillField.setDateValue(calendar.getTimeInMillis());
                                        return filledAutofillField;
                                    }, View.AUTOFILL_TYPE_DATE))
                    .put(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH,
                            new AutofillHintProperties(
                                    View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH,
                                    SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD, PARTITION_CREDIT_CARD,
                                    (seed) -> {
                                        CharSequence[] months = monthRange();
                                        int month = seed % months.length;
                                        Calendar calendar = Calendar.getInstance();
                                        calendar.set(Calendar.MONTH, month);
                                        FilledAutofillField filledAutofillField =
                                                new FilledAutofillField(
                                                        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH);
                                        filledAutofillField.setListValue(months, month);
                                        filledAutofillField.setTextValue(Integer.toString(month));
                                        filledAutofillField.setDateValue(calendar.getTimeInMillis());
                                        return filledAutofillField;
                                    }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST,
                                    View.AUTOFILL_TYPE_DATE))
                    .put(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR, new AutofillHintProperties(
                            View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD, PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField = new FilledAutofillField(
                                        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR);
                                Calendar calendar = Calendar.getInstance();
                                int expYear = calendar.get(Calendar.YEAR) + seed;
                                calendar.set(Calendar.YEAR, expYear);
                                filledAutofillField.setDateValue(calendar.getTimeInMillis());
                                filledAutofillField.setTextValue(Integer.toString(expYear));
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST,
                            View.AUTOFILL_TYPE_DATE))
                    .put(View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY, new AutofillHintProperties(
                            View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD, PARTITION_CREDIT_CARD,
                            (seed) -> {
                                CharSequence[] days = dayRange();
                                int day = seed % days.length;
                                FilledAutofillField filledAutofillField = new FilledAutofillField(
                                        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY);
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(Calendar.DATE, day);
                                filledAutofillField.setListValue(days, day);
                                filledAutofillField.setTextValue(Integer.toString(day));
                                filledAutofillField.setDateValue(calendar.getTimeInMillis());
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST,
                            View.AUTOFILL_TYPE_DATE))
                    .put(W3cHints.HONORIFIC_PREFIX, new AutofillHintProperties(
                            W3cHints.HONORIFIC_PREFIX, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField = new FilledAutofillField(
                                        W3cHints.HONORIFIC_PREFIX);
                                CharSequence[] examplePrefixes = {"Miss", "Ms.", "Mr.", "Mx.",
                                        "Sr.", "Dr.", "Lady", "Lord"};
                                filledAutofillField.setListValue(examplePrefixes,
                                        seed % examplePrefixes.length);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.GIVEN_NAME, new AutofillHintProperties(W3cHints.GIVEN_NAME,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.GIVEN_NAME);
                                filledAutofillField.setTextValue("name" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.ADDITIONAL_NAME, new AutofillHintProperties(
                            W3cHints.ADDITIONAL_NAME, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.ADDITIONAL_NAME);
                                filledAutofillField.setTextValue("addtlname" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.FAMILY_NAME, new AutofillHintProperties(
                            W3cHints.FAMILY_NAME, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.FAMILY_NAME);
                                filledAutofillField.setTextValue("famname" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.HONORIFIC_SUFFIX, new AutofillHintProperties(
                            W3cHints.HONORIFIC_SUFFIX, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.HONORIFIC_SUFFIX);
                                CharSequence[] exampleSuffixes = {"san", "kun", "chan", "sama"};
                                filledAutofillField.setListValue(exampleSuffixes,
                                        seed % exampleSuffixes.length);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.NEW_PASSWORD, new AutofillHintProperties(
                            W3cHints.NEW_PASSWORD, SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.NEW_PASSWORD);
                                filledAutofillField.setTextValue("login" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.CURRENT_PASSWORD, new AutofillHintProperties(
                            View.AUTOFILL_HINT_PASSWORD, SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(View.AUTOFILL_HINT_PASSWORD);
                                filledAutofillField.setTextValue("login" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.ORGANIZATION_TITLE, new AutofillHintProperties(
                            W3cHints.ORGANIZATION_TITLE, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.ORGANIZATION_TITLE);
                                filledAutofillField.setTextValue("org" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.ORGANIZATION, new AutofillHintProperties(W3cHints.ORGANIZATION,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.ORGANIZATION);
                                filledAutofillField.setTextValue("org" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.STREET_ADDRESS, new AutofillHintProperties(
                            W3cHints.STREET_ADDRESS, SaveInfo.SAVE_DATA_TYPE_ADDRESS,
                            PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.STREET_ADDRESS);
                                filledAutofillField.setTextValue(
                                        "" + seed + " Fake Ln, Fake, FA, FAA 10001");
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.ADDRESS_LINE1, new AutofillHintProperties(W3cHints.ADDRESS_LINE1,
                            SaveInfo.SAVE_DATA_TYPE_ADDRESS,
                            PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.ADDRESS_LINE1);
                                filledAutofillField.setTextValue("" + seed + " Fake Ln");
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.ADDRESS_LINE2, new AutofillHintProperties(W3cHints.ADDRESS_LINE2,
                            SaveInfo.SAVE_DATA_TYPE_ADDRESS, PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.ADDRESS_LINE2);
                                filledAutofillField.setTextValue("Apt. " + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.ADDRESS_LINE3, new AutofillHintProperties(W3cHints.ADDRESS_LINE3,
                            SaveInfo.SAVE_DATA_TYPE_ADDRESS, PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.ADDRESS_LINE3);
                                filledAutofillField.setTextValue("FA" + seed + ", FA, FAA");
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.ADDRESS_LEVEL4, new AutofillHintProperties(
                            W3cHints.ADDRESS_LEVEL4, SaveInfo.SAVE_DATA_TYPE_ADDRESS,
                            PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.ADDRESS_LEVEL4);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.ADDRESS_LEVEL3, new AutofillHintProperties(
                            W3cHints.ADDRESS_LEVEL3, SaveInfo.SAVE_DATA_TYPE_ADDRESS,
                            PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.ADDRESS_LEVEL3);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.ADDRESS_LEVEL2, new AutofillHintProperties(
                            W3cHints.ADDRESS_LEVEL2, SaveInfo.SAVE_DATA_TYPE_ADDRESS,
                            PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.ADDRESS_LEVEL2);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.ADDRESS_LEVEL1, new AutofillHintProperties(
                            W3cHints.ADDRESS_LEVEL1, SaveInfo.SAVE_DATA_TYPE_ADDRESS,
                            PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.ADDRESS_LEVEL1);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.COUNTRY, new AutofillHintProperties(W3cHints.COUNTRY,
                            SaveInfo.SAVE_DATA_TYPE_ADDRESS, PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.COUNTRY);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.COUNTRY_NAME, new AutofillHintProperties(W3cHints.COUNTRY_NAME,
                            SaveInfo.SAVE_DATA_TYPE_ADDRESS, PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.COUNTRY_NAME);
                                CharSequence[] exampleCountries = {"USA", "Mexico", "Canada"};
                                filledAutofillField.setListValue(exampleCountries,
                                        seed % exampleCountries.length);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.POSTAL_CODE, new AutofillHintProperties(
                            View.AUTOFILL_HINT_POSTAL_CODE, SaveInfo.SAVE_DATA_TYPE_ADDRESS,
                            PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(View.AUTOFILL_HINT_POSTAL_CODE);
                                filledAutofillField.setTextValue("" + seed + seed + seed + seed +
                                        seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.CC_NAME, new AutofillHintProperties(W3cHints.CC_NAME,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD,
                            PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.CC_NAME);
                                filledAutofillField.setTextValue("firstname" + seed + "lastname" +
                                        seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.CC_GIVEN_NAME, new AutofillHintProperties(W3cHints.CC_GIVEN_NAME,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD, PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.CC_GIVEN_NAME);
                                filledAutofillField.setTextValue("givenname" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.CC_ADDITIONAL_NAME, new AutofillHintProperties(
                            W3cHints.CC_ADDITIONAL_NAME, SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD,
                            PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.CC_ADDITIONAL_NAME);
                                filledAutofillField.setTextValue("addtlname" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.CC_FAMILY_NAME, new AutofillHintProperties(
                            W3cHints.CC_FAMILY_NAME, SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD,
                            PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.CC_FAMILY_NAME);
                                filledAutofillField.setTextValue("familyname" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.CC_NUMBER, new AutofillHintProperties(
                            View.AUTOFILL_HINT_CREDIT_CARD_NUMBER,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD, PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField = new FilledAutofillField(
                                        View.AUTOFILL_HINT_CREDIT_CARD_NUMBER);
                                filledAutofillField.setTextValue("" + seed + "234567");
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.CC_EXPIRATION, new AutofillHintProperties(
                            View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD, PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField = new FilledAutofillField(
                                        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE);
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + seed);
                                filledAutofillField.setDateValue(calendar.getTimeInMillis());
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_DATE))
                    .put(W3cHints.CC_EXPIRATION_MONTH, new AutofillHintProperties(
                            View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD, PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField = new FilledAutofillField(
                                        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH);
                                CharSequence[] months = monthRange();
                                filledAutofillField.setListValue(months,
                                        seed % months.length);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.CC_EXPIRATION_YEAR, new AutofillHintProperties(
                            View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD, PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField = new FilledAutofillField(
                                        View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR);
                                Calendar calendar = Calendar.getInstance();
                                int expYear = calendar.get(Calendar.YEAR) + seed;
                                calendar.set(Calendar.YEAR, expYear);
                                filledAutofillField.setDateValue(calendar.getTimeInMillis());
                                filledAutofillField.setTextValue("" + expYear);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.CC_CSC, new AutofillHintProperties(
                            View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD, PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField = new FilledAutofillField(
                                        View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE);
                                filledAutofillField.setTextValue("" + seed + seed + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.CC_TYPE, new AutofillHintProperties(W3cHints.CC_TYPE,
                            SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD, PARTITION_CREDIT_CARD,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.CC_TYPE);
                                filledAutofillField.setTextValue("type" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.TRANSACTION_CURRENCY, new AutofillHintProperties(
                            W3cHints.TRANSACTION_CURRENCY, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.TRANSACTION_CURRENCY);
                                CharSequence[] exampleCurrencies = {"USD", "CAD", "KYD", "CRC"};
                                filledAutofillField.setListValue(exampleCurrencies,
                                        seed % exampleCurrencies.length);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.TRANSACTION_AMOUNT, new AutofillHintProperties(
                            W3cHints.TRANSACTION_AMOUNT, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.TRANSACTION_AMOUNT);
                                filledAutofillField.setTextValue("" + seed * 100);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.LANGUAGE, new AutofillHintProperties(W3cHints.LANGUAGE,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.LANGUAGE);
                                CharSequence[] exampleLanguages = {"Bulgarian", "Croatian", "Czech",
                                        "Danish", "Dutch", "English", "Estonian"};
                                filledAutofillField.setListValue(exampleLanguages,
                                        seed % exampleLanguages.length);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.BDAY, new AutofillHintProperties(W3cHints.BDAY,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.BDAY);
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - seed * 10);
                                calendar.set(Calendar.MONTH, seed % 12);
                                calendar.set(Calendar.DATE, seed % 27);
                                filledAutofillField.setDateValue(calendar.getTimeInMillis());
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_DATE))
                    .put(W3cHints.BDAY_DAY, new AutofillHintProperties(W3cHints.BDAY_DAY,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.BDAY_DAY);
                                filledAutofillField.setTextValue("" + seed % 27);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.BDAY_MONTH, new AutofillHintProperties(W3cHints.BDAY_MONTH,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.BDAY_MONTH);
                                filledAutofillField.setTextValue("" + seed % 12);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.BDAY_YEAR, new AutofillHintProperties(W3cHints.BDAY_YEAR,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.BDAY_YEAR);
                                int year = Calendar.getInstance().get(Calendar.YEAR) - seed * 10;
                                filledAutofillField.setTextValue("" + year);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.SEX, new AutofillHintProperties(W3cHints.SEX,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.SEX);
                                filledAutofillField.setTextValue("Other");
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.URL, new AutofillHintProperties(W3cHints.URL,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.URL);
                                filledAutofillField.setTextValue("http://google.com");
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.PHOTO, new AutofillHintProperties(W3cHints.PHOTO,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.PHOTO);
                                filledAutofillField.setTextValue("photo" + seed + ".jpg");
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.PREFIX_SECTION, new AutofillHintProperties(
                            W3cHints.PREFIX_SECTION, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.PREFIX_SECTION);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.SHIPPING, new AutofillHintProperties(W3cHints.SHIPPING,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.SHIPPING);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.BILLING, new AutofillHintProperties(W3cHints.BILLING,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_ADDRESS,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.BILLING);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.PREFIX_HOME, new AutofillHintProperties(W3cHints.PREFIX_HOME,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.PREFIX_HOME);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.PREFIX_WORK, new AutofillHintProperties(W3cHints.PREFIX_WORK,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.PREFIX_WORK);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.PREFIX_FAX, new AutofillHintProperties(W3cHints.PREFIX_FAX,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.PREFIX_FAX);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.PREFIX_PAGER, new AutofillHintProperties(W3cHints.PREFIX_PAGER,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.PREFIX_PAGER);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.TEL, new AutofillHintProperties(W3cHints.TEL,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.TEL);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.TEL_COUNTRY_CODE, new AutofillHintProperties(
                            W3cHints.TEL_COUNTRY_CODE, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.TEL_COUNTRY_CODE);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.TEL_NATIONAL, new AutofillHintProperties(W3cHints.TEL_NATIONAL,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.TEL_NATIONAL);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.TEL_AREA_CODE, new AutofillHintProperties(
                            W3cHints.TEL_AREA_CODE, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.TEL_AREA_CODE);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.TEL_LOCAL, new AutofillHintProperties(
                            W3cHints.TEL_LOCAL, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.TEL_LOCAL);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.TEL_LOCAL_PREFIX, new AutofillHintProperties(
                            W3cHints.TEL_LOCAL_PREFIX, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.TEL_LOCAL_PREFIX);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.TEL_LOCAL_SUFFIX, new AutofillHintProperties(
                            W3cHints.TEL_LOCAL_SUFFIX, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.TEL_LOCAL_SUFFIX);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.TEL_EXTENSION, new AutofillHintProperties(W3cHints.TEL_EXTENSION,
                            SaveInfo.SAVE_DATA_TYPE_GENERIC, PARTITION_OTHER,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.TEL_EXTENSION);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .put(W3cHints.EMAIL, new AutofillHintProperties(
                            View.AUTOFILL_HINT_EMAIL_ADDRESS, SaveInfo.SAVE_DATA_TYPE_GENERIC,
                            PARTITION_EMAIL,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(View.AUTOFILL_HINT_EMAIL_ADDRESS);
                                filledAutofillField.setTextValue("email" + seed);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT))
                    .put(W3cHints.IMPP, new AutofillHintProperties(W3cHints.IMPP,
                            SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS, PARTITION_EMAIL,
                            (seed) -> {
                                FilledAutofillField filledAutofillField =
                                        new FilledAutofillField(W3cHints.IMPP);
                                return filledAutofillField;
                            }, View.AUTOFILL_TYPE_TEXT, View.AUTOFILL_TYPE_LIST))
                    .build();

    private AutofillHints() {
    }

    public static boolean isValidTypeForHints(String[] hints, int type) {
        if (hints != null) {
            for (String hint : hints) {
                if (hint != null && sValidHints.containsKey(hint)) {
                    boolean valid = sValidHints.get(hint).isValidType(type);
                    if (valid) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isValidHint(String hint) {
        return sValidHints.containsKey(hint);
    }

    public static int getSaveTypeForHints(String[] hints) {
        int saveType = 0;
        if (hints != null) {
            for (String hint : hints) {
                if (hint != null && sValidHints.containsKey(hint)) {
                    saveType |= sValidHints.get(hint).getSaveType();
                }
            }
        }
        return saveType;
    }

    public static FilledAutofillField getFakeField(String hint, int seed) {
        return sValidHints.get(hint).generateFakeField(seed);
    }

    public static FilledAutofillFieldCollection getFakeFieldCollection(int partition, int seed) {
        FilledAutofillFieldCollection filledAutofillFieldCollection =
                new FilledAutofillFieldCollection();
        for (String hint : sValidHints.keySet()) {
            if (hint != null && sValidHints.get(hint).getPartition() == partition) {
                FilledAutofillField fakeField = getFakeField(hint, seed);
                filledAutofillFieldCollection.add(fakeField);
            }
        }
        return filledAutofillFieldCollection;
    }

    private static String getStoredHintName(String hint) {
        return sValidHints.get(hint).getAutofillHint();
    }

    public static void convertToStoredHintNames(String[] hints) {
        for (int i = 0; i < hints.length; i++) {
            hints[i] = getStoredHintName(hints[i]);
        }
    }

    private static CharSequence[] dayRange() {
        CharSequence[] days = new CharSequence[27];
        for (int i = 0; i < days.length; i++) {
            days[i] = Integer.toString(i);
        }
        return days;
    }

    private static CharSequence[] monthRange() {
        CharSequence[] months = new CharSequence[12];
        for (int i = 0; i < months.length; i++) {
            months[i] = Integer.toString(i);
        }
        return months;
    }

    public static String[] filterForSupportedHints(String[] hints) {
        String[] filteredHints = new String[hints.length];
        int i = 0;
        for (String hint : hints) {
            if (AutofillHints.isValidHint(hint)) {
                filteredHints[i++] = hint;
            } else {
                Log.w(AutofillHints.class.getName(), "Invalid autofill hint: " + hint);
            }
        }
        if (i == 0) {
            return null;
        }
        String[] finalFilteredHints = new String[i];
        System.arraycopy(filteredHints, 0, finalFilteredHints, 0, i);
        return finalFilteredHints;
    }
}
