package com.patarapolw.wordify;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Gibberish {
    private Set<String> initials;
    private Set<String> vowels;
    private Set<String> finals;

    private static final Set<String> ASCII_LOWERCASE = new HashSet<>(Arrays.asList("abcdefghijklmnopqrstuvwxyz".split("(?!^)")));
    private static final Set<String> AEIOU = new HashSet<>(Arrays.asList("aeiou".split("(?!^)")));

    private SecureRandom random = new SecureRandom();

    public Gibberish (Context context) {
        try {
            InputStream inputStream = context.getAssets().open("wordify/word_components.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            JsonElement root = new JsonParser().parse(new String(buffer));
            initials = componentLoader(root.getAsJsonObject().get("initials"));
            vowels = componentLoader(root.getAsJsonObject().get("vowels"));
            finals = componentLoader(root.getAsJsonObject().get("finals"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        initials.addAll(ASCII_LOWERCASE);
        initials.removeAll(AEIOU);

        finals.addAll(ASCII_LOWERCASE);
        finals.removeAll(AEIOU);
    }

    public String generateWord(Set<String> start, Set<String> end) {
        boolean startVowel = true;
        boolean endVowel = true;

        if(start.size() == 0){
            start = initials;
        } else {
            for(String item: ASCII_LOWERCASE){
                if(start.contains(item)){
                    startVowel = false;
                    break;
                }
            }
        }
        if(end.size() == 0){
            end = finals;
        } else {
            for(String item: ASCII_LOWERCASE){
                if(end.contains(item)){
                    endVowel = false;
                    break;
                }
            }
        }

        ArrayList<String[]> letterList = new ArrayList<>();
        if(startVowel){
            if(start.size() == 0){
                letterList.add(initials.toArray(new String[0]));
                letterList.add(vowels.toArray(new String[0]));
            } else {
                letterList.add(start.toArray(new String[0])); // Vowels must be followed by a consonant.
            }
            letterList.add(finals.toArray(new String[0]));
        } else {
            // if startVowel is false, start is not empty.
            letterList.add(start.toArray(new String[0]));
        }
        if(endVowel) {
            if(startVowel){
                // If both startVowel and endVowel, consider making the word longer.
                letterList.add(initials.toArray(new String[0]));
                letterList.add(vowels.toArray(new String[0]));

            }
            if(end.size() != 0){
                letterList.add(end.toArray(new String[0]));
            }
        } else {
            letterList.add(vowels.toArray(new String[0]));
            letterList.add(end.toArray(new String[0]));
        }

        StringBuilder passwordBuilder = new StringBuilder();
        for(String[] letters: letterList){
            passwordBuilder.append(letters[random.nextInt(letters.length)]);
        }

        return passwordBuilder.toString();
    }

    private Set<String> componentLoader(JsonElement component) {
        Gson gson = new Gson();
        String[][] preComponent = gson.fromJson(component, String[][].class);

        Set<String> componentArray = new HashSet<>();
        for(String[] category: preComponent){
            Collections.addAll(componentArray, category);
        }

        return componentArray;
    }
}
