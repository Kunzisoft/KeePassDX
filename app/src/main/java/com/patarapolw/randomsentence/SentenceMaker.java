package com.patarapolw.randomsentence;

import android.content.Context;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SentenceMaker {
    private ArrayList<String> taggedSents = new ArrayList<>();
    private Map<String, String> taggedDictionary = new HashMap<>();

    private SecureRandom random = new SecureRandom();

    public SentenceMaker(Context context) {
        try {
            InputStream is = context.getAssets().open("randomsentence/sentences-tagged.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                taggedSents.add(line);
            }
            reader.close();
            is.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            InputStream is = context.getAssets().open("randomsentence/dictionary-tagged.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while((line = reader.readLine()) != null){
                String[] entry = line.split("\t");
                taggedDictionary.put(entry[0], entry[1]);
            }
            reader.close();
            is.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String makeSentence(String[] keywordList) {
        String[][] sampleSentence;
        String[] resultSentence;
        String[] tagged_token;

        Gson gson = new Gson();

        String[] keywordTags = getPOS(keywordList);
        long start = System.currentTimeMillis();
        int timeoutMillis = 3000;
        while (System.currentTimeMillis() - start < timeoutMillis) {
            int keywordTagsIndex = 0;

            sampleSentence = gson.fromJson(randomSentence(), String[][].class);
            resultSentence = new String[sampleSentence.length];

            for (int token_i = 0; token_i < sampleSentence.length; token_i++) {
                tagged_token = sampleSentence[token_i];
                if (keywordTagsIndex < keywordList.length &&
                        ((tagged_token[1].length() > 2 ? tagged_token[1].substring(0, 2) : tagged_token[1])
                                .equalsIgnoreCase(
                                        keywordTags[keywordTagsIndex].length() > 2 ? keywordTags[keywordTagsIndex].substring(0, 2) : keywordTags[keywordTagsIndex]))) {
                    resultSentence[token_i] = String.format("[%s]", keywordList[keywordTagsIndex]);
                    keywordTagsIndex++;
                } else {
                    resultSentence[token_i] = sampleSentence[token_i][0];
                }
            }

            if (keywordTagsIndex == keywordList.length) {
                Detokenizer detokenizer = new Detokenizer();
                return detokenizer.detokenize(resultSentence);
            }
        }

        return "";
    }

    private String randomSentence () {
        return taggedSents.get(random.nextInt(taggedSents.size()));
    }

    private String[] getPOS (String[] wordList) {
        String[] result = new String[wordList.length];

        for(int i=0; i<wordList.length; i++) {
            result[i] = taggedDictionary.get(wordList[i]);

            if(result[i] == null){
                result[i] = "NN";
            }
        }

        return result;
    }
}
