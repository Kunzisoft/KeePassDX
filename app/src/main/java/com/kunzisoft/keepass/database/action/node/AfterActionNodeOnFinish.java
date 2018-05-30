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
package com.kunzisoft.keepass.database.action.node;

import android.os.Handler;

import com.kunzisoft.keepass.database.PwNode;
import com.kunzisoft.keepass.database.action.OnFinishRunnable;

import javax.annotation.Nullable;

public abstract class AfterActionNodeOnFinish extends OnFinishRunnable {

    public AfterActionNodeOnFinish() {
        super(new Handler());
    }

    public abstract void run(@Nullable PwNode oldNode, @Nullable PwNode newNode);
}
