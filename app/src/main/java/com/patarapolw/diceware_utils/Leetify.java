package com.patarapolw.diceware_utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;

/**
 * Created by patarapolw on 5/6/18.
 * Based on minimal leet substitutions, not really to make password more secure,
 * but simply to conform to password policy.
 */

public class Leetify extends Activity {
    JSONObject leet;

    public Leetify(){}

    public Leetify(Context context){
        String json = null;

        try {
            InputStream is = context.getAssets().open("leetspeak.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            leet = new JSONObject(json);
        }
        catch (JSONException ex){
            ex.printStackTrace();
        }
    }

    public String doLeetify(String word){
        SecureRandom random = new SecureRandom();

        StringBuilder builder = new StringBuilder();
        int charIndex = random.nextInt(word.length());

        for(int i=0; i<word.length(); i++){
            if(i != charIndex) {
                builder.append(word.charAt(i));
            } else {
                String character = "";
                character += Character.toLowerCase(word.charAt(i));
                Log.d("memorable_password", "character: " + character);
                try {
                    JSONArray leetJSONArray = leet.getJSONArray(character);
                    String[] leetChoice = new String[leetJSONArray.length()];
                    for(int x=0; x<leetJSONArray.length(); x++){
                        leetChoice[x] = leetJSONArray.getString(x);
                    }
                    builder.append(leetChoice[random.nextInt(leetChoice.length)]);
                } catch (JSONException ex) {
                    builder.append(word.charAt(i));
                }
            }
        }

        return builder.toString();
    }
}
