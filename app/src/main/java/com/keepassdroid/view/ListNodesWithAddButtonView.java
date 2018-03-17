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
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
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
    private long animationDuration;

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

        animationDuration = 300L;

        viewButtonMenuAnimation = new AddButtonAnimation(addButton);
        viewMenuAnimationAddEntry = new ViewMenuAnimation(addEntry, 0L, 150L);
        viewMenuAnimationAddGroup = new ViewMenuAnimation(addGroup, 150L, 0L);

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

        // Hide when scroll
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.nodes_list);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (state.equals(State.CLOSE)) {
                    if (dy > 0 && addButton.getVisibility() == View.VISIBLE) {
                        hideButton();
                    } else if (dy < 0 && addButton.getVisibility() != View.VISIBLE) {
                        showButton();
                    }
                }
            }
        });
	}

	public void showButton() {
        addButton.show(onAddButtonVisibilityChangedListener);
    }

	public void hideButton() {
        addButton.hide(onAddButtonVisibilityChangedListener);
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
        if (enable && addEntry != null && addEntry.getVisibility() != VISIBLE)
            addEntry.setVisibility(INVISIBLE);
    }

    /**
     * Enable or not the possibility to add a group by pressing a button
     * @param enable true to enable
     */
    public void enableAddGroup(boolean enable) {
	    this.addGroupEnable = enable;
        if (enable && addGroup != null && addGroup.getVisibility() != VISIBLE)
            addGroup.setVisibility(INVISIBLE);
    }

    public void setAddGroupClickListener(OnClickListener onClickListener) {
        if (addGroupEnable)
            addGroup.setOnClickListener(onClickListener);
    }

    public void setAddEntryClickListener(OnClickListener onClickListener) {
        if (addEntryEnable)
            addEntry.setOnClickListener(onClickListener);
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

	private class AddButtonAnimation implements ViewPropertyAnimatorListener {

        private View view;
        private boolean isRotate;

        private Interpolator interpolator;

        AddButtonAnimation(View view) {
            this.view = view;
            this.isRotate = false;
            interpolator = new AccelerateDecelerateInterpolator();
        }

        @Override
        public void onAnimationStart(View view) {
            allowAction = false;
        }

        @Override
        public void onAnimationEnd(View view) {
            allowAction = true;
            isRotate = !isRotate;
            if (isRotate)
                state = State.OPEN;
            else
                state = State.CLOSE;
        }

        @Override
        public void onAnimationCancel(View view) {}

        void startAnimation() {
            if(!isRotate) {
                ViewCompat.animate(view)
                        .rotation(135.0F)
                        .withLayer()
                        .setDuration(animationDuration)
                        .setInterpolator(interpolator)
                        .setListener(this)
                        .start();
            } else {
                ViewCompat.animate(view)
                        .rotation(0.0F)
                        .withLayer()
                        .setDuration(animationDuration)
                        .setInterpolator(interpolator)
                        .setListener(this)
                        .start();
            }
        }
    }

	private class ViewMenuAnimation implements ViewPropertyAnimatorListener {

        private View view;
        private Interpolator interpolator;
        private float translation;
        private boolean wasInvisible;
        private long delayIn;
        private long delayOut;

        ViewMenuAnimation(View view, long delayIn, long delayOut) {
            this.view = view;
            this.interpolator = new FastOutSlowInInterpolator();
            this.wasInvisible = true;
            this.translation = 0;
            this.delayIn = delayIn;
            this.delayOut = delayOut;
        }

        ViewMenuAnimation(View view) {
            this(view, 0, 0);
        }

        @Override
        public void onAnimationStart(View view) {}

        @Override
        public void onAnimationEnd(View view) {
            if(wasInvisible) {
                view.setVisibility(VISIBLE);
            } else {
                view.setVisibility(INVISIBLE);
            }
        }

        @Override
        public void onAnimationCancel(View view) {}

        void startAnimation() {
            if(view.getVisibility() == VISIBLE) {
                // In
                wasInvisible = false;
                ViewCompat.animate(view)
                        .translationY(-translation)
                        .translationX(view.getWidth()/3)
                        .alpha(0.0F)
                        .scaleX(0.33F)
                        .setDuration(animationDuration-delayIn)
                        .setInterpolator(interpolator)
                        .setListener(this)
                        .setStartDelay(delayIn)
                        .start();
            } else {
                // The first time
                if (translation == 0) {
                    translation = view.getY() + view.getHeight()/2 - addButton.getY() - addButton.getHeight()/2;
                    view.setTranslationY(-translation);
                    view.setTranslationX(view.getWidth()/3);
                    view.setAlpha(0.0F);
                    view.setScaleX(0.33F);
                }

                // Out
                view.setVisibility(VISIBLE);
                wasInvisible = true;
                ViewCompat.animate(view)
                        .translationY(1)
                        .translationX(1)
                        .alpha(1.0F)
                        .scaleX(1)
                        .setDuration(animationDuration-delayOut)
                        .setInterpolator(interpolator)
                        .setListener(this)
                        .setStartDelay(delayOut)
                        .start();
            }
        }
    }
}
