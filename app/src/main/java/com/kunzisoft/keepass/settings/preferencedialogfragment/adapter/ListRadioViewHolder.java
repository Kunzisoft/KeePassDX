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
package com.kunzisoft.keepass.settings.preferencedialogfragment.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RadioButton;

import com.kunzisoft.keepass.R;

public class ListRadioViewHolder extends RecyclerView.ViewHolder {

    public RadioButton radioButton;

    public ListRadioViewHolder(View itemView) {
        super(itemView);

        radioButton = itemView.findViewById(R.id.pref_dialog_list_radio);
    }
}
