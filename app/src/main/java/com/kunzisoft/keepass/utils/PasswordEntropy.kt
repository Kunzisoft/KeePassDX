/*
 * Copyright 2022 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.utils

import android.graphics.Color
import com.kunzisoft.keepass.app.database.IOActionTask
import me.gosimple.nbvcxz.Nbvcxz
import me.gosimple.nbvcxz.resources.Configuration
import me.gosimple.nbvcxz.resources.ConfigurationBuilder
import java.util.*
import kotlin.math.ln
import kotlin.math.min

class PasswordEntropy(actionOnInitFinished: (() -> Unit)? = null) {

    private var mPasswordEntropyCalculator: Nbvcxz? = null

    init {
        IOActionTask({
            // Create the password generator object
            val configuration: Configuration = ConfigurationBuilder()
                .setLocale(Locale.getDefault())
                .setMinimumEntropy(80.0)
                .createConfiguration()
            mPasswordEntropyCalculator = Nbvcxz(configuration)
        }, {
            actionOnInitFinished?.invoke()
        }).execute()
    }

    enum class Strength(val color: Int) {
        RISKY(Color.rgb(224, 56, 56)),
        VERY_GUESSABLE(Color.rgb(196, 63, 49)),
        SOMEWHAT_GUESSABLE(Color.rgb(219, 152, 55)),
        SAFELY_UNGUESSABLE(Color.rgb(118, 168, 24)),
        VERY_UNGUESSABLE(Color.rgb(37, 152, 41))
    }

    data class EntropyStrength(val strength: Strength,
                               val estimationPercent: Int) {
        override fun toString(): String {
            return "EntropyStrength(strength=$strength, estimationPercent=$estimationPercent)"
        }
    }

    fun getEntropyStrength(passwordString: String,
                           entropyStrengthResult: (EntropyStrength) -> Unit) {
        val basicScore = mPasswordEntropyCalculator?.estimate(passwordString)?.basicScore ?: 0
        val percentScore = min(((ln((basicScore+2.0)) *passwordString.length)*100/60).toInt(), 100)
        val strength =
            if (basicScore == 0 || percentScore < 10) {
                Strength.RISKY
            } else if (basicScore == 1 || percentScore < 20) {
                Strength.VERY_GUESSABLE
            } else if (basicScore == 2 || percentScore < 33) {
                Strength.SOMEWHAT_GUESSABLE
            } else if (basicScore == 3 || percentScore < 52) {
                Strength.SAFELY_UNGUESSABLE
            } else if (basicScore == 4) {
                Strength.VERY_UNGUESSABLE
            } else {
                Strength.RISKY
            }
        IOActionTask(
            {
                EntropyStrength(strength, percentScore)
            },
            {
                it?.let { entropyStrength -> entropyStrengthResult.invoke(entropyStrength) }
            }
        ).execute()
    }
}