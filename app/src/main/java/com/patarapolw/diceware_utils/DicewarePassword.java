package com.patarapolw.diceware_utils;

import android.content.Context;
import android.support.annotation.NonNull;

import java.security.SecureRandom;

/**
 * Created by patarapolw on 5/6/18.
 * Password Generator based on https://github.com/patarapolw/diceware_utils
 * that is, a diceware-passphrase generator, but the length of the passphrase
 * is truncated to 10 - 20 character, while inserting some symbols.
 */

public class DicewarePassword {
    private String password = "";
    private String[] keywordList;

    private SecureRandom random = new SecureRandom();
    private Policy policy;
    private KeywordListLoader keywordListLoader;

    public DicewarePassword(Context context){
        policy = new Policy(context);
        keywordListLoader = new KeywordListLoader(context);
    }

    public String getPassword(){
        return password;
    }

    public String[] getKeywordList(){
        return keywordList;
    }

    public void setPolicy(Policy policy1){
        policy = policy1;
    }

    public void generateModifiedDicewarePassword(int numberOfKeywords) {
        String[] keywordResource = new String[numberOfKeywords];
        for(int i=0; i<numberOfKeywords; i++){
            keywordResource[i] = keywordListLoader.getKeyword();
        }
        keywordList = keywordResource.clone();

        String prePassword;

        keywordResource = title_case_all(keywordResource);
        keywordResource = policy.insert_number(keywordResource, policy.getDigit_count());
        for(int i=0; i<policy.getPunctuation_count(); i++){
            keywordResource = policy.insert_symbol_one(keywordResource);
        }

        prePassword = toPassword(keywordResource);
        while (prePassword.length() > policy.getLength_max()) {
            keywordResource = shorten_one(keywordResource);
            prePassword = toPassword(keywordResource);
        }

        password = prePassword;
    }

    public void generateTrueDicewarePassword(int numberOfKeywords) {
        String[] keywordResource = new String[numberOfKeywords];
        for(int i=0; i<numberOfKeywords; i++){
            keywordResource[i] = keywordListLoader.getKeyword();
        }
        keywordList = keywordResource.clone();

        keywordResource = title_case_all(keywordResource);
        password = toPassword(keywordResource);
    }

    @NonNull
    private String toPassword(String[] keywordList){
        StringBuilder builder = new StringBuilder();

        for (String s: keywordList){
            builder.append(s);
        }

        return builder.toString();
    }

    private String[] shorten_one(String[] listOfKeywords){
        int maxLength = 3;
        int minLength = 2;
        int length = random.nextInt(maxLength - minLength) + minLength;
        while (true) {
            int index = random.nextInt(listOfKeywords.length);
            for (int i = 0; i < listOfKeywords.length; i++) {
                if (i == index) {
                    if (listOfKeywords[i].length() >= length) {
                        listOfKeywords[i] = listOfKeywords[i].substring(0, length);
                        return listOfKeywords;
                    }
                }
            }
        }
    }

    private String[] title_case_all(String[] listOfKeywords){
        String keyword;
        for(int i=0; i<listOfKeywords.length; i++){
            keyword = listOfKeywords[i];
            listOfKeywords[i] = Character.toUpperCase(keyword.charAt(0)) + keyword.substring(1);
        }
        return listOfKeywords;
    }
}
