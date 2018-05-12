package com.patarapolw.wordify;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Wordify {
    private Map<String, Set<String>> desubstitutions = new HashMap<>();
    private Gibberish gibberish;

    private static final Set<String> NOT_CHARS = new HashSet<>(Arrays.asList("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~0123456789".split("(?!^)")));

    public Wordify (Context context) {
//        try {
//            InputStream inputStream = context.getAssets().open("wordify/leetspeak.json");
//            int size = inputStream.available();
//            byte[] buffer = new byte[size];
//            inputStream.read(buffer);
//            inputStream.close();
//
//            JSONObject leetspeak = new JSONObject(new String(buffer));
//
//            JSONObject leetspeakMin = leetspeak.getJSONObject("min");
//            Iterator<?> keys = leetspeakMin.keys();
//            while (keys.hasNext()){
//                String key = (String) keys.next();
//                JSONArray values = leetspeakMin.getJSONArray(key);
//                for(int i=0; i<values.length(); i++){
//                    String character = values.getString(i);
//                    if(NOT_CHARS.contains(character)){
//                        Set<String> desubContent = desubstitutions.get(character);
//                        if(desubContent == null){
//                            desubContent = new HashSet<>();
//                        }
//                        desubContent.add(key);
//                        desubstitutions.put(character, desubContent);
//                    }
//                }
//            }
//
//            JSONObject leetspeakReverse = leetspeak.getJSONObject("reverse");
//            keys = leetspeakReverse.keys();
//            while (keys.hasNext()){
//                String key = (String) keys.next();
//                String value = leetspeakReverse.getString(key);
//
//                if(NOT_CHARS.contains(key)){
//                    Set<String> desubContent = desubstitutions.get(key);
//                    if(desubContent == null){
//                        desubContent = new HashSet<>();
//                    }
//                    desubContent.add(value);
//                    desubstitutions.put(key, desubContent);
//                }
//            }
//        } catch (IOException | JSONException ex) {
//            ex.printStackTrace();
//        }

        try {
            InputStream inputStream = context.getAssets().open("wordify/mnemonic.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            JSONObject majorSystem = new JSONObject(new String(buffer)).getJSONObject("major_system");
            Iterator<?> keys = majorSystem.keys();
            while(keys.hasNext()){
                String key = (String) keys.next();

                if(NOT_CHARS.contains(key)){
                    JSONArray values = majorSystem.getJSONArray(key);
                    int valuesLength = values.length();
                    Set<String> desubContent = desubstitutions.get(key);
                    if(desubContent == null){
                        desubContent = new HashSet<>();
                    }
                    for(int i=0; i<valuesLength; i++) {
                        desubContent.add(values.getString(i));
                    }
                    desubstitutions.put(key, desubContent);
                }
            }
        } catch (IOException | JSONException ex) {
            ex.printStackTrace();
        }

        gibberish = new Gibberish(context);
    }

    public String[] wordify (String password) {
        ArrayList<String> wordList = new ArrayList<>();

        ArrayList<Set<String>> passwordArray = letterify(password);
        int passwordArrayLength = passwordArray.size();

        for(int start=0; start<passwordArrayLength/2; start++){
            String gibberishWord = gibberish.generateWord(passwordArray.get(2*start), passwordArray.get(2*start+1));
            if(gibberishWord != null){
                wordList.add(gibberishWord);
            }
        }

        if(passwordArrayLength%2 == 1){
            String gibberishWord = gibberish.generateWord(passwordArray.get(passwordArrayLength-1), new HashSet<String>());
            if(gibberishWord != null){
                wordList.add(gibberishWord);
            }
        }

        return wordList.toArray(new String[0]);
    }

    private ArrayList<Set<String>> letterify (String password) {
        ArrayList<Set<String>> passwordArray = new ArrayList<>();
        String[] passwordChars = password.toLowerCase().split("(?!^)");
        for(String passwordChar: passwordChars){
            if(NOT_CHARS.contains(passwordChar)){
                Set<String> desub = desubstitutions.get(passwordChar);
                if(!desub.contains(null)){
                    passwordArray.add(desub);
                }
            } else {
                Set<String> charSet = new HashSet<>();
                charSet.add(passwordChar);
                passwordArray.add(charSet);
            }
        }

        Log.d("Wordify", passwordArray.toString());
        return passwordArray;
    }
}
