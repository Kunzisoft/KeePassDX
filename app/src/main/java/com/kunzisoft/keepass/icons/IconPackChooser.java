package com.kunzisoft.keepass.icons;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.kunzisoft.keepass.BuildConfig;
import com.kunzisoft.keepass.R;

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
                    if (BuildConfig.FULL_VERSION)
                        try {
                            iconPackList.add(new IconPack(context));
                            // Do something
                        } catch (Exception e) {
                            Log.e(TAG, "Icon pack can't be load", e);
                            System.exit(0);
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
