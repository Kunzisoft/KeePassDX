package com.patarapolw.randomsentence;

import android.app.Activity;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.security.SecureRandom;

public class SentenceMaker extends Activity {
    private String[][][] taggedSents;
    private POSTaggerPreExported posTagger;

    private SecureRandom random = new SecureRandom();

    public SentenceMaker() {}

    public SentenceMaker(Context context) {
        try {
            InputStream is = context.getAssets().open("randomsentence/brownTaggedSents.ser");
            ObjectInputStream objectInputStream = new ObjectInputStream(is);
            taggedSents = (String[][][]) objectInputStream.readObject();
            is.close();
        }
        catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        posTagger = new POSTaggerPreExported(context);
    }

    public String makeSentence(String[] keywordList) {
        String[][] sampleSentence;
        String[] resultSentence;
        String[] tagged_token;

        String[] keywordTags = posTagger.tag(keywordList);
        long start = System.currentTimeMillis();
        int timeoutMillis = 3000;
        while(System.currentTimeMillis() - start < timeoutMillis){
            int keywordTagsIndex = 0;

            sampleSentence = taggedSents[random.nextInt(taggedSents.length)];
            resultSentence = new String[sampleSentence.length];

            for(int token_i=0; token_i < sampleSentence.length; token_i++){
                tagged_token = sampleSentence[token_i];
                if(keywordTagsIndex < keywordList.length &&
                        ((tagged_token[1].length() > 2 ? tagged_token[1].substring(0, 2) : tagged_token[1])
                                .equalsIgnoreCase(
                                        keywordTags[keywordTagsIndex].length() > 2 ? keywordTags[keywordTagsIndex].substring(0, 2) : keywordTags[keywordTagsIndex]))){
                    resultSentence[token_i] = keywordList[keywordTagsIndex];
                    keywordTagsIndex++;
                } else {
                    resultSentence[token_i] = sampleSentence[token_i][0];
                }
            }

            if(keywordTagsIndex == keywordList.length){
                Detokenizer detokenizer = new Detokenizer();
                return detokenizer.detokenize(resultSentence);
            }
        }

        return "";
    }
}
