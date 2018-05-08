package com.patarapolw.randomsentence;

import android.app.Activity;
import android.content.Context;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class POSTaggerPreExported extends Activity {
    private String[][] taggedDictionary;

    public POSTaggerPreExported() {}

    public POSTaggerPreExported(Context context) {
        try {
            InputStream is = context.getAssets().open("randomsentence/taggedDictionary.ser");
            ObjectInputStream objectInputStream = new ObjectInputStream(is);
            taggedDictionary = (String[][]) objectInputStream.readObject();
            is.close();
        }
        catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public String[] tag (String[] keywordList) {
        ArrayList<String> taggedArrayList = new ArrayList<>();

        for(String keyword: keywordList){
            boolean added = false;

            for(String[] taggedEntry: taggedDictionary){
                if(taggedEntry[0].equalsIgnoreCase(keyword)){
                    taggedArrayList.add(taggedEntry[1]);
                    added = true;
                    break;
                }
            }
            if(!added){
                taggedArrayList.add("XX");
            }
        }

        return taggedArrayList.toArray(new String[0]);
    }
}
