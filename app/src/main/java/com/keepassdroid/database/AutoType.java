package com.keepassdroid.database;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AutoType implements Cloneable, Serializable {
    private static final long OBF_OPT_NONE = 0;

    public boolean enabled = true;
    public long obfuscationOptions = OBF_OPT_NONE;
    public String defaultSequence = "";

    private HashMap<String, String> windowSeqPairs = new HashMap<>();

    @SuppressWarnings("unchecked")
    public AutoType clone() {
        AutoType auto;
        try {
            auto = (AutoType) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        auto.windowSeqPairs = (HashMap<String, String>) windowSeqPairs.clone();
        return auto;
    }

    public void put(String key, String value) {
        windowSeqPairs.put(key, value);
    }

    public Set<Map.Entry<String, String>> entrySet() {
        return windowSeqPairs.entrySet();
    }

}
