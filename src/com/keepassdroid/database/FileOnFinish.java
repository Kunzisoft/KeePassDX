package com.keepassdroid.database;

public class FileOnFinish extends OnFinish {
	private String mFilename = "";
	protected FileOnFinish mOnFinish;
	
	public FileOnFinish(FileOnFinish finish) {
		super(finish);
		
		mOnFinish = finish;
	}
	
	public void setFilename(String filename) {
		mFilename = filename;
	}
	
	public String getFilename() {
		return mFilename;
	}

}
