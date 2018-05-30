/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
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
package com.kunzisoft.keepass.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.RelativeLayout;

import com.kunzisoft.keepass.R;

public class AddNodeButtonView extends RelativeLayout {

    private enum State {
        OPEN, CLOSE
    }

    private FloatingActionButton addButtonView;
    private View addEntryView;
    private View addGroupView;

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

	public AddNodeButtonView(Context context) {
		this(context, null);
	}
	
	public AddNodeButtonView(Context context, AttributeSet attrs) {
		super(context, attrs);
		inflate(context);
	}
	
	protected void inflate(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
        inflater.inflate(R.layout.add_node_button, this);

        addEntryEnable = true;
        addGroupEnable = true;

        addButtonView = findViewById(R.id.add_button);
        addEntryView = findViewById(R.id.container_add_entry);
        addGroupView = findViewById(R.id.container_add_group);

        animationDuration = 300L;

        viewButtonMenuAnimation = new AddButtonAnimation(addButtonView);
        viewMenuAnimationAddEntry = new ViewMenuAnimation(addEntryView, 0L, 150L);
        viewMenuAnimationAddGroup = new ViewMenuAnimation(addGroupView, 150L, 0L);

        allowAction = true;
        state = State.CLOSE;

        onAddButtonClickListener = v -> startGlobalAnimation();
        addButtonView.setOnClickListener(onAddButtonClickListener);

        onAddButtonVisibilityChangedListener = new FloatingActionButton.OnVisibilityChangedListener() {
            @Override
            public void onHidden(FloatingActionButton fab) {
                super.onHidden(fab);
                addButtonView.setOnClickListener(null);
                addButtonView.setClickable(false);
            }
            @Override
            public void onShown(FloatingActionButton fab) {
                super.onShown(fab);
                addButtonView.setOnClickListener(onAddButtonClickListener);
            }
        };
	}

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Rect viewButtonRect = new Rect(),
        viewEntryRect = new Rect(),
        viewGroupRect = new Rect();
        addButtonView.getGlobalVisibleRect(viewButtonRect);
        addEntryView.getGlobalVisibleRect(viewEntryRect);
        addGroupView.getGlobalVisibleRect(viewGroupRect);
        if (! (viewButtonRect.contains((int) event.getRawX(), (int) event.getRawY())
                && viewEntryRect.contains((int) event.getRawX(), (int) event.getRawY())
                && viewGroupRect.contains((int) event.getRawX(), (int) event.getRawY()) )) {
            closeButtonIfOpen();
        }
        return super.onTouchEvent(event);
    }

    public void hideButtonOnScrollListener(int dy) {
        if (state.equals(State.CLOSE)) {
            if (dy > 0 && addButtonView.getVisibility() == View.VISIBLE) {
                hideButton();
            } else if (dy < 0 && addButtonView.getVisibility() != View.VISIBLE) {
                showButton();
            }
        }
    }

	public void showButton() {
        addButtonView.show(onAddButtonVisibilityChangedListener);
    }

	public void hideButton() {
        addButtonView.hide(onAddButtonVisibilityChangedListener);
    }

    /**
     * Start the animation to close the button
     */
    public void openButtonIfClose() {
        if(state.equals(State.CLOSE)) {
            startGlobalAnimation();
        }
    }

    /**
     * Start the animation to close the button
     */
    public void closeButtonIfOpen() {
        if(state.equals(State.OPEN)) {
            startGlobalAnimation();
        }
    }

    /**
     * Enable or not the possibility to add an entry by pressing a button
     * @param enable true to enable
     */
    public void enableAddEntry(boolean enable) {
        this.addEntryEnable = enable;
        if (enable && addEntryView != null && addEntryView.getVisibility() != VISIBLE)
            addEntryView.setVisibility(INVISIBLE);
    }

    /**
     * Enable or not the possibility to add a group by pressing a button
     * @param enable true to enable
     */
    public void enableAddGroup(boolean enable) {
	    this.addGroupEnable = enable;
        if (enable && addGroupView != null && addGroupView.getVisibility() != VISIBLE)
            addGroupView.setVisibility(INVISIBLE);
    }

    public void setAddGroupClickListener(OnClickListener onClickListener) {
        if (addGroupEnable)
            addGroupView.setOnClickListener(view -> {
                onClickListener.onClick(view);
                closeButtonIfOpen();
            });
    }

    public void setAddEntryClickListener(OnClickListener onClickListener) {
        if (addEntryEnable) {
            addEntryView.setOnClickListener(view -> {
                onClickListener.onClick(view);
                closeButtonIfOpen();
            });
            addEntryView.setOnClickListener(view -> {
                onClickListener.onClick(view);
                closeButtonIfOpen();
            });
        }
    }

    private void startGlobalAnimation() {
        if (allowAction) {
            viewButtonMenuAnimation.startAnimation();

            if (addEntryEnable) {
                viewMenuAnimationAddEntry.startAnimation();
            }
            if (addGroupEnable) {
                viewMenuAnimationAddGroup.startAnimation();
            }
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
                    translation = view.getY() + view.getHeight()/2 - addButtonView.getY() - addButtonView.getHeight()/2;
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
