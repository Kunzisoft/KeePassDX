package com.keepassdroid.groupentity;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

public class GroupViewHolder extends BasicViewHolder {

    GroupViewHolder(View itemView) {
        super(itemView);
        container = itemView.findViewById(R.id.group_container);
        icon = (ImageView) itemView.findViewById(R.id.group_icon);
        text = (TextView) itemView.findViewById(R.id.group_text);
    }
}
