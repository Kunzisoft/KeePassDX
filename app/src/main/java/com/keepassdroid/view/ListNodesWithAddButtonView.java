/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
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
package com.keepassdroid.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;

import com.kunzisoft.keepass.R;

public class ListNodesWithAddButtonView extends RelativeLayout {

    private enum State {
        OPEN, CLOSE
    }

    private FloatingActionButton addButton;
    private View addEntry;
    private View addGroup;

    private boolean addEntryEnable;
    private boolean addGroupEnable;

    private State state;
    private boolean allowAction;
    private OnClickListener onAddButtonClickListener;
    private FloatingActionButton.OnVisibilityChangedListener onAddButtonVisibilityChangedListener;
    private AddButtonAnimation viewButtonMenuAnimation;
    private ViewMenuAnimation viewMenuAnimationAddEntry;
    private ViewMenuAnimation viewMenuAnimationAddGroup;

	public ListNodesWithAddButtonView(Context context) {
		this(context, null);
	}
	
	public ListNodesWithAddButtonView(Context context, AttributeSet attrs) {
		super(context, attrs);
		inflate(context);
	}
	
	protected void inflate(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
        inflater.inflate(R.layout.list_nodes_with_add_button, this);

        addEntryEnable = true;
        addGroupEnable = true;

        addButton = (FloatingActionButton) findViewById(R.id.add_button);
        addEntry = findViewById(R.id.add_entry);
        addGroup = findViewById(R.id.add_group);

        viewButtonMenuAnimation = new AddButtonAnimation(addButton);
        viewMenuAnimationAddEntry = new ViewMenuAnimation(addEntry);
        viewMenuAnimationAddGroup = new ViewMenuAnimation(addGroup);

        allowAction = true;
        state = State.CLOSE;

        onAddButtonClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allowAction && state.equals(State.CLOSE)) {
                    startGlobalAnimation();
                }
            }
        };
        addButton.setOnClickListener(onAddButtonClickListener);

        onAddButtonVisibilityChangedListener = new FloatingActionButton.OnVisibilityChangedListener() {
            @Override
            public void onHidden(FloatingActionButton fab) {
                super.onHidden(fab);
                addButton.setOnClickListener(null);
                addButton.setClickable(false);
            }
            @Override
            public void onShown(FloatingActionButton fab) {
                super.onShown(fab);
                addButton.setOnClickListener(onAddButtonClickListener);
            }
        };

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.nodes_list);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && addButton.getVisibility() == View.VISIBLE) {
                    addButton.hide(onAddButtonVisibilityChangedListener);
                } else if (dy < 0 && addButton.getVisibility() != View.VISIBLE) {
                    addButton.show(onAddButtonVisibilityChangedListener);
                }
            }
        });
	}

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Rect viewRectG = new Rect();
        getGlobalVisibleRect(viewRectG);
        if (viewRectG.contains((int) ev.getRawX(), (int) ev.getRawY())) {
            if(allowAction && state.equals(State.OPEN)) {
                startGlobalAnimation();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Enable or not the possibility to add an entry by pressing a button
     * @param enable true to enable
     */
    public void enableAddEntry(boolean enable) {
        this.addEntryEnable = enable;
    }

    /**
     * Enable or not the possibility to add a group by pressing a button
     * @param enable true to enable
     */
    public void enableAddGroup(boolean enable) {
	    this.addGroupEnable = enable;
    }

    private void startGlobalAnimation() {
        viewButtonMenuAnimation.startAnimation();
        if (addEntryEnable) {
            viewMenuAnimationAddEntry.startAnimation();
        }
        if (addGroupEnable) {
            viewMenuAnimationAddGroup.startAnimation();
        }
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
            allowAction = false;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            allowAction = true;
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
            if(view.getVisibility() == VISIBLE) {
                view.setVisibility(INVISIBLE);
                state = State.CLOSE;
            } else {
                view.setVisibility(VISIBLE);
                state = State.OPEN;
            }
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
