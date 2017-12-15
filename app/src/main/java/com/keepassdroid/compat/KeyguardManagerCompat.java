/*
 * Copyright 2017 Brian Pellin.
 *
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.compat;

import android.app.KeyguardManager;

import java.lang.reflect.Method;

public class KeyguardManagerCompat {
    private static Method isKeyguardSecure;

    private static boolean available;

    static {
        try {
            isKeyguardSecure = KeyguardManager.class.getMethod("isKeyguardSecure", (Class[]) null);

            available = true;
        } catch (Exception e) {
            available = false;
        }
    }

    public static boolean isKeyguardSecure(KeyguardManager inst) {
        if (!available) {
            return false;
        }

        try {
            return (boolean) isKeyguardSecure.invoke(inst, null);

        } catch (Exception e) {
            return false;
        }
    }
}
