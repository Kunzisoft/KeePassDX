package com.keepassdroid.view;

import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
	
	public void updateExternalStorageWarning() {
		int warning = -1;
		String state = Environment.getExternalStorageState();
		if (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			warning = R.string.warning_read_only;
		} else if (!state.equals(Environment.MEDIA_MOUNTED)) {
			warning = R.string.warning_unmounted;
		}
		
		TextView tv = (TextView) findViewById(R.id.label_warning);
		TextView label = (TextView) findViewById(R.id.label_open_by_filename);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		
		if (warning != -1) {
			tv.setText(warning);
			tv.setVisibility(VISIBLE);
			
			lp.addRule(RelativeLayout.BELOW, R.id.label_warning);
		} else {
			tv.setVisibility(INVISIBLE);
		}
		
		label.setLayoutParams(lp);
	}
}
