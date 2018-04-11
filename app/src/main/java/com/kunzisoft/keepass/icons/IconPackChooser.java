package com.kunzisoft.keepass.icons;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to select an IconPack
 */
public class IconPackChooser {

    private static final String TAG = IconPackChooser.class.getName();

    private static List<IconPack> iconPackList = new ArrayList<>();

    private static volatile IconPackChooser sIconPackBuilder;

    private IconPackChooser(){
        if (sIconPackBuilder != null){
            throw new RuntimeException("Use build() method to get the single instance of this class.");
        }
    }

    public static IconPackChooser build(Context context) {
        if (sIconPackBuilder == null) { //if there is no instance available... create new one
            synchronized (IconPackChooser.class) {
                if (sIconPackBuilder == null) {
                    sIconPackBuilder = new IconPackChooser();
                    try {
                        iconPackList.add(new IconPack(context));
                        // Do something
                    } catch (Exception e) {
                        Log.e(TAG, "Icon pack can't be load", e);
                    }
                }
            }
        }

        return sIconPackBuilder;
    }

    public static IconPack getDefaultIconPack(Context context) {
        build(context);
        return iconPackList.get(0);
        /*
        try {
            return iconPackList.get(0);
        } catch (IndexOutOfBoundsException e) {
            //throw new IconPackUnknownException(); TODO exception
        }
        */
    }
}
