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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kunzisoft.keepass.R;
import com.kunzisoft.keepass.database.security.ProtectedString;
import com.kunzisoft.keepass.utils.Util;

import java.text.DateFormat;
import java.util.Date;

public class EntryContentsView extends LinearLayout {

    private boolean fontInVisibility;
    private int colorAccent;

    private View userNameContainerView;
    private TextView userNameView;
    private ImageView userNameActionView;

    private View passwordContainerView;
    private TextView passwordView;
    private ImageView passwordActionView;

    private View urlContainerView;
    private TextView urlView;

    private View commentContainerView;
    private TextView commentView;

    private ViewGroup extrasView;

    private DateFormat dateFormat;
    private DateFormat timeFormat;

    private TextView creationDateView;
    private TextView modificationDateView;
    private TextView lastAccessDateView;
    private TextView expiresDateView;

	public EntryContentsView(Context context) {
		this(context, null);
	}
	
	public EntryContentsView(Context context, AttributeSet attrs) {
		super(context, attrs);

		fontInVisibility = false;

        dateFormat = android.text.format.DateFormat.getDateFormat(context);
        timeFormat = android.text.format.DateFormat.getTimeFormat(context);
		
		inflate(context);

        int[] attrColorAccent = {R.attr.colorAccentCompat};
        TypedArray taColorAccent = context.getTheme().obtainStyledAttributes(attrColorAccent);
        this.colorAccent = taColorAccent.getColor(0, Color.BLACK);
        taColorAccent.recycle();
	}
	
	private void inflate(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
		inflater.inflate(R.layout.entry_view_contents, this);

        userNameContainerView = findViewById(R.id.entry_user_name_container);
        userNameView = findViewById(R.id.entry_user_name);
        userNameActionView = findViewById(R.id.entry_user_name_action_image);

        passwordContainerView = findViewById(R.id.entry_password_container);
        passwordView = findViewById(R.id.entry_password);
        passwordActionView = findViewById(R.id.entry_password_action_image);

        urlContainerView = findViewById(R.id.entry_url_container);
        urlView = findViewById(R.id.entry_url);

        commentContainerView = findViewById(R.id.entry_comment_container);
        commentView = findViewById(R.id.entry_comment);

        extrasView = findViewById(R.id.extra_strings);

        creationDateView = findViewById(R.id.entry_created);
        modificationDateView = findViewById(R.id.entry_modified);
        lastAccessDateView = findViewById(R.id.entry_accessed);
        expiresDateView = findViewById(R.id.entry_expires);
	}

    public void applyFontVisibilityToFields(boolean fontInVisibility) {
        this.fontInVisibility = fontInVisibility;
    }

	public void assignUserName(String userName) {
        if (userName != null && !userName.isEmpty()) {
            userNameContainerView.setVisibility(VISIBLE);
            userNameView.setText(userName);
            if (fontInVisibility)
                Util.applyFontVisibilityTo(getContext(), userNameView);
        } else {
            userNameContainerView.setVisibility(GONE);
        }
    }

    public void assignUserNameCopyListener(OnClickListener onClickListener) {
        userNameActionView.setOnClickListener(onClickListener);
    }

    public boolean isUserNamePresent() {
        return userNameContainerView.getVisibility() == VISIBLE;
    }

    public void assignPassword(String password, boolean allowCopyPassword) {
        if (password != null && !password.isEmpty()) {
            passwordContainerView.setVisibility(VISIBLE);
            passwordView.setText(password);
            if (fontInVisibility)
                Util.applyFontVisibilityTo(getContext(), passwordView);
            if (!allowCopyPassword) {
                passwordActionView.setColorFilter(ContextCompat.getColor(getContext(), R.color.grey_dark));
            } else {
                passwordActionView.setColorFilter(colorAccent);
            }
        } else {
            passwordContainerView.setVisibility(GONE);
        }
    }

    public void assignPasswordCopyListener(OnClickListener onClickListener) {
	    if (onClickListener == null)
	        setClickable(false);
        passwordActionView.setOnClickListener(onClickListener);
    }

    public boolean isPasswordPresent() {
	    return passwordContainerView.getVisibility() == VISIBLE;
    }

    public boolean atLeastOneFieldProtectedPresent() {
        for (int i = 0; i < extrasView.getChildCount(); i++) {
            View childCustomView = extrasView.getChildAt(i);
            if (childCustomView instanceof EntryCustomFieldProtected)
                return true;
        }
        return false;
    }

    public void setHiddenPasswordStyle(boolean hiddenStyle) {
        if ( !hiddenStyle ) {
            passwordView.setTransformationMethod(null);
        } else {
            passwordView.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        // Hidden style for custom fields
        for (int i = 0; i < extrasView.getChildCount(); i++) {
            View childCustomView = extrasView.getChildAt(i);
            if (childCustomView instanceof EntryCustomFieldProtected)
                ((EntryCustomFieldProtected) childCustomView).setHiddenPasswordStyle(hiddenStyle);
        }
    }

    public void assignURL(String url) {
        if (url != null && !url.isEmpty()) {
            urlContainerView.setVisibility(VISIBLE);
            urlView.setText(url);
        } else {
            urlContainerView.setVisibility(GONE);
        }
    }

    public void assignComment(String comment) {
        if (comment != null && !comment.isEmpty()) {
            commentContainerView.setVisibility(VISIBLE);
            commentView.setText(comment);
            if (fontInVisibility)
                Util.applyFontVisibilityTo(getContext(), commentView);
        } else {
            commentContainerView.setVisibility(GONE);
        }
    }

    public void addExtraField(String title, ProtectedString value, boolean showAction, OnClickListener onActionClickListener) {
        EntryCustomField entryCustomField;
	    if (value.isProtected())
	        entryCustomField = new EntryCustomFieldProtected(getContext(), null, title, value, showAction, onActionClickListener);
	    else
	        entryCustomField = new EntryCustomField(getContext(), null, title, value, showAction, onActionClickListener);
        entryCustomField.applyFontVisibility(fontInVisibility);
        extrasView.addView(entryCustomField);
    }

    public void clearExtraFields() {
        extrasView.removeAllViews();
    }

    private String getDateTime(Date date) {
        return dateFormat.format(date) + " " + timeFormat.format(date);
    }

    public void assignCreationDate(Date date) {
        creationDateView.setText(getDateTime(date));
    }

    public void assignModificationDate(Date date) {
        modificationDateView.setText(getDateTime(date));
    }

    public void assignLastAccessDate(Date date) {
        lastAccessDateView.setText(getDateTime(date));
    }

    public void assignExpiresDate(Date date) {
        expiresDateView.setText(getDateTime(date));
    }

    public void assignExpiresDate(String constString) {
        expiresDateView.setText(constString);
    }

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	}

}
