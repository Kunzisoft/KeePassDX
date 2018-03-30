package com.keepassdroid.view;

import android.content.Context;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;

import com.keepassdroid.database.security.ProtectedString;

public class EntryCustomFieldProtected extends EntryCustomField{

    public EntryCustomFieldProtected(Context context) {
        super(context);
    }

    public EntryCustomFieldProtected(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EntryCustomFieldProtected(Context context, AttributeSet attrs, String title, ProtectedString value) {
        super(context, attrs, title, value);
    }

    public EntryCustomFieldProtected(Context context, AttributeSet attrs, String label, ProtectedString value, OnClickListener onClickActionListener) {
        super(context, attrs, label, value, onClickActionListener);
    }

    public void setValue(ProtectedString value) {
        if (value != null) {
            valueView.setText(value.toString());
            setHiddenPasswordStyle(value.isProtected());
        }
    }

    public void setHiddenPasswordStyle(boolean hiddenStyle) {
        if ( !hiddenStyle ) {
            valueView.setTransformationMethod(null);
        } else {
            valueView.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
    }
}
