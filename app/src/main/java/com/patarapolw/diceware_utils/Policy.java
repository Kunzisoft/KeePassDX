package com.patarapolw.diceware_utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * Created by patarapolw on 5/6/18.
 * Policy conformization of the password.
 */

public class Policy {
    private int digit_count;
    private int punctuation_count;
    private int length_min;
    private int length_max;

    private SecureRandom random = new SecureRandom();
    private Leetify leetify;
    private static final String punctuations = "!\"#$%&\\'()*+,-./:;<=>?@[\\\\]^_`{|}~";
    private static final String numbers = "0123456789";

    public Policy(Context context) {
        leetify = new Leetify(context);

        String json = null;
        try {
            InputStream is = context.getAssets().open("diceware_utils/policy.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            JSONObject policy = new JSONObject(json);
            digit_count = policy.getInt("digit_count");
            punctuation_count = policy.getInt("punctuation_count");
            length_min = policy.getJSONObject("length").getInt("min");
            length_max = policy.getJSONObject("length").getInt("max");
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    public void setDigit_count(int digit_count1){
        digit_count = digit_count1;
    }

    public void setPunctuation_count(int punctuation_count1){
        punctuation_count = punctuation_count1;
    }

    public void setLength_min(int length_min1){
        length_min = length_min1;
    }

    public void setLength_max(int length_max1){
        length_max = length_max1;
    }

    public String[] insert_symbol_one(String[] listOfKeywords){
        ArrayList<String> result = new ArrayList<>();
        int index = random.nextInt(listOfKeywords.length + 1);

        for(int i=0; i<listOfKeywords.length; i++){
            result.add(listOfKeywords[i]);
            if(i == index){
                int punctuationIndex = random.nextInt(punctuations.length());
                result.add(Character.toString(punctuations.charAt(punctuationIndex)));
            }
        }

        if(index > listOfKeywords.length){
            int punctuationIndex = random.nextInt(punctuations.length());
            result.add(Character.toString(punctuations.charAt(punctuationIndex)));
        }

        return result.toArray(new String[0]);
    }

    public boolean isConform(String[] keyword){
        String password = toPassword(keyword);
        char[] passwordArray = password.toCharArray();

        boolean haveLower = false;
        boolean haveUpper = false;
        int actualDigitCount = 0;
        int actualPunctuationCount = 0;

        for(char character: passwordArray){
            if(Character.isLowerCase(character)){
                haveLower = true;
            } else if(Character.isUpperCase(character)){
                haveUpper = true;
            }
            if(Character.isDigit(character)){
                actualDigitCount++;
            }
            if(punctuations.indexOf(character) != -1){
                actualPunctuationCount++;
            }
        }

        if(!haveLower | !haveUpper){
            return false;
        }
        if(actualDigitCount < digit_count){
            return false;
        }
        return actualPunctuationCount >= punctuation_count;
    }

    public int getLength_max(){
        return length_max;
    }

    public String[] conformize(String[] listOfKeywords){
        int numberOfPossibleChoices = 4;
        int index = random.nextInt(numberOfPossibleChoices);
        switch (index) {
            case 0:
                return leetify_one(listOfKeywords);
            case 1:
                return insert_symbol_one(listOfKeywords);
            case 2:
                return insert_number_one(listOfKeywords);
            case 3:
                return switch_case_one(listOfKeywords);
        }
        return null;
    }

    private String[] leetify_one(String[] listOfKeywords){
        int index = random.nextInt(listOfKeywords.length);
        listOfKeywords[index] = leetify.doLeetify(listOfKeywords[index]);

        return listOfKeywords;
    }

    @NonNull
    private String[] insert_number_one(String[] listOfKeywords){
        ArrayList<String> result = new ArrayList<>();
        int index = random.nextInt(listOfKeywords.length + 1);

        for(int i=0; i<listOfKeywords.length; i++){
            result.add(listOfKeywords[i]);
            if(i == index){
                int numberIndex = random.nextInt(numbers.length());
                result.add(Character.toString(numbers.charAt(numberIndex)));
            }
        }

        if(index > listOfKeywords.length){
            int numberIndex = random.nextInt(numbers.length());
            result.add(Character.toString(numbers.charAt(numberIndex)));
        }

        return result.toArray(new String[0]);
    }

    private String[] switch_case_one(String[] listOfKeywords){
        int wordIndex = random.nextInt(listOfKeywords.length);
        Log.d("memorable_pasword", "wordIndex: " + wordIndex);
        int charIndex = random.nextInt(listOfKeywords[wordIndex].length());
        Log.d("memorable_password", "charIndex: " + charIndex);
        System.out.print(charIndex);
        StringBuilder newWord = new StringBuilder();

        for(int i=0; i<listOfKeywords[wordIndex].length(); i++){
            if(i != charIndex) {
                newWord.append(listOfKeywords[wordIndex].charAt(i));
            } else {
                char character = listOfKeywords[wordIndex].charAt(i);
                char newCharacter;
                if(Character.isUpperCase(character)){
                    newCharacter = Character.toLowerCase(character);
                } else if(Character.isLowerCase(character)){
                    newCharacter = Character.toUpperCase(character);
                } else {
                    newCharacter = character;
                }
                newWord.append(newCharacter);
            }
        }

        listOfKeywords[wordIndex] = newWord.toString();
        return listOfKeywords;
    }

    @NonNull
    private String toPassword(String[] keywordList){
        StringBuilder builder = new StringBuilder();

        for (String s: keywordList){
            builder.append(s);
        }

        return builder.toString();
    }
}
