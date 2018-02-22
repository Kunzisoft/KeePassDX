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
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kunzisoft.keepass.R;

import java.text.DateFormat;
import java.util.Date;

public class EntryContentsView extends LinearLayout {

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

        dateFormat = android.text.format.DateFormat.getDateFormat(context);
        timeFormat = android.text.format.DateFormat.getTimeFormat(context);
		
		inflate(context);
	}
	
	private void inflate(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		assert inflater != null;
		inflater.inflate(R.layout.entry_view_contents, this);

        userNameContainerView = findViewById(R.id.entry_user_name_container);
        userNameView = (TextView) findViewById(R.id.entry_user_name);
        userNameActionView = (ImageView) findViewById(R.id.entry_user_name_action_image);

        passwordContainerView = findViewById(R.id.entry_password_container);
        passwordView = (TextView) findViewById(R.id.entry_password);
        passwordActionView = (ImageView) findViewById(R.id.entry_password_action_image);

        urlContainerView = findViewById(R.id.entry_url_container);
        urlView = (TextView) findViewById(R.id.entry_url);

        commentContainerView = findViewById(R.id.entry_comment_container);
        commentView = (TextView) findViewById(R.id.entry_comment);

        extrasView = (ViewGroup) findViewById(R.id.extra_strings);

        creationDateView = (TextView) findViewById(R.id.entry_created);
        modificationDateView = (TextView) findViewById(R.id.entry_modified);
        lastAccessDateView = (TextView) findViewById(R.id.entry_accessed);
        expiresDateView = (TextView) findViewById(R.id.entry_expires);
	}

	public void assignUserName(String userName) {
        if (userName != null && !userName.isEmpty()) {
            userNameContainerView.setVisibility(VISIBLE);
            userNameView.setText(userName);
        }
    }

    public void assignUserNameCopyListener(OnClickListener onClickListener) {
        userNameActionView.setOnClickListener(onClickListener);
    }

    public void assignPassword(String password) {
        if (password != null && !password.isEmpty()) {
            passwordContainerView.setVisibility(VISIBLE);
            passwordView.setText(password);
        }
    }

    public void assignPasswordCopyListener(OnClickListener onClickListener) {
        passwordActionView.setOnClickListener(onClickListener);
    }

    public void setHiddenPasswordStyle(boolean hiddenStyle) {
        if ( !hiddenStyle ) {
            passwordView.setTransformationMethod(null);
        } else {
            passwordView.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
    }

    public void assignURL(String url) {
        if (url != null && !url.isEmpty()) {
            urlContainerView.setVisibility(VISIBLE);
            urlView.setText(url);
        }
    }


    public void assignComment(String comment) {
        if (comment != null && !comment.isEmpty()) {
            commentContainerView.setVisibility(VISIBLE);
            commentView.setText(comment);
        }
    }

    public void addExtraField(String title, String value, OnClickListener onActionClickListener) {
        View view = new EntryNewField(getContext(), null, title, value, onActionClickListener);
        extrasView.addView(view);
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
