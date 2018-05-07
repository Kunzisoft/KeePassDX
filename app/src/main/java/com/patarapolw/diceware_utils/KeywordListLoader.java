package com.patarapolw.diceware_utils;

import android.app.Activity;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * Created by patarapolw on 5/6/18.
 * Load KeywordList from file (eff-long.txt), and make it available for PIN mnemonics
 */

public class KeywordListLoader extends Activity {
    String[] allKeywords;
    String[][] allKeywordsNumber = new String[10][];
    SecureRandom random = new SecureRandom();

    public KeywordListLoader(){}

    public KeywordListLoader(Context context){
        byte[] buffer;
        try {
            InputStream is = context.getAssets().open("diceware_utils/eff-long.txt");
            int size = is.available();
            buffer = new byte[size];
            is.read(buffer);
            is.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
            buffer = null;
        }

        allKeywords = new String(buffer).trim().split("\n");

        String json;
        try {
            InputStream is = context.getAssets().open("diceware_utils/major_system.json");
            int size = is.available();
            buffer = new byte[size];
            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");
        }
        catch (IOException ex) {
            ex.printStackTrace();
            buffer = null;
            json = null;
        }

        String[][] majorSystemSubstitution = new String[10][];
        try {
            JSONObject majorSystem = new JSONObject(json);
            for(int i=0; i<majorSystemSubstitution.length; i++){
                majorSystemSubstitution[i] = new String[majorSystem.getJSONArray(Integer.toString(i)).length()];
                for(int x=0; x<majorSystemSubstitution[i].length; x++){
                    majorSystemSubstitution[i][x] = majorSystem.getJSONArray(Integer.toString(i)).getString(x);
                }
            }
        }
        catch (JSONException ex){
            ex.printStackTrace();
        }

        ArrayList<ArrayList<String>> allKeywordNumberArrayList = new ArrayList<ArrayList<String>>();
        for(int i=0; i<10; i++){
            allKeywordNumberArrayList.add(new ArrayList<String>());
        }

        for(String keyword: allKeywords){
            for(int i=0; i<10; i++){
                for(String substitution: majorSystemSubstitution[i]){
                    if(keyword.startsWith(substitution)){
                        allKeywordNumberArrayList.get(i).add(keyword);
                    }
                }
            }
        }
        for(int i=0; i<10; i++){
            allKeywordsNumber[i] = allKeywordNumberArrayList.get(i).toArray(new String[0]);
        }
    }

    public String getKeyword(){
        return allKeywords[random.nextInt(allKeywords.length)];
    }

    public String getKeywordForInt(int i){
        return allKeywordsNumber[i][random.nextInt(allKeywordsNumber[i].length)];
    }
}
