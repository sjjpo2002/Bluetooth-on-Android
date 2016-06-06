
package com.example.android.BluetoothChat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current file transfer session.
 */
public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;   

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private TextView mTitle;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    //
    /////////////////////////////////////File Sharing ////////////////////////
    private File sourcedir;
    private File sourcefile;
    Button send_file_button;
    Boolean canreadsourcefile;
    Boolean canwritetodestinationfile;    
    FileInputStream file_is;
    BufferedInputStream buffered_file_is;    
    long file_size;    
    int offset = 0;
    int numread;
    boolean first_time_recieve = true;
    boolean file_recieved = false;
    //////////////////////////////////////////////////////// End of File Sharing ////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
    	try {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    	} catch (Exception e) {
    		Log.d(TAG, e.toString());
    	}
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);
        mOutEditText.setVisibility(View.GONE);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });
        mSendButton.setVisibility(View.GONE);
     // Initialize the BluetoothChatService to perform Bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
        //
        ///////////////////////////////////////////////////////////////////// File Sharing ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        //
        sourcedir = new File(Environment.getExternalStorageDirectory().toString() + "/sdcard2/mydir/");  // source dir on external memory
        Log.d(TAG, "source dir: " + sourcedir.toString());
        ////////////////// 
        ///////////////////////////////    Source and destination files are selected    //////////////////////////////
        sourcefile = new File(sourcedir, "source.mp3");
        file_size = sourcefile.length();    // Size of the source file in bytes        
        canreadsourcefile = sourcefile.canRead();        
        Log.d(TAG, "can read " + sourcefile.getName() + " ? :" + canreadsourcefile);        
        mConversationArrayAdapter.add(sourcefile.getName() + " ( " + file_size + " bytes )"+ " is selected to be sent");
        mConversationArrayAdapter.add("Can Read From " + "Source File: " + sourcefile.getName() +" ?:  " +  canreadsourcefile.toString() );
        // /////////////////////////////////////////
        ////////////////////////////////////////////////// File size will be checked ///////////////////////////////        
        if (file_size>Integer.MAX_VALUE) {
        	Log.d(TAG, "source file is too big to read");
        	mConversationArrayAdapter.add("The file is too big" );
        }
        ////
        //////////////////////////////////////////// Define the buffer to hold the bytes read from the file //////////////////////        
        try {
			file_is = new FileInputStream(sourcefile);
		} catch (FileNotFoundException e) {
			Log.d(TAG,e.toString());
			e.printStackTrace();
		}
		buffered_file_is = new BufferedInputStream(file_is);        
        send_file_button = (Button) findViewById(R.id.send_button);
        send_file_button.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
		            Log.d(TAG, "Not connected ====>  Send is not working");
		            mConversationArrayAdapter.add("Not connected! Don't try !!!! ");
				} else {
					byte[] buffer = new byte[512];
					/*ProgressBar file_progress_bar = (ProgressBar) findViewById(R.id.progressBar);
					file_progress_bar.setVisibility(View.VISIBLE);
					file_progress_bar.setMax((int) file_size);*/
					int completed_percentage = 0;
					try {
						while ((numread = buffered_file_is.read(buffer)) != -1 ) {
							Log.d(TAG, "read from source file:   " + completed_percentage + " out of " + file_size);  // will show the number of bytes read in one iteration
							mChatService.write(buffer);							
							// file_progress_bar.incrementProgressBy(numread);
							completed_percentage = completed_percentage + numread;							
							// offset += numread;
							// buffered file input stream takes care of increasing the offset everytime the bytes is read from the file							
						}						
					} catch (IOException e) {
						Log.d(TAG, e.toString());
						e.printStackTrace();
					} finally {							
						Log.d(TAG, "finally  ....");						
						Log.d(TAG, "read from source file:   " + completed_percentage + " out of " + file_size);
						mChatService.write(buffer);
						mConversationArrayAdapter.add("source file has been sent succesfully" );
						mConversationArrayAdapter.add("total bytes read from the file: " + completed_percentage );
						Log.d(TAG, "buffer bytes size: " + file_size);
						try {
							file_is.close();
							buffered_file_is.close();
						} catch (IOException e) {
							Log.d(TAG, e.toString());
							e.printStackTrace();
						}
					}  // End of finally
				
				}
			}
		});        
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            /* case MESSAGE_WRITE:
            	
            	break;
                /*byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage); */
                
            case MESSAGE_READ:                
            	if (first_time_recieve) {
                	Log.d(TAG, "Recieving a file ....");
                	mConversationArrayAdapter.add("Recieving a file .... ");
                	first_time_recieve = false;
                    }
                /*/ construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                */
                //////////////////////////////////////// Recieve File //////////////////////////////////////////////
                // Make the File output sockets                
                
                file_size = (int) file_size - msg.arg1;
                if ((file_size <2000) && (!file_recieved)) {
                	mConversationArrayAdapter.add("File recived successfully");
                	file_recieved = true;
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        case R.id.file:
        	startActivity(new Intent("com.example.android.BluetoothChat.FileTransfer"));
            return true;
        }
        return false;
    }

}