package com.keepassdroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import com.android.keepass.R;

public class FileNameView extends RelativeLayout {

	public FileNameView(Context context) {
		super(context);
		
		inflate(context);
	}

	public FileNameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		inflate(context);
	}
	

	
	private void inflate(Context context) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.file_selection_filename, this);
	}
}
