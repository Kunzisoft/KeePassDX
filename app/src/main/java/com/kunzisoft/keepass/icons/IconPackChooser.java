/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.icons;

import android.content.Context;
import android.util.Log;

import com.kunzisoft.keepass.BuildConfig;
import com.kunzisoft.keepass.app.App;
import com.kunzisoft.keepass.settings.PreferencesUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to built and select an IconPack dynamically by libraries importation
 *
 * @author J-Jamet
 */
public class IconPackChooser {

    private static final String TAG = IconPackChooser.class.getName();

    private static List<IconPack> iconPackList = new ArrayList<>();
    private static IconPack iconPackSelected = null;

    private static IconPackChooser sIconPackBuilder;

    /**
     * IconPackChooser as singleton
     */
    private IconPackChooser(){
        if (sIconPackBuilder != null){
            throw new RuntimeException("Use build() method to get the single instance of this class.");
        }
    }

    /**
     * Built the icon pack chooser based on imports made in <i>build.gradle</i>
     *
     * <p>Dynamic import can be done for each flavor by prefixing the 'implementation' command with the name of the flavor.< br/>
     * (ex : {@code libreImplementation project(path: ':icon-pack-classic')} <br />
     * Each name of icon pack must be in {@code ICON_PACK_ARRAY} in the build.gradle file</p>
     *
     * @param context Context to construct each pack with the resources
     * @return An unique instance of {@link IconPackChooser}, recall {@link #build(Context)} provide the same instance
     */
    @SuppressWarnings("JavaDoc")
    public static IconPackChooser build(Context context) {
        if (sIconPackBuilder == null) { //if there is no instance available... create new one
            synchronized (IconPackChooser.class) {
                if (sIconPackBuilder == null) {
                    sIconPackBuilder = new IconPackChooser();

                    for (String iconPackString : BuildConfig.ICON_PACK_ARRAY) {
                        addOrCatchNewIconPack(context, iconPackString);
                    }
                    if (iconPackList.isEmpty()) {
                        Log.e(TAG, "Icon packs can't be load, retry with one by default");
                        addDefaultIconPack(context);
                    }
                }
            }
        }

        return sIconPackBuilder;
    }

    /**
     * Construct dynamically the icon pack provide by the default string resource "resource_prefix"
     */
    private static void addDefaultIconPack(Context context) {
        int resourcePrefixId = context.getResources().getIdentifier("resource_prefix", "string", context.getPackageName());
        iconPackList.add(new IconPack(context, resourcePrefixId));
    }

    /**
     * Utility method to add new icon pack or catch exception if not retrieve
     */
    private static void addOrCatchNewIconPack(Context context, String iconPackString) {
        try {
            iconPackList.add(new IconPack(context, context.getResources().getIdentifier(
                    iconPackString + "_resource_prefix",
                    "string",
                    context.getPackageName())));
        } catch (Exception e) {
            Log.w(TAG, "Icon pack "+ iconPackString +" can't be load");
        }
    }

    public static void setSelectedIconPack(String iconPackIdString) {
        for(IconPack iconPack : iconPackList) {
            if (iconPack.getId().equals(iconPackIdString)) {
                App.getDB().getDrawFactory().clearCache();
                iconPackSelected = iconPack;
                break;
            }
        }
    }

    /**
     * Get the current IconPack used
     *
     * @param context Context to build the icon pack if not already build
     * @return IconPack currently in usage
     */
    public static IconPack getSelectedIconPack(Context context) {
        build(context);
        if (iconPackSelected == null)
            setSelectedIconPack(PreferencesUtil.getIconPackSelectedId(context));
        return iconPackSelected;
    }

    /**
     * @return Get the list of IconPack available
     */
    public static List<IconPack> getIconPackList() {
        return iconPackList;
    }
}
