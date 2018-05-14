package com.patarapolw.wordify;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MajorSystemPeg {
    private Map<String, String[]> majorSystem = new HashMap<>();

    private SecureRandom random = new SecureRandom();

    public MajorSystemPeg (Context context) {
        try {
            InputStream inputStream = context.getAssets().open("wordify/peg.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String[]>>(){}.getType();
            majorSystem = gson.fromJson(new String(buffer), type);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String[] toWords(String pin) {
        Log.d("Peg", pin);

        ArrayList<String> wordArray = new ArrayList<>();

        int passwordLength = pin.length();
        int passwordLength_2 = passwordLength/2;
        for(int i=0; i<passwordLength_2; i++){
            String subString = pin.substring(2*i, 2*i+2);
            Log.d("Peg", subString);

            String[] words = majorSystem.get(subString);
            wordArray.add(words[random.nextInt(words.length)]);
        }
        if(passwordLength%2 == 1){
            String subString = pin.substring(passwordLength - 1);
            Log.d("Peg", subString);

            String[] words = majorSystem.get(subString);
            wordArray.add(words[random.nextInt(words.length)]);
        }

        return wordArray.toArray(new String[0]);
    }
}
