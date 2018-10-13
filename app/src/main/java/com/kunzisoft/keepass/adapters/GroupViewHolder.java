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
package com.kunzisoft.keepass.adapters;

import android.view.View;

import com.kunzisoft.keepass.R;

class GroupViewHolder extends BasicViewHolder {

    GroupViewHolder(View itemView) {
        super(itemView);
        container = itemView.findViewById(R.id.group_container);
        icon = itemView.findViewById(R.id.group_icon);
        text = itemView.findViewById(R.id.group_text);
        subText = itemView.findViewById(R.id.group_subtext);
    }
}
