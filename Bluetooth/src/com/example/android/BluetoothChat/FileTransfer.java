package com.example.android.BluetoothChat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class FileTransfer extends Activity {
	File sourcefile = null;
	File destfile = null;
	FileInputStream is = null;
	FileOutputStream os = null;
	BufferedInputStream bis = null;
	BufferedOutputStream bos = null;
	byte[] buffer = new byte[5000];
	String tag;
	int fileendflag;
	int bytecount = 2048;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_transfer);
		File sourcedir = new File(Environment.getExternalStorageDirectory().toString() + "/sdcard2/mydir/");
		if (sourcedir.mkdirs()) 
			Log.d(tag, "The directory just created");
		else
			Log.d(tag, "Source directory exists or can not be accessed");
		sourcefile = new File(sourcedir, "source.mp3");
		boolean fileexist = sourcefile.exists();
		if (fileexist) {
			Log.d(tag, "source file exists");
		} else
			Log.d(tag, "source file doesn't exist");
		boolean canread = sourcefile.canRead();
		if (canread) {
			Log.d(tag, "source file can be read");
		} else
			Log.d(tag, "source file can NOT be read");
	}

}
