package com.android.keepass.fileselect;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class FileListAdapter extends BaseAdapter {
	private final Context mCtx;
	
	FileListAdapter(Context ctx) {
		mCtx = ctx;
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return 20;
	}

	@Override
	public Object getItem(int arg0) {
		return arg0;
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}

	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		TextView tv = new TextView(mCtx);
		tv.setText("This is the " + arg0 + " list item.");
		return tv;
	}

}
