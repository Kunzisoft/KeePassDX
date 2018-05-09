package com.patarapolw.randomsentence;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.security.SecureRandom;

public class SentenceMakerSQLite extends SQLiteAssetHelper {
    private static final String DATABASE_NAME = "SentenceMaker.db";
    private static final int DATABASE_VERSION = 1;

    private SecureRandom random = new SecureRandom();

    public SentenceMakerSQLite(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
        SQLiteDatabase db = getReadableDatabase();

        String tableName = "tagged_sents";
        long count = DatabaseUtils.queryNumEntries(db, tableName);
        String[] columns = { "tagged_sentence" };
        String where = String.format("%s=?", "id");
        String[] whereArgs = { Integer.toString(random.nextInt((int) count)) };

        Cursor cursor = db.query(tableName, columns, where, whereArgs, null, null, null);
        String sentence;
        if(cursor.moveToFirst()){
            sentence = cursor.getString(0);
        } else {
            sentence = "";
        }

        cursor.close();

        return sentence;
    }

    private String[] getPOS (String[] wordList) {
        SQLiteDatabase db = getReadableDatabase();

        String[] result = new String[wordList.length];

        String tableName = "dictionary";
        String[] columns = { "pos" };
        String where = String.format("%s=?", "word");

        Cursor cursor;
        for(int i=0; i<wordList.length; i++) {
            String[] whereArgs = {wordList[i]};

            cursor = db.query(tableName, columns, where, whereArgs, null, null, null);

            if(cursor.moveToFirst()) {
                result[i] = cursor.getString(0);
            } else {
                result[i] = "NN";
            }
            cursor.close();
        }

        return result;
    }
}
