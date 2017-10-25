/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.keepassdroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;

import com.android.keepass.R;

public class GroupAddEntryView extends RelativeLayout {

    protected View addButton;

    protected View addEntry;
    protected boolean addEntryActivated;
    protected View addGroup;
    protected boolean addGroupActivated;


    private boolean animInProgress;
    private AddButtonAnimation viewButtonMenuAnimation;
    private ViewMenuAnimation viewMenuAnimationAddEntry;
    private ViewMenuAnimation viewMenuAnimationAddGroup;

	public GroupAddEntryView(Context context) {
		this(context, null);
	}
	
	public GroupAddEntryView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		inflate(context);
	}
	
	protected void inflate(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.group_add_entry, this);

        addEntryActivated = true;
        addGroupActivated = true;

        addButton = findViewById(R.id.add_button);
        addEntry = findViewById(R.id.add_entry);
        addGroup = findViewById(R.id.add_group);

        viewButtonMenuAnimation = new AddButtonAnimation(addButton);
        viewMenuAnimationAddEntry = new ViewMenuAnimation(addEntry);
        viewMenuAnimationAddGroup = new ViewMenuAnimation(addGroup);

        addButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!animInProgress) {
                    viewButtonMenuAnimation.startAnimation();
                    if (addEntryActivated) {
                        viewMenuAnimationAddEntry.startAnimation();
                    }
                    if (addGroupActivated) {
                        viewMenuAnimationAddGroup.startAnimation();
                    }
                }
            }
        });
	}

	private class AddButtonAnimation implements Animation.AnimationListener {

        private View view;

        private boolean isRotate;

        private Animation rightAnim;
        private Animation leftAnim;

        AddButtonAnimation(View view) {
            this.view = view;

            this.isRotate = false;

            rightAnim = new RotateAnimation(0f, 135f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rightAnim.setDuration(300);
            rightAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            rightAnim.setFillAfter(true);
            leftAnim = new RotateAnimation(135f, 0f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            leftAnim.setDuration(300);
            leftAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            leftAnim.setFillAfter(true);

        }

        @Override
        public void onAnimationStart(Animation animation) {
            animInProgress = true;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            animInProgress = false;
            isRotate = !isRotate;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {}

        void startAnimation() {
            Animation animation;
            if(!isRotate)
                animation = rightAnim;
            else
                animation = leftAnim;
            animation.setAnimationListener(this);
            view.startAnimation(animation);
        }
    }

	private class ViewMenuAnimation implements Animation.AnimationListener {

        private View view;

        private Animation animViewShow;
        private Animation animViewHide;

        ViewMenuAnimation(View view) {
            this.view = view;

            animViewShow = new AlphaAnimation(0.0f, 1.0f);
            animViewShow.setDuration(300);

            animViewHide = new AlphaAnimation(1.0f, 0.0f);
            animViewHide.setDuration(300);
        }

        @Override
        public void onAnimationStart(Animation animation) {}

        @Override
        public void onAnimationEnd(Animation animation) {
            if(view.getVisibility() == VISIBLE)
                view.setVisibility(GONE);
            else
                view.setVisibility(VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {}

        void startAnimation() {
            Animation animation;
            if(view.getVisibility() == VISIBLE)
                animation = animViewHide;
            else
                animation = animViewShow;
            animation.setAnimationListener(this);
            view.startAnimation(animation);
        }
    }
}
